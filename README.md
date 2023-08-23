# Channel preferences microservice
Micro-service responsible for providing an API to CDS services.

# Table of Contents
- [Channel preferences Microservice](#channel-preferences-microservice)
- [API](#api)
    - [Description](#description)
    - [Endpoints](#endpoints)
        - [POST /channel-preferences/enrolment](#post-channel-preferencesenrolment)
        - [POST /channel-preferences/confirm](#post-channel-preferencesconfirm)

# API

- Link to OpenApi definitions: [schema](https://github.com/hmrc/channel-preferences/blob/public/schema.json)

## Description
| Path                                       | Supported Methods | Description                                                                                                                          |
| -------------------------------------------| ----------------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| ```/channel-preferences/enrolment```       | POST              | Allow agents to associate ITSA id's to existing customers preferences [More...](#post-channel-preferencesenrolment)                  |
| ```/channel-preferences/confirm```         | POST              | Confirm and add ITSA enrolments to preferences [More...](#post-channel-preferencesconfirm)                                           |

## Endpoints
### POST /channel-preferences/enrolment

It allows agent to link ITSA ID to existing preferences whose `nino` and `sautr` are passed as a parameters on user's behalf.

| Name           | Description                                                                    |
| -------------- | ------------------------------------------------------------------------------ |
| `arn`          | Agent Id The Id of the agent performing the auto-enrolment                     |
| `itsaId`       | The enrolment of the customer being auto-enrolled                              |
| `nino`         | The NINO of the customer being auto-enrolled                                   |
| `sautr`        | The SAUTR enrolment of the customer being auto-enrolled                        |

#### Example request :

```json
{
  "arn": "TARN0000001",
  "itsaId": "HMRC-MTD-IT~MTDITID~XTIT00000003773",
  "nino": "AB202101A",
  "sautr": "IR-SA~SA-UTR~9999999999"
}
```

Responds with status:

* `200` if there is a work item returned
```json
{
  "processingDate":"2021-09-07T14:39:51.507Z",
  "status":"OK"
}
```
* `401` if no work items to return
```json
{
  "reason": "entityId already has a different itsaId linked to it in entity resolver"
}
```
* `400` if no work items to return
```json
{
  "failures":[
    {
      "code":"INVALID_REGIME",
      "reason":"Submission has not passed validation. Invalid regime."
    },
    {
      "code":"INVALID_CORRELATIONID",
      "reason":"Submission has not passed validation. Invalid header CorrelationId."
    }
  ]
}
```
* `422` if no work items to return
```json
{
   "failures":[
      {
         "code":"INVALID_REGIME",
         "reason":"The remote endpoint has indicated that the REGIME provided is invalid.       "
      }
   ]
}
```
* `500` if no work items to return
```json
{
   "failures":[
      {
         "code":"SERVER_ERROR",
         "reason":"IF is currently experiencing problems that re‐quire live service intervention."
      }
   ]
}
```

### POST /channel-preferences/confirm

Confirm and add ITSA enrolments to preferences.

| Name           | Description                                                                    |
| -------------- | ------------------------------------------------------------------------------ |
| `entityId`     | This is the id provided when redirected back to the preference capture process |
| `itsaId`       | The ITSA MTD-ID that is stored in the enrolment                                |

#### Example request :

```json
{
  "entityId": "d44570b8-3d78-11ec-afe9-973d65d21fc5",
  "itsaId": "HMRC-MTD-IT~MTDITID~XTIT00000003773",
}
```

Responds with status:

* `200` if work item status changed successfully
* `401` if invalid request format
```json
{
  "reason": "SAUTR in Auth token is linked to a different entityId in entity resolver"
}
```

## Run the project locally

Ensure you have sm2 environment setup:

`sm2 --start DC_TWSM_ALL`

`sm2 --stop CHANNEL_PREFERENCES`

`sbt "run 9052 -Dplay.http.router=testOnlyDoNotUseInAppConf.Routes"`

## Run the tests and sbt fmt before raising a PR

Ensure you have sm2 environment setup:

Format:

`sbt fmt`

Then run the tests and coverage report:

`sbt clean coverage test coverageReport`

If your build fails due to poor test coverage, *DO NOT* lower the test coverage threshold, instead inspect the generated report located here on your local repo: `/target/scala-2.12/scoverage-report/index.html`

Then run the integration tests:

`sm2 --start DC_CHANNEL_PREFERENCES_IT`
`sbt it:test`
`sm2 --stop DC_CHANNEL_PREFERENCES_IT`

## Swagger endpoint

Available locally here: http://localhost:9052/assets/schema.json

# License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
