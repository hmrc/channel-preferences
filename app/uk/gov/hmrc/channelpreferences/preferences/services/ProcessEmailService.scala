/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.channelpreferences.preferences.services

import com.google.inject.ImplementedBy
import uk.gov.hmrc.channelpreferences.connectors.PreferencesConnector
import uk.gov.hmrc.channelpreferences.model.ChannelPreferencesError
import uk.gov.hmrc.channelpreferences.preferences.model.Event

import javax.inject.Inject
import scala.concurrent.Future

@ImplementedBy(classOf[ProcessBounceService])
trait ProcessEmail {
  def process(event: Event): Future[Either[ChannelPreferencesError, String]]
}

class ProcessBounceService @Inject()(preferencesConnector: PreferencesConnector) extends ProcessEmail {
  def process(event: Event): Future[Either[ChannelPreferencesError, String]] =
    preferencesConnector.update(event)
}
