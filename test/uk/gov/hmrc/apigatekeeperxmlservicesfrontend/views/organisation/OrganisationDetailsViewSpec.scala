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

package uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.organisation

import play.api.test.FakeRequest

import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.config.AppConfig
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.utils.OrganisationTestData
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.helper.CommonViewSpec

import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.organisation.OrganisationDetailsView
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class OrganisationDetailsViewSpec extends CommonViewSpec {

  trait Setup extends OrganisationTestData {
    val mockAppConfig = mock[AppConfig]
    val organisationDetailsView = app.injector.instanceOf[OrganisationDetailsView]

  }

  "Organisation Details View" should {

    "render the organisation details correctly and display the team member when present" in new Setup {

      val page = organisationDetailsView.render(organisationWithCollaborators, organisationUsers, FakeRequest(), messagesProvider.messages, mockAppConfig)
      val document: Document = Jsoup.parse(page.body)

      document.getElementById("org-name-heading").text() shouldBe "Name"
      document.getElementById("org-name-value").text() shouldBe org1.name

      document.getElementById("vendor-id-heading").text() shouldBe "Vendor ID"
      document.getElementById("vendor-id-value").text() shouldBe org1.vendorId.value.toString

      document.getElementById("team-members-heading").text() shouldBe "Team members"
      document.getElementById("user-email-0").text() shouldBe "a@b.com"
      document.getElementById("user-services-0").text() shouldBe "xml api 1<BR/>xml api 3"

    }

      "render the organisation details correctly and display the team member when not present" in new Setup {

      val page = organisationDetailsView.render(organisationWithCollaborators, organisationUsers, FakeRequest(), messagesProvider.messages, mockAppConfig)
      val document: Document = Jsoup.parse(page.body)


      document.getElementById("team-members-heading").text() shouldBe "Team members"

      document.getElementById("copy-emails").attr("onClick") shouldBe "copyToClipboard('a@b.com;');"
    }

  }
}
