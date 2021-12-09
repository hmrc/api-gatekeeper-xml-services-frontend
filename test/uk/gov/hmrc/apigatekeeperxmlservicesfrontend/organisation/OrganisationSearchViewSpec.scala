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

package uk.gov.hmrc.apigatekeeperxmlservicesfrontend.organisation

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.helper.CommonViewSpec
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.organisation.OrganisationSearchView
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.config.AppConfig
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.Organisation
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.OrganisationId
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.VendorId

import java.{util => ju}
import scala.collection.JavaConverters._

class OrganisationSearchViewSpec extends CommonViewSpec {

  trait Setup {
    val mockAppConfig = mock[AppConfig]
    val organisationSearchView = app.injector.instanceOf[OrganisationSearchView]

    val org1 = Organisation(organisationId = OrganisationId(ju.UUID.randomUUID()), vendorId = VendorId(1), name = "Org 1")
    val org2 = org1.copy(vendorId = VendorId(2), name = "Org 2")
    val org3 = org1.copy(vendorId = VendorId(3), name = "Org 3")

    val organisations = List(org1, org2, org3)

    def testRadioButton(document: Document, radioButtonId: String, isChecked: Boolean) = {
      withClue(s"radio button $radioButtonId test failed") {
        document.getElementById(radioButtonId)
          .attributes().asList().asScala.map(_.getKey)
          .contains("checked") shouldBe isChecked
      }
    }
  }

  "Organisation Search View" should {

    "render page correctly when organisations list is empty" in new Setup {
      val page: Html = organisationSearchView.render(List.empty, FakeRequest(), messagesProvider.messages, mockAppConfig)
      val document: Document = Jsoup.parse(page.body)
      document.title() shouldBe "Manage XML Organisations - HMRC API Gatekeeper"
      document.getElementById("page-heading").text() shouldBe "Search for XML organisations"
      document.getElementById("search-by-hint").text() shouldBe "Choose to search by vendor ID, email address or organisation."

      testRadioButton(document, "vendor-id-input", true)
      document.getElementById("vendor-id-label").text() shouldBe "Vendor ID"

      Option(document.getElementById("search-organisation-input")).isEmpty shouldBe false
      document.getElementById("search-organisation-button").text() shouldBe "Search"

      Option(document.getElementById("results-table")).isDefined shouldBe false
    }

    "render page correctly when organisations list is populated" in new Setup {
      val page: Html = organisationSearchView.render(organisations, FakeRequest(), messagesProvider.messages, mockAppConfig)
      val document: Document = Jsoup.parse(page.body)
      document.title() shouldBe "Manage XML Organisations - HMRC API Gatekeeper"
      document.getElementById("page-heading").text() shouldBe "Search for XML organisations"
      document.getElementById("search-by-hint").text() shouldBe "Choose to search by vendor ID, email address or organisation."

      testRadioButton(document, "vendor-id-input", true)
      document.getElementById("vendor-id-label").text() shouldBe "Vendor ID"

      Option(document.getElementById("search-organisation-input")).isEmpty shouldBe false
      document.getElementById("search-organisation-button").text() shouldBe "Search"

      Option(document.getElementById("results-table")).isDefined shouldBe true
      document.getElementById("vendor-id-0").text() shouldBe org1.vendorId.value.toString
      document.getElementById("name-0").text() shouldBe org1.name
      document.getElementById("vendor-id-1").text() shouldBe org2.vendorId.value.toString
      document.getElementById("name-1").text() shouldBe org2.name
      document.getElementById("vendor-id-2").text() shouldBe org3.vendorId.value.toString
      document.getElementById("name-2").text() shouldBe org3.name
    }

  }
}
