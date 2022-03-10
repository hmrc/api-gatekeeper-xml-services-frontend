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

package uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.csvupload

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.FakeRequest
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.config.AppConfig
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.LoggedInUser
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.helper.CommonViewSpec
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.csvupload.UsersCsvUploadView
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.helper.WithCSRFAddToken
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.forms.Forms.CsvData
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.utils.ViewSpecHelpers

class UsersCsvUploadViewSpec extends CommonViewSpec with WithCSRFAddToken with ViewSpecHelpers {

  trait Setup {
    val mockAppConfig = mock[AppConfig]
    val usersCsvUploadView = app.injector.instanceOf[UsersCsvUploadView]
    val loggedInUser = LoggedInUser(Some("Test User"))
  }

  "CSV Upload View" should {

    def validateFormErrors(document: Document, isError: Boolean)={
      if(isError){
        document.getElementById("data-field-error-csv-data-input").text() shouldBe "Error: Enter CSV data"
      }

      Option(document.getElementById("data-field-error-csv-data-input")).isDefined shouldBe isError
    }

    "render the users csv upload page correctly when no errors" in new Setup {
      val page = usersCsvUploadView.render(CsvData.form, FakeRequest().withCSRFToken, loggedInUser, messagesProvider.messages)
      val document: Document = Jsoup.parse(page.body)

      validateFormErrors(document, false)
      validateUsersCSVUploadPage(document)
     
    }

    "render the users csv upload page correctly when errors exist" in new Setup {

      val page = usersCsvUploadView.render(CsvData.form.withError("csv-data-input", "csvdata.error.required"),
                                      FakeRequest().withCSRFToken, loggedInUser, messagesProvider.messages)
      val document: Document = Jsoup.parse(page.body)

      validateFormErrors(document, true)

      validateUsersCSVUploadPage(document)
    }
  }
}

