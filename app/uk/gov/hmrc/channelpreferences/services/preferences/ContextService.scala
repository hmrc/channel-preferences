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
import uk.gov.hmrc.channelpreferences.repository.ContextRepository
import uk.gov.hmrc.channelpreferences.controllers
import uk.gov.hmrc.channelpreferences.model.context.{ ContextStorageError, ContextStoreAcknowledged, ContextStoreStatus }

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

trait ContextService {
  def store(context: controllers.model.ContextPayload): Future[Either[ContextStorageError, ContextStoreStatus]]
  def replace(context: controllers.model.ContextPayload): Future[Either[ContextStorageError, ContextStoreStatus]]
  def retrieve(key: String): Future[Either[ContextStorageError, controllers.model.ContextPayload]]
  def remove(key: String): Future[Either[ContextStorageError, ContextStoreStatus]]
}

@Singleton
class ContextServiceImpl @Inject()(contextRepository: ContextRepository)(implicit ec: ExecutionContext)
    extends ContextService {

  def store(context: controllers.model.ContextPayload): Future[Either[ContextStorageError, ContextStoreStatus]] =
    contextRepository
      .addContext(context)
      .map {
        case result: InsertOneResult if result.wasAcknowledged() => Right(new ContextStoreAcknowledged())
        case _                                                   => Left(new ContextStorageError("There was a problem storing the context"))
      }

  def replace(context: controllers.model.ContextPayload): Future[Either[ContextStorageError, ContextStoreStatus]] =
    contextRepository
      .updateContext(context)
      .map {
        case result: UpdateResult if result.wasAcknowledged() => Right(new ContextStoreAcknowledged())
        case _                                                => Left(new ContextStorageError("There was a problem updating the context"))
      }

  def retrieve(key: String): Future[Either[ContextStorageError, controllers.model.ContextPayload]] =
    contextRepository
      .findContext(key)
      .map {
        case Some(context) => Right(context)
        case _             => Left(new ContextStorageError("There was a problem retrieving the context"))
      }

  def remove(key: String): Future[Either[ContextStorageError, ContextStoreStatus]] =
    contextRepository.deleteContext(key).map {
      case result: DeleteResult if result.wasAcknowledged() => Right(new ContextStoreAcknowledged())
      case _                                                => Left(new ContextStorageError("There was a problem deleting the context"))
    }
}
