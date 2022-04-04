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

import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import play.api.http.Status
import play.api.libs.json.Json
import play.api.mvc.Headers
import play.api.test.Helpers.{ contentAsJson, contentAsString, defaultAwaitTimeout, status }
import play.api.test.{ FakeRequest, Helpers }
import uk.gov.hmrc.channelpreferences.controllers.model.ContextPayload

import scala.io.Source

class ContextControllerSpec extends PlaySpec with ScalaFutures {

  "Create context" should {
    "return status Created for a valid payload" in new TestClass {
      val controller = new ContextController(Helpers.stubControllerComponents())

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
      val controller = new ContextController(Helpers.stubControllerComponents())

      val payload = Json.parse(readResources("contextPayload.json"))

      val result =
        controller
          .update("61ea7c5951d7a42da4fd4608")
          .apply(FakeRequest("PUT", "", Headers("Content-Type" -> "application/json"), payload))

      status(result) mustBe Status.OK
      contentAsString(result) mustBe "61ea7c5951d7a42da4fd4608 updated"
    }
  }

  "GET context by key" should {
    "return status OK with context details" in {
      val controller = new ContextController(Helpers.stubControllerComponents())
      val result =
        controller
          .get("b25fb7aab4d911ecb9090242ac120002")
          .apply(FakeRequest("GET", "/channel-preferences/context/:key"))
      status(result) mustBe 200
      val contextResponse = contentAsJson(result).validate[ContextPayload].get
      contextResponse.key mustBe "b25fb7aab4d911ecb9090242ac120002"
      contextResponse.resourcePath mustBe "path"
    }
  }

  "DELETE context by key" should {
    "return status Accepted" in {
      val controller = new ContextController(Helpers.stubControllerComponents())
      val result =
        controller
          .delete("b25fb7aab4d911ecb9090242ac120002")
          .apply(FakeRequest("GET", "/channel-preferences/context/:key"))
      status(result) mustBe Status.ACCEPTED
    }
  }

  class TestClass {

    def readResources(fileName: String): String = {
      val resource = Source.fromURL(getClass.getResource("/context/" + fileName))
      val str = resource.mkString
      resource.close()
      str
    }

  }

}
