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

package uk.gov.hmrc.apigatekeeperxmlservicesfrontend.controllers.csvupload

import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.connectors.XmlServicesConnector
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.controllers.{ControllerBaseSpec, ControllerSetupBase}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.OrganisationName
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.OrganisationWithNameAndVendorId
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.VendorId
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.services.CsvService
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.utils.OrganisationTestData
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.helper.WithCSRFAddToken
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.ErrorTemplate
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.ForbiddenView
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.csvupload.{OrganisationCsvUploadView, UsersCsvUploadView}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.utils.ViewSpecHelpers
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.ParsedUser
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.ServiceName

class CsvUploadControllerSpec extends ControllerBaseSpec with WithCSRFAddToken with ViewSpecHelpers {

  trait Setup extends ControllerSetupBase with OrganisationTestData {
    val organisationPageRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/organisation-page")
    val organisationActionRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/organisations-action")
    private lazy val forbiddenView = app.injector.instanceOf[ForbiddenView]
    private lazy val errorTemplate = app.injector.instanceOf[ErrorTemplate]
    private lazy val organisationCsvUploadView = app.injector.instanceOf[OrganisationCsvUploadView]
    private lazy val usersCsvUploadView = app.injector.instanceOf[UsersCsvUploadView]

    val mockCsvService: CsvService = mock[CsvService]
    val mockXmlServiceConnector: XmlServicesConnector = mock[XmlServicesConnector]

    val controller = new CsvUploadController(
      mcc,
      organisationCsvUploadView,
      usersCsvUploadView,
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

    val email = "a@b.com"
    override val firstName = "Joe"
    override val lastName = "Bloggs"
    val servicesString = "service1|service2"
    val vendorIds = List(VendorId(20001), VendorId(20002))

    val csvUsersTestData = s"""EMAIL,FIRSTNAME,LASTNAME,SERVICES,VENDORIDS
    $email, $firstName, $lastName, $servicesString, $vendorIds"""

    val parsedServices = List(ServiceName("service1"), ServiceName("service2"))
    val parsedUser = ParsedUser(email, firstName, lastName, parsedServices, vendorIds)

    def validatePageIsRendered(result: Future[Result]) = {
      status(result) shouldBe Status.OK
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
      contentAsString(result) contains "Upload organisations as CSV"
    }

    def createUploadUsersRequest(payload: String): FakeRequest[AnyContentAsFormUrlEncoded] = {
      FakeRequest("POST", "/organisation-action")
        .withCSRFToken
        .withFormUrlEncodedBody("csv-data-input" -> payload)
    }
  }

  "organisationPage" should {
    "return 200" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()
      validatePageIsRendered(controller.organisationPage(organisationPageRequest.withCSRFToken))
    }

    "return forbidden view when not authorised" in new Setup {
      givenAUnsuccessfulLogin()
      val result = controller.organisationPage(organisationPageRequest)
      status(result) shouldBe Status.SEE_OTHER
    }
  }

  "uploadOrganisationsCsvAction" should {

    "display organisation page with error messages when invalid form provided" in new Setup {
      val request: FakeRequest[AnyContentAsFormUrlEncoded] = createUploadUsersRequest("")
      givenTheGKUserIsAuthorisedAndIsANormalUser()
      val result: Future[Result] = controller.uploadOrganisationsCsvAction()(request)
      status(result) shouldBe BAD_REQUEST

      verifyZeroInteractions(mockCsvService)
    }

    "return forbidden view when not authorised" in new Setup {
      val request: FakeRequest[AnyContentAsFormUrlEncoded] = createUploadUsersRequest(validCsvPayloadWithOneRow)
      givenAUnsuccessfulLogin()

      val result: Future[Result] = controller.uploadOrganisationsCsvAction()(request)

      status(result) shouldBe Status.SEE_OTHER
      verifyZeroInteractions(mockCsvService)
      verifyZeroInteractions(mockXmlServiceConnector)
    }

    "display internal server error when failure result returned from CsvUploadService" in new Setup {
      val request: FakeRequest[AnyContentAsFormUrlEncoded] = createUploadUsersRequest(validCsvPayloadWithTwoRows)
      givenTheGKUserIsAuthorisedAndIsANormalUser()
      val exceptionMessage = "Parse Exception"
      when(mockCsvService.mapToOrganisationFromCsv(*)).thenThrow(new RuntimeException(exceptionMessage))

      val result: Future[Result] = controller.uploadOrganisationsCsvAction()(request)
      status(result) shouldBe INTERNAL_SERVER_ERROR

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("page-heading").text() shouldBe "Internal Server Error"
      document.getElementById("page-body").text() shouldBe exceptionMessage

      verify(mockCsvService).mapToOrganisationFromCsv(*)
      verifyZeroInteractions(mockXmlServiceConnector)
    }

    "Redirect to the organisation page when organisations are successfully parsed and the connector returns Right" in new Setup {
      val request: FakeRequest[AnyContentAsFormUrlEncoded] = createUploadUsersRequest(validCsvPayloadWithTwoRows)
      givenTheGKUserIsAuthorisedAndIsANormalUser()
      when(mockXmlServiceConnector.bulkAddOrganisations(*)(*)).thenReturn(Future.successful(Right(())))
      when(mockCsvService.mapToOrganisationFromCsv(*)).thenReturn(organisationsWithNameAndVendorIds)

      val result: Future[Result] = controller.uploadOrganisationsCsvAction()(request)
      status(result) shouldBe SEE_OTHER

      verify(mockCsvService).mapToOrganisationFromCsv(*)
      verify(mockXmlServiceConnector).bulkAddOrganisations(*)(*)
    }

    "Show error page when organisations are successfully parsed but the connector returns Left" in new Setup {
      val request: FakeRequest[AnyContentAsFormUrlEncoded] = createUploadUsersRequest(validCsvPayloadWithTwoRows)
      givenTheGKUserIsAuthorisedAndIsANormalUser()
      when(mockXmlServiceConnector.bulkAddOrganisations(*)(*)).thenReturn(Future.successful(Left(UpstreamErrorResponse("", INTERNAL_SERVER_ERROR, 1, Map.empty))))
      when(mockCsvService.mapToOrganisationFromCsv(*)).thenReturn(organisationsWithNameAndVendorIds)

      val result: Future[Result] = controller.uploadOrganisationsCsvAction()(request)
      status(result) shouldBe INTERNAL_SERVER_ERROR

      verify(mockCsvService).mapToOrganisationFromCsv(*)
      verify(mockXmlServiceConnector).bulkAddOrganisations(*)(*)
    }
  }

  "usersPage" should {
    "return 200 and render the page" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()

      val result: Future[Result] = controller.usersPage()(organisationPageRequest.withCSRFToken)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      validateUsersCSVUploadPage(document)
    }

    "return forbidden view when not authorised" in new Setup {
      givenAUnsuccessfulLogin()
      val result: Future[Result] = controller.usersPage()(organisationPageRequest.withCSRFToken)
      status(result) shouldBe Status.SEE_OTHER
    }
  }

  "uploadUsersCsvAction" should {

    "Redirect to the users page when users are successfully parsed" in new Setup {
      val request: FakeRequest[AnyContentAsFormUrlEncoded] = createUploadUsersRequest(csvUsersTestData)
      givenTheGKUserIsAuthorisedAndIsANormalUser()
      when(mockCsvService.mapToUsersFromCsv(*)(*)).thenReturn(Future.successful(List(parsedUser)))
      when(mockXmlServiceConnector.bulkAddUsers(eqTo(Seq(parsedUser)))(*))
        .thenReturn(Future.successful(Right(())))

      val result: Future[Result] = controller.uploadUsersCsvAction()(request)
      status(result) shouldBe SEE_OTHER

      verify(mockCsvService).mapToUsersFromCsv(*)(*)
      verify(mockXmlServiceConnector).bulkAddUsers(*)(*)
    }

    "show error page when call to upload users fails" in new Setup {
      val request: FakeRequest[AnyContentAsFormUrlEncoded] = createUploadUsersRequest(csvUsersTestData)
      givenTheGKUserIsAuthorisedAndIsANormalUser()
      when(mockCsvService.mapToUsersFromCsv(*)(*)).thenReturn(Future.successful(List(parsedUser)))
      when(mockXmlServiceConnector.bulkAddUsers(eqTo(Seq(parsedUser)))(*))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("", INTERNAL_SERVER_ERROR, 1, Map.empty))))

      val result: Future[Result] = controller.uploadUsersCsvAction()(request)
      status(result) shouldBe INTERNAL_SERVER_ERROR

      verify(mockCsvService).mapToUsersFromCsv(*)(*)
      verify(mockXmlServiceConnector).bulkAddUsers(*)(*)
    }

    "Redirect to the error page when service throws an exception" in new Setup {
      val request: FakeRequest[AnyContentAsFormUrlEncoded] = createUploadUsersRequest(csvUsersTestData)
      val exceptionMessage = "parse error"
      givenTheGKUserIsAuthorisedAndIsANormalUser()
      when(mockCsvService.mapToUsersFromCsv(*)(*)).thenThrow(new RuntimeException(exceptionMessage))

      val result: Future[Result] = controller.uploadUsersCsvAction()(request)
      status(result) shouldBe INTERNAL_SERVER_ERROR

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("page-heading").text() shouldBe "Internal Server Error"
      document.getElementById("page-body").text() shouldBe exceptionMessage

      verify(mockCsvService).mapToUsersFromCsv(*)(*)
      verifyZeroInteractions(mockXmlServiceConnector)
    }

    "display users page with error messages when invalid form provided" in new Setup {
      val request: FakeRequest[AnyContentAsFormUrlEncoded] = createUploadUsersRequest("")
      givenTheGKUserIsAuthorisedAndIsANormalUser()

      val result: Future[Result] = controller.uploadUsersCsvAction()(request)
      status(result) shouldBe BAD_REQUEST

      verifyZeroInteractions(mockCsvService)
      verifyZeroInteractions(mockXmlServiceConnector)
    }

    "return forbidden view when not authorised" in new Setup {
      val request: FakeRequest[AnyContentAsFormUrlEncoded] = createUploadUsersRequest("")
      givenAUnsuccessfulLogin()

      val result: Future[Result] = controller.uploadUsersCsvAction()(request)
      status(result) shouldBe Status.SEE_OTHER

      verifyZeroInteractions(mockCsvService)
      verifyZeroInteractions(mockXmlServiceConnector)
    }
  }
}
