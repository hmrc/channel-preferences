/*
 * Copyright 2023 HM Revenue & Customs
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

import cats.syntax.either._
import akka.http.scaladsl.model.StatusCodes
import akka.stream.Materializer
import akka.stream.testkit.NoMaterializer
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.{ any, anyString }
import org.mockito.ArgumentMatchersSugar.*
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
import play.api.http.Status.{ BAD_GATEWAY, BAD_REQUEST, CREATED, NOT_FOUND, NOT_IMPLEMENTED, OK, SERVICE_UNAVAILABLE, UNAUTHORIZED }
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{ Retrieval, ~ }
import uk.gov.hmrc.channelpreferences.model.cds.{ Channel, Email, EmailVerification, Phone }
import uk.gov.hmrc.channelpreferences.model.eis.ItsaETMPUpdate
import uk.gov.hmrc.channelpreferences.model.preferences.EnrolmentKey.CustomsServiceKey
import uk.gov.hmrc.channelpreferences.model.preferences.IdentifierKey.EORINumber
import uk.gov.hmrc.channelpreferences.model.preferences.PreferenceError.{ ParseError, UnsupportedChannelError, UpstreamError }
import uk.gov.hmrc.channelpreferences.model.preferences.{ EnrolmentKey, Event, IdentifierKey, IdentifierValue, PreferencesConnectorError, UnExpectedError }
import uk.gov.hmrc.channelpreferences.services.eis.EISContactPreference
import uk.gov.hmrc.channelpreferences.services.entityresolver.EntityResolver
import uk.gov.hmrc.channelpreferences.services.preferences.{ PreferenceService, ProcessEmail }
import uk.gov.hmrc.channelpreferences.utils.emailaddress.EmailAddress
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ ExecutionContext, Future }

class PreferenceControllerSpec extends PlaySpec with ScalaCheckPropertyChecks with ScalaFutures with MockitoSugar {

  implicit val mat: Materializer = NoMaterializer
  implicit val hc: HeaderCarrier = HeaderCarrier()
  private val emailVerification = EmailVerification(EmailAddress("some@email.com"), new DateTime(1987, 3, 20, 1, 2, 3))
  private val validEmailVerification = """{"address":"some@email.com","timestamp":"1987-03-20T01:02:03.000Z"}"""

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockEntityResolver: EntityResolver = mock[EntityResolver]
  val mockEISContactPreference: EISContactPreference = mock[EISContactPreference]
  val mockProcessEmail: ProcessEmail = mock[ProcessEmail]
  val mockAuditConnector: AuditConnector = mock[AuditConnector]
  val preferenceService: PreferenceService = mock[PreferenceService]

  "Calling preference" should {
    "return a BAD GATEWAY (502) when get preference returns an unexpected error status" in {
      val controller = new PreferenceController(
        preferenceService,
        mockEntityResolver,
        mockEISContactPreference,
        mockProcessEmail,
        mockAuthConnector,
        mockAuditConnector,
        Helpers.stubControllerComponents()
      )

      when(
        preferenceService.getChannelPreference(*[EnrolmentKey], *[IdentifierKey], *[IdentifierValue], *[Channel])(
          *[HeaderCarrier],
          *[ExecutionContext]))
        .thenReturn(Future.successful(ParseError("boom").asLeft))

      val response =
        controller.preference(CustomsServiceKey, EORINumber, IdentifierValue(""), Email).apply(FakeRequest("GET", "/"))
      status(response) mustBe BAD_GATEWAY
    }

    "return NotFound when get preference returns a 404 error status" in {
      val controller = new PreferenceController(
        preferenceService,
        mockEntityResolver,
        mockEISContactPreference,
        mockProcessEmail,
        mockAuthConnector,
        mockAuditConnector,
        Helpers.stubControllerComponents()
      )

      when(
        preferenceService.getChannelPreference(*[EnrolmentKey], *[IdentifierKey], *[IdentifierValue], *[Channel])(
          *[HeaderCarrier],
          *[ExecutionContext]))
        .thenReturn(Future.successful(UpstreamError("boom", StatusCodes.NotFound).asLeft))

      val response =
        controller.preference(CustomsServiceKey, EORINumber, IdentifierValue(""), Email).apply(FakeRequest("GET", "/"))
      status(response) mustBe NOT_FOUND
    }

    "return NotImplemented when get preference returns a 501 error status" in {
      val controller = new PreferenceController(
        preferenceService,
        mockEntityResolver,
        mockEISContactPreference,
        mockProcessEmail,
        mockAuthConnector,
        mockAuditConnector,
        Helpers.stubControllerComponents()
      )

      when(
        preferenceService.getChannelPreference(*[EnrolmentKey], *[IdentifierKey], *[IdentifierValue], *[Channel])(
          *[HeaderCarrier],
          *[ExecutionContext]))
        .thenReturn(Future.successful(UnsupportedChannelError(Phone).asLeft))

      val response =
        controller.preference(CustomsServiceKey, EORINumber, IdentifierValue(""), Email).apply(FakeRequest("GET", "/"))
      status(response) mustBe NOT_IMPLEMENTED
    }

    "return OK (200) with the email verification if found" in {
      val controller = new PreferenceController(
        preferenceService,
        mockEntityResolver,
        mockEISContactPreference,
        mockProcessEmail,
        mockAuthConnector,
        mockAuditConnector,
        Helpers.stubControllerComponents()
      )

      when(
        preferenceService.getChannelPreference(*[EnrolmentKey], *[IdentifierKey], *[IdentifierValue], *[Channel])(
          *[HeaderCarrier],
          *[ExecutionContext]))
        .thenReturn(Future.successful(Json.toJson(emailVerification).asRight))

      val response =
        controller.preference(CustomsServiceKey, EORINumber, IdentifierValue(""), Email).apply(FakeRequest("GET", "/"))
      status(response) mustBe OK
      contentAsString(response) mustBe validEmailVerification
    }
  }

  "Calling itsa activation stub endpoint " should {

    "Forward the result form the entity-resolver" in new TestSetup with ConfirmGenerator {
      forAll(entityIgGen, itsaIdGen, httpResponseGen) { (entityId, itsaId, httpResponse) =>
        when(mockEntityResolver.confirm(anyString(), anyString())(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.successful(httpResponse))
        val retrievals = new ~(Some("somesautr"), Some("somenino"))
        when(
          mockAuthConnector.authorise[~[Option[String], Option[String]]](
            any[Predicate],
            any[Retrieval[~[Option[String], Option[String]]]])(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.successful(retrievals))

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
      "is not ok" in new TestSetup with EnrolmentGenerator {
      forAll(agentArnGen, ninoGen, sautrGen, itsaIdGen, httpResponseGen) {
        (agentArn, nino, sautr, itsaId, httpResponse) =>
          when(mockEntityResolver.enrolment(any[JsValue]())(any[HeaderCarrier], any[ExecutionContext]))
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
      val itsaId = "MTD-IT~MTDITID~XMIT983509385093485"
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

      when(mockEntityResolver.enrolment(any[JsValue]())(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(httpResponse))
      private val successBody: JsObject = Json.obj("processingDate" -> "2021-09-07T14:39:51.507Z", "status" -> "OK")
      when(mockEISContactPreference.updateContactPreference(anyString(), any[ItsaETMPUpdate], any[Option[String]]()))
        .thenReturn(Future.successful(HttpResponse(OK, successBody, Map[String, Seq[String]]())))

      val expectedResponseBody = Json.obj("reason" -> "ITSA ID successfully added")
      val postData: JsValue = Json.obj("arn"       -> agentArn, "nino" -> nino, "sautr" -> sautr, "itsaId" -> itsaId)
      val fakePostRequest = FakeRequest("POST", "", Headers("Content-Type" -> "application/json"), postData)
      val response = controller.enrolment().apply(fakePostRequest)
      status(response) mustBe httpResponse.status
      contentAsJson(response) mustBe expectedResponseBody

    }

    "forward ETMP failure when the call fails" in new TestSetup {
      val agentArn = "agent"
      val nino = "nino"
      val sautr = "sautr"
      val itsaId = "MTD-IT~MTDITID~XMIT983509385093485"
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

      when(mockEntityResolver.enrolment(any[JsValue]())(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(httpResponse))
      private val failureBody: JsObject = Json.obj("failure" -> "some error")
      private val etmpHttpResponse: HttpResponse = HttpResponse(BAD_REQUEST, failureBody, Map[String, Seq[String]]())
      when(mockEISContactPreference.updateContactPreference(anyString(), any[ItsaETMPUpdate], any[Option[String]]()))
        .thenReturn(Future.successful(etmpHttpResponse))

      val postData: JsValue = Json.obj("arn" -> agentArn, "nino" -> nino, "sautr" -> sautr, "itsaId" -> itsaId)
      val fakePostRequest = FakeRequest("POST", "", Headers("Content-Type" -> "application/json"), postData)
      val response = controller.enrolment().apply(fakePostRequest)
      status(response) mustBe etmpHttpResponse.status
      contentAsJson(response) mustBe failureBody

    }

    "update ETMP when the entity resolver return a digital customer and email not verified" in new TestSetup {
      val agentArn = "agent"
      val nino = "nino"
      val sautr = "sautr"
      val itsaId = "MTD-IT~MTDITID~XMIT983509385093485"
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

      when(mockEntityResolver.enrolment(any[JsValue]())(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(httpResponse))
      private val successBody: JsObject = Json.obj("processingDate" -> "2021-09-07T14:39:51.507Z", "status" -> "OK")
      when(mockEISContactPreference.updateContactPreference(anyString(), any[ItsaETMPUpdate], any[Option[String]]()))
        .thenReturn(Future.successful(HttpResponse(OK, successBody, Map[String, Seq[String]]())))

      val expectedResponseBody = Json.obj("reason" -> "ITSA ID successfully added")
      val postData: JsValue = Json.obj("arn"       -> agentArn, "nino" -> nino, "sautr" -> sautr, "itsaId" -> itsaId)
      val fakePostRequest = FakeRequest("POST", "", Headers("Content-Type" -> "application/json"), postData)
      val response = controller.enrolment().apply(fakePostRequest)
      status(response) mustBe httpResponse.status
      contentAsJson(response) mustBe expectedResponseBody

    }

    "update ETMP when the entity resolver return a digital customer that has email bounces" in new TestSetup {
      val agentArn = "agent"
      val nino = "nino"
      val sautr = "sautr"
      val itsaId = "MTD-IT~MTDITID~XMIT983509385093485"
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

      when(mockEntityResolver.enrolment(any[JsValue]())(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(httpResponse))
      private val successBody: JsObject = Json.obj("processingDate" -> "2021-09-07T14:39:51.507Z", "status" -> "OK")
      when(mockEISContactPreference.updateContactPreference(anyString(), any[ItsaETMPUpdate], any[Option[String]]()))
        .thenReturn(Future.successful(HttpResponse(OK, successBody, Map[String, Seq[String]]())))

      val expectedResponseBody = Json.obj("reason" -> "ITSA ID successfully added")
      val postData: JsValue = Json.obj("arn"       -> agentArn, "nino" -> nino, "sautr" -> sautr, "itsaId" -> itsaId)
      val fakePostRequest = FakeRequest("POST", "", Headers("Content-Type" -> "application/json"), postData)
      val response = controller.enrolment().apply(fakePostRequest)
      status(response) mustBe httpResponse.status
      contentAsJson(response) mustBe expectedResponseBody

    }

    "update ETMP when the entity resolver return a non digital customer" in new TestSetup {
      val agentArn = "agent"
      val nino = "nino"
      val sautr = "sautr"
      val itsaId = "MTD-IT~MTDITID~XMIT983509385093485"
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

      when(mockEntityResolver.enrolment(any[JsValue]())(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(httpResponse))
      private val successBody: JsObject = Json.obj("processingDate" -> "2021-09-07T14:39:51.507Z", "status" -> "OK")
      when(mockEISContactPreference.updateContactPreference(anyString(), any[ItsaETMPUpdate], any[Option[String]]()))
        .thenReturn(Future.successful(HttpResponse(OK, successBody, Map[String, Seq[String]]())))

      val expectedResponseBody = Json.obj("reason" -> "ITSA ID successfully added")
      val postData: JsValue = Json.obj("arn"       -> agentArn, "nino" -> nino, "sautr" -> sautr, "itsaId" -> itsaId)
      val fakePostRequest = FakeRequest("POST", "", Headers("Content-Type" -> "application/json"), postData)
      val response = controller.enrolment().apply(fakePostRequest)
      status(response) mustBe httpResponse.status
      contentAsJson(response) mustBe expectedResponseBody

    }

    "not update ETMP when the invalid itsa enrolment id found" in new TestSetup {
      val postData: JsValue =
        Json.obj("arn" -> "agent", "nino" -> "nino", "sautr" -> "sautr", "itsaId" -> "Invalid~XMIT983509385093485")
      val fakePostRequest = FakeRequest("POST", "", Headers("Content-Type" -> "application/json"), postData)
      val response = controller.enrolment().apply(fakePostRequest)
      status(response) mustBe UNAUTHORIZED
    }

    "NOT update ETMP when the entity resolver when the preferences is not found" in new TestSetup {
      val agentArn = "agent"
      val nino = "nino"
      val sautr = "sautr"
      val itsaId = "MTD-IT~MTDITID~XMIT983509385093485"
      val entityResolverResponseBody =
        Json.obj(
          "reason" -> "No preferences found"
        )
      val httpResponse =
        HttpResponse(UNAUTHORIZED, entityResolverResponseBody, Map.empty[String, Seq[String]])

      when(mockEntityResolver.enrolment(any[JsValue]())(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(httpResponse))
      private val successBody: JsObject = Json.obj("processingDate" -> "2021-09-07T14:39:51.507Z", "status" -> "OK")
      when(mockEISContactPreference.updateContactPreference(anyString(), any[ItsaETMPUpdate], any[Option[String]]()))
        .thenReturn(Future.successful(HttpResponse(OK, successBody, Map[String, Seq[String]]())))

      val postData: JsValue = Json.obj("arn" -> agentArn, "nino" -> nino, "sautr" -> sautr, "itsaId" -> itsaId)
      val fakePostRequest = FakeRequest("POST", "", Headers("Content-Type" -> "application/json"), postData)
      val response = controller.enrolment().apply(fakePostRequest)
      status(response) mustBe UNAUTHORIZED
      contentAsJson(response) mustBe entityResolverResponseBody

    }

    "NOT update ETMP when the entity resolver fails" in new TestSetup {
      val agentArn = "agent"
      val nino = "nino"
      val sautr = "sautr"
      val itsaId = "MTD-IT~MTDITID~XMIT983509385093485"
      val entityResolverResponseBody =
        Json.obj(
          "reason" -> "random one"
        )
      val httpResponse =
        HttpResponse(UNAUTHORIZED, entityResolverResponseBody, Map.empty[String, Seq[String]])

      when(mockEntityResolver.enrolment(any[JsValue]())(any[HeaderCarrier], any[ExecutionContext]))
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
          preferenceService,
          mockEntityResolver,
          mockEISContactPreference,
          mockProcessEmail,
          mockAuthConnector,
          mockAuditConnector,
          Helpers.stubControllerComponents()
        )

      val result = preferenceController.processBounce().apply(fakeProcessBounce)
      contentAsString(result) mustBe "Email bounce processed successfully"
    }

    "return NotModified when processEmail returns error" in new TestSetup {
      when(mockProcessEmail.process(any[Event]))
        .thenReturn(Future.successful(Left(PreferencesConnectorError("error from preferences"))))
      val preferenceController =
        new PreferenceController(
          preferenceService,
          mockEntityResolver,
          mockEISContactPreference,
          mockProcessEmail,
          mockAuthConnector,
          mockAuditConnector,
          Helpers.stubControllerComponents()
        )

      val result = preferenceController.processBounce().apply(fakeProcessBounce)
      status(result) mustBe Status.NOT_MODIFIED
    }

    "return InternalServerError when processEmail returns unexpected error" in new TestSetup {
      when(mockProcessEmail.process(any[Event])).thenReturn(Future.failed(UnExpectedError()))
      val preferenceController =
        new PreferenceController(
          preferenceService,
          mockEntityResolver,
          mockEISContactPreference,
          mockProcessEmail,
          mockAuthConnector,
          mockAuditConnector,
          Helpers.stubControllerComponents()
        )

      val result = preferenceController.processBounce().apply(fakeProcessBounce)
      status(result) mustBe Status.INTERNAL_SERVER_ERROR
    }

  }

  "Calling update contact" should {
    "return OK when the call to EIS succeed" in new TestSetup {
      private val successBody: JsObject = Json.obj("processingDate" -> "2021-09-07T14:39:51.507Z", "status" -> "OK")
      when(mockEISContactPreference.updateContactPreference(anyString(), any[ItsaETMPUpdate], any[Option[String]]()))
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
      when(mockEISContactPreference.updateContactPreference(anyString(), any[ItsaETMPUpdate], any[Option[String]]()))
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
    val preferenceService: PreferenceService = mock[PreferenceService]

    when(
      preferenceService.getChannelPreference(*[EnrolmentKey], *[IdentifierKey], *[IdentifierValue], *[Channel])(
        *[HeaderCarrier],
        *[ExecutionContext]))
      .thenReturn(Future.successful(UpstreamError("boom", StatusCodes.ServiceUnavailable).asLeft))

    val controller = new PreferenceController(
      preferenceService,
      mockEntityResolver,
      mockEISContactPreference,
      mockProcessEmail,
      mockAuthConnector,
      mockAuditConnector,
      Helpers.stubControllerComponents()
    )

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
          "enrolment" -> "MTD-IT~MTDITID~XMIT983509385093485",
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
