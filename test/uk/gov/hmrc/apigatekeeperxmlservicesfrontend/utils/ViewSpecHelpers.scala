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

package uk.gov.hmrc.apigatekeeperxmlservicesfrontend.utils

import org.jsoup.nodes.Document
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.Organisation

trait ViewSpecHelpers extends Matchers {

  def validateFormErrors(document: Document, expectedError: Option[String] = None) = {

    // form group is always displayed
    val formGroupElement = Option(document.getElementById("form-group"))
    formGroupElement.isDefined shouldBe true

    if (expectedError.isDefined) {
      Option(document.getElementById("error-summary-display")).isDefined shouldBe true
      document.getElementById("error-summary-title").text() shouldBe "There is a problem"
      document.getElementById("error-list").children().eachText().contains(expectedError.getOrElse("")) shouldBe true
      formGroupElement.head.classNames().contains("govuk-form-group--error") shouldBe true
    }else{
      Option(document.getElementById("error-summary-display")).isDefined shouldBe false
      Option(document.getElementById("error-summary-title")).isDefined shouldBe false
      Option(document.getElementById("error-list")).isDefined shouldBe false
    }

  }

  def validateAddOrganisationDetailsPage(document: Document) ={
    document.getElementById("organisation-name-label").text() shouldBe "Organisation name"
    Option(document.getElementById("organisationName")).isDefined shouldBe true
    Option(document.getElementById("continue-button")).isDefined shouldBe true
  }

  def validateUpdateOrganisationDetailsPage(document: Document) ={
    document.getElementById("organisation-name-label").text() shouldBe "Change organisation name"
    Option(document.getElementById("organisationName")).isDefined shouldBe true
    Option(document.getElementById("continue-button")).isDefined shouldBe true
  }

  def validateAddTeamMemberPage(document: Document) = {
    document.getElementById("page-heading").text() shouldBe "Add a team member"
    document.getElementById("email-address-label").text() shouldBe "Email address"
    Option(document.getElementById("email-address-input")).isDefined shouldBe true
    Option(document.getElementById("continue-button")).isDefined shouldBe true
  }

  def validateRemoveTeamMemberPage(document: Document) = {
    document.getElementById("page-heading").text() shouldBe "Are you sure you want to remove email1?"
    Option(document.getElementById("yes")).isDefined shouldBe true
    Option(document.getElementById("no")).isDefined shouldBe true
    Option(document.getElementById("continue-button")).isDefined shouldBe true
  }

  def validateManageTeamMembersPage(document: Document, organisation: Organisation) = {
    document.getElementById("org-name-caption").text() shouldBe organisation.name
    document.getElementById("team-member-heading").text() shouldBe "Manage team members"

    if (organisation.collaborators.nonEmpty) {
      document.getElementById("team-members-email-0").text() shouldBe organisation.collaborators.head.email
      document.getElementById("remove-team-member-link-0").attr("href") shouldBe s"/api-gatekeeper-xml-services/organisations/${organisation.organisationId.value}/team-members/userId1/remove"
    }

  }
}
