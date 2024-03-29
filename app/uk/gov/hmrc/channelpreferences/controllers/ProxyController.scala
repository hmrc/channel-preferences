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

import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import org.slf4j.MDC
import play.api.Logging
import play.api.libs.streams.Accumulator
import play.api.mvc._
import uk.gov.hmrc.channelpreferences.services.entityresolver.OutboundProxy
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.util.UUID.randomUUID
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ProxyController @Inject() (
  controllerComponents: ControllerComponents,
  outboundProxy: OutboundProxy
)(implicit ec: ExecutionContext)
    extends BackendController(controllerComponents) with Logging {

  private[this] def streamedBodyParser: BodyParser[Source[ByteString, Any]] = BodyParser { _ =>
    Accumulator.source[ByteString].map(Right.apply)
  }

  def proxy(path: String): Action[Source[ByteString, _]] =
    Action.async(streamedBodyParser) { implicit request =>
      populateMdc(request)

      logger.debug(s"Inbound Request: ${request.method} ${request.uri}")

      outboundProxy.proxy(request).recover { case ex: Exception =>
        logger.error(s"An error occurred proxying $path , error: ${ex.getMessage}")
        InternalServerError(ex.getMessage)
      }
    }

  private[this] def populateMdc(implicit request: Request[Source[ByteString, _]]): Unit = {
    val extraDiagnosticContext = Map(
      "transaction_id" -> randomUUID.toString
    ) ++ request.headers.get(USER_AGENT).toList.map(USER_AGENT -> _)

    (hc.mdcData ++ extraDiagnosticContext).foreach { case (k, v) =>
      MDC.put(k, v)
    }
  }

}
