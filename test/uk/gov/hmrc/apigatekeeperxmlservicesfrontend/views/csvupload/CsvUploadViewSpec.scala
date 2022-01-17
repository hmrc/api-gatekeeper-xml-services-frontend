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
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.forms.CsvData
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.utils.OrganisationTestData
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.helper.CommonViewSpec
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.upload.CsvUploadView
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.helper.WithCSRFAddToken

class CsvUploadViewSpec extends CommonViewSpec with WithCSRFAddToken {

  trait Setup extends OrganisationTestData {
    val mockAppConfig = mock[AppConfig]
    val csvUploadView = app.injector.instanceOf[CsvUploadView]

  }

  "CSV Upload View" should {

    def validateFormErrors(document: Document, isError: Boolean)={
      if(isError){
        document.getElementById("data-field-error-csv-data-input").text() shouldBe "Error: Enter CSV data"
      }

      Option(document.getElementById("data-field-error-csv-data-input")).isDefined shouldBe isError
    }

    "render the csv upload page correctly when no errors" in new Setup {
      val page = csvUploadView.render(CsvData.form, FakeRequest().withCSRFToken, messagesProvider.messages)
      val document: Document = Jsoup.parse(page.body)

      validateFormErrors(document, false)

      document.getElementById("page-heading").text() shouldBe "Upload organisations as CSV"
      document.getElementById("csv-data-input-label").text() shouldBe "Provide CSV input here please"
      Option(document.getElementById("csv-data-input")).isDefined shouldBe true
      Option(document.getElementById("upload-csv-button")).isDefined shouldBe true
    }

    "render the csv upload page correctly when errors exist" in new Setup {

      val page = csvUploadView.render(CsvData.form.withError("csv-data-input", "csvdata.error.required"), 
                                      FakeRequest().withCSRFToken, messagesProvider.messages)
      val document: Document = Jsoup.parse(page.body)

      validateFormErrors(document, true)

      document.getElementById("page-heading").text() shouldBe "Upload organisations as CSV"
      document.getElementById("csv-data-input-label").text() shouldBe "Provide CSV input here please"
      Option(document.getElementById("csv-data-input")).isDefined shouldBe true
      Option(document.getElementById("upload-csv-button")).isDefined shouldBe true
    }
  }
}
