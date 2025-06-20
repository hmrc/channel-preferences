/*
 * Copyright 2024 HM Revenue & Customs
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

import org.apache.pekko.util.ByteString
import play.api.Logger
import play.api.http.{ ContentTypes, HttpEntity }
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.{ Action, AnyContent, ControllerComponents, Request, ResponseHeader, Result, Results }
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{ AffinityGroup, AuthConnector, AuthorisationException, AuthorisedFunctions, ConfidenceLevel }
import uk.gov.hmrc.channelpreferences.audit.Auditing
import uk.gov.hmrc.channelpreferences.utils.CustomHeaders
import uk.gov.hmrc.channelpreferences.model.cds.Channel
import uk.gov.hmrc.channelpreferences.model.eis.{ ItsaETMPUpdate, StatusUpdate }
import uk.gov.hmrc.channelpreferences.model.entityresolver.{ AgentEnrolment, Enrolment, EnrolmentResponseBody, ItsaIdUpdateResponse }
import uk.gov.hmrc.channelpreferences.model.preferences.{ EnrolmentKey, Event, IdentifierKey, IdentifierValue, PreferenceError }
import uk.gov.hmrc.channelpreferences.services.eis.EISContactPreference
import uk.gov.hmrc.channelpreferences.services.entityresolver.EntityResolver
import uk.gov.hmrc.channelpreferences.services.preferences.{ PreferenceService, ProcessEmail }
import uk.gov.hmrc.channelpreferences.utils.EntityIdCrypto
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

@Singleton
class PreferenceController @Inject() (
  preferenceService: PreferenceService,
  entityResolver: EntityResolver,
  eisContactPreference: EISContactPreference,
  processEmail: ProcessEmail,
  override val authConnector: AuthConnector,
  override val auditConnector: AuditConnector,
  override val controllerComponents: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(controllerComponents) with AuthorisedFunctions with Auditing with EntityIdCrypto {

  private val logger: Logger = Logger(this.getClass)

  def preference(
    enrolmentKey: EnrolmentKey,
    identifierKey: IdentifierKey,
    identifierValue: IdentifierValue,
    channel: Channel
  ): Action[AnyContent] =
    Action.async { implicit request =>
      preferenceService
        .getChannelPreference(enrolmentKey, identifierKey, identifierValue, channel)
        .map(toResult)
    }

  private def toResult(resolution: Either[PreferenceError, JsValue]): Result = resolution.fold(
    preferenceError =>
      Result(
        header = ResponseHeader(preferenceError.statusCode.intValue()),
        body = HttpEntity.Strict(ByteString.apply(preferenceError.message), Some(ContentTypes.TEXT))
      ),
    json => Ok(json)
  )

  def confirm(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[Enrolment] { enrolment =>
      val entityId = decryptString(enrolment.entityId) match {
        case Left(value) => value
        case Right(e) =>
          logger warn s"Unable to decrypt ${enrolment.entityId}, reason: ${e.message}"
          enrolment.entityId
      }
      for {
        resp <- entityResolver.confirm(entityId, enrolment.itsaId)
        _ <- auditConfirm(
               resp.status,
               resp.body,
               enrolment,
               authorised((AffinityGroup.Organisation or AffinityGroup.Individual) and ConfidenceLevel.L200)
                 .retrieve(Retrievals.saUtr and Retrievals.nino)
             )
      } yield Status(resp.status)(resp.json)
    }
  }

  def process(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    logger warn s"Request received with headers ${request.headers.headers}; Body ${request.body} "
    entityResolver
      .processItsa(request.body)
      .flatMap { resp =>
        val resultBody = Try(Json.parse(resp.body)).toOption.flatMap(_.asOpt[ItsaIdUpdateResponse])
        (resp.status, resultBody) match {
          case (OK, Some(result)) if result.preference.isDefined =>
            updateEtmpWithContactPreference(result)
          case _ =>
            logger.warn(s"ItsaId update is failed with status ${resp.status} $resultBody")
            Future.successful(Status(resp.status)(resp.body))
        }
      }
      .recoverWith {
        case err: AuthorisationException =>
          Future.successful(Unauthorized(Json.obj("error" -> err.getMessage)))
        case e =>
          logger.error(s"Failed to update ItsatID ${e.getMessage}")
          Future.successful(InternalServerError(Json.obj("error" -> e.getMessage)))
      }
  }

  private def updateEtmpWithContactPreference(
    result: ItsaIdUpdateResponse
  )(implicit request: Request[JsValue]): Future[Result] = {
    val itsaId = (request.body \ "mtditsaid").as[String]
    val correlationId = request.headers.get(CustomHeaders.RequestId)

    logger.warn(s"Update ETMP after successful update of itsaId($itsaId)")

    eisContactPreference
      .updateContactPreference(ITSA_REGIME, ItsaETMPUpdate("MTDBSA", itsaId, result.isDigitalStatus), correlationId)
      .map { response =>
        if (response.status == OK)
          Ok(Json.obj("response" -> "MTD ITSA ID value updated successfully"))
        else
          Status(response.status)(response.json)
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

  private val ITSA_REGIME = "ITSA"

  private def updateEtmp(status: Boolean)(implicit request: Request[JsValue]): Future[Result] =
    withJsonBody[AgentEnrolment] { agentEnrolment =>
      val correlationId = request.headers
        .get(CustomHeaders.RequestId)
      val statusUpdate = StatusUpdate(agentEnrolment.itsaId, status)
      statusUpdate.substituteMTDITIDValue match {
        case Right(itsaETMPUpdate) =>
          eisContactPreference.updateContactPreference(ITSA_REGIME, itsaETMPUpdate, correlationId).map { response =>
            if (response.status == OK) {
              Status(OK)(Json.obj("reason" -> "ITSA ID successfully added"))
            } else {
              Status(response.status)(response.json)
            }
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
          case Left(error) =>
            logger.error(s"Failed to update email bounce for eventId ${event.eventId}: $error")
            NotModified
        }
        .recover { case error =>
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

  def auditConfirm(
    responseStatus: Int,
    responseBody: String,
    e: Enrolment,
    auth: AuthorisedFunctionWithResult[Option[String] ~ Option[String]]
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    def getIds: Future[Map[String, String]] =
      auth(r => Future.successful(r.a.map(("SAUTR", _)).toMap ++ r.b.map(("NINO", _)).toMap)).recover {
        case e: AuthorisationException =>
          logger.error("Authorisation error", e)
          Map.empty[String, String]
      }

    val details = Map("regime" -> ITSA_REGIME, "itsaId" -> e.itsaId, "entityId" -> e.entityId)
    val (auditType, auditDetails) = responseStatus match {
      case OK => ("ItsaIdConfirmed", details)
      case _ =>
        (
          "ItsaIdConfirmError",
          details ++ Map("errorCode" -> responseStatus.toString, "errorDescription" -> responseBody)
        )
    }

    getIds.map { ids =>
      sendAuditEvent(auditType, auditDetails ++ ids)
    }
  }

}
