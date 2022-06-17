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
import org.scalatest.EitherValues
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.channelpreferences.controllers.model.{ Consent, Version }
import uk.gov.hmrc.channelpreferences.model.preferences.{ CustomsServiceEnrolment, IdentifierValue, _ }
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global

class PreferenceRepositorySpec
    extends PlaySpec with MockitoSugar with EitherValues with DefaultPlayMongoRepositorySupport[Preference] {

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    dropCollection()
  }

  "insert" should {
    "return true when preference record is inserted to mongo" in new TestClass {
      insertPreference(List(CustomsServiceEnrolment(IdentifierValue("GB123456789")))).right.get mustBe true
    }

    "return error when preference record is inserted to mongo" in new TestClass {
      insertPreference(List(CustomsServiceEnrolment(IdentifierValue("GB123456789")))).right.get mustBe true
      insertPreference(List(CustomsServiceEnrolment(IdentifierValue("GB123456789")))).left.get.toString must include(
        "duplicate key error")
    }
  }

  "get" should {
    "return a preference that matches single enrolment and preference also has single enrolment" in new TestClass {
      val taxIdentifierValue = "GB123456788"
      val taxIdentifiers: NonEmptyList[Enrolment] =
        NonEmptyList.of(Enrolment.fromValue(s"HMRC-CUS-ORG~EORINumber~$taxIdentifierValue").right.value)

      insertPreference(List(CustomsServiceEnrolment(IdentifierValue(taxIdentifierValue))))

      val result = preferenceRepository.get(taxIdentifiers).futureValue
      result.value mustBe
        preference(List(CustomsServiceEnrolment(IdentifierValue(taxIdentifierValue))))

    }

    "return a preference that matches single enrolment and preference has multiple enrolments" in new TestClass {
      val taxIdentifierValue = "GB123456789"
      val taxIdentifierValue2 = "GB123456788"

      val enrolment = CustomsServiceEnrolment(IdentifierValue(taxIdentifierValue))

      insertPreference(
        List(
          CustomsServiceEnrolment(IdentifierValue(taxIdentifierValue)),
          CustomsServiceEnrolment(IdentifierValue(taxIdentifierValue2))))

      val enrolments = NonEmptyList.of(enrolment)

      val result = preferenceRepository.get(enrolments).futureValue.value

      result mustBe
        preference(
          List(
            CustomsServiceEnrolment(IdentifierValue(taxIdentifierValue)),
            CustomsServiceEnrolment(IdentifierValue(taxIdentifierValue2))))

    }

    "return a preference that matches multiple enrolments and preference with same multiple enrolments" in new TestClass {
      val taxIdentifierValue = "GB123456789"
      val taxIdentifierValue2 = "GB123456788"
      val taxIdentifiers =
        NonEmptyList.of(
          Enrolment.fromValue(s"HMRC-CUS-ORG~EORINumber~$taxIdentifierValue").right.value,
          Enrolment.fromValue(s"HMRC-CUS-ORG~EORINumber~$taxIdentifierValue2").right.value
        )

      insertPreference(
        List(
          CustomsServiceEnrolment(IdentifierValue(taxIdentifierValue)),
          CustomsServiceEnrolment(IdentifierValue(taxIdentifierValue2))))

      val result = preferenceRepository.get(taxIdentifiers).futureValue.value
      result mustBe
        preference(
          List(
            CustomsServiceEnrolment(IdentifierValue(taxIdentifierValue)),
            CustomsServiceEnrolment(IdentifierValue(taxIdentifierValue2))))
    }

    "do not return a preference when there is no matching enrolment in preference" in new TestClass {
      val taxIdentifierValue1 = "GB123456788"
      val taxIdentifierValue2 = "GB123456788"
      val taxIdentifiersDiff = NonEmptyList.of(
        Enrolment.fromValue("HMRC-CUS-ORG~EORINumber~GB123456781").right.value,
        Enrolment.fromValue("HMRC-CUS-ORG~EORINumber~GB123456782").right.value
      )
      insertPreference(
        List(
          CustomsServiceEnrolment(IdentifierValue(taxIdentifierValue1)),
          CustomsServiceEnrolment(IdentifierValue(taxIdentifierValue2))))
      val result = preferenceRepository.get(taxIdentifiersDiff).futureValue
      result mustBe None

    }
  }

  class TestClass {
    val preferenceRepository = new PreferenceRepository(mongoComponent)

    val timestamp: LocalDateTime = LocalDateTime.of(2022, 5, 11, 14, 33, 11);

    val createdValue = Created(timestamp)
    val updatedValue = Updated(timestamp)

    def insertPreference(enrolments: List[Enrolment]): Either[PreferenceError, Boolean] =
      preferenceRepository
        .insert(
          preference(enrolments, createdValue, updatedValue)
        )
        .futureValue

    def preference(
      enrolments: List[Enrolment] = List(CustomsServiceEnrolment(IdentifierValue("GB123456789"))),
      created: Created = createdValue,
      updated: Updated = updatedValue): Preference =
      Preference(
        NonEmptyList.fromList(enrolments).get,
        created,
        NonEmptyList(
          Consent(
            DefaultConsentType,
            ConsentStatus(true),
            updated,
            Version(3, 2, 1),
            List(DigitalCommunicationsPurpose)),
          Nil),
        List(
          EmailPreference(
            PrimaryIndex,
            EmailAddress("test@test.com"),
            TextPlain,
            EnglishLanguage,
            Contactable(true),
            List(DigitalCommunicationsPurpose))),
        Active
      )
  }
  override protected def repository: PlayMongoRepository[Preference] =
    new PlayMongoRepository[Preference](mongoComponent, "preference", Preference.format, Seq.empty)

}
