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

import org.mockito.IdiomaticMockito
import org.mockito.ArgumentMatchersSugar.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import play.api.http.Status
import play.api.libs.json.Json
import play.api.mvc.Headers
import play.api.test.Helpers.{ contentAsJson, contentAsString, defaultAwaitTimeout, status }
import play.api.test.{ FakeRequest, Helpers }
import uk.gov.hmrc.channelpreferences.controllers.model.ContextPayload
import uk.gov.hmrc.channelpreferences.model.context.ContextStoreAcknowledged
import uk.gov.hmrc.channelpreferences.services.preferences.ContextService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source

class ContextControllerSpec extends PlaySpec with ScalaFutures with IdiomaticMockito {

  "Create context" should {
    "return status Created for a valid payload" in new TestClass {

      contextServiceMock.store(*[ContextPayload]) returns Future.successful(Right(new ContextStoreAcknowledged()))

      val controller = new ContextController(contextServiceMock, Helpers.stubControllerComponents())

      val payload = Json.parse(readResources("contextPayload.json"))

      val result =
        controller.create
          .apply(FakeRequest("POST", "", Headers("Content-Type" -> "application/json"), payload))

      status(result) mustBe Status.CREATED
      contentAsString(result) mustBe "61ea7c5951d7a42da4fd4608"
    }
  }

  "Update context" should {
    "return status OK for a valid payload" in new TestClass {

      val payload = Json.parse(readResources("contextPayload.json"))

      contextServiceMock.replace(*[ContextPayload]) returns Future.successful(Right(new ContextStoreAcknowledged()))

      val controller = new ContextController(contextServiceMock, Helpers.stubControllerComponents())

      val result =
        controller
          .update("61ea7c5951d7a42da4fd4608")
          .apply(FakeRequest("PUT", "", Headers("Content-Type" -> "application/json"), payload))

      status(result) mustBe Status.OK
      contentAsString(result) mustBe "61ea7c5951d7a42da4fd4608 updated"
    }
  }

  "Get context by key" should {
    "return status OK with context details" in new TestClass {

      val payload = Json.parse(readResources("contextPayload.json"))
      val context = ContextPayload.contextPayloadFormat.reads(payload)

      contextServiceMock.retrieve(*[String]) returns Future.successful(Right(context.get))

      val controller = new ContextController(contextServiceMock, Helpers.stubControllerComponents())
      val result =
        controller
          .get("61ea7c5951d7a42da4fd4608")
          .apply(FakeRequest("GET", "/channel-preferences/context/:key"))
      status(result) mustBe 200
      val contextResponse = contentAsJson(result).validate[ContextPayload].get
      contextResponse.key mustBe "61ea7c5951d7a42da4fd4608"
      contextResponse.resourcePath mustBe "email[index=primary]"
    }
  }

  "Delete context by key" should {
    "return status Accepted" in new TestClass {

      contextServiceMock.remove(*[String]) returns Future.successful(Right(new ContextStoreAcknowledged()))

      val controller = new ContextController(contextServiceMock, Helpers.stubControllerComponents())
      val result =
        controller
          .delete("b25fb7aab4d911ecb9090242ac120002")
          .apply(FakeRequest("GET", "/channel-preferences/context/:key"))
      status(result) mustBe Status.ACCEPTED
    }
  }

  class TestClass {
    val contextServiceMock: ContextService = mock[ContextService]

    def readResources(fileName: String): String = {
      val resource = Source.fromURL(getClass.getResource("/context/" + fileName))
      val str = resource.mkString
      resource.close()
      str
    }
  }

}
