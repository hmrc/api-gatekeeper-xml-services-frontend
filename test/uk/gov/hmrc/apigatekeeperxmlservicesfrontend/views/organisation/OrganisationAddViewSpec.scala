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

import play.api.test.FakeRequest
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.config.AppConfig
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.utils.OrganisationTestData
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.helper.{CommonViewSpec, WithCSRFAddToken}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.organisation.OrganisationAddView
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.controllers.OrganisationController.AddOrganisationForm
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.LoggedInUser
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.utils.ViewSpecHelpers
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationServiceMockModule

class OrganisationAddViewSpec extends CommonViewSpec with WithCSRFAddToken with ViewSpecHelpers {

  trait Setup extends OrganisationTestData {
    val mockAppConfig = mock[AppConfig]
    val organisationAddView = app.injector.instanceOf[OrganisationAddView]
     val loggedInUser = LoggedInUser(Some(StrideAuthorisationServiceMockModule.StrideUserName))
  }

  "Organisation Add View" should {


    "render the organisation add page correctly when no errors" in new Setup {

      val page = organisationAddView.render(AddOrganisationForm.form, FakeRequest().withCSRFToken, loggedInUser, messagesProvider.messages, mockAppConfig)
      val document: Document = Jsoup.parse(page.body)
      hasBackLink(document) shouldBe true
      validateFormErrors(document)
      validateAddOrganisationPage(document)
    }

    "render the organisation add page correctly when errors exist" in new Setup {
      val form = AddOrganisationForm.form
        .withError("organisationName", "organisationname.error.required")
        .withError("emailAddress", "emailAddress.error.required.field")

      val page = organisationAddView.render(form, FakeRequest().withCSRFToken, loggedInUser, messagesProvider.messages, mockAppConfig)
      val document: Document = Jsoup.parse(page.body)

      validateFormErrors(document, Some("Enter an organisation name"))
      validateFormErrors(document, Some("Enter an email address"))
      hasBackLink(document) shouldBe true
      document.getElementById("page-heading").text() shouldBe "Add organisation"
      document.getElementById("organisation-name-label").text() shouldBe "Organisation name"
      Option(document.getElementById("organisationName")).isDefined shouldBe true
      Option(document.getElementById("continue-button")).isDefined shouldBe true
    }
  }
}
