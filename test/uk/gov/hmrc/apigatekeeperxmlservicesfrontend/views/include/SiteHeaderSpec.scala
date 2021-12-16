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

package uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.include

import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.helper.CommonViewSpec
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.config.AppConfig
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.include.SiteHeader
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

import scala.collection.JavaConverters._
class SiteHeaderSpec extends CommonViewSpec {

  trait Setup {
    val mockAppConfig = mock[AppConfig]
    val siteHeader = app.injector.instanceOf[SiteHeader]
    val expectedMenuItems =  List("Applications", "Developers","Email", "API Approvals", "XML")
  }

  "SiteHeader" should {

    "render correctly" in new Setup {
       val component = siteHeader.render(messagesProvider.messages)
       val document = Jsoup.parse(component.body)
       val navigation: Element = document.getElementById("navigation")
       val navigationTextItems =  navigation.children().eachText()
       navigationTextItems.asScala.filterNot(expectedMenuItems.contains(_)).isEmpty shouldBe true
        // check the links somehow

    }
  }

}
