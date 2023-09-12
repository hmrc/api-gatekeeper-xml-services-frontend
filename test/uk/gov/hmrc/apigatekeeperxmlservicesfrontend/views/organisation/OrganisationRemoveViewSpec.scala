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
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.controllers.OrganisationController.RemoveOrganisationConfirmationForm
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.LoggedInUser
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.utils.{OrganisationTestData, ViewSpecHelpers}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.helper.{CommonViewSpec, WithCSRFAddToken}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.organisation.OrganisationRemoveView
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationServiceMockModule

class OrganisationRemoveViewSpec extends CommonViewSpec with WithCSRFAddToken with ViewSpecHelpers {

  trait Setup extends OrganisationTestData {
    val mockAppConfig = mock[AppConfig]
    val organisationRemoveView = app.injector.instanceOf[OrganisationRemoveView]
    val loggedInUser = LoggedInUser(Some(StrideAuthorisationServiceMockModule.StrideUserName))
  }

  "Organisation Remove View" should {

    "render the remove organisation page correctly when no errors" in new Setup {

      val page = organisationRemoveView.render(RemoveOrganisationConfirmationForm.form, org1, FakeRequest().withCSRFToken, loggedInUser, messagesProvider.messages, mockAppConfig)
      val document: Document = Jsoup.parse(page.body)
      hasBackLink(document) shouldBe true
      validateFormErrors(document)

      validateRemoveOrganisationPage(document, org1.name)
    }

    "render the remove organisation page correctly when errors exist" in new Setup {

      val page = organisationRemoveView.render(RemoveOrganisationConfirmationForm.form.withError("confirm", "organisation.error.confirmation.no.choice.field"),
        org1, FakeRequest().withCSRFToken, loggedInUser, messagesProvider.messages, mockAppConfig)

      val document: Document = Jsoup.parse(page.body)
      hasBackLink(document) shouldBe true
      validateFormErrors(document, Some("Please select an option"))

      validateRemoveOrganisationPage(document, org1.name)
    }
  }
}
