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

package uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.helper.CommonViewSpec
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.HelloWorldPage

class HelloWorldViewSpec extends CommonViewSpec {

  trait Setup {
    val helloWorldPage = app.injector.instanceOf[HelloWorldPage]
  }

  "HelloWorldPage" should {
 
    "render hello world page correctly" in new Setup {
       val page : Html =    helloWorldPage.render(FakeRequest(), messagesProvider.messages)
       val document: Document = Jsoup.parse(page.body)
       document.getElementById("page-heading").text() shouldBe "api-gatekeeper-xml-services-frontend"
       document.getElementById("page-body").text() shouldBe "This is your new service"
    }
  }

}
