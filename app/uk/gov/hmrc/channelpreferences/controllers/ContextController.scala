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
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.{ Inject, Singleton }
import scala.concurrent.Future

@Singleton
class ContextController @Inject()(controllerComponents: ControllerComponents)
    extends BackendController(controllerComponents) {

  def create: Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[ContextPayload] { context =>
      Future.successful(Created(context.key))
    }
  }

  def get(key: String): Action[AnyContent] = Action.async { _ =>
    val context = ContextPayload(
      key,
      "path",
      LocalDateTime.now(),
      Context(
        Consented("DEFAULT", true, LocalDateTime.now(), Version(1, 2, 5), List.empty),
        Verification(UUID.fromString("b25fb7aa-b4d9-11ec-b909-0242ac120002"), "test@test.com", LocalDateTime.now()),
        Confirm(UUID.fromString("b25fb7aa-b4d9-11ec-b909-0242ac120002"), LocalDateTime.now())
      )
    )
    Future.successful(Ok(Json.toJson(context)))
  }

  def update(id: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[ContextPayload] { _ =>
      Future.successful(Ok(s"$id updated"))
    }
  }

  def delete(key: String): Action[AnyContent] = Action.async { _ =>
    Future.successful(Accepted(s"$key deleted"))
  }

}
