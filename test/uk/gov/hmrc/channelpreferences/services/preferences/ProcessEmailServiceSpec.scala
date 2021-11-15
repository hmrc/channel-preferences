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

package uk.gov.hmrc.channelpreferences.services.preferences

import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import uk.gov.hmrc.channelpreferences.connectors.PreferencesConnector
import uk.gov.hmrc.channelpreferences.model.preferences.Event

import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.Future

class ProcessEmailServiceSpec extends PlaySpec with ScalaFutures with MockitoSugar {

  "send" must {
    "return response as returned from preferences connector" in new TestClass {
      val processBounceService = new ProcessBounceService(preferencesConnectorMock)
      processBounceService.process(event).futureValue mustBe Right(s"bounce processed for id $eventId")
    }
  }

  class TestClass {
    val eventId = UUID.randomUUID()
    val event = Event(eventId, "subject", "groupId", LocalDateTime.now(), Json.parse("{}"))
    val preferencesConnectorMock = mock[PreferencesConnector]
    when(preferencesConnectorMock.serviceUrl).thenReturn("")
    when(preferencesConnectorMock.update(event))
      .thenReturn(Future.successful(Right(s"bounce processed for id $eventId")))
  }
}
