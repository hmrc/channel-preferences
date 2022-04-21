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

package uk.gov.hmrc.channelpreferences

import play.api.http.Status
import uk.gov.hmrc.channelpreferences.controllers.model.ContextPayload

class ContextApiISpec extends ISpec {

  "POST to /channel-preferences/context endpoint" should {
    "work with valid payload" in {
      val postData = s"""
                        {
                        |  "contextId" : "HMRC-CUS-ORG~EORINumber~GB123456789",
                        |  "expiry" : "1987-03-20T14:33:48.00064",
                        |  "context" : {
                        |    "consentType" : "Default",
                        |    "status" : true,
                        |    "updated" : "1987-03-20T14:33:48.000640Z",
                        |    "version" : {
                        |      "major" : 1,
                        |      "minor" : 1,
                        |      "patch" : 1
                        |    },
                        |    "purposes" : [ "DigitalCommunications" ]
                        |  }
                        |}
      """.stripMargin
      val response =
        wsClient
          .url(resource("/channel-preferences/context"))
          .withHttpHeaders(("Content-Type" -> "application/json"))
          .post(postData)
          .futureValue

      response.status mustBe Status.CREATED
    }
  }

  "PUT to /channel-preferences/context/:key endpoint" should {
    "work with valid payload" in {
      val putData = s"""
                       {
                       |  "contextId" : "HMRC-CUS-ORG~EORINumber~GB123456789",
                       |  "expiry" : "1987-03-20T14:33:48.00064",
                       |  "context" : {
                       |    "consentType" : "Default",
                       |    "status" : true,
                       |    "updated" : "1987-03-20T14:33:48.000640Z",
                       |    "version" : {
                       |      "major" : 1,
                       |      "minor" : 1,
                       |      "patch" : 1
                       |    },
                       |    "purposes" : [ "DigitalCommunications" ]
                       |  }
                       |}
      """.stripMargin
      val response =
        wsClient
          .url(resource(s"/channel-preferences/context/61ea7c5951d7a42da4fd4608"))
          .withHttpHeaders(("Content-Type" -> "application/json"))
          .put(putData)
          .futureValue

      response.status mustBe Status.OK
    }
  }

  "GET to /channel-preferences/context/:key endpoint" should {
    "return a context" in {
      val response =
        wsClient
          .url(resource(s"/channel-preferences/context/HMRC-CUS-ORG~EORINumber~GB123456789"))
          .withHttpHeaders(("Content-Type" -> "application/json"))
          .get
          .futureValue

      response.status mustBe Status.OK
      response.json.validate[ContextPayload].get.contextId.value mustBe "HMRC-CUS-ORG~EORINumber~GB123456789"
    }
  }

  "DELETE to /channel-preferences/context/:key endpoint" should {
    "return accepted response" in {
      val response =
        wsClient
          .url(resource(s"/channel-preferences/context/HMRC-CUS-ORG~EORINumber~GB123456789"))
          .withHttpHeaders(("Content-Type" -> "application/json"))
          .delete
          .futureValue

      response.status mustBe Status.ACCEPTED
    }
  }

}
