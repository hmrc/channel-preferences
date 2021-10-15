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
import akka.stream.testkit.NoMaterializer
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.{ any, anyString }
import org.mockito.Mockito._
import org.scalacheck.Gen
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.http.{ HeaderNames, Status }
import play.api.libs.json.{ JsObject, JsTrue, JsValue, Json }
import play.api.mvc.Headers
import play.api.test.Helpers.{ contentAsJson, contentAsString, defaultAwaitTimeout, status }
import uk.gov.hmrc.auth.core.AuthConnector
import play.api.test.{ FakeRequest, Helpers }
import play.api.http.Status.{ BAD_GATEWAY, BAD_REQUEST, CREATED, OK, SERVICE_UNAVAILABLE, UNAUTHORIZED }
import uk.gov.hmrc.channelpreferences.connectors.{ EISConnector, EntityResolverConnector }
import uk.gov.hmrc.channelpreferences.hub.cds.model.{ Channel, Email, EmailVerification }
import uk.gov.hmrc.channelpreferences.hub.cds.services.CdsPreference
import uk.gov.hmrc.channelpreferences.model.{ ItsaEnrolment, PreferencesConnectorError, UnExpectedError }
import uk.gov.hmrc.channelpreferences.preferences.model.Event
import uk.gov.hmrc.channelpreferences.preferences.services.ProcessEmail
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ ExecutionContext, Future }

class PreferenceControllerSpec extends PlaySpec with ScalaCheckPropertyChecks with ScalaFutures with MockitoSugar {

  implicit val mat: Materializer = NoMaterializer
  implicit val hc: HeaderCarrier = HeaderCarrier()
  private val emailVerification = EmailVerification(EmailAddress("some@email.com"), new DateTime(1987, 3, 20, 1, 2, 3))
  private val validEmailVerification = """{"address":"some@email.com","timestamp":"1987-03-20T01:02:03.000Z"}"""

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockEntityResolverConnector: EntityResolverConnector = mock[EntityResolverConnector]
  val mockEISConnector: EISConnector = mock[EISConnector]
  val mockProcessEmail: ProcessEmail = mock[ProcessEmail]

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
        mockEISConnector,
        mockProcessEmail,
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
        mockEISConnector,
        mockProcessEmail,
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
        when(mockEntityResolverConnector.confirm(anyString(), anyString())(any[HeaderCarrier]))
          .thenReturn(Future.successful(httpResponse))
        val postData: JsValue = Json.obj("entityId" -> entityId, "itsaId" -> itsaId)
        val fakePostRequest = FakeRequest("POST", "", Headers("Content-Type" -> "application/json"), postData)
        val response = controller.confirm().apply(fakePostRequest)
        status(response) mustBe httpResponse.status
        contentAsJson(response) mustBe Json.parse(httpResponse.body)
      }

    }
  }

  "Calling Agent Enrolment" should {

    "Not update ETMP and forward the result form the entity-resolver enrolment endpoint when the status" +
      "is not ok or the reason is not 'no preference found'" in new TestSetup with EnrolmentGenerator {
      forAll(agentArnGen, ninoGen, sautrGen, itsaIdGen, httpResponseGen) {
        (agentArn, nino, sautr, itsaId, httpResponse) =>
          when(mockEntityResolverConnector.enrolment(any[JsValue]())(any[HeaderCarrier]))
            .thenReturn(Future.successful(httpResponse))
          val postData: JsValue = Json.obj("arn" -> agentArn, "nino" -> nino, "sautr" -> sautr, "itsaId" -> itsaId)
          val fakePostRequest = FakeRequest("POST", "", Headers("Content-Type" -> "application/json"), postData)
          val response = controller.enrolment().apply(fakePostRequest)
          status(response) mustBe httpResponse.status
          contentAsJson(response) mustBe httpResponse.json
      }
    }

    "update ETMP when the entity resolver return a digital customer" in new TestSetup {
      val agentArn = "agent"
      val nino = "nino"
      val sautr = "sautr"
      val itsaId = "MTD-IT~MTDBSA~XMIT983509385093485"
      val entityResolverResponseBody =
        Json.obj(
          "reason" -> "Ok",
          "preference" ->
            Json.parse("""{
                         |    "digital": true,
                         |    "email": {
                         |        "status": "verified",
                         |        "mailboxFull": false,
                         |        "hasBounces": false,
                         |        "isVerified": true,
                         |        "email": "pihklyljtgoxeoh@mail.com"
                         |    }
                         |}""".stripMargin)
        )
      val httpResponse =
        HttpResponse(OK, entityResolverResponseBody, Map.empty[String, Seq[String]])

      when(mockEntityResolverConnector.enrolment(any[JsValue]())(any[HeaderCarrier]))
        .thenReturn(Future.successful(httpResponse))
      private val successBody: JsObject = Json.obj("processingDate" -> "2021-09-07T14:39:51.507Z", "status" -> "OK")
      when(mockEISConnector.updateContactPreference(anyString(), any[ItsaEnrolment], any[Option[String]]()))
        .thenReturn(Future.successful(HttpResponse(OK, successBody, Map[String, Seq[String]]())))

      val postData: JsValue = Json.obj("arn" -> agentArn, "nino" -> nino, "sautr" -> sautr, "itsaId" -> itsaId)
      val fakePostRequest = FakeRequest("POST", "", Headers("Content-Type" -> "application/json"), postData)
      val response = controller.enrolment().apply(fakePostRequest)
      status(response) mustBe httpResponse.status
      contentAsJson(response) mustBe successBody

    }

    "update ETMP when the entity resolver return a digital customer and email not verified" in new TestSetup {
      val agentArn = "agent"
      val nino = "nino"
      val sautr = "sautr"
      val itsaId = "MTD-IT~MTDBSA~XMIT983509385093485"
      val entityResolverResponseBody =
        Json.obj(
          "reason" -> "Ok",
          "preference" ->
            Json.parse("""{
                         |    "digital": true,
                         |    "email": {
                         |        "hasBounces": false,
                         |        "isVerified": false,
                         |        "email": "pihklyljtgoxeoh@mail.com"
                         |    }
                         |}""".stripMargin)
        )
      val httpResponse =
        HttpResponse(OK, entityResolverResponseBody, Map.empty[String, Seq[String]])

      when(mockEntityResolverConnector.enrolment(any[JsValue]())(any[HeaderCarrier]))
        .thenReturn(Future.successful(httpResponse))
      private val successBody: JsObject = Json.obj("processingDate" -> "2021-09-07T14:39:51.507Z", "status" -> "OK")
      when(mockEISConnector.updateContactPreference(anyString(), any[ItsaEnrolment], any[Option[String]]()))
        .thenReturn(Future.successful(HttpResponse(OK, successBody, Map[String, Seq[String]]())))

      val postData: JsValue = Json.obj("arn" -> agentArn, "nino" -> nino, "sautr" -> sautr, "itsaId" -> itsaId)
      val fakePostRequest = FakeRequest("POST", "", Headers("Content-Type" -> "application/json"), postData)
      val response = controller.enrolment().apply(fakePostRequest)
      status(response) mustBe httpResponse.status
      contentAsJson(response) mustBe successBody

    }

    "update ETMP when the entity resolver return a digital customer that has email bounces" in new TestSetup {
      val agentArn = "agent"
      val nino = "nino"
      val sautr = "sautr"
      val itsaId = "MTD-IT~MTDBSA~XMIT983509385093485"
      val entityResolverResponseBody =
        Json.obj(
          "reason" -> "Ok",
          "preference" ->
            Json.parse("""{
                         |    "digital": true,
                         |    "email": {
                         |        "hasBounces": true,
                         |        "isVerified": true,
                         |        "email": "pihklyljtgoxeoh@mail.com"
                         |    }
                         |}""".stripMargin)
        )
      val httpResponse =
        HttpResponse(OK, entityResolverResponseBody, Map.empty[String, Seq[String]])

      when(mockEntityResolverConnector.enrolment(any[JsValue]())(any[HeaderCarrier]))
        .thenReturn(Future.successful(httpResponse))
      private val successBody: JsObject = Json.obj("processingDate" -> "2021-09-07T14:39:51.507Z", "status" -> "OK")
      when(mockEISConnector.updateContactPreference(anyString(), any[ItsaEnrolment], any[Option[String]]()))
        .thenReturn(Future.successful(HttpResponse(OK, successBody, Map[String, Seq[String]]())))

      val postData: JsValue = Json.obj("arn" -> agentArn, "nino" -> nino, "sautr" -> sautr, "itsaId" -> itsaId)
      val fakePostRequest = FakeRequest("POST", "", Headers("Content-Type" -> "application/json"), postData)
      val response = controller.enrolment().apply(fakePostRequest)
      status(response) mustBe httpResponse.status
      contentAsJson(response) mustBe successBody

    }

    "update ETMP when the entity resolver return a non digital customer" in new TestSetup {
      val agentArn = "agent"
      val nino = "nino"
      val sautr = "sautr"
      val itsaId = "MTD-IT~MTDBSA~XMIT983509385093485"
      val entityResolverResponseBody =
        Json.obj(
          "reason" -> "Ok",
          "preference" ->
            Json.parse("""{
                         |    "digital": false
                         |}""".stripMargin)
        )
      val httpResponse =
        HttpResponse(OK, entityResolverResponseBody, Map.empty[String, Seq[String]])

      when(mockEntityResolverConnector.enrolment(any[JsValue]())(any[HeaderCarrier]))
        .thenReturn(Future.successful(httpResponse))
      private val successBody: JsObject = Json.obj("processingDate" -> "2021-09-07T14:39:51.507Z", "status" -> "OK")
      when(mockEISConnector.updateContactPreference(anyString(), any[ItsaEnrolment], any[Option[String]]()))
        .thenReturn(Future.successful(HttpResponse(OK, successBody, Map[String, Seq[String]]())))

      val postData: JsValue = Json.obj("arn" -> agentArn, "nino" -> nino, "sautr" -> sautr, "itsaId" -> itsaId)
      val fakePostRequest = FakeRequest("POST", "", Headers("Content-Type" -> "application/json"), postData)
      val response = controller.enrolment().apply(fakePostRequest)
      status(response) mustBe httpResponse.status
      contentAsJson(response) mustBe successBody

    }

    "update ETMP when the entity resolver when the preferences is not found" in new TestSetup {
      val agentArn = "agent"
      val nino = "nino"
      val sautr = "sautr"
      val itsaId = "MTD-IT~MTDBSA~XMIT983509385093485"
      val entityResolverResponseBody =
        Json.obj(
          "reason" -> "No preferences found"
        )
      val httpResponse =
        HttpResponse(UNAUTHORIZED, entityResolverResponseBody, Map.empty[String, Seq[String]])

      when(mockEntityResolverConnector.enrolment(any[JsValue]())(any[HeaderCarrier]))
        .thenReturn(Future.successful(httpResponse))
      private val successBody: JsObject = Json.obj("processingDate" -> "2021-09-07T14:39:51.507Z", "status" -> "OK")
      when(mockEISConnector.updateContactPreference(anyString(), any[ItsaEnrolment], any[Option[String]]()))
        .thenReturn(Future.successful(HttpResponse(OK, successBody, Map[String, Seq[String]]())))

      val postData: JsValue = Json.obj("arn" -> agentArn, "nino" -> nino, "sautr" -> sautr, "itsaId" -> itsaId)
      val fakePostRequest = FakeRequest("POST", "", Headers("Content-Type" -> "application/json"), postData)
      val response = controller.enrolment().apply(fakePostRequest)
      status(response) mustBe OK
      contentAsJson(response) mustBe successBody

    }

    "NOT update ETMP when the entity resolver fails" in new TestSetup {
      val agentArn = "agent"
      val nino = "nino"
      val sautr = "sautr"
      val itsaId = "MTD-IT~MTDBSA~XMIT983509385093485"
      val entityResolverResponseBody =
        Json.obj(
          "reason" -> "random one"
        )
      val httpResponse =
        HttpResponse(UNAUTHORIZED, entityResolverResponseBody, Map.empty[String, Seq[String]])

      when(mockEntityResolverConnector.enrolment(any[JsValue]())(any[HeaderCarrier]))
        .thenReturn(Future.successful(httpResponse))

      val postData: JsValue = Json.obj("arn" -> agentArn, "nino" -> nino, "sautr" -> sautr, "itsaId" -> itsaId)
      val fakePostRequest = FakeRequest("POST", "", Headers("Content-Type" -> "application/json"), postData)
      val response = controller.enrolment().apply(fakePostRequest)
      status(response) mustBe httpResponse.status
      contentAsJson(response) mustBe httpResponse.json

    }

  }

  "Calling processBounce endpoint to process an email bounce event" should {

    """return OK (200) if valid Json event valid""" in new TestSetup {
      val postData: JsValue = Json.parse(s"""
                                            |{
                                            |    "subject": "bounced-email",
                                            |    "eventId" : "77ed39b7-d5d8-46ed-abab-a5a8ff416dae",
                                            |    "groupId": "20180622211249.1.2A6098970A380E12@example.org",
                                            |    "timestamp" : "2021-04-07T09:46:29+00:00",
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
      when(mockProcessEmail.process(any[Event])).thenReturn(Future.successful(Right("")))

      val fakePostRequest = FakeRequest("POST", "", Headers("Content-Type" -> "application/json"), postData)
      val response = controller.processBounce().apply(fakePostRequest)
      status(response) mustBe OK
    }

    """return BAD REQUEST (400) when the payload has an invalid UUID as an [eventId] value""" in new TestSetup {
      val postData: JsValue = Json.parse(s"""
                                            |{
                                            |    "subject": "bounced-email",
                                            |    "eventId" : "invalid",
                                            |    "groupId": "20180622211249.1.2A6098970A380E12@example.org",
                                            |    "timestamp" : "2021-04-07T09:46:29+00:00",
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
      when(mockProcessEmail.process(any[Event])).thenReturn(Future.successful(Right("")))
      val fakePostRequest = FakeRequest("POST", "", Headers("Content-Type" -> "application/json"), postData)
      val response = controller.processBounce().apply(fakePostRequest)
      status(response) mustBe BAD_REQUEST
    }

    """return BAD REQUEST (400) when the payload has an invalid date format as a [timestamp] value""" in new TestSetup {
      val postData: JsValue = Json.parse(s"""
                                            |{
                                            |    "subject": "bounced-email",
                                            |    "eventId" : "77ed39b7-d5d8-46ed-abab-a5a8ff416dae",
                                            |    "groupId": "20180622211249.1.2A6098970A380E12@example.org",
                                            |    "timestamp" : "invalid",
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
      when(mockProcessEmail.process(any[Event])).thenReturn(Future.successful(Right("")))
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
                                            |    "timestamp" : "invalid",
                                            |    "event" : ""
                                            |}
    """.stripMargin)
      when(mockProcessEmail.process(any[Event])).thenReturn(Future.successful(Right("")))
      val fakePostRequest = FakeRequest("POST", "", Headers("Content-Type" -> "application/json"), postData)
      val response = controller.processBounce().apply(fakePostRequest)
      status(response) mustBe BAD_REQUEST
    }

    List(
      "subject",
      "eventId",
      "groupId",
      "timestamp",
      "event"
    ).foreach((fieldName: String) =>
      s"""return BAD REQUEST (400) when the [$fieldName] field is missing""" in new TestSetup {
        val postData: JsValue = Json.parse(s"""
                                              |{
                                              |    "subject": "bounced-email",
                                              |    "eventId" : "77ed39b7-d5d8-46ed-abab-a5a8ff416dae",
                                              |    "groupId": "20180622211249.1.2A6098970A380E12@example.org",
                                              |    "timestamp" : "2021-04-07T09:46:29+00:00",
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
      "timestamp"
    ).foreach((fieldName: String) =>
      s"""return BAD REQUEST (400) when the [$fieldName] field is empty""" in new TestSetup {
        val postData: JsValue = Json.parse(s"""
                                              |{
                                              |    "subject": "bounced-email",
                                              |    "eventId" : "77ed39b7-d5d8-46ed-abab-a5a8ff416dae",
                                              |    "groupId": "20180622211249.1.2A6098970A380E12@example.org",
                                              |    "timestamp" : "2021-04-07T09:46:29+00:00",
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

    "return OK(200) when processEmail returns success" in new TestSetup {
      when(mockProcessEmail.process(any[Event]))
        .thenReturn(Future.successful(Right("Email bounce processed successfully")))
      val preferenceController =
        new PreferenceController(
          mockCdsPreference,
          mockAuthConnector,
          mockEntityResolverConnector,
          mockEISConnector,
          mockProcessEmail,
          Helpers.stubControllerComponents())

      val result = preferenceController.processBounce().apply(fakeProcessBounce)
      contentAsString(result) mustBe "Email bounce processed successfully"
    }

    "return NotModified when processEmail returns error" in new TestSetup {
      when(mockProcessEmail.process(any[Event]))
        .thenReturn(Future.successful(Left(PreferencesConnectorError("error from preferences"))))
      val preferenceController =
        new PreferenceController(
          mockCdsPreference,
          mockAuthConnector,
          mockEntityResolverConnector,
          mockEISConnector,
          mockProcessEmail,
          Helpers.stubControllerComponents())

      val result = preferenceController.processBounce().apply(fakeProcessBounce)
      status(result) mustBe Status.NOT_MODIFIED
    }

    "return InternalServerError when processEmail returns unexpected error" in new TestSetup {
      when(mockProcessEmail.process(any[Event])).thenReturn(Future.failed(UnExpectedError()))
      val preferenceController =
        new PreferenceController(
          mockCdsPreference,
          mockAuthConnector,
          mockEntityResolverConnector,
          mockEISConnector,
          mockProcessEmail,
          Helpers.stubControllerComponents())

      val result = preferenceController.processBounce().apply(fakeProcessBounce)
      status(result) mustBe Status.INTERNAL_SERVER_ERROR
    }

  }

  "Calling update contact" should {
    "return OK when the call to EIS succeed" in new TestSetup {
      private val successBody: JsObject = Json.obj("processingDate" -> "2021-09-07T14:39:51.507Z", "status" -> "OK")
      when(mockEISConnector.updateContactPreference(anyString(), any[ItsaEnrolment], any[Option[String]]()))
        .thenReturn(Future.successful(HttpResponse(OK, successBody, Map[String, Seq[String]]())))

      val result = controller.update("itsa").apply(updateStatusRequest)
      status(result) mustBe OK
      contentAsJson(result) mustBe successBody
    }

    "return INTERNAL_SERVER_ERROR (500) when the call to EIS fails" in new TestSetup {
      val failureBody = Json.parse("""{
          "failures": [
              {
                "code": "INVALID_REGIME",
                "reason": "Submission has not passed validation. Invalid regime."
              }
          ]
          }""")
      when(mockEISConnector.updateContactPreference(anyString(), any[ItsaEnrolment], any[Option[String]]()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, failureBody, Map[String, Seq[String]]())))

      val result = controller.update("itsa").apply(updateStatusRequest)
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe failureBody
    }

    "return BAD_REQUEST when the key is not supported" in new TestSetup {

      private val unsupportedKey = "unsupportedKey"
      val result = controller.update(unsupportedKey).apply(updateStatusRequest)
      status(result) mustBe Status.BAD_REQUEST
      contentAsString(result) mustBe "The key unsupportedKey is not supported"
    }

    "return BAD_REQUEST when the enrolment is invalid" in new TestSetup {

      val invalidEnrolmentRequest =
        FakeRequest(
          "POST",
          "/channel-preferences/preference/itsa/status",
          Headers(HeaderNames.CONTENT_TYPE -> "application/json"),
          Json.obj(
            "enrolment" -> "invalid-enrolment",
            "status"    -> JsTrue
          )
        )
      val result = controller.update("itsa").apply(invalidEnrolmentRequest)
      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe "Invalid enrolment"
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
      mockEntityResolverConnector,
      mockEISConnector,
      mockProcessEmail,
      Helpers.stubControllerComponents()
    )

    val mockCdsPreference = mock[CdsPreference]

    val processBouncePayload: JsValue = Json.parse(s"""
                                                      |{
                                                      |    "subject": "bounced-email",
                                                      |    "eventId" : "77ed39b7-d5d8-46ed-abab-a5a8ff416dae",
                                                      |    "groupId": "20180622211249.1.2A6098970A380E12@example.org",
                                                      |    "timestamp" : "2021-04-07T09:46:29+00:00",
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
    when(mockProcessEmail.process(any[Event]))
      .thenReturn(Future.successful(Right("Email bounce processed successfully")))

    val fakeProcessBounce = FakeRequest("POST", "", Headers("Content-Type" -> "application/json"), processBouncePayload)

    val updateStatusRequest =
      FakeRequest(
        "POST",
        "/channel-preferences/preference/itsa/status",
        Headers(HeaderNames.CONTENT_TYPE -> "application/json"),
        Json.obj(
          "enrolment" -> "MTD-IT~MTDBSA~XMIT983509385093485",
          "status"    -> JsTrue
        )
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

  trait EnrolmentGenerator {
    val agentArnGen: Gen[String] = Gen.choose(0, 9999999).map(number => s"ARN$number")
    val ninoGen: Gen[String] = Gen.choose(0, 100000).map(num => f"CE$num%06dD")
    val sautrGen: Gen[String] = Gen.choose(0, 100000).map(num => f"$num%09d")
    val itsaIdGen: Gen[String] = Gen.choose(0, 9999999).map(_.toString)
    val randomWordGen: Gen[String] = Gen.oneOf("foo", "bar", "baz", "fizz", "buzz", "toto", "tata")
    val httpResponseGen: Gen[HttpResponse] =
      for {
        status <- Gen.oneOf(BAD_GATEWAY, BAD_REQUEST, CREATED, SERVICE_UNAVAILABLE, UNAUTHORIZED)
        key    <- randomWordGen
        value  <- randomWordGen
        body = Json.obj(key -> value)
      } yield HttpResponse(status, body, Map.empty[String, Seq[String]])
  }

}
