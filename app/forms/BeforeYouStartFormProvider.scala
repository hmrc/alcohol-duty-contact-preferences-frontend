/*
 * Copyright 2026 HM Revenue & Customs
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

package forms

import config.Constants.emailAddressRegexString
import models.BeforeYouStartAnswer
import play.api.data.Forms.{of, single}
import play.api.data.format.Formatter
import play.api.data.{Form, FormError}

import javax.inject.Inject

class BeforeYouStartFormProvider @Inject() {

  private val formatter: Formatter[BeforeYouStartAnswer] = new Formatter[BeforeYouStartAnswer] {

    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], BeforeYouStartAnswer] =
      data.get("contactPreferenceEmail") match {
        case Some("true")  =>
          data.get("emailAddress").map(_.trim).filter(_.nonEmpty) match {
            case None                                                   =>
              Left(Seq(FormError("emailAddress", "changePreferences.enter-email-address.empty-error")))
            case Some(email) if !email.matches(emailAddressRegexString) =>
              Left(Seq(FormError("emailAddress", "changePreferences.enter-email-address.format-error")))
            case Some(email)                                            =>
              Right(BeforeYouStartAnswer(contactPreferenceEmail = true, emailAddress = Some(email)))
          }
        case Some("false") =>
          Right(BeforeYouStartAnswer(contactPreferenceEmail = false, emailAddress = None))
        case _             =>
          Left(Seq(FormError("contactPreferenceEmail", "contactPreference.error.required")))
      }

    override def unbind(key: String, value: BeforeYouStartAnswer): Map[String, String] =
      Map("contactPreferenceEmail" -> value.contactPreferenceEmail.toString) ++
        value.emailAddress.map("emailAddress" -> _).toMap
  }

  def apply(): Form[BeforeYouStartAnswer] = Form(single("beforeYouStart" -> of(formatter)))
}
