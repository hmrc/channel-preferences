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

import com.mongodb.client.result.InsertOneResult
import org.mockito.ArgumentMatchersSugar.*
import org.mockito.IdiomaticMockito
import org.mongodb.scala.{ FindObservable, MongoCollection, Observable, SingleObservable }
import org.mongodb.scala.bson.BsonObjectId
import org.mongodb.scala.bson.conversions.Bson
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.channelpreferences.repository.model.{ Context, TestModels }
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.util.UUID

class ContextRepositorySpec extends AnyFlatSpec with Matchers with IdiomaticMockito with ScalaFutures with TestModels {

  behavior of "ContextRepository.addContext"

  it should "return a mongo InsertOneResult when the underlying repo adds a new context to the context repo" in new Scope {
    mongoCollectionMock.insertOne(*[Context]) returns SingleObservable(insertOneResult)
    contextRepository.addContext(context).futureValue should be(insertOneResult)
  }

  behavior of "ContextRepository.find"

  it should "return a context when the underlying repo successfully finds one for that context ID" in new Scope {
    findObservableMock.map[Context](*) returns Observable(Seq(context))
    mongoCollectionMock.find[Context](*[Bson])(*, *) returns findObservableMock
    contextRepository.find(UUID.randomUUID()).futureValue should be(Some(context))
  }

  trait Scope {
    val playMongoContextRepository: PlayMongoRepository[Context] = mock[PlayMongoRepository[Context]]
    val findObservableMock: FindObservable[Context] = mock[FindObservable[Context]]
    val mongoCollectionMock: MongoCollection[Context] = mock[MongoCollection[Context]]
    val contextRepository: ContextRepository = new ContextRepository(playMongoContextRepository)
    val insertOneResult: InsertOneResult = InsertOneResult.acknowledged(BsonObjectId())

    playMongoContextRepository.collection returns mongoCollectionMock
  }
}
