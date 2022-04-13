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

import com.mongodb.client.result.{ DeleteResult, InsertOneResult, UpdateResult }
import org.mockito.IdiomaticMockito
import org.mockito.ArgumentMatchersSugar.*
import org.mongodb.scala.bson.BsonObjectId
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import play.api.http.Status
import play.api.libs.json.Json
import play.api.mvc.Headers
import play.api.test.Helpers.{ contentAsJson, contentAsString, defaultAwaitTimeout, status }
import play.api.test.{ FakeRequest, Helpers }
import uk.gov.hmrc.channelpreferences.controllers.model.ContextPayload
import uk.gov.hmrc.channelpreferences.repository
import uk.gov.hmrc.channelpreferences.repository.ContextRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source

class ContextControllerSpec extends PlaySpec with ScalaFutures with IdiomaticMockito {

  "Create context" should {
    "return status Created for a valid payload" in new TestClass {
      val insertOneResult: InsertOneResult = InsertOneResult.acknowledged(BsonObjectId())
      contextRepositoryMock.addContext(*[repository.model.ContextPayload]) returns Future.successful(insertOneResult)

      val controller = new ContextController(contextRepositoryMock, Helpers.stubControllerComponents())

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

      val updateOneResult: UpdateResult = UpdateResult.acknowledged(1, 1, BsonObjectId())
      contextRepositoryMock.updateContext(*[repository.model.ContextPayload]) returns Future.successful(
        updateOneResult)

      val controller = new ContextController(contextRepositoryMock, Helpers.stubControllerComponents())

      val result =
        controller
          .update("61ea7c5951d7a42da4fd4608")
          .apply(FakeRequest("PUT", "", Headers("Content-Type" -> "application/json"), payload))

      status(result) mustBe Status.OK
      contentAsString(result) mustBe "61ea7c5951d7a42da4fd4608 updated"
    }
  }

  "GET context by key" should {
    "return status OK with context details" in new TestClass {
      val payload = Json.parse(readResources("contextPayload.json"))
      val context = repository.model.ContextPayload.contextPayloadFormat.reads(payload)

      contextRepositoryMock.findContext(*[String]) returns Future.successful(Some(context.get))

      val controller = new ContextController(contextRepositoryMock, Helpers.stubControllerComponents())
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

  "DELETE context by key" should {
    "return status Accepted" in new TestClass {

      val deleteResult: DeleteResult = DeleteResult.acknowledged(1)
      contextRepositoryMock.deleteContext(*[String]) returns Future.successful(deleteResult)

      val controller = new ContextController(contextRepositoryMock, Helpers.stubControllerComponents())
      val result =
        controller
          .delete("b25fb7aab4d911ecb9090242ac120002")
          .apply(FakeRequest("GET", "/channel-preferences/context/:key"))
      status(result) mustBe Status.ACCEPTED
    }
  }

  class TestClass {
    val contextRepositoryMock: ContextRepository = mock[ContextRepository]

    def readResources(fileName: String): String = {
      val resource = Source.fromURL(getClass.getResource("/context/" + fileName))
      val str = resource.mkString
      resource.close()
      str
    }
  }

}
