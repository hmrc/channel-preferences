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
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.Headers
import uk.gov.hmrc.auth.core.{ AffinityGroup, AuthConnector, AuthorisationException }
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers.{ contentAsString, defaultAwaitTimeout, status }
import uk.gov.hmrc.channelpreferences.hub.cds.services.CdsPreference
import uk.gov.hmrc.http.HeaderCarrier
import play.api.test.{ FakeRequest, Helpers, NoMaterializer }
import uk.gov.hmrc.channelpreferences.hub.cds.model.{ Channel, Email, EmailVerification }
import play.api.http.Status.{ BAD_GATEWAY, BAD_REQUEST, OK, SERVICE_UNAVAILABLE, UNAUTHORIZED }
import uk.gov.hmrc.emailaddress.EmailAddress

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ ExecutionContext, Future }

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
class PreferenceControllerSpec extends PlaySpec with ScalaFutures with MockitoSugar {

  implicit val mat: Materializer = NoMaterializer
  implicit val hc: HeaderCarrier = HeaderCarrier()
  private val emailVerification = EmailVerification(EmailAddress("some@email.com"), new DateTime(1987, 3, 20, 1, 2, 3))
  private val validEmailVerification = """{"address":"some@email.com","timestamp":"1987-03-20T01:02:03.000Z"}"""

  val mockAuthConnector: AuthConnector = mock[AuthConnector]

  "Calling preference" should {
    "return a Bad Gateway for unexpected error status" in {
      val controller = new PreferenceController(
        new CdsPreference {
          override def getPreference(c: Channel, enrolmentKey: String, taxIdName: String, taxIdValue: String)(
            implicit hc: HeaderCarrier,
            ec: ExecutionContext): Future[Either[Int, EmailVerification]] =
            Future.successful(Left(SERVICE_UNAVAILABLE))
        },
        mockAuthConnector,
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
        mockAuthConnector,
        Helpers.stubControllerComponents()
      )

      val response = controller.preference(Email, "", "", "").apply(FakeRequest("GET", "/"))
      status(response) mustBe OK
      contentAsString(response) mustBe validEmailVerification
    }
  }
  "Calling itsa activation stub endpoint " should {
    """return OK for any "non-magic" entityId""" in new TestSetup {
      val postData: JsValue = Json.parse(s"""{"entityId": "00000","itsaId": "itsa-id"}""")
      val fakePostRequest = FakeRequest("POST", "", Headers("Content-Type" -> "application/json"), postData)
      val response = controller.confirm().apply(fakePostRequest)
      status(response) mustBe OK
    }
    """return CONFLICT for a "magic" entityId""" in new TestSetup {
      val postData: JsValue =
        Json.parse(s"""{"entityId": "450262a0-1842-4885-8fa1-6fbc2aeb867d","itsaId": "itsa-id"}""")
      val fakePostRequest = FakeRequest("POST", "", Headers("Content-Type" -> "application/json"), postData)
      val response = controller.confirm().apply(fakePostRequest)
      status(response) mustBe CONFLICT
    }
  }
  "Calling Agent Enrolment Stub endpoint " should {
    """return OK for any AgentReferenceNumber(ARN) and itsaId""" in new TestSetup {
      when(
        mockAuthConnector.authorise[Option[AffinityGroup]](any[Predicate](), any[Retrieval[Option[AffinityGroup]]]())(
          any[HeaderCarrier](),
          any[ExecutionContext]()))
        .thenReturn(Future.successful(Some(AffinityGroup.Agent)))

      val postData: JsValue = Json.parse(s"""
                                            |{
                                            |  "arn": "testARN",
                                            |  "itsaId": "testItsaId",
                                            |  "nino": "SB000003A",
                                            |  "sautr": "1234567890"
                                            |}
      """.stripMargin)

      val fakePostRequest = FakeRequest("POST", "", Headers("Content-Type" -> "application/json"), postData)
      val response = controller.enrolment().apply(fakePostRequest)
      status(response) mustBe OK
    }

    """return 400 for missing AgentReferenceNumber(ARN) and itsaId""" in new TestSetup {
      when(
        mockAuthConnector.authorise[Option[AffinityGroup]](any[Predicate](), any[Retrieval[Option[AffinityGroup]]]())(
          any[HeaderCarrier](),
          any[ExecutionContext]()))
        .thenReturn(Future.successful(Some(AffinityGroup.Agent)))

      val postData: JsValue = Json.obj()

      val fakePostRequest = FakeRequest("POST", "", Headers("Content-Type" -> "application/json"), postData)
      val response = controller.enrolment().apply(fakePostRequest)
      status(response) mustBe BAD_REQUEST
    }
    """return 400 for missing sautr""" in new TestSetup {
      when(
        mockAuthConnector.authorise[Option[AffinityGroup]](any[Predicate](), any[Retrieval[Option[AffinityGroup]]]())(
          any[HeaderCarrier](),
          any[ExecutionContext]()))
        .thenReturn(Future.successful(Some(AffinityGroup.Agent)))

      val postData: JsValue = Json.parse(s"""
                                            |{
                                            |  "arn": "testARN",
                                            |  "itsaId": "testItsaId",
                                            |  "nino": "SB000003A"
                                            |}
      """.stripMargin)

      val fakePostRequest = FakeRequest("POST", "", Headers("Content-Type" -> "application/json"), postData)
      val response = controller.enrolment().apply(fakePostRequest)
      status(response) mustBe BAD_REQUEST
    }
    """return 400 for missing nino""" in new TestSetup {
      when(
        mockAuthConnector.authorise[Option[AffinityGroup]](any[Predicate](), any[Retrieval[Option[AffinityGroup]]]())(
          any[HeaderCarrier](),
          any[ExecutionContext]()))
        .thenReturn(Future.successful(Some(AffinityGroup.Agent)))

      val postData: JsValue = Json.parse(s"""
                                            |{
                                            |  "arn": "testARN",
                                            |  "itsaId": "testItsaId",
                                            |  "sautr": "1234567890"
                                            |}
      """.stripMargin)

      val fakePostRequest = FakeRequest("POST", "", Headers("Content-Type" -> "application/json"), postData)
      val response = controller.enrolment().apply(fakePostRequest)
      status(response) mustBe BAD_REQUEST
    }
    """Check for UnAuthorisation for the AffinityGroup other than Agent""" in new TestSetup {
      when(
        mockAuthConnector.authorise[Option[AffinityGroup]](any[Predicate](), any[Retrieval[Option[AffinityGroup]]]())(
          any[HeaderCarrier](),
          any[ExecutionContext]()))
        .thenReturn(Future.failed(AuthorisationException.fromString("UnsupportedAffinityGroup")))

      val postData: JsValue = Json.parse(s"""
                                            |{
                                            |  "arn": "testARN",
                                            |  "itsaId": "testItsaId",
                                            |  "nino": "SB000003A",
                                            |  "sautr": "1234567890"
                                            |}
      """.stripMargin)

      val fakePostRequest = FakeRequest("POST", "", Headers("Content-Type" -> "application/json"), postData)
      val response = controller.enrolment().apply(fakePostRequest)
      status(response) mustBe UNAUTHORIZED
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
      mockAuthConnector,
      Helpers.stubControllerComponents()
    )

  }

}
