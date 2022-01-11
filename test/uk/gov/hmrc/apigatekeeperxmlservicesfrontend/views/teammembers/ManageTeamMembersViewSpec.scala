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

import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.teammembers.ManageTeamMembersView
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class ManageTeamMembersViewSpec extends CommonViewSpec {

  trait Setup extends OrganisationTestData {
    val mockAppConfig = mock[AppConfig]
    val manageTeamMembersView = app.injector.instanceOf[ManageTeamMembersView]

  }

  "Manage Team Members View" should {

    "render the team members correctly" in new Setup {

      val page = manageTeamMembersView.render(organisationWithCollaborators, FakeRequest(), messagesProvider.messages, mockAppConfig)
      val document: Document = Jsoup.parse(page.body)

      document.getElementById("org-name-caption").text() shouldBe org1.name
      document.getElementById("team-member-heading").text() shouldBe "Manage team members"
      
      document.getElementById("team-members-email-0").text() shouldBe organisationWithCollaborators.collaborators.head.email
      document.getElementById("remove-team-member-link-0").attr("href") shouldBe s"/api-gatekeeper-xml-services/organisations/${organisationWithCollaborators.organisationId.value}/remove-team-member/userId1"
    }

    
  }
}
