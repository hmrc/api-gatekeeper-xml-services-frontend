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


class OrganisationSearchViewSpec extends CommonViewSpec {

  trait Setup {
    val organisationSearchView = app.injector.instanceOf[OrganisationSearchView]
  }

  "Organisation Search View" should {

    "render page correctly" in new Setup {
       val page : Html =    organisationSearchView.render(FakeRequest(), messagesProvider.messages)
       val document: Document = Jsoup.parse(page.body)
       document.getElementById("page-heading").text() shouldBe "Manage XML Organisations - HMRC API Gatekeeper"
       document.getElementById("page-body").text() shouldBe "This is your new service"
    }
  }

}
