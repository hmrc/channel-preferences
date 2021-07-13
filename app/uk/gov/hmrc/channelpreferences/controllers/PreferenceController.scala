/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.{ Action, AnyContent, ControllerComponents, Result }
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{ AuthConnector, AuthorisationException, AuthorisedFunctions }
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.channelpreferences.connectors.EntityResolverConnector
import uk.gov.hmrc.channelpreferences.hub.cds.model.Channel
import uk.gov.hmrc.channelpreferences.hub.cds.services.CdsPreference
import uk.gov.hmrc.channelpreferences.model._
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
@Singleton
class PreferenceController @Inject()(
  cdsPreference: CdsPreference,
  val authConnector: AuthConnector,
  val entityResolverConnector: EntityResolverConnector,
  override val controllerComponents: ControllerComponents)(implicit ec: ExecutionContext)
    extends BackendController(controllerComponents) with AuthorisedFunctions {

  implicit val emailWrites = uk.gov.hmrc.channelpreferences.hub.cds.model.EmailVerification.emailVerificationFormat
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
      authorised( /* TODO Do we need to pass any affinity group here? */ )
        .retrieve(Retrievals.saUtr) {
          case authToken_saUtr =>
            doConfirmItsa(enrolment.entityId, enrolment.itsaId, authToken_saUtr)
        }
        .recoverWith {
          case e: AuthorisationException =>
            reply(UNAUTHORIZED, e.getMessage)
        }
    }
  }

  def enrolment(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[AgentEnrolment] { enrolment =>
      authorised(Agent)
        .retrieve(Retrievals.affinityGroup) { _ =>
          Future.successful(Ok(s"Enrolment Successful for arn: '${enrolment.arn}', itsaId :'${enrolment.itsaId}'" +
            s", nino: '${enrolment.nino}', sautr: '${enrolment.sautr}'"))
        }
        .recoverWith {
          case e: AuthorisationException => Future.successful(Unauthorized(e.getMessage))
        }
    }
  }

  def processBounce(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[Event] { event =>
      Future.successful(
        Created(s"Bounce sucessfully processed: '${event.eventId}', subject :'${event.subject}'" +
          s", groupId: '${event.groupId}', timeStamp: '${event.timeStamp}', event: '${event.event}'"))
    }
  }

  // ----------------------

  private def doConfirmItsa(passedBack_entityId: String, passedBack_itsaId: String, authToken_saUtr: Option[String])(
    implicit hc: HeaderCarrier): Future[Result] =
    entityResolverConnector
      .resolveBy(passedBack_entityId)
      .flatMap { resolvedEntity =>
        if (resolvedEntity.itsa.isDefined) {
          if (resolvedEntity.itsa.get == passedBack_itsaId) {
            reply(OK, "itsaId successfully linked to entityId")
          } else {
            /*
             | Case 1.5 - entityId already has a different itsaId linked to it in entity resolver
             |
             |   Given I am a customer who has successfully enrolled in ITSA
             |   When the entityId already has a different itsaId linked to it in entity resolver
             |   Then my itsaId will not be added (i.e linked to) any entityId
             |   And an error will be generated
             */
            reply(UNAUTHORIZED, s"entityId already has a different itsaId linked to it in entity resolver")

            // TODO Isn't the following acceptance criteria already included in Case 1.5 ???
            /*
             | Case 1.6 - itsaId is already linked to a different entityId in entity resolver.
             |
             |   Given I am a customer who has successfully enrolled in ITSA
             |   When my itsaId is already linked to a different entityId in entity resolver
             |   Then my itsaId will not be added (i.e linked to) any entityId
             |   And an error will be generated
           */
          }
        } else {
          // entity.itsa.nonDefined, which means no link has been created yet
          if (!authToken_saUtr.isDefined) {
            /*
             | Case 1.4 - SAUTR does not exist in the  Auth token
             |
             |   Given I am a customer who has successfully enrolled in ITSA
             |   When SAUTR does not exist in the Auth token
             |   Then my itsaId will be added (i.e linked to) the entityId
             */
            entityResolverConnector
              .update(resolvedEntity.copy(itsa = Some(passedBack_itsaId)))
              .flatMap { _ =>
                reply(OK, "itsaId successfully linked to entityId")
              }
          } else {
            // authToken_saUtr.isDefined
            if (resolvedEntity.saUtr.isDefined && (resolvedEntity.saUtr.get == authToken_saUtr.get)) {
              /*
               | Case 1.1 - SAUTR in Auth token is the same as SAUTR in entity resolver
               |
               |   Given I am a customer who has successfully enrolled in ITSA
               |   When SAUTR in Auth token is the same as SAUTR in entity resolver
               |   Then my itsaId will be added (i.e linked to) the entityId
               */
              entityResolverConnector
                .update(resolvedEntity.copy(itsa = Some(passedBack_itsaId)))
                .flatMap { _ =>
                  reply(OK, "itsaId successfully linked to entityId")
                }
            } else {
              /*
               | Case 1.2 - SAUTR in Auth token is different from  SAUTR in entity resolver
               |
               |   Given I am a customer who has successfully enrolled in ITSA
               |   When SAUTR in Auth token is different from  SAUTR in entity resolver
               |   Then my itsaId will not be added (i.e linked to) any entityId
               |   And an error will be generated
               */
              reply(UNAUTHORIZED, "SAUTR in Auth token is different from SAUTR in entity resolver")

              // TODO Isn't the following acceptance criteria already included in Case 1.2 ???
              // Case 1.3 - SAUTR in Auth token is linked to a different entityId  in entity resolver
              //
              //   Given I am a customer who has successfully enrolled in ITSA
              //   When SAUTR in Auth token is linked to a different entityId  in entity resolver
              //   Then my itsaId will not be added (i.e linked to) any entityId
              //   And an error will be generated
            }
          }
        }
      }
      .recoverWith {
        case _: Throwable =>
          /*
           | Case 3.1 - entityId passed back by ITSA does not exist in entity resolver
           |
           |   Given I am a customer who has successfully enrolled in ITSA
           |   When the entityId passed back does not exist in entity resolver
           |   Then  my itsaId will not be added to any entityid
           |   And an error will be generated
           */
          reply(NOT_FOUND, "Invalid entity id or entity id has expired")
      }

  private def reply(code: Int, reason: String) =
    Future.successful(Status(code).apply(Json.obj("reason" -> reason)))
}
