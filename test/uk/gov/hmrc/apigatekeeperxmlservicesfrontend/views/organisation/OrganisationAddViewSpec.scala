/*
 * Copyright 2021 HM Revenue & Customs
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
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.forms.AddOrganisation
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.organisation.OrganisationAddView
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class OrganisationAddViewSpec extends CommonViewSpec with WithCSRFAddToken {

  trait Setup extends OrganisationTestData {
    val mockAppConfig = mock[AppConfig]
    val organisationAddView = app.injector.instanceOf[OrganisationAddView]

  }

  "Organisation Add View" should {

    def validateFormErrors(document: Document, isError: Boolean)={
      Option(document.getElementById("error-summary-display")).isDefined shouldBe isError
      if(isError){
        document.getElementById("error-summary-title").text() shouldBe "There is a problem"
        document.getElementById("error-list").children().eachText().contains("Enter an organistion name") shouldBe true
      
      }
      
      Option(document.getElementById("data-field-error-organisationname")).isDefined shouldBe isError
      val formGroupElement = Option(document.getElementById("form-group"))
      formGroupElement.isDefined shouldBe true
      formGroupElement.head.classNames().contains("govuk-form-group--error") shouldBe isError


    }

    "render the organisation add page correctly when no errors" in new Setup {
  
      val page = organisationAddView.render(AddOrganisation.form, FakeRequest().withCSRFToken, messagesProvider.messages, mockAppConfig)
      val document: Document = Jsoup.parse(page.body)

      validateFormErrors(document, false)

      document.getElementById("page-heading").text() shouldBe "Add organisation"
      document.getElementById("organisation-name-label").text() shouldBe "Organisation name"
      Option(document.getElementById("organisationname")).isDefined shouldBe true
      Option(document.getElementById("continue-button")).isDefined shouldBe true
    }

    "render the organisation add page correctly when errors exist" in new Setup {

      val page = organisationAddView.render(AddOrganisation.form.withError("organisationname", "organisationname.error.required"), FakeRequest().withCSRFToken, messagesProvider.messages, mockAppConfig)
      val document: Document = Jsoup.parse(page.body)

      validateFormErrors(document, true)

      document.getElementById("page-heading").text() shouldBe "Add organisation"
      document.getElementById("organisation-name-label").text() shouldBe "Organisation name"
      Option(document.getElementById("organisationname")).isDefined shouldBe true
      Option(document.getElementById("continue-button")).isDefined shouldBe true
    }
  }
}