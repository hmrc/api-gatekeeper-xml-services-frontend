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

package uk.gov.hmrc.apigatekeeperxmlservicesfrontend.controllers

import play.api.http.Status
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.ForbiddenView
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.organisation.OrganisationSearchView

import scala.concurrent.Future
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.VendorId

import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.ErrorTemplate
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.connectors.XmlServicesConnector
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.controllers.OrganisationController.vendorIdParameterName
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.utils.OrganisationTestData
import uk.gov.hmrc.http.Upstream4xxResponse
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.http.HeaderCarrier
import org.jsoup.Jsoup
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.organisation.OrganisationDetailsView
import akka.stream.TLSClientAuth

class OrganisationControllerSpec extends ControllerBaseSpec {

  trait Setup extends ControllerSetupBase with OrganisationTestData {
    val fakeRequest = FakeRequest("GET", "/organisations")
    val organisationSearchRequest = FakeRequest("GET", "/organisations-search")
    private lazy val forbiddenView = app.injector.instanceOf[ForbiddenView]
    private lazy val errorTemplate = app.injector.instanceOf[ErrorTemplate]
    private lazy val organisationSearchView = app.injector.instanceOf[OrganisationSearchView]
    private lazy val organisationDetailsView = app.injector.instanceOf[OrganisationDetailsView]

    val mockXmlServiceConnector = mock[XmlServicesConnector]

    val controller = new OrganisationController(
      mcc,
      organisationSearchView,
      organisationDetailsView,
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

    "return 200 and render search page when invalid search type provided and valid vendor id" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()

      when(mockXmlServiceConnector.findOrganisationsByParams(eqTo(Some(VendorId(vendorId))), eqTo(None))(*))
        .thenReturn(Future.successful(Right(organisations)))

      val result = controller.organisationsSearchAction("unknown", Some(vendorId.toString))(organisationSearchRequest)
      validatePageIsRendered(result)

      verify(mockXmlServiceConnector).findOrganisationsByParams(eqTo(Some(VendorId(vendorId))), *)(*)
    }

    "return 200 and render search page and empty table when connector returns not found error " in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()

      when(mockXmlServiceConnector.findOrganisationsByParams(eqTo(Some(VendorId(vendorId))), eqTo(None))(*))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("", 404, 404, Map.empty))))

      val result = controller.organisationsSearchAction("unknown", Some(vendorId.toString))(organisationSearchRequest)
      validatePageIsRendered(result)
      // check table head is rendered
      val document = Jsoup.parse(contentAsString(result))
      Option(document.getElementById("results-table")).isDefined shouldBe true
      Option(document.getElementById("vendor-head")).isDefined shouldBe true
      Option(document.getElementById("organisation-head")).isDefined shouldBe true

      verify(mockXmlServiceConnector).findOrganisationsByParams(eqTo(Some(VendorId(vendorId))), *)(*)
    }

    "return 500 and render error page when connector returns any error other than 404" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()

      when(mockXmlServiceConnector.findOrganisationsByParams(eqTo(Some(VendorId(vendorId))), eqTo(None))(*))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("", 500, 500))))

      val result = controller.organisationsSearchAction("unknown", Some(vendorId.toString))(organisationSearchRequest)
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

    "return 200 and display details view page" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()
      when(mockXmlServiceConnector.getOrganisationByOrganisationId(eqTo(org1.organisationId))(*))
      .thenReturn(Future.successful(Right(org1)))

      val result = controller.manageOrganisation(org1.organisationId)(fakeRequest)
      status(result) shouldBe Status.OK

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("org-name-heading").text() shouldBe "Name"
      document.getElementById("org-name-value").text() shouldBe org1.name.value

      document.getElementById("vendor-id-heading").text() shouldBe "Vendor ID"
      document.getElementById("vendor-id-value").text() shouldBe org1.vendorId.value.toString
    }

      "return 500 and render error page when connector returns any error other than 404" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()

     when(mockXmlServiceConnector.getOrganisationByOrganisationId(eqTo(org1.organisationId))(*))
      .thenReturn(Future.successful(Left(UpstreamErrorResponse("", 500, 500))))

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
}
