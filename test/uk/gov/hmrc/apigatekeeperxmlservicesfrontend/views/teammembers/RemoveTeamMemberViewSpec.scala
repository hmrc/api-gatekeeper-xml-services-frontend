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

package uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.teammembers

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.FakeRequest
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.config.AppConfig
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.forms.Forms.RemoveTeamMemberConfirmationForm
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.utils.OrganisationTestData
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.helper.{CommonViewSpec, WithCSRFAddToken}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.teammembers.RemoveTeamMemberView

class RemoveTeamMemberViewSpec extends CommonViewSpec with WithCSRFAddToken {

  trait Setup extends OrganisationTestData {
    val mockAppConfig = mock[AppConfig]
    val removeTeamMemberView = app.injector.instanceOf[RemoveTeamMemberView]

  }

  "Remove Team Member View" should {

    def validateFormErrors(document: Document, isError: Boolean)={
      Option(document.getElementById("error-summary-display")).isDefined shouldBe isError
      if(isError){
        document.getElementById("error-summary-title").text() shouldBe "There is a problem"
        document.getElementById("error-list").children().eachText().contains("Invalid Email provided") shouldBe true
      }

      val formGroupElement = Option(document.getElementById("form-group"))
      formGroupElement.isDefined shouldBe true
      formGroupElement.head.classNames().contains("govuk-form-group--error") shouldBe isError

    }

    "render the remove team member page correctly when no errors" in new Setup {

      val page = removeTeamMemberView.render(RemoveTeamMemberConfirmationForm.form, org1.organisationId, collaborator1.userId, collaborator1.email, FakeRequest().withCSRFToken, messagesProvider.messages, mockAppConfig)
      val document: Document = Jsoup.parse(page.body)

      validateFormErrors(document, false)

      document.getElementById("page-heading").text() shouldBe "Are you sure you want to remove email1?"
      Option(document.getElementById("yes")).isDefined shouldBe true
      Option(document.getElementById("no")).isDefined shouldBe true
      Option(document.getElementById("continue-button")).isDefined shouldBe true
    }

    "render the remove team member page correctly when errors exist" in new Setup {

      val page = removeTeamMemberView.render(RemoveTeamMemberConfirmationForm.form.withError("email", "removeteammember.email.error.required"),
        org1.organisationId, collaborator1.userId, collaborator1.email, FakeRequest().withCSRFToken, messagesProvider.messages, mockAppConfig)

      val document: Document = Jsoup.parse(page.body)

      println(page.body)
      validateFormErrors(document, true)

      document.getElementById("page-heading").text() shouldBe "Are you sure you want to remove email1?"
      Option(document.getElementById("yes")).isDefined shouldBe true
      Option(document.getElementById("no")).isDefined shouldBe true
      Option(document.getElementById("continue-button")).isDefined shouldBe true
    }
  }
}
