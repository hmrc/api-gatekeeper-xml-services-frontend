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

package uk.gov.hmrc.apigatekeeperxmlservicesfrontend.utils

import org.jsoup.nodes.Document
import org.scalatest.matchers.should.Matchers

import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.Organisation
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationServiceMockModule

trait ViewSpecHelpers extends Matchers {

  def validateFormErrors(document: Document, expectedError: Option[String] = None) = {

    // form group is always displayed
    val formGroupElement = Option(document.getElementById("form-group"))
    formGroupElement.isDefined shouldBe true

    if (expectedError.isDefined) {
      Option(document.getElementById("error-summary-display")).isDefined shouldBe true
      document.getElementById("error-summary-title").text() shouldBe "There is a problem"
      withClue(s"Expected Error ${expectedError.getOrElse("")} is not displayed") {
        document.getElementById("error-list").children().eachText().forEach(println)
        document.getElementById("error-list").children().eachText().contains(expectedError.getOrElse("")) shouldBe true
      }
      formGroupElement.head.classNames().contains("govuk-form-group--error") shouldBe true
    } else {
      Option(document.getElementById("error-summary-display")).isDefined shouldBe false
      Option(document.getElementById("error-summary-title")).isDefined shouldBe false
      Option(document.getElementById("error-list")).isDefined shouldBe false
    }

  }

  def validateAddOrganisationPage(document: Document) = {

    document.getElementById("page-heading").text() shouldBe "Add vendor"
    document.getElementById("organisation-name-label").text() shouldBe "Vendor name"
    Option(document.getElementById("organisationName")).isDefined shouldBe true

    Option(document.getElementById("team-member-legend")).isDefined shouldBe true
    document.getElementById("team-member-legend").text() shouldBe "Team member"

    Option(document.getElementById("team-member-hint")).isDefined shouldBe true
    document.getElementById("team-member-hint").text shouldBe "Vendors need at least 1 team member. You can add more team members later."

    document.getElementById("email-label").text() shouldBe "Email address"
    Option(document.getElementById("emailAddress")).isDefined shouldBe true

    Option(document.getElementById("continue-button")).isDefined shouldBe true
  }

  def validateOrganisationAddNewUserPage(document: Document, expectedOrganisation: String, expectedEmail: String) = {

    document.getElementById("page-heading").text() shouldBe "Add the team member’s name"
    document.getElementById("first-name-label").text() shouldBe "First name"
    document.getElementById("last-name-label").text() shouldBe "Last name"
    document.getElementById("organisationname-hidden").`val`() shouldBe expectedOrganisation
    document.getElementById("email-hidden").`val`() shouldBe expectedEmail
    document.getElementById("logged-in-user").text() shouldBe StrideAuthorisationServiceMockModule.StrideUserName
    Option(document.getElementById("continue-button")).isDefined shouldBe true
  }

  def validateUpdateOrganisationDetailsPage(document: Document) = {

    document.getElementById("organisation-name-label").text() shouldBe "Change vendor name"
    Option(document.getElementById("organisationName")).isDefined shouldBe true
    Option(document.getElementById("continue-button")).isDefined shouldBe true
    document.getElementById("logged-in-user").text() shouldBe StrideAuthorisationServiceMockModule.StrideUserName
  }

  def validateRemoveOrganisationPage(document: Document, organisationName: String) = {
    document.getElementById("page-heading").text() shouldBe s"Are you sure you want to remove $organisationName?"
    Option(document.getElementById("yes")).isDefined shouldBe true
    Option(document.getElementById("no")).isDefined shouldBe true
    Option(document.getElementById("continue-button")).isDefined shouldBe true
    document.getElementById("logged-in-user").text() shouldBe StrideAuthorisationServiceMockModule.StrideUserName
  }

  def validateRemoveOrganisationSuccessPage(document: Document, organisationName: String) = {
    Option(document.getElementById("panel-heading")).isDefined shouldBe true
    document.getElementById("panel-heading").text() shouldBe s"You removed $organisationName"
    Option(document.getElementById("back-to-xml-link")).isDefined shouldBe true
    document.getElementById("back-to-xml-link").text() shouldBe "Back to XML vendors"
    document.getElementById("logged-in-user").text() shouldBe StrideAuthorisationServiceMockModule.StrideUserName
  }

  def validateAddTeamMemberPage(document: Document) = {
    document.getElementById("page-heading").text() shouldBe "Add a team member"
    document.getElementById("email-address-label").text() shouldBe "Email address"
    Option(document.getElementById("emailAddress")).isDefined shouldBe true
    Option(document.getElementById("continue-button")).isDefined shouldBe true
    document.getElementById("logged-in-user").text() shouldBe StrideAuthorisationServiceMockModule.StrideUserName
  }

  def validateCreateTeamMemberPage(document: Document, expectedEmail: String) = {
    document.getElementById("page-heading").text() shouldBe "Add the team member’s name"
    document.getElementById("first-name-label").text() shouldBe "First name"
    document.getElementById("last-name-label").text() shouldBe "Last name"
    document.getElementById("email-hidden").`val`() shouldBe expectedEmail
    Option(document.getElementById("continue-button")).isDefined shouldBe true
    document.getElementById("logged-in-user").text() shouldBe StrideAuthorisationServiceMockModule.StrideUserName
  }

  def validateRemoveTeamMemberPage(document: Document) = {
    document.getElementById("page-heading").text() shouldBe "Are you sure you want to remove email1@email.com?"
    Option(document.getElementById("yes")).isDefined shouldBe true
    Option(document.getElementById("no")).isDefined shouldBe true
    Option(document.getElementById("continue-button")).isDefined shouldBe true
    document.getElementById("logged-in-user").text() shouldBe StrideAuthorisationServiceMockModule.StrideUserName
  }

  def validateManageTeamMembersPage(document: Document, organisation: Organisation) = {
    document.getElementById("org-name-caption").text() shouldBe organisation.name
    document.getElementById("team-member-heading").text() shouldBe "Manage team members"
    document.getElementById("logged-in-user").text() shouldBe StrideAuthorisationServiceMockModule.StrideUserName

    if (organisation.collaborators.nonEmpty) {
      document.getElementById("team-members-email-0").text() shouldBe organisation.collaborators.head.email
      document.getElementById("remove-team-member-link-0").attr(
        "href"
      ) shouldBe s"/api-gatekeeper-xml-services/organisations/${organisation.organisationId.value}/team-members/userId1/remove"
    }

  }

  // CSV PAGES
  def validateUsersCSVUploadPage(document: Document) = {
    document.getElementById("page-heading").text() shouldBe "Upload users as CSV"
    document.getElementById("csv-data-input-label").text() shouldBe "Provide CSV input here please"
    Option(document.getElementById("csv-data-input")).isDefined shouldBe true
    Option(document.getElementById("upload-csv-button")).isDefined shouldBe true
    document.getElementById("logged-in-user").text() shouldBe StrideAuthorisationServiceMockModule.StrideUserName
  }
}
