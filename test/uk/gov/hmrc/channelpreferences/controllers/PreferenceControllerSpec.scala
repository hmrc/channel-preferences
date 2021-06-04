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

package uk.gov.hmrc.channelpreferences.controllers

import akka.stream.Materializer
import controllers.Assets.CONFLICT
import org.joda.time.DateTime
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers.{ contentAsString, defaultAwaitTimeout, status }
import uk.gov.hmrc.channelpreferences.hub.cds.services.CdsPreference
import uk.gov.hmrc.http.HeaderCarrier
import play.api.test.{ FakeRequest, Helpers, NoMaterializer }
import uk.gov.hmrc.channelpreferences.hub.cds.model.{ Channel, Email, EmailVerification }
import play.api.http.Status.{ BAD_GATEWAY, OK, SERVICE_UNAVAILABLE }
import uk.gov.hmrc.emailaddress.EmailAddress

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ ExecutionContext, Future }

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
class PreferenceControllerSpec extends PlaySpec with ScalaFutures with MockitoSugar {

  implicit val mat: Materializer = NoMaterializer
  implicit val hc: HeaderCarrier = HeaderCarrier()
  private val emailVerification = EmailVerification(EmailAddress("some@email.com"), new DateTime(1987, 3, 20, 1, 2, 3))
  private val validEmailVerification = """{"address":"some@email.com","timestamp":"1987-03-20T01:02:03.000Z"}"""

  "Calling preference" should {
    "return a Bad Gateway for unexpected error status" in {
      val controller = new PreferenceController(
        new CdsPreference {
          override def getPreference(c: Channel, enrolmentKey: String, taxIdName: String, taxIdValue: String)(
            implicit hc: HeaderCarrier,
            ec: ExecutionContext): Future[Either[Int, EmailVerification]] =
            Future.successful(Left(SERVICE_UNAVAILABLE))
        },
        Helpers.stubControllerComponents()
      )

      val response = controller.preference(Email, "", "", "").apply(FakeRequest("GET", "/"))
      status(response) mustBe BAD_GATEWAY
    }

    "return Ok with the email verification if found" in {
      val controller = new PreferenceController(
        new CdsPreference {
          override def getPreference(c: Channel, enrolmentKey: String, taxIdName: String, taxIdValue: String)(
            implicit hc: HeaderCarrier,
            ec: ExecutionContext): Future[Either[Int, EmailVerification]] =
            Future.successful(Right(emailVerification))
        },
        Helpers.stubControllerComponents()
      )

      val response = controller.preference(Email, "", "", "").apply(FakeRequest("GET", "/"))
      status(response) mustBe OK
      contentAsString(response) mustBe validEmailVerification
    }
  }
  "Calling itsa activation stub endpoint " should {
    """return OK for any "non-magic" entityId""" in new TestSetup {
      val response = controller.activate("00000", "itsa-id").apply(FakeRequest("GET", "/"))
      status(response) mustBe OK
    }
    """return CONFLICT for a "magic" entityId""" in new TestSetup {
      val response =
        controller.activate("450262a0-1842-4885-8fa1-6fbc2aeb867d", "itsa-id").apply(FakeRequest("GET", "/"))
      status(response) mustBe CONFLICT
    }
  }
  trait TestSetup {
    val controller = new PreferenceController(
      new CdsPreference {
        override def getPreference(c: Channel, enrolmentKey: String, taxIdName: String, taxIdValue: String)(
          implicit hc: HeaderCarrier,
          ec: ExecutionContext): Future[Either[Int, EmailVerification]] =
          Future.successful(Left(SERVICE_UNAVAILABLE))
      },
      Helpers.stubControllerComponents()
    )

  }

}
