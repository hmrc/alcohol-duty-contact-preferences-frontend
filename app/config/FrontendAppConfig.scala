/*
 * Copyright 2024 HM Revenue & Customs
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

package config

import com.google.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.i18n.Lang
import play.api.mvc.RequestHeader
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class FrontendAppConfig @Inject() (configuration: Configuration, servicesConfig: ServicesConfig) {

  val host: String    = configuration.get[String]("host")
  val appName: String = configuration.get[String]("appName")

  private val contactHost                         = configuration.get[String]("contact-frontend.host")
  private val contactFormServiceIdentifier        = "alcohol-duty-contact-preferences-frontend"
  private lazy val contactPreferencesHost: String = servicesConfig.baseUrl("alcohol-duty-contact-preferences")

  def feedbackUrl(implicit request: RequestHeader): String =
    s"$contactHost/contact/beta-feedback?service=$contactFormServiceIdentifier&backUrl=${host + request.uri}"

  val loginUrl: String                  = configuration.get[String]("urls.login")
  val loginContinueUrl: String          = configuration.get[String]("urls.loginContinue")
  val signOutUrl: String                = configuration.get[String]("urls.signOut")
  val businessTaxAccountUrl: String     = configuration.get[String]("urls.businessTaxAccount")
  val accessibilityStatementUrl: String = configuration.get[String]("accessibility-statement.host") ++
    configuration.get[String]("accessibility-statement.url")

  private val exitSurveyBaseUrl: String = configuration.get[String]("urls.feedbackFrontendBase")
  val exitSurveyUrl: String             = s"$exitSurveyBaseUrl/feedback/alcohol-duty-contact-preferences-frontend"

  val languageTranslationEnabled: Boolean =
    configuration.get[Boolean]("features.welsh-translation")

  def languageMap: Map[String, Lang] = Map(
    "en" -> Lang("en"),
    "cy" -> Lang("cy")
  )

  val timeout: Int                   = configuration.get[Int]("timeout-dialog.timeout")
  val countdown: Int                 = configuration.get[Int]("timeout-dialog.countdown")
  val enrolmentServiceName: String   = configuration.get[String]("enrolment.serviceName")
  val enrolmentIdentifierKey: String = configuration.get[String]("enrolment.identifierKey")

  def ecpUserAnswersGetUrl(appaId: String): String =
    s"$contactPreferencesHost/alcohol-duty-contact-preferences/user-answers/$appaId"

  def ecpUserAnswersUrl(): String =
    s"$contactPreferencesHost/alcohol-duty-contact-preferences/user-answers"

  def ecpUserAnswersClearAllUrl(): String =
    s"$contactPreferencesHost/alcohol-duty-contact-preferences/test-only/user-answers/clear-all"

  def ecpGetEmailVerificationUrl(credId: String): String =
    s"$contactPreferencesHost/alcohol-duty-contact-preferences/get-email-verification/$credId"

  def ecpSubmitContactPreferencesUrl(appaId: String): String =
    s"$contactPreferencesHost/alcohol-duty-contact-preferences/submit-preferences/$appaId"

  private val startEmailVerificationContinueBaseUrl: String   =
    configuration.get[String]("microservice.services.contact-preferences-frontend.prefix")
  private val startEmailVerificationContinueUrlSuffix: String =
    configuration.get[String]("microservice.services.contact-preferences-frontend.url.checkYourAnswersPage")
  private val startEmailVerificationBackUrlSuffix: String     =
    configuration.get[String]("microservice.services.contact-preferences-frontend.url.enterEmailPage")

  val startEmailVerificationContinueUrl: String =
    s"$startEmailVerificationContinueBaseUrl$startEmailVerificationContinueUrlSuffix"
  val startEmailVerificationBackUrl: String     =
    s"$startEmailVerificationContinueBaseUrl$startEmailVerificationBackUrlSuffix"

  private val startEmailVerificationJourneyBaseUrl: String   = servicesConfig.baseUrl("email-verification")
  private val startEmailVerificationJourneyUrlSuffix: String =
    configuration.get[String]("microservice.services.email-verification.url.startEmailVerificationJourney")

  val startEmailVerificationJourneyUrl: String =
    s"$startEmailVerificationJourneyBaseUrl$startEmailVerificationJourneyUrlSuffix"

  val emailVerificationRedirectBaseUrl: String =
    configuration.get[String]("microservice.services.email-verification-frontend.prefix")

}
