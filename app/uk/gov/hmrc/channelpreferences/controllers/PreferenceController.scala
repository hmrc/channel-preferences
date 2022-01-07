/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.channelpreferences.controllers

import play.api.Logger
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.{ Action, AnyContent, ControllerComponents, Request, Result }
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{ AffinityGroup, AuthConnector, AuthorisationException, AuthorisedFunctions, ConfidenceLevel }
import uk.gov.hmrc.channelpreferences.audit.Auditing
import uk.gov.hmrc.channelpreferences.utils.CustomHeaders
import uk.gov.hmrc.channelpreferences.model.cds.Channel
import uk.gov.hmrc.channelpreferences.model.eis.StatusUpdate
import uk.gov.hmrc.channelpreferences.model.entityresolver.{ AgentEnrolment, Enrolment, EnrolmentResponseBody }
import uk.gov.hmrc.channelpreferences.model.preferences.Event
import uk.gov.hmrc.channelpreferences.services.cds.CdsPreference
import uk.gov.hmrc.channelpreferences.services.eis.EISContactPreference
import uk.gov.hmrc.channelpreferences.services.entityresolver.EntityResolver
import uk.gov.hmrc.channelpreferences.services.preferences.ProcessEmail
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.util.Try

@Singleton
class PreferenceController @Inject()(
  cdsPreference: CdsPreference,
  entityResolver: EntityResolver,
  eisContactPreference: EISContactPreference,
  processEmail: ProcessEmail,
  override val authConnector: AuthConnector,
  override val auditConnector: AuditConnector,
  override val controllerComponents: ControllerComponents)(implicit ec: ExecutionContext)
    extends BackendController(controllerComponents) with AuthorisedFunctions with Auditing {

  private val logger: Logger = Logger(this.getClass)

  private val ITSA_REGIME = "ITSA"

  implicit val emailWrites = uk.gov.hmrc.channelpreferences.model.cds.EmailVerification.emailVerificationFormat
  def preference(channel: Channel, enrolmentKey: String, taxIdName: String, taxIdValue: String): Action[AnyContent] =
    Action.async { implicit request =>
      cdsPreference.getPreference(channel, enrolmentKey, taxIdName, taxIdValue).map {
        case Right(e)              => Ok(Json.toJson(e))
        case Left(NOT_FOUND)       => NotFound
        case Left(NOT_IMPLEMENTED) => NotImplemented
        case Left(_)               => BadGateway
      }
    }

  def confirm(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[Enrolment] { enrolment =>
      for {
        resp <- entityResolver.confirm(enrolment.entityId, enrolment.itsaId)
        _ <- auditConfirm(
              resp.status,
              enrolment,
              authorised((AffinityGroup.Organisation or AffinityGroup.Individual) and ConfidenceLevel.L200)
                .retrieve(Retrievals.saUtr and Retrievals.nino)
            )
      } yield Status(resp.status)(resp.json)
    }
  }

  def enrolment(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    entityResolver.enrolment(request.body).flatMap { resp =>
      val resultBody = Try(Json.parse(resp.body)).toOption.flatMap(_.asOpt[EnrolmentResponseBody])
      (resp.status, resultBody) match {
        case (OK, Some(result)) =>
          updateEtmp(result.isDigitalStatus)
        case _ =>
          Future.successful(Status(resp.status)(resp.body))
      }
    }
  }

  private def updateEtmp(status: Boolean)(implicit request: Request[JsValue]): Future[Result] =
    withJsonBody[AgentEnrolment] { agentEnrolment =>
      val correlationId = request.headers
        .get(CustomHeaders.RequestId)
      val statusUpdate = StatusUpdate(agentEnrolment.itsaId, status)
      statusUpdate.substituteMTDITIDValue match {
        case Right(itsaETMPUpdate) =>
          eisContactPreference.updateContactPreference(ITSA_REGIME, itsaETMPUpdate, correlationId).map { response =>
            Status(response.status)(response.json)
          }
        case Left(_) =>
          // should not happen as the entity-resolver perform the same check
          Future.successful(Status(UNAUTHORIZED)("Invalid itsa enrolment"))
      }
    }

  def processBounce(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[Event] { event =>
      processEmail
        .process(event)
        .map {
          case Right(content) => Ok(content)
          case Left(error) => {
            logger.error(s"Failed to update email bounce $error")
            NotModified
          }
        }
        .recover {
          case error =>
            logger.error(s"Failed to update email bounce ${error.getMessage}")
            InternalServerError
        }
    }
  }

  def update(key: String): Action[JsValue] =
    Action.async(parse.json) { implicit request =>
      key.toUpperCase() match {
        case ITSA_REGIME =>
          withJsonBody[StatusUpdate] { statusUpdate =>
            statusUpdate.getItsaETMPUpdate match {
              case Right(itsaETMPUpdate) =>
                val correlationId = request.headers
                  .get(CustomHeaders.RequestId)
                eisContactPreference.updateContactPreference(ITSA_REGIME, itsaETMPUpdate, correlationId).map {
                  response =>
                    Status(response.status)(response.json)
                }
              case Left(err) =>
                Future.successful(BadRequest(err))
            }

          }
        case _ => Future.successful(BadRequest(s"The key $key is not supported"))
      }
    }

  def auditConfirm(status: Int, e: Enrolment, auth: AuthorisedFunctionWithResult[Option[String] ~ Option[String]])(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext): Future[Unit] = {
    def getIds: Future[Map[String, String]] =
      auth(
        r => Future.successful(r.a.map(("SAUTR", _)).toMap ++ r.b.map(("NINO", _)).toMap)
      ).recover {
        case e: AuthorisationException =>
          logger.error("Authorisation error", e)
          Map.empty[String, String]
      }

    getIds.map { ids =>
      sendAuditEvent(
        if (status == OK) "ItsaIdConfirmed" else "ItsaIdConfirmError",
        Map("regime" -> ITSA_REGIME, "itsaId" -> e.itsaId, "entityId" -> e.entityId) ++ ids)
    }
  }

}
