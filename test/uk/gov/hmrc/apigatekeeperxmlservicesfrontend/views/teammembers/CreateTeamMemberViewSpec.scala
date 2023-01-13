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

package uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.teammembers

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.FakeRequest
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.config.AppConfig
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.controllers.TeamMembersController.CreateAndAddTeamMemberForm
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.LoggedInUser
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.utils.{OrganisationTestData, ViewSpecHelpers}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.helper.{CommonViewSpec, WithCSRFAddToken}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.teammembers.CreateTeamMemberView
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationServiceMockModule

class CreateTeamMemberViewSpec extends CommonViewSpec with WithCSRFAddToken with ViewSpecHelpers {

  trait Setup extends OrganisationTestData {
    val mockAppConfig = mock[AppConfig]
    val createTeamMemberView = app.injector.instanceOf[CreateTeamMemberView]
     val loggedInUser = LoggedInUser(Some(StrideAuthorisationServiceMockModule.StrideUserName))
  }

  "Create Team Member View" should {

    "render the create team member page correctly when no errors" in new Setup {

      val page = createTeamMemberView.render(
        CreateAndAddTeamMemberForm.form,
        org1.organisationId,
        emailAddress,
        FakeRequest().withCSRFToken,
        loggedInUser,
        messagesProvider.messages,
        mockAppConfig
      )

      val document: Document = Jsoup.parse(page.body)
      getBackLink(document) should not be None
      validateFormErrors(document)
      validateCreateTeamMemberPage(document, emailAddress)
    }

    "render the create team member page correctly when errors exist" in new Setup {

      val page = createTeamMemberView
        .render(
          CreateAndAddTeamMemberForm.form.fill(CreateAndAddTeamMemberForm(emailAddress, "", ""))
            .withError("firstName", "firstname.error.required")
            .withError("lastName", "lastname.error.required"),
          org1.organisationId,
          "",
          FakeRequest().withCSRFToken,
          loggedInUser,
          messagesProvider.messages,
          mockAppConfig
        )

      val document: Document = Jsoup.parse(page.body)
      getBackLink(document) should not be None
      validateFormErrors(document, Some("Enter a first name"))
      validateFormErrors(document, Some("Enter a last name"))
      validateCreateTeamMemberPage(document, emailAddress)
    }
  }
}
