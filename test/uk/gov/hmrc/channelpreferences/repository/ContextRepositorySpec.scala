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

import com.mongodb.client.result
import org.mockito.ArgumentMatchersSugar.*
import org.mockito.IdiomaticMockito
import org.mongodb.scala.{ FindObservable, MongoCollection, Observable, SingleObservable }
import org.mongodb.scala.bson.BsonObjectId
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.result.{ DeleteResult, InsertOneResult, UpdateResult }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.channelpreferences.repository.model.{ ContextPayload, TestModels }
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import scala.concurrent.Future

class ContextRepositorySpec extends AnyFlatSpec with Matchers with IdiomaticMockito with ScalaFutures with TestModels {

  behavior of "ContextRepository.addContext"

  it should "return a mongo InsertOneResult when the underlying repo adds a new context to the context repo" in new Scope {
    mongoCollectionMock.insertOne(*[ContextPayload]) returns SingleObservable(insertOneResult)
    contextRepository.addContext(contextPayload).futureValue should be(insertOneResult)
  }

  behavior of "ContextRepository.find"

  it should "return a context when the underlying repo successfully finds one for that context ID" in new Scope {
    findObservableMock.map[ContextPayload](*) returns Observable(Seq(contextPayload))
    findObservableMock.headOption() returns Future.successful(Some(contextPayload))
    mongoCollectionMock.find[ContextPayload](*[Bson])(*, *) returns findObservableMock
    contextRepository.findContext(keyIdentifier).futureValue should be(Some(contextPayload))
  }

  behavior of "ContextRepository.updateContext"

  it should "return a context update confirmation when the underlying repo successfully updates an entire context" in new Scope {
    mongoCollectionMock.replaceOne(*[Bson], *[ContextPayload]) returns SingleObservable(replaceOneResult)
    contextRepository.updateContext(contextPayload).futureValue should be(replaceOneResult)
  }

  behavior of "ContextRepository.deleteContext"

  it should "return a context deletion confirmation when the underlying repo successfully deletes a context" in new Scope {
    mongoCollectionMock.deleteOne(*[Bson]) returns SingleObservable(deleteResult)
    contextRepository.deleteContext(keyIdentifier).futureValue should be(deleteResult)
  }

  trait Scope {
    val playMongoContextRepository: PlayMongoRepository[ContextPayload] = mock[PlayMongoRepository[ContextPayload]]
    val findObservableMock: FindObservable[ContextPayload] = mock[FindObservable[ContextPayload]]
    val mongoCollectionMock: MongoCollection[ContextPayload] = mock[MongoCollection[ContextPayload]]
    val contextRepository: ContextRepository = new ContextRepository(playMongoContextRepository)
    val insertOneResult: InsertOneResult = result.InsertOneResult.acknowledged(BsonObjectId())
    val replaceOneResult: UpdateResult = result.UpdateResult.acknowledged(1, 1, BsonObjectId())
    val deleteResult: DeleteResult = result.DeleteResult.acknowledged(1)

    playMongoContextRepository.collection returns mongoCollectionMock
  }
}
