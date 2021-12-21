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

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.config.AppConfig
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.helper.CommonViewSpec


import scala.collection.JavaConverters._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.include.FieldError
import play.api.data.FormError

class FieldErrorSpec extends CommonViewSpec {

  trait Setup {
    val mockAppConfig = mock[AppConfig]

   
  }

  "FieldError" should {

    "render correctly" in new Setup {
      val formErrors = Seq(FormError("organisationname", "organisationname.error.required"))
     val view = FieldError.render(formErrors, "organisationname", messagesProvider.messages)

     val document = Jsoup.parse(view.body) 
     println(view.body)
    }

    // where key = fieldname
    // key != fieldname

  }

}

