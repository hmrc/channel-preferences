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

### GET /channel-preferences/preferences/enrolments/HMRC-PODS-ORG~PSAID~GB123456789

Responds with status:

* `200` when a preference exists
```json
{
  "context": {
    "consentType": "Default",
    "status": true,
    "updated": "1987-03-20T14:33:48.000640Z",
    "version": {
      "major": 1,
      "minor": 1,
      "patch": 1
    },
    "purposes": [
      "DigitalCommunications"
    ]
  }
}
```

### PUT /channel-preferences/preferences/enrolments/PODSAdmin/consent

Create or update consent for an enrolment

| Name           | Description                                                                    |
| -------------- | ------------------------------------------------------------------------------ |
| `consentType`     | The type of the consent, current possible values: `Default`  |
| `status`       | Boolean indicating whether consented or not                                |
| `updated`       | Timestamp of when the consent was created or last updated                               |
| `version`       | The version of the consent                               |
| `purposes`       | A list of purposes this consent is valid for, possible values:  `DigitalCommunications`                             |


#### Example request :

```shell
 curl -H "Content-Type: application/json" -XPUT http://localhost:9052/channel-preferences/preferences/enrolments/PODSAdmin/consent -d '{
"consentType": "Default",
"status": true,
"updated": "1987-03-20T14:33:48.000640Z",
"version": {
"major": 1,
"minor": 1,
"patch": 1
},
"purposes": [
"DigitalCommunications"
]
}'

```

Responds with status:

* `201` when update succeeds
```json
{
  "context": {
    "consentType": "Default",
    "status": true,
    "updated": "1987-03-20T14:33:48.000640Z",
    "version": {
      "major": 1,
      "minor": 1,
      "patch": 1
    },
    "purposes": [
      "DigitalCommunications"
    ]
  }
}
```

### POST /channel-preferences/preferences/enrolments/PODSAdmin/channels/email/index/primary/verify

Create a new verification for the target channel (email)

| Name           | Description                                                                    |
| -------------- | ------------------------------------------------------------------------------ |
| `value`        |  the email address to create the verification for              |


#### Example request :

```shell
curl -H "Content-Type: application/json" -XPOST http://localhost:9052/channel-preferences/preferences/enrolments/HMRC-PODS-ORG~PSAID~GB123456789/channels/email/index/primary/verify -d '{
  "value": "test@test.com"
}'
```

Responds with status:

* `201` when the verification has been created
```json
{
  "context": {
    "consent": {
      "consentType": "Default",
      "status": true,
      "updated": "1987-03-20T14:33:48.000640Z",
      "version": {
        "major": 1,
        "minor": 1,
        "patch": 1
      },
      "purposes": [
        "DigitalCommunications"
      ]
    },
    "verification": {
      "id": "7b708a10-4b9b-4971-8073-436d67b39bd8",
      "email": "test@test.com",
      "sent": "1987-03-20T14:33:48.000640Z"
    }
  }
}
```

### POST /channel-preferences/preferences/verify/7b708a10-4b9b-4971-8073-436d67b39bd8/confirm

Confirms the email verification

```shell
curl -H "Content-Type: application/json" -XPUT http://localhost:9052/channel-preferences/preferences/verify/7b708a10-4b9b-4971-8073-436d67b39bd8/confirm
```

Responds with status:

* `201` when the verification has been created
```json
{
  "enrolments" : [ "HMRC-PODS-ORG~PSAID~GB123456789" ],
  "created" : "1987-03-20T14:33:48.000640Z",
  "consents" : [ {
    "consentType" : "Default",
    "status" : true,
    "updated" : "1987-03-20T14:33:48.000640Z",
    "version" : {
      "major" : 1,
      "minor" : 1,
      "patch" : 1
    },
    "purposes" : [ "DigitalCommunications" ]
  } ],
  "emailPreferences" : [ {
    "index" : "Primary",
    "email" : "test@test.com",
    "contentType" : "text/plain",
    "language" : "en",
    "contactable" : true,
    "purposes" : [ "DigitalCommunications" ]
  } ],
  "status" : "Active"
}
```

## Run the project locally

Ensure you have service-manager python environment setup:

`source ../servicemanager/bin/activate`

`sm --start DC_TWSM_ALL`

`sm --stop CHANNEL_PREFERENCES`

`sbt "run 9052 -Dplay.http.router=testOnlyDoNotUseInAppConf.Routes"`

## Run the tests and sbt fmt before raising a PR

Ensure you have service-manager python environment setup:

`source ../servicemanager/bin/activate`

Format:

`sbt fmt`

Then run the tests and coverage report:

`sbt clean coverage test coverageReport`

If your build fails due to poor test coverage, *DO NOT* lower the test coverage threshold, instead inspect the generated report located here on your local repo: `/target/scala-2.12/scoverage-report/index.html`

Then run the integration tests:

`sbt it:test`

## Swagger endpoint

Available locally here: http://localhost:9052/assets/schema.json

# License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
