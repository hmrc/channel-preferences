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

package uk.gov.hmrc.channelpreferences.services.entityresolver

import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.google.inject.ImplementedBy
import play.api.mvc.{ Request, Result }
import uk.gov.hmrc.channelpreferences.connectors.OutboundProxyConnector

import javax.inject.Inject
import scala.concurrent.Future

@ImplementedBy(classOf[OutboundProxyService])
trait OutboundProxy {
  def proxy(inboundRequest: Request[Source[ByteString, _]]): Future[Result]
}

class OutboundProxyService @Inject()(outboundProxyConnector: OutboundProxyConnector) extends OutboundProxy {

  def proxy(inboundRequest: Request[Source[ByteString, _]]): Future[Result] =
    outboundProxyConnector.proxy(inboundRequest)
}
