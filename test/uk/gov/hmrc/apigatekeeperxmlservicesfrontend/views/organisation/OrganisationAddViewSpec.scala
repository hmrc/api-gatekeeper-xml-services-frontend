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
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.helper.CommonViewSpec
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.forms.AddOrganisation

import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.organisation.OrganisationAddView
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class OrganisationAddViewSpec extends CommonViewSpec {

  trait Setup extends OrganisationTestData {
    val mockAppConfig = mock[AppConfig]
    val organisationAddView = app.injector.instanceOf[OrganisationAddView]

  }

  "Organisation Add View" should {

    "render the organisation add page correctly" in new Setup {
  
      val page = organisationAddView.render(AddOrganisation.form, FakeRequest(), messagesProvider.messages, mockAppConfig)
      val document: Document = Jsoup.parse(page.body)

      document.getElementById("page-heading").text() shouldBe "Add organisation"
      document.getElementById("organisation-name-label").text() shouldBe "Organisation name"
      Option(document.getElementById("organisation-name-input")).isDefined shouldBe true
      Option(document.getElementById("continue-button")).isDefined shouldBe true
    }
  }
}
