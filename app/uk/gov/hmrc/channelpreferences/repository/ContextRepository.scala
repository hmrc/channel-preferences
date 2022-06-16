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

import cats.data.NonEmptyList
import com.google.inject.{ Inject, Singleton }
import com.mongodb.client.model.Indexes.ascending
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.{ Filters, IndexModel, IndexOptions }
import org.mongodb.scala.result.{ DeleteResult, InsertOneResult, UpdateResult }
import uk.gov.hmrc.channelpreferences.controllers.model.{ ContextPayload, VerificationId }
import uk.gov.hmrc.channelpreferences.model.preferences.Enrolment
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.util.concurrent.TimeUnit
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class ContextRepository @Inject()(mongoComponent: MongoComponent)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[ContextPayload](
      collectionName = "context",
      mongoComponent = mongoComponent,
      domainFormat = ContextPayload.contextPayloadFormat,
      indexes = Seq(
        IndexModel(
          ascending("contextId.enrolments"),
          IndexOptions().name("contextIdIndex")
        ),
        IndexModel(
          ascending("expiry"),
          IndexOptions().name("expiryTtlIndex").expireAfter(7, TimeUnit.DAYS)
        )
      ),
      replaceIndexes = true
    ) {

  def addContext(contextPayload: ContextPayload): Future[InsertOneResult] =
    collection.insertOne(contextPayload).toFuture()

  def updateContext(contextPayload: ContextPayload): Future[UpdateResult] =
    collection
      .replaceOne(Filters.or(enrolmentKeysQuery(contextPayload.contextId.enrolments): _*), contextPayload)
      .toFuture()

  def deleteContext(keys: NonEmptyList[Enrolment]): Future[DeleteResult] =
    collection.deleteOne(Filters.or(enrolmentKeysQuery(keys): _*)).toFuture()

  def findContext(keys: NonEmptyList[Enrolment]): Future[Option[ContextPayload]] =
    collection
      .find(Filters.or(enrolmentKeysQuery(keys): _*))
      .headOption()

  def findVerification(verificationId: VerificationId): Future[Option[ContextPayload]] =
    collection
      .find(equal("context.verification.id", verificationId.id.toString))
      .headOption()

  private def enrolmentKeysQuery(enrolments: NonEmptyList[Enrolment]) =
    enrolments.toList
      .map(_.value)
      .map(Filters.eq("contextId.enrolments", _))
}
