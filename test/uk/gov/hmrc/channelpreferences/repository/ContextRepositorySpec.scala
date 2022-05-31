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

package uk.gov.hmrc.channelpreferences.repository

import org.mockito.IdiomaticMockito
import org.mongodb.scala.result.{ DeleteResult, InsertOneResult, UpdateResult }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.channelpreferences.repository.model.TestModels
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import org.mongodb.scala.model.Filters
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.channelpreferences.controllers.model.ContextPayload

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global

class ContextRepositorySpec
    extends AnyFlatSpec with BeforeAndAfterEach with DefaultPlayMongoRepositorySupport[ContextPayload] with Matchers
    with IdiomaticMockito with ScalaFutures with TestModels {

  behavior of "ContextRepository.addContext"

  it should "return a mongo InsertOneResult when the underlying repo adds a new context to the context repo" in {
    val insertOneResult: InsertOneResult = repository.addContext(contextPayload).futureValue
    insertOneResult.wasAcknowledged() should be(true)
    repository.collection.find(Filters.in("contextId.enrolments", enrolmentValue)).toFuture().futureValue
    repository.collection.deleteOne(Filters.in("contextId.enrolments", enrolmentValue)).toFuture().futureValue
  }

  behavior of "ContextRepository.find"

  it should "return a context when the underlying repo successfully finds one for that context ID" in {
    repository.collection.insertOne(contextPayload).toFuture().futureValue
    repository.findContext(enrolments).futureValue should be(Some(contextPayload))
    repository.collection.deleteOne(Filters.in("contextId.enrolments", enrolmentValue)).toFuture().futureValue
  }

  behavior of "ContextRepository.updateContext"

  it should "return a context update confirmation when the underlying repo successfully replaces an entire context" in {
    repository.collection.insertOne(contextPayload).toFuture().futureValue
    val newExpiry = LocalDateTime.now()
    val newContextPayload = contextPayload.copy(expiry = newExpiry)
    val replaceOneResult: UpdateResult = repository.updateContext(newContextPayload).futureValue
    replaceOneResult.wasAcknowledged() should be(true)
    val existing =
      repository.collection.find(Filters.in("contextId.enrolments", enrolmentValue)).toFuture().futureValue
    existing.head should be(newContextPayload)
    repository.collection.deleteOne(Filters.in("contextId.enrolments", enrolmentValue)).toFuture().futureValue
  }

  behavior of "ContextRepository.deleteContext"

  it should "return a context deletion confirmation when the underlying repo successfully deletes a context" in {
    repository.collection.insertOne(contextPayload).toFuture().futureValue
    val firstCount =
      repository.collection.countDocuments(Filters.in("contextId.enrolments", enrolmentValue)).toFuture().futureValue
    firstCount should be(1)
    val deleteResult: DeleteResult = repository.deleteContext(enrolments).futureValue
    deleteResult.wasAcknowledged() should be(true)
    val lastCount =
      repository.collection.countDocuments(Filters.in("contextId.enrolments", enrolmentValue)).toFuture().futureValue
    lastCount should be(0)
  }

  override protected def repository: ContextRepository = new ContextRepository(mongoComponent)
}
