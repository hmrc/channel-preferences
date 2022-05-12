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

import cats.syntax.either._
import com.mongodb.client.model.Indexes.ascending
import org.mongodb.scala.MongoException
import org.mongodb.scala.model.{ Filters, IndexModel, IndexOptions }
import uk.gov.hmrc.channelpreferences.model.preferences.Preference
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
    collection.insertOne(preference).toFuture().map(_.wasAcknowledged().asRight).recover {
      case error: MongoException => PreferenceNotFound(error.getMessage).asLeft
    }

  def get(enrolments: Seq[String]): Future[Seq[Preference]] = {
    val query = enrolments.map(e => Filters.eq("enrolments", e))

    collection.find[Preference](Filters.or(query: _*)).sort(Filters.equal("_id", -1)).toFuture()
  }
}

trait PreferenceError
final case class PreferenceCreationError(message: String) extends PreferenceError
final case class PreferenceNotFound(message: String) extends PreferenceError
