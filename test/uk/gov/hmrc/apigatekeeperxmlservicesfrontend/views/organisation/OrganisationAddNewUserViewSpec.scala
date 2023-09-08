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

package uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.organisation

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.FakeRequest
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.config.AppConfig
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.controllers.OrganisationController.AddOrganisationWithNewUserForm
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.LoggedInUser
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.utils.{OrganisationTestData, ViewSpecHelpers}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.helper.{CommonViewSpec, WithCSRFAddToken}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.organisation.OrganisationAddNewUserView
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationServiceMockModule

class OrganisationAddNewUserViewSpec extends CommonViewSpec with WithCSRFAddToken with ViewSpecHelpers {

  trait Setup extends OrganisationTestData {
    val mockAppConfig = mock[AppConfig]
    val organisationAddNewUserView = app.injector.instanceOf[OrganisationAddNewUserView]
     val loggedInUser = LoggedInUser(Some(StrideAuthorisationServiceMockModule.StrideUserName))
  }

  "Organisation Add new User View" should {

    "render the add new user page correctly when no errors" in new Setup {
      val orgName = "orgName"
      val email = "email"

      val page = organisationAddNewUserView
        .render(AddOrganisationWithNewUserForm.form, Some(orgName), Some(email), FakeRequest().withCSRFToken, loggedInUser, messagesProvider.messages, mockAppConfig)
      val document: Document = Jsoup.parse(page.body)
      hasBackLink(document) shouldBe true
      validateFormErrors(document)
      validateOrganisationAddNewUserPage(document, orgName, email)
    }

    "render the organisation add page correctly when errors exist" in new Setup {
      val orgName = "orgName"
      val email = "email"
      val form = AddOrganisationWithNewUserForm.form
        .withError("firstName", "firstname.error.required")
        .withError("lastName", "lastname.error.required")

      val page = organisationAddNewUserView.render(form, Some(orgName), Some(email), FakeRequest().withCSRFToken, loggedInUser, messagesProvider.messages, mockAppConfig)
      val document: Document = Jsoup.parse(page.body)
      hasBackLink(document) shouldBe true
      validateFormErrors(document, Some("Enter a first name"))
      validateFormErrors(document, Some("Enter a last name"))
      validateOrganisationAddNewUserPage(document, orgName, email)
    }
  }
}
