/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.channelpreferences

import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.ws.WSClient
import uk.gov.hmrc.channelpreferences.UrlHelper.-/
import scala.concurrent.ExecutionContext

trait ISpec
    extends PlaySpec with ScalaFutures with IntegrationPatience with GuiceOneServerPerSuite with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  val wsClient: WSClient = app.injector.instanceOf[WSClient]

  def resource(path: String): String =
    s"http://localhost:$port/${-/(path)}"
}

object UrlHelper {
  def -/(uri: String): String =
    if (uri.startsWith("/")) uri.drop(1) else uri
}
