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

package uk.gov.hmrc.channelpreferences.controllers.model

import play.api.libs.json._
import cats.syntax.option._

sealed trait Context {
  val navigation: Option[Map[String, String]]
}

case class ConsentContext(
  consent: Consent,
  navigation: Option[Map[String, String]]
) extends Context

object ConsentContext {
  implicit val format: OFormat[ConsentContext] = Json.format[ConsentContext]
}

case class NavigationContext(
  navigation: Option[Map[String, String]]
) extends Context

object NavigationContext {
  implicit val format: OFormat[NavigationContext] = Json.format[NavigationContext]
}

case class VerificationContext(
  verification: Verification,
  navigation: Option[Map[String, String]]
) extends Context

object VerificationContext {
  implicit val format: OFormat[VerificationContext] = Json.format[VerificationContext]
}

case class ConsentVerificationContext(
  consented: Consent,
  verification: Verification,
  navigation: Option[Map[String, String]]
) extends Context

object ConsentVerificationContext {
  implicit val format: OFormat[ConsentVerificationContext] = Json.format[ConsentVerificationContext]
}

case class ConfirmationContext(
  consented: Consent,
  verification: Verification,
  confirm: Confirm,
  navigation: Option[Map[String, String]]
) extends Context

object ConfirmationContext {
  implicit val format: OFormat[ConfirmationContext] = Json.format[ConfirmationContext]
}

object Context {
  implicit object Format extends Format[Context] {
    override def writes(o: Context): JsValue = o match {
      case v: VerificationContext         => VerificationContext.format.writes(v)
      case cv: ConsentVerificationContext => ConsentVerificationContext.format.writes(cv)
      case c: ConfirmationContext         => ConfirmationContext.format.writes(c)
      case c: ConsentContext              => ConsentContext.format.writes(c)
      case c: NavigationContext           => NavigationContext.format.writes(c)
    }

    override def reads(json: JsValue): JsResult[Context] = json match {
      case JsObject(_) =>
        ConfirmationContext.format
          .reads(json)
          .orElse(ConsentVerificationContext.format.reads(json))
          .orElse(VerificationContext.format.reads(json))
          .orElse(ConsentContext.format.reads(json))
          .orElse(NavigationContext.format.reads(json))

      case other => JsError(s"expected json object for Context but got $other")
    }
  }

  implicit class ContextOps(val context: Context) extends AnyVal {
    def updateConsent(consent: Consent): Context = context match {
      case c: ConsentContext             => c.copy(consent = consent)
      case NavigationContext(navigation) => ConsentContext(consent, navigation)
      case VerificationContext(verification, navigation) =>
        ConsentVerificationContext(consent, verification, navigation)
      case c: ConsentVerificationContext => c.copy(consented = consent)
      case c: ConfirmationContext        => c.copy(consented = consent)
    }

    def updateNavigation(navigation: Map[String, String]): Context = context match {
      case c: ConsentContext             => c.copy(navigation = updateNavigationMap(c.navigation, navigation))
      case n: NavigationContext          => n.copy(navigation = updateNavigationMap(n.navigation, navigation))
      case v: VerificationContext        => v.copy(navigation = updateNavigationMap(v.navigation, navigation))
      case c: ConsentVerificationContext => c.copy(navigation = updateNavigationMap(c.navigation, navigation))
      case c: ConfirmationContext        => c.copy(navigation = updateNavigationMap(c.navigation, navigation))
    }

    private def updateNavigationMap(
      previous: Option[Map[String, String]],
      toAdd: Map[String, String]): Option[Map[String, String]] =
      previous.map(_ ++ toAdd).orElse(toAdd.some)

    def updateVerification(verification: Verification): Context = context match {
      case ConsentContext(consent, navigation) => ConsentVerificationContext(consent, verification, navigation)
      case NavigationContext(navigation)       => VerificationContext(verification, navigation)
      case v: VerificationContext              => v.copy(verification = verification)
      case c: ConsentVerificationContext       => c.copy(verification = verification)
      case c: ConfirmationContext              => c.copy(verification = verification)
    }
  }
}
