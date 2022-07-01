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

class ContextApiSpec extends ISpec {

  "POST to /channel-preferences/context endpoint" should {
    "work with valid payload" in {
      val postData = s"""
                        |{
                        |    "key": "61ea7c5951d7a42da4fd4608",
                        |    "resourcePath": "email",
                        |    "expiry": "2022-01-28T09:26:49.556Z",
                        |    "context": {
                        |        "consented": {
                        |            "consentType": "default",
                        |            "status": true,
                        |            "created": "2022-01-28T09:26:49.556Z",
                        |            "version": {
                        |                "major": 2,
                        |                "minor": 1,
                        |                "patch": 123
                        |            },
                        |            "purposes": [
                        |                "123",
                        |                "34",
                        |                "11"
                        |            ]
                        |        },
                        |        "verification": {
                        |            "id": "3cbcbfd1-71c8-49d6-905e-4eca464fd0a7",
                        |            "email": "test@test.com",
                        |            "sent": "2022-01-28T09:26:49.556Z"
                        |        },
                        |        "confirm": {
                        |            "started": "2022-01-28T09:26:49.556Z",
                        |            "id": "3cbcbfd1-71c8-49d6-905e-4eca464fd0a7"
                        |        }
                        |    }
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
                       |{
                       |    "key": "61ea7c5951d7a42da4fd4608",
                       |    "resourcePath": "email",
                       |    "expiry": "2022-01-28T09:26:49.556Z",
                       |    "context": {
                       |        "consented": {
                       |            "consentType": "default",
                       |            "status": true,
                       |            "created": "2022-01-28T09:26:49.556Z",
                       |            "version": {
                       |                "major": 2,
                       |                "minor": 1,
                       |                "patch": 123
                       |            },
                       |            "purposes": [
                       |                "123",
                       |                "34",
                       |                "11"
                       |            ]
                       |        },
                       |        "verification": {
                       |            "id": "3cbcbfd1-71c8-49d6-905e-4eca464fd0a7",
                       |            "email": "test@test.com",
                       |            "sent": "2022-01-28T09:26:49.556Z"
                       |        },
                       |        "confirm": {
                       |            "started": "2022-01-28T09:26:49.556Z",
                       |            "id": "3cbcbfd1-71c8-49d6-905e-4eca464fd0a7"
                       |        }
                       |    }
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
          .url(resource(s"/channel-preferences/context/61ea7c5951d7a42da4fd4608"))
          .withHttpHeaders(("Content-Type" -> "application/json"))
          .get
          .futureValue

      response.status mustBe Status.OK
      response.json.validate[ContextPayload].get.key mustBe "61ea7c5951d7a42da4fd4608"
    }
  }

  "DELETE to /channel-preferences/context/:key endpoint" should {
    "return accepted response" in {
      val response =
        wsClient
          .url(resource(s"/channel-preferences/context/61ea7c5951d7a42da4fd4608"))
          .withHttpHeaders(("Content-Type" -> "application/json"))
          .delete
          .futureValue

      response.status mustBe Status.ACCEPTED

    }
  }

}
