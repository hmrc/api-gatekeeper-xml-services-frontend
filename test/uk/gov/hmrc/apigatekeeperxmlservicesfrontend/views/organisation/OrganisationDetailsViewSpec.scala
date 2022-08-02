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

import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.config.AppConfig
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.utils.OrganisationTestData
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.helper.CommonViewSpec
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.organisation.OrganisationDetailsView
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class OrganisationDetailsViewSpec extends CommonViewSpec {

  trait Setup extends BaseSetup with OrganisationTestData {
    val mockAppConfig = mock[AppConfig]
    val organisationDetailsView = app.injector.instanceOf[OrganisationDetailsView]

    def validateActionsAvailable(document: Document) = {
      Option(document.getElementById("change-org-details-link")) shouldBe defined
      Option(document.getElementById("manage-team-members-link")) shouldBe defined
      Option(document.getElementById("remove-organisation-button")) shouldBe defined
    }

    def validateActionsUnavailable(document: Document) = {
      Option(document.getElementById("change-org-details-link")) shouldBe None
      Option(document.getElementById("manage-team-members-link")) shouldBe None
      Option(document.getElementById("remove-organisation-button")) shouldBe None
    }

    def validateCopyEmailsAvailable(document: Document) = {
      Option(document.getElementById("copy-emails")) shouldBe defined
    }

    def validateCopyEmailsUnavailable(document: Document) = {
      Option(document.getElementById("copy-emails")) shouldBe None
    }
  }

  "Organisation Details View" should {

    "render the organisation details correctly and display the team member when present" in new Setup with StrideAuth {
      when(mockAppConfig.apiGatekeeperUrl).thenReturn("https://admin.qa.tax.service.gov.uk/api-gatekeeper")
      val page = organisationDetailsView.render(organisationWithCollaborators, organisationUsers, loggedInRequest, loggedInUser, messagesProvider.messages, mockAppConfig)
      val document: Document = Jsoup.parse(page.body)

      getBackLink(document) should not be None
      document.getElementById("org-name-heading").text() shouldBe "Name"
      document.getElementById("org-name-value").text() shouldBe org1.name

      document.getElementById("vendor-id-heading").text() shouldBe "Vendor ID"
      document.getElementById("vendor-id-value").text() shouldBe org1.vendorId.value.toString

      document.getElementById("xml-preferences-heading").text() shouldBe "XML email preferences"
      document.getElementById("xml-preferences-value").text() shouldBe "xml api 1 xml api 2 xml api 3"


      document.getElementById("team-members-heading").text() shouldBe "Team members"
      document.getElementById("user-email-0").text() shouldBe "a@b.com"
      document.getElementById("user-link-0").attr("href") shouldBe s"https://admin.qa.tax.service.gov.uk/api-gatekeeper/developer?developerId=${organisationUsers.head.userId.value}"
      document.getElementById("user-services-0").text() shouldBe "xml api 1 xml api 2"

      document.getElementById("user-email-1").text() shouldBe "a@b.com2"
      document.getElementById("user-link-1").attr("href") shouldBe s"https://admin.qa.tax.service.gov.uk/api-gatekeeper/developer?developerId=${organisationUsers.tail.head.userId.value}"
      document.getElementById("user-services-1").text() shouldBe "xml api 1 xml api 3"

      validateActionsAvailable(document)
      validateCopyEmailsAvailable(document)
    }

    "render without action buttons for LDAP" in new Setup with LdapAuth {
      when(mockAppConfig.apiGatekeeperUrl).thenReturn("https://admin.qa.tax.service.gov.uk/api-gatekeeper")
      val page = organisationDetailsView.render(organisationWithCollaborators, organisationUsers, loggedInRequest, loggedInUser, messagesProvider.messages, mockAppConfig)
      val document: Document = Jsoup.parse(page.body)

      getBackLink(document) should not be None
      document.getElementById("org-name-heading").text() shouldBe "Name"
      document.getElementById("org-name-value").text() shouldBe org1.name

      document.getElementById("vendor-id-heading").text() shouldBe "Vendor ID"
      document.getElementById("vendor-id-value").text() shouldBe org1.vendorId.value.toString

      document.getElementById("xml-preferences-heading").text() shouldBe "XML email preferences"
      document.getElementById("xml-preferences-value").text() shouldBe "xml api 1 xml api 2 xml api 3"


      document.getElementById("team-members-heading").text() shouldBe "Team members"
      document.getElementById("user-email-0").text() shouldBe "a@b.com"
      document.getElementById("user-link-0").attr("href") shouldBe s"https://admin.qa.tax.service.gov.uk/api-gatekeeper/developer?developerId=${organisationUsers.head.userId.value}"
      document.getElementById("user-services-0").text() shouldBe "xml api 1 xml api 2"

      document.getElementById("user-email-1").text() shouldBe "a@b.com2"
      document.getElementById("user-link-1").attr("href") shouldBe s"https://admin.qa.tax.service.gov.uk/api-gatekeeper/developer?developerId=${organisationUsers.tail.head.userId.value}"
      document.getElementById("user-services-1").text() shouldBe "xml api 1 xml api 3"

      validateActionsUnavailable(document)
      validateCopyEmailsUnavailable(document)
    }

    "render the organisation details correctly and display the team member when not present" in new Setup with StrideAuth {

      val page = organisationDetailsView.render(organisationWithCollaborators, List.empty, loggedInRequest, loggedInUser, messagesProvider.messages, mockAppConfig)
      val document: Document = Jsoup.parse(page.body)

      getBackLink(document) should not be None
      document.getElementById("org-name-heading").text() shouldBe "Name"
      document.getElementById("org-name-value").text() shouldBe org1.name

      document.getElementById("vendor-id-heading").text() shouldBe "Vendor ID"
      document.getElementById("vendor-id-value").text() shouldBe org1.vendorId.value.toString

      document.getElementById("xml-preferences-heading").text() shouldBe "XML email preferences"
      document.getElementById("xml-preferences-value").text() shouldBe ""

      document.getElementById("team-members-heading").text() shouldBe "Team members"

      validateActionsAvailable(document)
      validateCopyEmailsUnavailable(document)
    }

  }
}
