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

package services

import com.google.inject.Singleton
import play.api.libs.json.{Json, OFormat}

import javax.inject.Inject
import scala.collection.immutable.ListMap

case class FcoCountry(name: String)

object FcoCountry {
  implicit val format: OFormat[FcoCountry] = Json.format[FcoCountry]
}

@Singleton
class CountryService @Inject() {

  private val countriesEn = parseCountriesResource()

  def tryLookupCountryName(code: String): Option[String] =
    countriesEn.get(code)

  private def parseCountriesResource(languageCode: String = "EN"): Map[String, String] = {

    val stream     = getClass.getResourceAsStream(s"/resources/countries$languageCode.json")
    val countryMap = Json
      .parse(stream)
      .as[Map[String, FcoCountry]]
      .map { entry =>
        entry._1 -> entry._2.name
      }

    ListMap(countryMap.toSeq.sortBy(_._2): _*)
  }
}
