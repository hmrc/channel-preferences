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

package uk.gov.hmrc.channelpreferences.services.preferences

import com.mongodb.client.result.{ DeleteResult, InsertOneResult, UpdateResult }
import org.mockito.IdiomaticMockito
import org.mockito.ArgumentMatchersSugar.*
import org.mongodb.scala.bson.BsonObjectId
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.channelpreferences.repository
import uk.gov.hmrc.channelpreferences.repository.ContextRepository
import uk.gov.hmrc.channelpreferences.controllers.model.TestModels
import uk.gov.hmrc.channelpreferences.model.mapping.ContextConversion

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source

class ContextServiceSpec extends PlaySpec with ScalaFutures with IdiomaticMockito with TestModels {

  "Create a new context" should {
    "return a success status in response to a successful DB insert" in new TestClass {
      val insertOneResult: InsertOneResult = InsertOneResult.acknowledged(BsonObjectId())
      contextRepositoryMock.addContext(*[repository.model.ContextPayload]) returns Future.successful(insertOneResult)

      val contextService = new ContextServiceImpl(contextRepositoryMock)
      val result = contextService.store(contextPayload).futureValue
      result.isRight mustBe true
    }
  }

  "Replace context" should {
    "return a success status in response to a successful DB upsert" in new TestClass {

      val updateOneResult: UpdateResult = UpdateResult.acknowledged(1, 1, BsonObjectId())
      contextRepositoryMock.updateContext(*[repository.model.ContextPayload]) returns Future.successful(
        updateOneResult)

      val contextService = new ContextServiceImpl(contextRepositoryMock)
      val result = contextService.replace(contextPayload).futureValue
      result.isRight mustBe true
    }
  }

  "Retrieve existing context by key" should {
    "return the associated context payload in response to a successful DB document retrieval" in new TestClass {

      contextRepositoryMock.findContext(*[String]) returns Future.successful(
        Some(ContextConversion.toDbContextPayload(contextPayload)))

      val contextService = new ContextServiceImpl(contextRepositoryMock)
      val result = contextService.retrieve(keyIdentifier).futureValue
      result.isRight mustBe true
      result.right.get mustBe contextPayload
    }
  }

  "Delete context by key" should {
    "return a success status in response to a successful DB document removal" in new TestClass {

      val deleteResult: DeleteResult = DeleteResult.acknowledged(1)
      contextRepositoryMock.deleteContext(*[String]) returns Future.successful(deleteResult)

      val contextService = new ContextServiceImpl(contextRepositoryMock)
      val result = contextService.remove(keyIdentifier).futureValue
      result.isRight mustBe true
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
