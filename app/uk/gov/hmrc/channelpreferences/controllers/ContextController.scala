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
import uk.gov.hmrc.channelpreferences.model.mapping.ContextConversion
import uk.gov.hmrc.channelpreferences.repository
import uk.gov.hmrc.channelpreferences.repository.ContextRepository
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class ContextController @Inject()(contextRepository: ContextRepository, controllerComponents: ControllerComponents)(
  implicit ec: ExecutionContext)
    extends BackendController(controllerComponents) {

  def create: Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[ContextPayload] { context =>
      contextRepository
        .addContext(contextPayload = ContextConversion.toDbContextPayload(context))
        .map(_ => Created(context.key))
    }
  }

  def get(key: String): Action[AnyContent] = Action.async { _ =>
    contextRepository
      .findContext(key)
      .map((context: Option[repository.model.ContextPayload]) =>
        context match {
          case Some(payload) => Ok(Json.toJson(ContextConversion.toApiContextPayload(payload)))
          case None          => NotFound("Context could not be found")
      })
  }

  def update(id: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[ContextPayload] { context =>
      contextRepository
        .updateContext(contextPayload = ContextConversion.toDbContextPayload(context))
        .map(_ => Ok(s"$id updated"))
    }
  }

  def delete(key: String): Action[AnyContent] = Action.async { _ =>
    Future.successful(Accepted(s"$key deleted"))
  }

}
