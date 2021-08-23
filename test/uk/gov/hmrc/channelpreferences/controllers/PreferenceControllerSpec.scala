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
import org.joda.time.DateTime
import play.api.libs.json.{ JsObject, JsValue, Json }
import play.api.mvc.Headers
import uk.gov.hmrc.auth.core.{ AffinityGroup, AuthConnector, AuthorisationException }
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import org.mockito.ArgumentMatchers.{ any, anyString }
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalacheck.Gen
import play.api.test.Helpers.{ contentAsJson, contentAsString, defaultAwaitTimeout, status }
import uk.gov.hmrc.channelpreferences.hub.cds.services.CdsPreference
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }
import play.api.test.{ FakeRequest, Helpers, NoMaterializer }
import uk.gov.hmrc.channelpreferences.hub.cds.model.{ Channel, Email, EmailVerification }
import play.api.http.Status.{ BAD_GATEWAY, BAD_REQUEST, CREATED, OK, SERVICE_UNAVAILABLE, UNAUTHORIZED }
import uk.gov.hmrc.channelpreferences.connectors.EntityResolverConnector
import uk.gov.hmrc.emailaddress.EmailAddress

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ ExecutionContext, Future }

class PreferenceControllerSpec extends PlaySpec with ScalaCheckPropertyChecks with ScalaFutures with MockitoSugar {

  implicit val mat: Materializer = NoMaterializer
  implicit val hc: HeaderCarrier = HeaderCarrier()
  private val emailVerification = EmailVerification(EmailAddress("some@email.com"), new DateTime(1987, 3, 20, 1, 2, 3))
  private val validEmailVerification = """{"address":"some@email.com","timestamp":"1987-03-20T01:02:03.000Z"}"""

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockEntityResolverConnector: EntityResolverConnector = mock[EntityResolverConnector]

  "Calling preference" should {
    "return a BAD GATEWAY (502) for unexpected error status" in {
      val controller = new PreferenceController(
        new CdsPreference {
          override def getPreference(c: Channel, enrolmentKey: String, taxIdName: String, taxIdValue: String)(
            implicit hc: HeaderCarrier,
            ec: ExecutionContext): Future[Either[Int, EmailVerification]] =
            Future.successful(Left(SERVICE_UNAVAILABLE))
        },
        mockAuthConnector,
        mockEntityResolverConnector,
        Helpers.stubControllerComponents()
      )

      val response = controller.preference(Email, "", "", "").apply(FakeRequest("GET", "/"))
      status(response) mustBe BAD_GATEWAY
    }

    "return OK (200) with the email verification if found" in {
      val controller = new PreferenceController(
        new CdsPreference {
          override def getPreference(c: Channel, enrolmentKey: String, taxIdName: String, taxIdValue: String)(
            implicit hc: HeaderCarrier,
            ec: ExecutionContext): Future[Either[Int, EmailVerification]] =
            Future.successful(Right(emailVerification))
        },
        mockAuthConnector,
        mockEntityResolverConnector,
        Helpers.stubControllerComponents()
      )

      val response = controller.preference(Email, "", "", "").apply(FakeRequest("GET", "/"))
      status(response) mustBe OK
      contentAsString(response) mustBe validEmailVerification
    }
  }

  "Calling itsa activation stub endpoint " should {

    "Forward the result form the entity-resolver" in new TestSetup with ConfirmGenerator {
      forAll(entityIgGen, itsaIdGen, httpResponseGen) { (entityId, itsaId, httpResponse) =>
        when(mockEntityResolverConnector.confirm(anyString(), anyString()))
          .thenReturn(Future.successful(httpResponse))
        val postData: JsValue = Json.obj("entityId" -> entityId, "itsaId" -> itsaId)
        val fakePostRequest = FakeRequest("POST", "", Headers("Content-Type" -> "application/json"), postData)
        val response = controller.confirm().apply(fakePostRequest)
        status(response) mustBe httpResponse.status
        contentAsJson(response) mustBe Json.parse(httpResponse.body)
      }

    }
  }

  "Calling Agent Enrolment Stub endpoint " should {

    """return OK (200) for any AgentReferenceNumber(ARN) and itsaId""" in new TestSetup {
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

    """return BAD REQUEST (400) for missing AgentReferenceNumber(ARN) and itsaId""" in new TestSetup {
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

    """return BAD REQUEST (400) for missing sautr""" in new TestSetup {
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

    """return BAD REQUEST (400) for missing nino""" in new TestSetup {
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

    """Check for UNAUTHORIZED (401) when the AffinityGroup does not match the Agent""" in new TestSetup {
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

  "Calling processBounce endpoint to process an email bounce event" should {

    """return CREATED (201) if valid Json event valid""" in new TestSetup {
      val postData: JsValue = Json.parse(s"""
                                            |{
                                            |    "subject": "bounced-email",
                                            |    "eventId" : "77ed39b7-d5d8-46ed-abab-a5a8ff416dae",
                                            |    "groupId": "20180622211249.1.2A6098970A380E12@example.org",
                                            |    "timeStamp" : "2021-04-07T09:46:29+00:00",
                                            |    "event" : {
                                            |        "event": "failed",
                                            |        "emailAddress": "hmrc-customer@some-domain.org",
                                            |        "detected": "2021-04-07T09:46:29+00:00",
                                            |        "code": 605,
                                            |        "reason": "Not delivering to previously bounced address",
                                            |        "enrolment": "HMRC-MTD-VAT~VRN~GB123456789"
                                            |    }
                                            |}
    """.stripMargin)

      val fakePostRequest = FakeRequest("POST", "", Headers("Content-Type" -> "application/json"), postData)
      val response = controller.processBounce().apply(fakePostRequest)
      status(response) mustBe CREATED
    }

    """return BAD REQUEST (400) when the payload has an invalid UUID as an [eventId] value""" in new TestSetup {
      val postData: JsValue = Json.parse(s"""
                                            |{
                                            |    "subject": "bounced-email",
                                            |    "eventId" : "invalid",
                                            |    "groupId": "20180622211249.1.2A6098970A380E12@example.org",
                                            |    "timeStamp" : "2021-04-07T09:46:29+00:00",
                                            |    "event" : {
                                            |        "event": "failed",
                                            |        "emailAddress": "hmrc-customer@some-domain.org",
                                            |        "detected": "2021-04-07T09:46:29+00:00",
                                            |        "code": 605,
                                            |        "reason": "Not delivering to previously bounced address",
                                            |        "enrolment": "HMRC-MTD-VAT~VRN~GB123456789"
                                            |    }
                                            |}
    """.stripMargin)

      val fakePostRequest = FakeRequest("POST", "", Headers("Content-Type" -> "application/json"), postData)
      val response = controller.processBounce().apply(fakePostRequest)
      status(response) mustBe BAD_REQUEST
    }

    """return BAD REQUEST (400) when the payload has an invalid date format as a [timeStamp] value""" in new TestSetup {
      val postData: JsValue = Json.parse(s"""
                                            |{
                                            |    "subject": "bounced-email",
                                            |    "eventId" : "77ed39b7-d5d8-46ed-abab-a5a8ff416dae",
                                            |    "groupId": "20180622211249.1.2A6098970A380E12@example.org",
                                            |    "timeStamp" : "invalid",
                                            |    "event" : {
                                            |        "event": "failed",
                                            |        "emailAddress": "hmrc-customer@some-domain.org",
                                            |        "detected": "2021-04-07T09:46:29+00:00",
                                            |        "code": 605,
                                            |        "reason": "Not delivering to previously bounced address",
                                            |        "enrolment": "HMRC-MTD-VAT~VRN~GB123456789"
                                            |    }
                                            |}
    """.stripMargin)

      val fakePostRequest = FakeRequest("POST", "", Headers("Content-Type" -> "application/json"), postData)
      val response = controller.processBounce().apply(fakePostRequest)
      status(response) mustBe BAD_REQUEST
    }

    """return BAD REQUEST (400) when the payload has an invalid [event] field format, other than a Json value""" in new TestSetup {
      val postData: JsValue = Json.parse(s"""
                                            |{
                                            |    "subject": "bounced-email",
                                            |    "eventId" : "77ed39b7-d5d8-46ed-abab-a5a8ff416dae",
                                            |    "groupId": "20180622211249.1.2A6098970A380E12@example.org",
                                            |    "timeStamp" : "invalid",
                                            |    "event" : ""
                                            |}
    """.stripMargin)

      val fakePostRequest = FakeRequest("POST", "", Headers("Content-Type" -> "application/json"), postData)
      val response = controller.processBounce().apply(fakePostRequest)
      status(response) mustBe BAD_REQUEST
    }

    List(
      "subject",
      "eventId",
      "groupId",
      "timeStamp",
      "event"
    ).foreach((fieldName: String) =>
      s"""return BAD REQUEST (400) when the [$fieldName] field is missing""" in new TestSetup {
        val postData: JsValue = Json.parse(s"""
                                              |{
                                              |    "subject": "bounced-email",
                                              |    "eventId" : "77ed39b7-d5d8-46ed-abab-a5a8ff416dae",
                                              |    "groupId": "20180622211249.1.2A6098970A380E12@example.org",
                                              |    "timeStamp" : "2021-04-07T09:46:29+00:00",
                                              |    "event" : {
                                              |        "event": "failed",
                                              |        "emailAddress": "hmrc-customer@some-domain.org",
                                              |        "detected": "2021-04-07T09:46:29+00:00",
                                              |        "code": 605,
                                              |        "reason": "Not delivering to previously bounced address",
                                              |        "enrolment": "HMRC-MTD-VAT~VRN~GB123456789"
                                              |    }
                                              |}
        """.stripMargin)

        val postDataWithoutField = postData.as[JsObject] - fieldName
        val fakePostRequestWithoutField =
          FakeRequest("POST", "", Headers("Content-Type" -> "application/json"), postDataWithoutField)
        val responseWithoutField = controller.processBounce().apply(fakePostRequestWithoutField)
        status(responseWithoutField) mustBe BAD_REQUEST
    })

    List(
      "subject",
      "eventId",
      "groupId",
      "timeStamp"
    ).foreach((fieldName: String) =>
      s"""return BAD REQUEST (400) when the [$fieldName] field is empty""" in new TestSetup {
        val postData: JsValue = Json.parse(s"""
                                              |{
                                              |    "subject": "bounced-email",
                                              |    "eventId" : "77ed39b7-d5d8-46ed-abab-a5a8ff416dae",
                                              |    "groupId": "20180622211249.1.2A6098970A380E12@example.org",
                                              |    "timeStamp" : "2021-04-07T09:46:29+00:00",
                                              |    "event" : {
                                              |        "event": "failed",
                                              |        "emailAddress": "hmrc-customer@some-domain.org",
                                              |        "detected": "2021-04-07T09:46:29+00:00",
                                              |        "code": 605,
                                              |        "reason": "Not delivering to previously bounced address",
                                              |        "enrolment": "HMRC-MTD-VAT~VRN~GB123456789"
                                              |    }
                                              |}
        """.stripMargin)

        val postDataWithEmptyField = postData.as[JsObject] ++ Json.obj(fieldName -> "")
        val fakePostRequestWithEmptyField =
          FakeRequest("POST", "", Headers("Content-Type" -> "application/json"), postDataWithEmptyField)
        val responseWithEmptyField = controller.processBounce().apply(fakePostRequestWithEmptyField)
        status(responseWithEmptyField) mustBe BAD_REQUEST
    })
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
      mockEntityResolverConnector,
      Helpers.stubControllerComponents()
    )

  }
  trait ConfirmGenerator {
    val entityIgGen: Gen[String] = Gen.uuid.map(_.toString)
    val itsaIdGen: Gen[String] = Gen.choose(0, 9999999).map(_.toString)
    val randomWordGen: Gen[String] = Gen.oneOf("foo", "bar", "baz", "fizz", "buzz", "toto", "tata")
    val httpResponseGen: Gen[HttpResponse] =
      for {
        status <- Gen.oneOf(BAD_GATEWAY, BAD_REQUEST, CREATED, OK, SERVICE_UNAVAILABLE, UNAUTHORIZED)
        key    <- randomWordGen
        value  <- randomWordGen
        body = Json.obj(key -> value)
      } yield HttpResponse(status, body, Map.empty[String, Seq[String]])
  }

}
