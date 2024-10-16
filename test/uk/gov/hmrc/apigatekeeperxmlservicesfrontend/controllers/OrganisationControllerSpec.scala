/*
 * Copyright 2023 HM Revenue & Customs
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

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import play.api.http.Status
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.apiplatform.modules.common.domain.models.UserId
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.config.ErrorHandler
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.connectors.{ThirdPartyDeveloperConnector, XmlServicesConnector}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.controllers.OrganisationController._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.thirdpartydeveloper.UserResponse
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.utils.{OrganisationTestData, ViewSpecHelpers}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.helper.WithCSRFAddToken
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.organisation._
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles
import uk.gov.hmrc.apiplatform.modules.gkauth.services.{LdapAuthorisationServiceMockModule, StrideAuthorisationServiceMockModule}

class OrganisationControllerSpec extends ControllerBaseSpec with WithCSRFAddToken {

  trait Setup extends ControllerSetupBase with OrganisationTestData with ViewSpecHelpers with StrideAuthorisationServiceMockModule with LdapAuthorisationServiceMockModule {
    val fakeRequest                                = FakeRequest("GET", "/organisations")
    val organisationSearchRequest                  = FakeRequest("GET", "/organisations-search")
    private lazy val errorHandler                  = app.injector.instanceOf[ErrorHandler]
    private lazy val organisationSearchView        = app.injector.instanceOf[OrganisationSearchView]
    private lazy val organisationDetailsView       = app.injector.instanceOf[OrganisationDetailsView]
    private lazy val organisationAddView           = app.injector.instanceOf[OrganisationAddView]
    private lazy val organisationAddNewUserView    = app.injector.instanceOf[OrganisationAddNewUserView]
    private lazy val organisationUpdateView        = app.injector.instanceOf[OrganisationUpdateView]
    private lazy val organisationRemoveView        = app.injector.instanceOf[OrganisationRemoveView]
    private lazy val organisationRemoveSuccessView = app.injector.instanceOf[OrganisationRemoveSuccessView]

    val mockXmlServiceConnector         = mock[XmlServicesConnector]
    val mockThirdPartDeveloperConnector = mock[ThirdPartyDeveloperConnector]

    val controller = new OrganisationController(
      mcc,
      organisationSearchView,
      organisationDetailsView,
      organisationAddView,
      organisationAddNewUserView,
      organisationUpdateView,
      organisationRemoveView,
      organisationRemoveSuccessView,
      LdapAuthorisationServiceMock.aMock,
      StrideAuthorisationServiceMock.aMock,
      errorHandler,
      mockXmlServiceConnector,
      mockThirdPartDeveloperConnector
    )

    def validatePageIsRendered(result: Future[Result]) = {
      status(result) shouldBe Status.OK
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
      contentAsString(result) contains "Search for XML vendors"
    }

    def createFakePostRequest(params: (String, String)*): FakeRequest[AnyContentAsFormUrlEncoded] = {
      FakeRequest().withMethod(POST)
        .withCSRFToken.withFormUrlEncodedBody(params: _*)
    }
  }

  "GET /organisations" should {
    "return 200 for Stride authorised user" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      validatePageIsRendered(controller.organisationsPage(fakeRequest))
    }

    "return 200 for LDAP User" in new Setup {
      StrideAuthorisationServiceMock.Auth.sessionRecordNotFound
      LdapAuthorisationServiceMock.Auth.succeeds

      validatePageIsRendered(controller.organisationsPage(fakeRequest))
    }

    "return forbidden view" in new Setup {
      StrideAuthorisationServiceMock.Auth.sessionRecordNotFound
      LdapAuthorisationServiceMock.Auth.notAuthorised
      val result = controller.organisationsPage(fakeRequest)

      status(result) shouldBe Status.SEE_OTHER
    }
  }

  "GET /organisations/search" should {

    "return 200 and render search page when vendor-id search type and valid vendor id" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      when(mockXmlServiceConnector.findOrganisationsByParams(eqTo(Some(VendorId(vendorId))), eqTo(None))(*))
        .thenReturn(Future.successful(Right(organisations)))

      val result = controller.organisationsSearchAction(vendorIdParameterName, Some(vendorId.toString))(organisationSearchRequest)

      validatePageIsRendered(result)
      verify(mockXmlServiceConnector).findOrganisationsByParams(eqTo(Some(VendorId(vendorId))), eqTo(None))(*)
    }

    "return 200 and render search page  when vendor-id search type and invalid (non numeric) vendor id" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      val result = controller.organisationsSearchAction(vendorIdParameterName, Some("NotANumber"))(organisationSearchRequest)

      validatePageIsRendered(result)
      verifyZeroInteractions(mockXmlServiceConnector)
    }

    "return 200 and render search page  when vendor-id search type and empty string provided for vendor id" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      when(mockXmlServiceConnector.findOrganisationsByParams(eqTo(None), eqTo(None))(*))
        .thenReturn(Future.successful(Right(organisations)))

      val result = controller.organisationsSearchAction(vendorIdParameterName, Some(""))(organisationSearchRequest)

      validatePageIsRendered(result)
      verify(mockXmlServiceConnector).findOrganisationsByParams(eqTo(None), eqTo(None))(*)
    }

    "return 200 and render search page when organisation-name search type and search text" in new Setup {
      val orgName = "I am an Org Name"
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      when(mockXmlServiceConnector.findOrganisationsByParams(eqTo(None), eqTo(Some(orgName)))(*))
        .thenReturn(Future.successful(Right(organisations)))

      val result = controller.organisationsSearchAction(organisationNameParamName, Some(orgName))(organisationSearchRequest)

      validatePageIsRendered(result)
      verify(mockXmlServiceConnector).findOrganisationsByParams(*, eqTo(Some(orgName)))(*)
    }

    "return 200 and render search page when organisation-name search type without search text" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      when(mockXmlServiceConnector.findOrganisationsByParams(eqTo(None), eqTo(Some("")))(*))
        .thenReturn(Future.successful(Right(organisations)))

      val result = controller.organisationsSearchAction(organisationNameParamName, Some(""))(organisationSearchRequest)

      validatePageIsRendered(result)
      verify(mockXmlServiceConnector).findOrganisationsByParams(*, eqTo(Some("")))(*)
    }

    "return 200 and render search page connector receives and error" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      when(mockXmlServiceConnector.findOrganisationsByParams(eqTo(None), eqTo(Some("")))(*))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("", NOT_FOUND, NOT_FOUND))))

      val result = controller.organisationsSearchAction(organisationNameParamName, Some(""))(organisationSearchRequest)

      validatePageIsRendered(result)
      verify(mockXmlServiceConnector).findOrganisationsByParams(*, eqTo(Some("")))(*)
    }

    "return 200 and render search page when no search type and without search text" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      val result = controller.organisationsSearchAction("", Some(""))(organisationSearchRequest)

      validatePageIsRendered(result)
      verifyZeroInteractions(mockXmlServiceConnector)
    }

    "return 200 and render search page when invalid search type provided and valid vendor id" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      val result   = controller.organisationsSearchAction("unknown", Some(vendorId.toString))(organisationSearchRequest)
      val document = Jsoup.parse(contentAsString(result))

      validatePageIsRendered(result)
      // check table head is rendered
      Option(document.getElementById("results-table")).isDefined shouldBe true
      Option(document.getElementById("vendor-head")).isDefined shouldBe true
      Option(document.getElementById("organisation-head")).isDefined shouldBe true
      verifyZeroInteractions(mockXmlServiceConnector)
    }

    "return 500 and render error page when connector returns any error other than 404" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      when(mockXmlServiceConnector.findOrganisationsByParams(eqTo(Some(VendorId(vendorId))), eqTo(None))(*))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR))))

      val result   = controller.organisationsSearchAction("vendor-id", Some(vendorId.toString))(organisationSearchRequest)
      val document = Jsoup.parse(contentAsString(result))

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
      document.getElementById("page-heading").text() shouldBe "Sorry, there is a problem with the service"
      document.getElementById("page-body").text() shouldBe "Try again later."
      verifyNoMoreInteractions(mockXmlServiceConnector)
    }

    "return view when LDAP" in new Setup {
      StrideAuthorisationServiceMock.Auth.sessionRecordNotFound
      LdapAuthorisationServiceMock.Auth.succeeds

      val result = controller.organisationsSearchAction("unknown", Some(vendorId.toString))(organisationSearchRequest)

      status(result) shouldBe Status.OK
    }

    "return forbidden view" in new Setup {
      StrideAuthorisationServiceMock.Auth.sessionRecordNotFound
      LdapAuthorisationServiceMock.Auth.notAuthorised

      val result = controller.organisationsSearchAction("unknown", Some(vendorId.toString))(organisationSearchRequest)

      status(result) shouldBe Status.SEE_OTHER
    }
  }

  "GET /organisations/orgId" should {

    def validatePageRender(document: Document, org: Organisation) = {
      document.getElementById("org-name-heading").text() shouldBe "Vendor Name"
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
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      when(mockXmlServiceConnector.getOrganisationByOrganisationId(eqTo(org1.organisationId))(*))
        .thenReturn(Future.successful(Right(org1)))

      when(mockXmlServiceConnector.getOrganisationUsersByOrganisationId(eqTo(org1.organisationId))(*))
        .thenReturn(Future.successful(Right(organisationUsers)))

      val result   = controller.viewOrganisationPage(org1.organisationId)(fakeRequest)
      val document = Jsoup.parse(contentAsString(result))

      status(result) shouldBe Status.OK
      validatePageRender(document, org1)
    }

    "return 200 and display details view page no users in org" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      when(mockXmlServiceConnector.getOrganisationByOrganisationId(eqTo(org1.organisationId))(*))
        .thenReturn(Future.successful(Right(org1)))
      when(mockXmlServiceConnector.getOrganisationUsersByOrganisationId(eqTo(org1.organisationId))(*))
        .thenReturn(Future.successful(Right(List.empty)))

      val result   = controller.viewOrganisationPage(org1.organisationId)(fakeRequest)
      val document = Jsoup.parse(contentAsString(result))

      status(result) shouldBe Status.OK
      validatePageRender(document, org1)
    }

    "return 500 and render error page when connector returns any error other than 404" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      when(mockXmlServiceConnector.getOrganisationByOrganisationId(eqTo(org1.organisationId))(*))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR))))
      when(mockXmlServiceConnector.getOrganisationUsersByOrganisationId(eqTo(org1.organisationId))(*))
        .thenReturn(Future.successful(Right(List.empty)))

      val result   = controller.viewOrganisationPage(org1.organisationId)(fakeRequest)
      val document = Jsoup.parse(contentAsString(result))

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
      document.getElementById("page-heading").text() shouldBe "Sorry, there is a problem with the service"
      document.getElementById("page-body").text() shouldBe "Try again later."
      verifyNoMoreInteractions(mockXmlServiceConnector)
    }

    "return 500 and render error page when connector returns error getting users" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      when(mockXmlServiceConnector.getOrganisationByOrganisationId(eqTo(org1.organisationId))(*))
        .thenReturn(Future.successful(Right(org1)))
      when(mockXmlServiceConnector.getOrganisationUsersByOrganisationId(eqTo(org1.organisationId))(*))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR))))

      val result   = controller.viewOrganisationPage(org1.organisationId)(fakeRequest)
      val document = Jsoup.parse(contentAsString(result))

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
      document.getElementById("page-heading").text() shouldBe "Sorry, there is a problem with the service"
      document.getElementById("page-body").text() shouldBe "Try again later."
      verifyNoMoreInteractions(mockXmlServiceConnector)
    }

    "return forbidden view" in new Setup {
      StrideAuthorisationServiceMock.Auth.sessionRecordNotFound
      LdapAuthorisationServiceMock.Auth.notAuthorised

      val result = controller.viewOrganisationPage(org1.organisationId)(fakeRequest)

      status(result) shouldBe Status.SEE_OTHER
    }
  }

  "organisationsAddPage" should {
    "display add page when authorised" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      val result   = controller.organisationsAddPage()(fakeRequest.withCSRFToken)
      val document = Jsoup.parse(contentAsString(result))

      status(result) shouldBe OK
      validateAddOrganisationPage(document)
      validateFormErrors(document, None)
    }

    "return forbidden view when not authorised" in new Setup {
      StrideAuthorisationServiceMock.Auth.sessionRecordNotFound

      val result = controller.organisationsAddPage()(fakeRequest.withCSRFToken)

      status(result) shouldBe Status.SEE_OTHER
    }
  }

  "organisationsAddAction" should {

    "display organisation details page when email is existing user and create successful result returned from connector" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      val userId       = UserId.random
      val userResponse = UserResponse(collaborator1.email, firstName, lastName, verified = true, userId)

      when(mockThirdPartDeveloperConnector.getByEmails(eqTo(List(collaborator1.email)))(*)).thenReturn(Future.successful(Right(List(userResponse))))
      when(mockXmlServiceConnector.addOrganisation(eqTo(org1.name), eqTo(collaborator1.email), *, *)(*)).thenReturn(Future.successful(CreateOrganisationSuccess(org1)))

      val result = controller.organisationsAddAction()(createFakePostRequest("organisationName" -> org1.name, "emailAddress" -> collaborator1.email))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).getOrElse("") shouldBe s"/api-gatekeeper-xml-services/organisations/${org1.organisationId.value}"
      verify(mockXmlServiceConnector).addOrganisation(eqTo(org1.name), eqTo(collaborator1.email), *, *)(*)
      verify(mockThirdPartDeveloperConnector).getByEmails(eqTo(List(collaborator1.email)))(*)
    }

    "display add new user page when user does not exist" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      when(mockThirdPartDeveloperConnector.getByEmails(eqTo(List(collaborator1.email)))(*)).thenReturn(Future.successful(Right(List.empty)))

      val result   = controller.organisationsAddAction()(createFakePostRequest("organisationName" -> org1.name, "emailAddress" -> collaborator1.email))
      val document = Jsoup.parse(contentAsString(result))

      status(result) shouldBe OK
      validateOrganisationAddNewUserPage(document, org1.name, collaborator1.email)
      verify(mockThirdPartDeveloperConnector).getByEmails(eqTo(List(collaborator1.email)))(*)
      verifyZeroInteractions(mockXmlServiceConnector)
    }

    "return 500 when third party developer returns error" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      when(mockThirdPartDeveloperConnector.getByEmails(eqTo(List(collaborator1.email)))(*)).thenReturn(Future.successful(Left(UpstreamErrorResponse(
        "some error",
        INTERNAL_SERVER_ERROR,
        INTERNAL_SERVER_ERROR
      ))))

      val result = controller.organisationsAddAction()(createFakePostRequest("organisationName" -> org1.name, "emailAddress" -> collaborator1.email))

      status(result) shouldBe INTERNAL_SERVER_ERROR
      verify(mockThirdPartDeveloperConnector).getByEmails(eqTo(List(collaborator1.email)))(*)
      verifyZeroInteractions(mockXmlServiceConnector)
    }

    "not allow spaces as organisation name" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      val result   = controller.organisationsAddAction()(createFakePostRequest("organisationName" -> "  ", "emailAddress" -> "  "))
      val document = Jsoup.parse(contentAsString(result))

      status(result) shouldBe BAD_REQUEST
      validateAddOrganisationPage(document)
      validateFormErrors(document, Some("Enter a vendor name"))
      validateFormErrors(document, Some("Provide a valid email address"))
      verifyZeroInteractions(mockXmlServiceConnector)
      verifyZeroInteractions(mockThirdPartDeveloperConnector)
    }

    "display internal server error when failure result returned from connector" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      val userId       = UserId.random
      val userResponse = UserResponse(collaborator1.email, firstName, lastName, verified = true, userId)

      when(mockThirdPartDeveloperConnector.getByEmails(eqTo(List(collaborator1.email)))(*))
        .thenReturn(Future.successful(Right(List(userResponse))))
      when(mockXmlServiceConnector.addOrganisation(eqTo(org1.name), eqTo(collaborator1.email), *, *)(*))
        .thenReturn(Future.successful(CreateOrganisationFailure(UpstreamErrorResponse("some error", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR))))

      val result = controller.organisationsAddAction()(createFakePostRequest("organisationName" -> org1.name, "emailAddress" -> collaborator1.email))

      status(result) shouldBe INTERNAL_SERVER_ERROR
      verify(mockXmlServiceConnector).addOrganisation(eqTo(org1.name), eqTo(collaborator1.email), *, *)(*)
    }

    "display add page with error messages when invalid form provided" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      val result   = controller.organisationsAddAction()(createFakePostRequest("organisationName" -> "", "emailAddress" -> ""))
      val document = Jsoup.parse(contentAsString(result))

      status(result) shouldBe BAD_REQUEST
      validateAddOrganisationPage(document)
      validateFormErrors(document, Some("Enter a vendor name"))
      validateFormErrors(document, Some("Enter an email address"))
      verifyZeroInteractions(mockXmlServiceConnector)
    }

    "return forbidden view when not authorised" in new Setup {
      StrideAuthorisationServiceMock.Auth.sessionRecordNotFound
      val result = controller.organisationsAddAction()(createFakePostRequest("organisationName" -> org1.name))

      status(result) shouldBe Status.SEE_OTHER
    }
  }

  "organisationsAddWithNewUserAction" should {

    "redirect to organisation page, call add organisation when user is authorised and form is valid" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      when(mockXmlServiceConnector.addOrganisation(eqTo(org1.name), eqTo(collaborator1.email), eqTo(firstName), eqTo(lastName))(*))
        .thenReturn(Future.successful(CreateOrganisationSuccess(org1)))

      val result = controller.organisationsAddWithNewUserAction()(
        createFakePostRequest("organisationName" -> organisationWithCollaborators.name, "emailAddress" -> collaborator1.email, "firstName" -> firstName, "lastName" -> lastName)
      )

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).getOrElse("") shouldBe s"/api-gatekeeper-xml-services/organisations/${org1.organisationId.value}"
      verify(mockXmlServiceConnector).addOrganisation(eqTo(org1.name), eqTo(collaborator1.email), eqTo(firstName), eqTo(lastName))(*)
    }

    "display the add new user page with errors when user is authorised but form is invalid" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      val result = controller.organisationsAddWithNewUserAction()(createFakePostRequest(
        "organisationName" -> organisationWithCollaborators.name,
        "emailAddress"     -> collaborator1.email,
        "firstName"        -> "",
        "lastName"         -> ""
      ))

      val document = Jsoup.parse(contentAsString(result))

      status(result) shouldBe BAD_REQUEST
      validateFormErrors(document, Some("Enter a first name"))
      validateFormErrors(document, Some("Enter a last name"))
      validateOrganisationAddNewUserPage(document, organisationWithCollaborators.name, collaborator1.email)
      verifyZeroInteractions(mockXmlServiceConnector)
    }

    "return 500 when call to add organisation fails but the user is authorised and form is valid" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      when(mockXmlServiceConnector.addOrganisation(eqTo(org1.name), eqTo(collaborator1.email), eqTo(firstName), eqTo(lastName))(*))
        .thenReturn(Future.successful(CreateOrganisationFailure(UpstreamErrorResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR))))

      val result = controller.organisationsAddWithNewUserAction()(createFakePostRequest(
        "organisationName" -> organisationWithCollaborators.name,
        "emailAddress"     -> collaborator1.email,
        "firstName"        -> firstName,
        "lastName"         -> lastName
      ))

      status(result) shouldBe INTERNAL_SERVER_ERROR
      verify(mockXmlServiceConnector).addOrganisation(eqTo(org1.name), eqTo(collaborator1.email), eqTo(firstName), eqTo(lastName))(*)
    }
  }

  "updateOrganisationsDetailsPage" should {
    "display updateOrganisationsDetails page when authorised and organisation exists" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      when(mockXmlServiceConnector.getOrganisationByOrganisationId(eqTo(organisationId1))(*[HeaderCarrier]))
        .thenReturn(Future.successful(Right(org1)))

      val result   = controller.updateOrganisationsDetailsPage(organisationId1)(fakeRequest.withCSRFToken)
      val document = Jsoup.parse(contentAsString(result))

      status(result) shouldBe OK
      validateUpdateOrganisationDetailsPage(document)
      validateFormErrors(document, None)

    }

    "display internal server error page when authorised and but organisation does not exist" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      when(mockXmlServiceConnector.getOrganisationByOrganisationId(eqTo(organisationId1))(*[HeaderCarrier]))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("", NOT_FOUND, NOT_FOUND))))

      val result = controller.updateOrganisationsDetailsPage(organisationId1)(fakeRequest.withCSRFToken)

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return forbidden view when not authorised" in new Setup {
      StrideAuthorisationServiceMock.Auth.sessionRecordNotFound

      val result = controller.updateOrganisationsDetailsPage(organisationId1)(fakeRequest.withCSRFToken)

      status(result) shouldBe Status.SEE_OTHER
    }

    "updateOrganisationsDetailsAction" should {

      "display organisation details page when create successful result returned from connector" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        when(mockXmlServiceConnector.getOrganisationByOrganisationId(eqTo(organisationId1))(*)).thenReturn(Future.successful(Right(org1)))
        when(mockXmlServiceConnector.updateOrganisationDetails(eqTo(organisationId1), eqTo(org1.name))(*)).thenReturn(Future.successful(UpdateOrganisationDetailsSuccess(org1)))

        val result = controller.updateOrganisationsDetailsAction(organisationId1)(createFakePostRequest("organisationName" -> org1.name))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).getOrElse("") shouldBe s"/api-gatekeeper-xml-services/organisations/${org1.organisationId.value}"
        verify(mockXmlServiceConnector).updateOrganisationDetails(eqTo(organisationId1), eqTo(org1.name))(*)
      }

      "display internal server error when failure result returned from connector get organisation" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

        when(mockXmlServiceConnector.getOrganisationByOrganisationId(eqTo(organisationId1))(*))
          .thenReturn(Future.successful(Left(UpstreamErrorResponse("some error", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR))))

        val result = controller.updateOrganisationsDetailsAction(organisationId1)(createFakePostRequest("organisationName" -> org1.name))
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      "display internal server error when failure result returned from connector" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        when(mockXmlServiceConnector.getOrganisationByOrganisationId(eqTo(organisationId1))(*)).thenReturn(Future.successful(Right(org1)))
        when(mockXmlServiceConnector.updateOrganisationDetails(eqTo(organisationId1), eqTo(org1.name))(*))
          .thenReturn(Future.successful(UpdateOrganisationDetailsFailure(UpstreamErrorResponse("some error", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR))))

        val result = controller.updateOrganisationsDetailsAction(organisationId1)(createFakePostRequest("organisationName" -> org1.name))
        status(result) shouldBe INTERNAL_SERVER_ERROR
        verify(mockXmlServiceConnector).updateOrganisationDetails(eqTo(organisationId1), eqTo(org1.name))(*)
      }

      "display update page with error messages when invalid form provided" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        when(mockXmlServiceConnector.getOrganisationByOrganisationId(eqTo(organisationId1))(*)).thenReturn(Future.successful(Right(org1)))

        val result   = controller.updateOrganisationsDetailsAction(organisationId1)(createFakePostRequest("organisationName" -> ""))
        val document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe BAD_REQUEST
        validateUpdateOrganisationDetailsPage(document)
        validateFormErrors(document, Some("Enter a vendor name"))
      }

      "not allow spaces in form" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        when(mockXmlServiceConnector.getOrganisationByOrganisationId(eqTo(organisationId1))(*)).thenReturn(Future.successful(Right(org1)))

        val result   = controller.updateOrganisationsDetailsAction(organisationId1)(createFakePostRequest("organisationName" -> "  "))
        val document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe BAD_REQUEST
        validateUpdateOrganisationDetailsPage(document)
        validateFormErrors(document, Some("Enter a vendor name"))
      }

      "return forbidden view when not authorised" in new Setup {
        StrideAuthorisationServiceMock.Auth.sessionRecordNotFound
        val result = controller.updateOrganisationsDetailsAction(organisationId1)(createFakePostRequest("organisationName" -> org1.name))

        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }

  "removeOrganisationAction" should {

    "returns removeOrganisationPage with errors when form is invalid" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      when(mockXmlServiceConnector.getOrganisationByOrganisationId(eqTo(organisationId1))(*[HeaderCarrier]))
        .thenReturn(Future.successful(Right(org1)))

      val result   = controller.removeOrganisationAction(organisationId1)(fakeRequest.withCSRFToken)
      val document = Jsoup.parse(contentAsString(result))

      status(result) shouldBe BAD_REQUEST
      validateRemoveOrganisationPage(document, org1.name)
      verify(mockXmlServiceConnector).getOrganisationByOrganisationId(eqTo(org1.organisationId))(*)
    }
  }

}
