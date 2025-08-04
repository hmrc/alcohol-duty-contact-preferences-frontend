/*
 * Copyright 2025 HM Revenue & Customs
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

package models

import play.api.mvc.JavascriptLiteral

sealed trait EntryMode

case object ChangePreference extends EntryMode
case object UpdateEmail extends EntryMode
case object BouncedEmail extends EntryMode

object EntryMode {

  implicit val jsLiteral: JavascriptLiteral[EntryMode] = new JavascriptLiteral[EntryMode] {
    override def to(value: EntryMode): String = value match {
      case ChangePreference => "ChangePreference"
      case UpdateEmail      => "UpdateEmail"
      case BouncedEmail     => "BouncedEmail"
    }
  }
}
