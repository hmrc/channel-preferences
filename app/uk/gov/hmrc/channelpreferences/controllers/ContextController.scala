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

import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.{ Action, AnyContent, ControllerComponents }
import uk.gov.hmrc.channelpreferences.controllers.model._
import uk.gov.hmrc.channelpreferences.model.context.ContextStoreAcknowledged
import uk.gov.hmrc.channelpreferences.services.preferences.ContextService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import javax.inject.{ Inject, Singleton }
import scala.concurrent.ExecutionContext

@Singleton
class ContextController @Inject()(contextService: ContextService, controllerComponents: ControllerComponents)(
  implicit ec: ExecutionContext)
    extends BackendController(controllerComponents) {

  def create: Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[ContextPayload] { context =>
      contextService
        .store(context)
        .map {
          case Right(_: ContextStoreAcknowledged) => Created(context.contextId.value)
          case _                                  => BadRequest(s"Context ${context.contextId.value} could not be created")
        }
    }
  }

  def get(contextId: String): Action[AnyContent] = Action.async { _ =>
    contextService
      .retrieve(contextId)
      .map {
        case Right(payload) => Ok(Json.toJson(payload))
        case _              => NotFound(s"Context $contextId could not be retrieved")
      }
  }

  def update(id: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[ContextPayload] { context =>
      contextService
        .replace(context)
        .map {
          case Right(_: ContextStoreAcknowledged) => Ok(s"${context.contextId.value} updated with id $id")
          case _                                  => BadRequest(s"Context ${context.contextId.value} with id: $id could not be updated")
        }
    }
  }

  def delete(contextId: String): Action[AnyContent] = Action.async { _ =>
    contextService
      .remove(contextId)
      .map {
        case Right(_: ContextStoreAcknowledged) => Accepted(s"$contextId deleted")
        case _                                  => BadRequest(s"Context $contextId could not be deleted")
      }
  }
}
