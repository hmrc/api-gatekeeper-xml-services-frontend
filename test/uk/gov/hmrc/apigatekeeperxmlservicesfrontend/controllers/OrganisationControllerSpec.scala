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

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.connectors.XmlServicesConnector
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.controllers.OrganisationController._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.utils.OrganisationTestData
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.helper.WithCSRFAddToken
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.organisation.{OrganisationAddView, OrganisationDetailsView, OrganisationSearchView}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.{ErrorTemplate, ForbiddenView}
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class OrganisationControllerSpec extends ControllerBaseSpec with WithCSRFAddToken {

  trait Setup extends ControllerSetupBase with OrganisationTestData {
    val fakeRequest = FakeRequest("GET", "/organisations")
    val organisationSearchRequest = FakeRequest("GET", "/organisations-search")
    private lazy val forbiddenView = app.injector.instanceOf[ForbiddenView]
    private lazy val errorTemplate = app.injector.instanceOf[ErrorTemplate]
    private lazy val organisationSearchView = app.injector.instanceOf[OrganisationSearchView]
    private lazy val organisationDetailsView = app.injector.instanceOf[OrganisationDetailsView]
    private lazy val organisationAddView = app.injector.instanceOf[OrganisationAddView]

    val mockXmlServiceConnector = mock[XmlServicesConnector]

    val controller = new OrganisationController(
      mcc,
      organisationSearchView,
      organisationDetailsView,
      organisationAddView,
      mockAuthConnector,
      forbiddenView,
      errorTemplate,
      mockXmlServiceConnector
    )

    def validatePageIsRendered(result: Future[Result]) = {
      status(result) shouldBe Status.OK
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
      contentAsString(result) contains "Search for XML organisations"
    }
  }

  "GET /organisations" should {
    "return 200" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()
      validatePageIsRendered(controller.organisationsPage(fakeRequest))
    }

    "return forbidden view" in new Setup {
      givenAUnsuccessfulLogin()
      val result = controller.organisationsPage(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
    }
  }

  "GET /organisations-search" should {

    "return 200 and render search page when vendor-id search type and valid vendor id" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()

      when(mockXmlServiceConnector.findOrganisationsByParams(eqTo(Some(VendorId(vendorId))), eqTo(None))(*))
        .thenReturn(Future.successful(Right(organisations)))

      val result = controller.organisationsSearchAction(vendorIdParameterName, Some(vendorId.toString))(organisationSearchRequest)
      validatePageIsRendered(result)

      verify(mockXmlServiceConnector).findOrganisationsByParams(eqTo(Some(VendorId(vendorId))), eqTo(None))(*)
    }

    "return 200 and render search page  when vendor-id search type and invalid (non numeric) vendor id" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()

      val result = controller.organisationsSearchAction(vendorIdParameterName, Some("NotANumber"))(organisationSearchRequest)
      validatePageIsRendered(result)

      verifyZeroInteractions(mockXmlServiceConnector)
    }

    "return 200 and render search page  when vendor-id search type and empty string provided for vendor id" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()
      when(mockXmlServiceConnector.findOrganisationsByParams(eqTo(None), eqTo(None))(*))
        .thenReturn(Future.successful(Right(organisations)))

      val result = controller.organisationsSearchAction(vendorIdParameterName, Some(""))(organisationSearchRequest)
      validatePageIsRendered(result)

      verify(mockXmlServiceConnector).findOrganisationsByParams(eqTo(None), eqTo(None))(*)
    }

    "return 200 and render search page when organisation-name search type and search text" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()
      val orgName = "I am an Org Name"
      when(mockXmlServiceConnector.findOrganisationsByParams(eqTo(None), eqTo(Some(orgName)))(*))
        .thenReturn(Future.successful(Right(organisations)))

      val result = controller.organisationsSearchAction(organisationNameParamName, Some(orgName))(organisationSearchRequest)
      validatePageIsRendered(result)

      verify(mockXmlServiceConnector).findOrganisationsByParams(*, eqTo(Some(orgName)))(*)
    }

    "return 200 and render search page when organisation-name search type without search text" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()

      when(mockXmlServiceConnector.findOrganisationsByParams(eqTo(None), eqTo(Some("")))(*))
        .thenReturn(Future.successful(Right(organisations)))

      val result = controller.organisationsSearchAction(organisationNameParamName, Some(""))(organisationSearchRequest)
      validatePageIsRendered(result)

      verify(mockXmlServiceConnector).findOrganisationsByParams(*, eqTo(Some("")))(*)
    }


    "return 200 and render search page connector receives and error" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()

      when(mockXmlServiceConnector.findOrganisationsByParams(eqTo(None), eqTo(Some("")))(*))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("", NOT_FOUND, NOT_FOUND))))

      val result = controller.organisationsSearchAction(organisationNameParamName, Some(""))(organisationSearchRequest)
      validatePageIsRendered(result)

      verify(mockXmlServiceConnector).findOrganisationsByParams(*, eqTo(Some("")))(*)
    }

    "return 200 and render search page when no search type and without search text" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()

      val result = controller.organisationsSearchAction("", Some(""))(organisationSearchRequest)
      validatePageIsRendered(result)

      verifyZeroInteractions(mockXmlServiceConnector)
    }

    "return 200 and render search page when invalid search type provided and valid vendor id" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()

      val result = controller.organisationsSearchAction("unknown", Some(vendorId.toString))(organisationSearchRequest)
      validatePageIsRendered(result)
      // check table head is rendered
      val document = Jsoup.parse(contentAsString(result))
      Option(document.getElementById("results-table")).isDefined shouldBe true
      Option(document.getElementById("vendor-head")).isDefined shouldBe true
      Option(document.getElementById("organisation-head")).isDefined shouldBe true

      verifyZeroInteractions(mockXmlServiceConnector)
    }

    "return 500 and render error page when connector returns any error other than 404" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()

      when(mockXmlServiceConnector.findOrganisationsByParams(eqTo(Some(VendorId(vendorId))), eqTo(None))(*))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR))))

      val result = controller.organisationsSearchAction("vendor-id", Some(vendorId.toString))(organisationSearchRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("page-heading").text() shouldBe "Internal Server Error"
      document.getElementById("page-body").text() shouldBe "Internal Server Error"

      verifyNoMoreInteractions(mockXmlServiceConnector)
    }

    "return forbidden view" in new Setup {
      givenAUnsuccessfulLogin()
      val result = controller.organisationsSearchAction("unknown", Some(vendorId.toString))(organisationSearchRequest)

      status(result) shouldBe Status.SEE_OTHER
    }
  }

  "GET /organisations/orgId" should {

    def validatePageRender(document: Document, org: Organisation) = {

      document.getElementById("org-name-heading").text() shouldBe "Name"
      document.getElementById("org-name-value").text() shouldBe org.name

      document.getElementById("vendor-id-heading").text() shouldBe "Vendor ID"
      document.getElementById("vendor-id-value").text() shouldBe org.vendorId.value.toString

      document.getElementById("team-members-heading").text() shouldBe "Team members"

      if (org.collaborators.nonEmpty) {
        document.getElementById("team-members-value").text() shouldBe "email1 email2"
        document.getElementById("copy-emails").attr("onClick") shouldBe "copyToClipboard('email1;email2;');"
      }
    }

    "return 200 and display details view page when users in org" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()
      when(mockXmlServiceConnector.getOrganisationByOrganisationId(eqTo(org1.organisationId))(*))
        .thenReturn(Future.successful(Right(org1)))

      val result = controller.manageOrganisation(org1.organisationId)(fakeRequest)
      status(result) shouldBe Status.OK
      val document = Jsoup.parse(contentAsString(result))
      validatePageRender(document, org1)

    }

    "return 200 and display details view page no users in org" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()
      when(mockXmlServiceConnector.getOrganisationByOrganisationId(eqTo(org1.organisationId))(*))
        .thenReturn(Future.successful(Right(org1)))

      val result = controller.manageOrganisation(org1.organisationId)(fakeRequest)
      status(result) shouldBe Status.OK

      val document = Jsoup.parse(contentAsString(result))
      validatePageRender(document, org1)
    }

    "return 500 and render error page when connector returns any error other than 404" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()

      when(mockXmlServiceConnector.getOrganisationByOrganisationId(eqTo(org1.organisationId))(*))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR))))

      val result = controller.manageOrganisation(org1.organisationId)(fakeRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("page-heading").text() shouldBe "Internal Server Error"
      document.getElementById("page-body").text() shouldBe "Internal Server Error"

      verifyNoMoreInteractions(mockXmlServiceConnector)
    }

    "return forbidden view" in new Setup {
      givenAUnsuccessfulLogin()
      val result = controller.manageOrganisation(org1.organisationId)(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
    }
  }

  "organisationsAddPage" should {
    "display add page when authorised" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()
      val result = controller.organisationsAddPage()(fakeRequest.withCSRFToken)
      status(result) shouldBe OK
    }

    "return forbidden view when not authorised" in new Setup {
      givenAUnsuccessfulLogin()
      val result = controller.organisationsAddPage()(fakeRequest.withCSRFToken)
      status(result) shouldBe Status.SEE_OTHER
    }
  }

  "organisationsAddAction" should {
    "display organisation details page when create successful result returned from connector" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()
      when(mockXmlServiceConnector.addOrganisation(eqTo(org1.name))(*)).thenReturn(Future.successful(CreateOrganisationSuccessResult(org1)))

      val result = controller.organisationsAddAction()(fakeRequest.withCSRFToken.withFormUrlEncodedBody("organisationname" -> org1.name))
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).getOrElse("") shouldBe s"/api-gatekeeper-xml-services/organisations/${org1.organisationId.value}"

      verify(mockXmlServiceConnector).addOrganisation(eqTo(org1.name))(*)
    }

    "display internal server error when failure result returned from connector" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()
      when(mockXmlServiceConnector.addOrganisation(eqTo(org1.name))(*))
        .thenReturn(Future.successful(CreateOrganisationFailureResult(UpstreamErrorResponse("some error", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR))))

      val result = controller.organisationsAddAction()(fakeRequest.withCSRFToken.withFormUrlEncodedBody("organisationname" -> org1.name))
      status(result) shouldBe INTERNAL_SERVER_ERROR

      verify(mockXmlServiceConnector).addOrganisation(eqTo(org1.name))(*)
    }

    "display add page with error messages when invalid form provided" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()

      val result = controller.organisationsAddAction()(fakeRequest.withCSRFToken.withFormUrlEncodedBody("organisationname" -> ""))
      status(result) shouldBe BAD_REQUEST

      verifyZeroInteractions(mockXmlServiceConnector)
    }

    "return forbidden view when not authorised" in new Setup {
      givenAUnsuccessfulLogin()
      val result = controller.organisationsAddAction()(fakeRequest.withCSRFToken.withFormUrlEncodedBody("organisationname" -> org1.name))

      status(result) shouldBe Status.SEE_OTHER

    }
  }

}
