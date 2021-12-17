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

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.config.AppConfig
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.utils.OrganisationTestData
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.helper.CommonViewSpec
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.organisation.OrganisationSearchView

import scala.collection.JavaConverters._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.Organisation

class OrganisationSearchViewSpec extends CommonViewSpec {

  trait Setup extends OrganisationTestData {
    val mockAppConfig = mock[AppConfig]
    val organisationSearchView = app.injector.instanceOf[OrganisationSearchView]

    def testRadioButton(document: Document, radioButtonId: String, isChecked: Boolean) = {
      withClue(s"radio button $radioButtonId test failed") {
        document.getElementById(radioButtonId)
          .attributes().asList().asScala.map(_.getKey)
          .contains("checked") shouldBe isChecked
      }
    }

    def testStandardComponents(document: Document) = {
      document.title() shouldBe "Manage XML Organisations - HMRC API Gatekeeper"
      document.getElementById("page-heading").text() shouldBe "Search for XML organisations"
      document.getElementById("search-by-hint").text() shouldBe "Choose to search by vendor ID, email address or organisation."

      testRadioButton(document, "vendor-id-input", isChecked = true)
      document.getElementById("vendor-id-label").text() shouldBe "Vendor ID"
    }
  }

  "Organisation Search View" should {

    def validateOrganisationRow(rowId: Int, org: Organisation, document: Document) = {
      document.getElementById(s"vendor-id-$rowId").text() shouldBe org.vendorId.value.toString
      document.getElementById(s"name-$rowId").text() shouldBe org.name.value
      document.getElementById(s"manage-org-$rowId-link").attr("href") shouldBe s"/api-gatekeeper-xml-services/organisations/${org.organisationId.value.toString}"
    }

    "render page correctly on initial load when organisations list is empty" in new Setup {
      val page: Html = organisationSearchView.render(List.empty, showTable = false, FakeRequest(), messagesProvider.messages, mockAppConfig)
      val document: Document = Jsoup.parse(page.body)
      testStandardComponents(document)

      Option(document.getElementById("search-organisation-input")).isEmpty shouldBe false
      document.getElementById("search-organisation-button").text() shouldBe "Search"

      Option(document.getElementById("results-table")).isDefined shouldBe false
      Option(document.getElementById("vendor-head")).isDefined shouldBe false
      Option(document.getElementById("organisation-head")).isDefined shouldBe false
    }

    "render page correctly when organisations list is populated" in new Setup {
      val page: Html = organisationSearchView.render(organisations, showTable = true, FakeRequest(), messagesProvider.messages, mockAppConfig)
      val document: Document = Jsoup.parse(page.body)
      testStandardComponents(document)

      Option(document.getElementById("search-organisation-input")).isEmpty shouldBe false
      document.getElementById("search-organisation-button").text() shouldBe "Search"

      Option(document.getElementById("results-table")).isDefined shouldBe true
      validateOrganisationRow(0, org1, document)
      validateOrganisationRow(1, org2, document)
      validateOrganisationRow(2, org3, document)

    }

    "render page correctly when organisations list is empty" in new Setup {
      val page: Html = organisationSearchView.render(List.empty, showTable = true, FakeRequest(), messagesProvider.messages, mockAppConfig)
      val document: Document = Jsoup.parse(page.body)
      testStandardComponents(document)

      Option(document.getElementById("search-organisation-input")).isEmpty shouldBe false
      document.getElementById("search-organisation-button").text() shouldBe "Search"

      Option(document.getElementById("results-table")).isDefined shouldBe true
      Option(document.getElementById("vendor-head")).isDefined shouldBe true
      Option(document.getElementById("organisation-head")).isDefined shouldBe true
    }

  }
}
