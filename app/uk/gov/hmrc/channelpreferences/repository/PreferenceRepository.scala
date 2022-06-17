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
import cats.syntax.either._
import com.mongodb.client.model.Indexes.ascending
import org.mongodb.scala.model.{ Filters, IndexModel, IndexOptions }
import uk.gov.hmrc.channelpreferences.model.preferences.PreferenceError.PreferenceCreationError
import uk.gov.hmrc.channelpreferences.model.preferences.{ Enrolment, Preference, PreferenceError }
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }

class PreferenceRepository @Inject()(mongoComponent: MongoComponent)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[Preference](
      mongoComponent,
      "preference",
      Preference.format,
      Seq(
        IndexModel(
          ascending("enrolments"),
          IndexOptions().name("enrolmentsIndex").unique(true)
        )),
      replaceIndexes = false
    ) {

  def insert(preference: Preference): Future[Either[PreferenceError, Boolean]] =
    collection
      .insertOne(preference)
      .toFuture()
      .map(_.wasAcknowledged().asRight)
      .recover {
        case error: Exception => PreferenceCreationError(error).asLeft
      }

  def update(preference: Preference): Future[Either[PreferenceError, Boolean]] =
    collection
      .replaceOne(Filters.or(enrolmentKeysQuery(preference.enrolments): _*), preference)
      .toFuture()
      .map(_.wasAcknowledged().asRight)
      .recover {
        case error: Exception => PreferenceCreationError(error).asLeft
      }

  def get(enrolments: NonEmptyList[Enrolment]): Future[Option[Preference]] = {
    val query = enrolmentKeysQuery(enrolments)

    collection
      .find[Preference](Filters.or(query: _*))
      .headOption()
  }

  private def enrolmentKeysQuery(enrolments: NonEmptyList[Enrolment]) =
    enrolments.toList
      .map(_.value)
      .map(Filters.eq("enrolments", _))
}
