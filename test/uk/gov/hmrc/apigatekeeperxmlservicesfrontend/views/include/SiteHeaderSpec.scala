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

package uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.include

import scala.jdk.CollectionConverters._

import org.jsoup.Jsoup

import play.api.test.FakeRequest

import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.config.AppConfig
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.LoggedInUser
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.helper.CommonViewSpec
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.include.SiteHeader
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationServiceMockModule

class SiteHeaderSpec extends CommonViewSpec {

  trait Setup {
    val mockAppConfig = mock[AppConfig]
    val siteHeader    = app.injector.instanceOf[SiteHeader]
    val loggedInUser  = LoggedInUser(Some(StrideAuthorisationServiceMockModule.StrideUserName))

    val apiGatekeeperUrl             = "/api-gatekeeper"
    val apiGatekeeperApisUrl         = "/api-gatekeeper-apis"
    val apiGatekeeperOrganisationUrl = "/api-gatekeeper-organisation"

    val expectedMenuItems = Map(
      "APIs"          -> apiGatekeeperApisUrl,
      "Applications"  -> s"$apiGatekeeperUrl/applications",
      "Developers"    -> s"$apiGatekeeperUrl/developers",
      "XML vendors"   -> "/api-gatekeeper-xml-services/organisations",
      "Email"         -> s"$apiGatekeeperUrl/emails",
      "Organisations" -> s"$apiGatekeeperOrganisationUrl/organisations"
    )
  }

  "SiteHeader" should {

    "render correctly" in new Setup {
      val component  = siteHeader.render(apiGatekeeperUrl, apiGatekeeperApisUrl, apiGatekeeperOrganisationUrl, FakeRequest(), loggedInUser, messagesProvider.messages)
      val document   = Jsoup.parse(component.body)
      val navigation = document.getElementsByClass("govuk-service-navigation__wrapper")

      val actualMenuItems = navigation.select("a")
        .asScala
        .map(i => (i.text(), i.attr("href"))).toMap

      withClue("navigation items did not match expected") {
        print(actualMenuItems)
        print("-----------")
        print(expectedMenuItems)
        actualMenuItems.equals(expectedMenuItems) shouldBe true
      }
    }
  }

}
