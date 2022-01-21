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

package uk.gov.hmrc.apigatekeeperxmlservicesfrontend.controllers

import play.api.http.Status
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.controllers.csvupload.CsvUploadController
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.services.CsvService
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.utils.OrganisationTestData
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.helper.WithCSRFAddToken
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.ErrorTemplate
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.ForbiddenView
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.csvupload.OrganisationCsvUploadView

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import org.jsoup.Jsoup
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.connectors.XmlServicesConnector
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.OrganisationWithNameAndVendorId
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.OrganisationName
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.VendorId
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.BulkFindAndCreateOrUpdateRequest

class CsvUploadControllerSpec extends ControllerBaseSpec with WithCSRFAddToken {

  trait Setup extends ControllerSetupBase with OrganisationTestData {
    val fakeRequest = FakeRequest("GET", "/organisation-page")
    val organisationActionRequest = FakeRequest("GET", "/organisations-action")
    private lazy val forbiddenView = app.injector.instanceOf[ForbiddenView]
    private lazy val errorTemplate = app.injector.instanceOf[ErrorTemplate]
    private lazy val organisationCsvUploadView = app.injector.instanceOf[OrganisationCsvUploadView]

    val mockCsvService = mock[CsvService]
    val mockXmlServiceConnector = mock[XmlServicesConnector]

    val controller = new CsvUploadController(
      mcc,
      organisationCsvUploadView,
      errorTemplate,
      mockAuthConnector,
      forbiddenView,
      mockCsvService,
      mockXmlServiceConnector
    )

    val validCsvPayloadWithOneRow = """VENDORID,NAME
    1110,TestOrganisation101"""

    val validCsvPayloadWithTwoRows = """VENDORID,NAME
    1110,TestOrganisation101
    1111,TestOrganisation102"""

    val organisationsWithNameAndVendorIds = Seq(
      OrganisationWithNameAndVendorId(OrganisationName("Test Organsation One"), VendorId(101)),
      OrganisationWithNameAndVendorId(OrganisationName("Test Organsation Two"), VendorId(102))
    )
    
    def validatePageIsRendered(result: Future[Result]) = {
      status(result) shouldBe Status.OK
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
      contentAsString(result) contains "Upload organisations as CSV"
    }
  }

  "GET /organisations-page" should {
    "return 200" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()
      validatePageIsRendered(controller.organisationPage(fakeRequest.withCSRFToken))
    }

    "return forbidden view when not authorised" in new Setup {
      givenAUnsuccessfulLogin()
      val result = controller.organisationPage(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
    }
  }

  "uploadOrganisationsCsvAction" should {

    "display organisation page with error messages when invalid form provided" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()

      val result = controller.uploadOrganisationsCsvAction()(fakeRequest.withCSRFToken.withFormUrlEncodedBody("csv-data-input" -> ""))
      status(result) shouldBe BAD_REQUEST

      verifyZeroInteractions(mockCsvService)
    }

    "return forbidden view when not authorised" in new Setup {
      givenAUnsuccessfulLogin()
      val result = controller.uploadOrganisationsCsvAction()(fakeRequest.withCSRFToken.withFormUrlEncodedBody("csv-data-input" -> validCsvPayloadWithOneRow))

      status(result) shouldBe Status.SEE_OTHER
      verifyZeroInteractions(mockCsvService)
    }

    "successfully parse the organisations then go back to organisation page" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()
      when(mockCsvService.mapToOrganisationFromCsv(*)).thenReturn(organisationsWithNameAndVendorIds)
      when(mockXmlServiceConnector.bulkFindAndCreateOrUpdate(eqTo(organisationsWithNameAndVendorIds))).thenReturn(Future.successful(Right(())))

      validatePageIsRendered(controller.uploadOrganisationsCsvAction()(fakeRequest.withCSRFToken.withFormUrlEncodedBody("csv-data-input" -> validCsvPayloadWithTwoRows)))
      verify(mockCsvService).mapToOrganisationFromCsv(*)
    }

    "display internal server error when failure result returned from CsvUploadService" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()
      val exceptionMessage = "Parse Exception"
      when(mockCsvService.mapToOrganisationFromCsv((*))).thenThrow(new RuntimeException(exceptionMessage))

      val result = controller.uploadOrganisationsCsvAction()(fakeRequest.withCSRFToken.withFormUrlEncodedBody("csv-data-input" -> validCsvPayloadWithTwoRows))
      status(result) shouldBe INTERNAL_SERVER_ERROR

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("page-heading").text() shouldBe "Internal Server Error"
      document.getElementById("page-body").text() shouldBe exceptionMessage

      verify(mockCsvService).mapToOrganisationFromCsv(*)
    }
  }
}
