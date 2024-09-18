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

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import org.jsoup.Jsoup

import play.api.http.Status
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.config.ErrorHandler
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.connectors.{ThirdPartyDeveloperConnector, XmlServicesConnector}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.thirdpartydeveloper.{UserId, UserResponse}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.utils.{OrganisationTestData, ViewSpecHelpers}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.helper.WithCSRFAddToken
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.teammembers.{AddTeamMemberView, CreateTeamMemberView, ManageTeamMembersView, RemoveTeamMemberView}
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles
import uk.gov.hmrc.apiplatform.modules.gkauth.services.{LdapAuthorisationServiceMockModule, StrideAuthorisationServiceMockModule}

class TeamMembersControllerSpec extends ControllerBaseSpec with WithCSRFAddToken with ViewSpecHelpers {

  trait Setup extends ControllerSetupBase with OrganisationTestData with LdapAuthorisationServiceMockModule with StrideAuthorisationServiceMockModule {
    val fakeRequest                        = FakeRequest().withMethod(GET)
    private lazy val errorHandler          = app.injector.instanceOf[ErrorHandler]
    private lazy val manageTeamMembersView = app.injector.instanceOf[ManageTeamMembersView]
    private lazy val addTeamMembersView    = app.injector.instanceOf[AddTeamMemberView]
    private lazy val createTeamMembersView = app.injector.instanceOf[CreateTeamMemberView]
    private lazy val removeTeamMembersView = app.injector.instanceOf[RemoveTeamMemberView]

    val mockXmlServiceConnector          = mock[XmlServicesConnector]
    val mockThirdPartyDeveloperConnector = mock[ThirdPartyDeveloperConnector]

    val controller = new TeamMembersController(
      mcc,
      manageTeamMembersView,
      addTeamMembersView,
      createTeamMembersView,
      removeTeamMembersView,
      errorHandler,
      mockXmlServiceConnector,
      mockThirdPartyDeveloperConnector,
      LdapAuthorisationServiceMock.aMock,
      StrideAuthorisationServiceMock.aMock
    )

    def validatePageIsRendered(result: Future[Result]) = {
      status(result) shouldBe Status.OK
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
      contentAsString(result) contains "Search for XML organisations"
    }

    def createFakePostRequest(params: (String, String)*): FakeRequest[AnyContentAsFormUrlEncoded] = {
      FakeRequest().withMethod(POST)
        .withCSRFToken.withFormUrlEncodedBody(params: _*)
    }
  }

  "manageTeamMembers" should {

    "return 200 and render the manage team member view when organisation exists" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      when(mockXmlServiceConnector.getOrganisationByOrganisationId(*[OrganisationId])(*))
        .thenReturn(Future.successful(Right(organisationWithCollaborators)))

      val result = controller.manageTeamMembers(
        organisationWithCollaborators.organisationId
      )(createFakePostRequest("organisationname" -> org1.name))

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("org-name-caption").text() shouldBe org1.name
      document.getElementById("team-member-heading").text() shouldBe "Manage team members"
      verify(mockXmlServiceConnector).getOrganisationByOrganisationId(eqTo(organisationWithCollaborators.organisationId))(*)
    }

    "return 500 and render the error page when organisation doesn't exist" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      when(mockXmlServiceConnector.getOrganisationByOrganisationId(*[OrganisationId])(*))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR))))

      val result = controller.manageTeamMembers(
        organisationWithCollaborators.organisationId
      )(createFakePostRequest("organisationname" -> org1.name))

      status(result) shouldBe INTERNAL_SERVER_ERROR
      verify(mockXmlServiceConnector).getOrganisationByOrganisationId(eqTo(organisationWithCollaborators.organisationId))(*)
    }

    "return forbidden view when not authorised" in new Setup {
      StrideAuthorisationServiceMock.Auth.sessionRecordNotFound
      val result = controller.manageTeamMembers(org1.organisationId)(createFakePostRequest("organisationname" -> org1.name))

      status(result) shouldBe Status.SEE_OTHER
    }
  }

  "addTeamMemberPage" should {

    "return 200 and display the add team member page" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      val result = controller.addTeamMemberPage(org1.organisationId)(fakeRequest.withCSRFToken)

      status(result) shouldBe Status.OK
      val document = Jsoup.parse(contentAsString(result))

      validateFormErrors(document)
      validateAddTeamMemberPage(document)
    }

    "return forbidden view when not authorised" in new Setup {
      StrideAuthorisationServiceMock.Auth.sessionRecordNotFound
      val result = controller.addTeamMemberPage(organisationId1)(fakeRequest.withCSRFToken)

      status(result) shouldBe Status.SEE_OTHER
    }
  }

  "addTeamMemberAction" should {
    "call add user with existing user details when they are not linked to the organisation and they exist in third party developer" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      val userId       = UserId(UUID.randomUUID())
      val userResponse = UserResponse(emailAddress, firstName, lastName, verified = true, userId)

      when(mockXmlServiceConnector.getOrganisationByOrganisationId(*[OrganisationId])(*))
        .thenReturn(Future.successful(Right(org1)))
      when(mockThirdPartyDeveloperConnector.getByEmails(eqTo(List(emailAddress)))(*))
        .thenReturn(Future.successful(Right(List(userResponse))))
      when(mockXmlServiceConnector.addTeamMember(eqTo(organisationId1), eqTo(emailAddress), eqTo(firstName), eqTo(lastName))(*[HeaderCarrier]))
        .thenReturn(Future.successful(AddCollaboratorSuccess(org1)))

      val result = controller.addTeamMemberAction(organisationId1)(createFakePostRequest("emailAddress" -> emailAddress))

      status(result) shouldBe Status.SEE_OTHER
      headers(result).getOrElse(LOCATION, "") shouldBe s"/api-gatekeeper-xml-services/organisations/${organisationId1.value.toString}/team-members"
      verify(mockXmlServiceConnector).addTeamMember(eqTo(organisationId1), eqTo(emailAddress), *, *)(*[HeaderCarrier])
    }

    "display the createTeamMemberView when the user does not exist in third party developer" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      when(mockXmlServiceConnector.getOrganisationByOrganisationId(*[OrganisationId])(*))
        .thenReturn(Future.successful(Right(organisationWithCollaborators)))
      when(mockThirdPartyDeveloperConnector.getByEmails(eqTo(List(emailAddress)))(*)).thenReturn(Future.successful(Right(List.empty)))

      val result = controller.addTeamMemberAction(organisationId1)(createFakePostRequest("emailAddress" -> emailAddress))

      status(result) shouldBe Status.OK
      val document = Jsoup.parse(contentAsString(result))

      validateCreateTeamMemberPage(document, emailAddress)
    }

    "return 400 and display the add team member page with errors when the collaborator already exists against the organisation" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      when(mockXmlServiceConnector.getOrganisationByOrganisationId(eqTo(organisationId1))(*))
        .thenReturn(Future.successful(Right(organisationWithCollaborators)))

      val result = controller.addTeamMemberAction(organisationId1)(createFakePostRequest("emailAddress" -> collaborator1.email))

      status(result) shouldBe Status.BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))

      validateFormErrors(document, Some("This user is already a team member on this organisation"))
      validateAddTeamMemberPage(document)

      verify(mockXmlServiceConnector).getOrganisationByOrganisationId(eqTo(organisationId1))(*)
      verifyZeroInteractions(mockThirdPartyDeveloperConnector)
    }

    "display internal server error page when third party developer returns error" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      when(mockXmlServiceConnector.getOrganisationByOrganisationId(eqTo(organisationId1))(*))
        .thenReturn(Future.successful(Right(organisationWithCollaborators)))
      when(mockThirdPartyDeveloperConnector.getByEmails(eqTo(List(emailAddress)))(*)).thenReturn(Future.successful(Left(UpstreamErrorResponse("", NOT_FOUND, NOT_FOUND))))

      val result = controller.addTeamMemberAction(organisationId1)(createFakePostRequest("emailAddress" -> emailAddress))

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "call add team member via connector, then show Internal server error page when call fails" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      val userId       = UserId(UUID.randomUUID())
      val userResponse = UserResponse(emailAddress, firstName, lastName, verified = true, userId)

      when(mockXmlServiceConnector.getOrganisationByOrganisationId(eqTo(organisationId1))(*))
        .thenReturn(Future.successful(Right(organisationWithCollaborators)))
      when(mockThirdPartyDeveloperConnector.getByEmails(eqTo(List(emailAddress)))(*))
        .thenReturn(Future.successful(Right(List(userResponse))))
      when(mockXmlServiceConnector.addTeamMember(eqTo(organisationId1), eqTo(emailAddress), *, *)(*[HeaderCarrier]))
        .thenReturn(Future.successful(AddCollaboratorFailure(UpstreamErrorResponse("", NOT_FOUND, NOT_FOUND))))

      val result = controller.addTeamMemberAction(organisationId1)(createFakePostRequest("emailAddress" -> emailAddress))

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      verify(mockXmlServiceConnector).addTeamMember(eqTo(organisationId1), eqTo(emailAddress), *, *)(*[HeaderCarrier])
    }

    "return 400 and display the add team member page with errors when the form is invalid" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      val result = controller.addTeamMemberAction(organisationId1)(createFakePostRequest("emailAddress" -> ""))

      status(result) shouldBe Status.BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))

      validateFormErrors(document, Some("Enter an email address"))
      validateAddTeamMemberPage(document)
      verifyZeroInteractions(mockXmlServiceConnector)
    }

    "return 400 and display the add team member page with errors when the email address is invalid" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      val result = controller.addTeamMemberAction(organisationId1)(createFakePostRequest("emailAddress" -> "invalid"))

      status(result) shouldBe Status.BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))

      validateFormErrors(document, Some("Provide a valid email address"))
      validateAddTeamMemberPage(document)
      verifyZeroInteractions(mockXmlServiceConnector)
    }

    "return 400 and display the add team member page with errors when the email address is invalid - no TLD" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      val result = controller.addTeamMemberAction(organisationId1)(createFakePostRequest("emailAddress" -> "invalid@example"))

      status(result) shouldBe Status.BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))

      validateFormErrors(document, Some("Provide a valid email address"))
      validateAddTeamMemberPage(document)
      verifyZeroInteractions(mockXmlServiceConnector)
    }

    "return forbidden view when not authorised" in new Setup {
      StrideAuthorisationServiceMock.Auth.sessionRecordNotFound
      val result = controller.addTeamMemberAction(organisationId1)(createFakePostRequest("emailAddress" -> "dontcareAbout@this"))

      status(result) shouldBe Status.SEE_OTHER
      verifyZeroInteractions(mockXmlServiceConnector)
    }
  }

  "createTeamMemberAction" should {
    "call add teamMember and redirect to the organisation page when form is valid and call is successful" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      when(mockXmlServiceConnector.addTeamMember(eqTo(organisationId1), eqTo(emailAddress), eqTo(firstName), eqTo(lastName))(*[HeaderCarrier]))
        .thenReturn(Future.successful(AddCollaboratorSuccess(org1)))

      val result = controller.createTeamMemberAction(organisationId1)(createFakePostRequest("emailAddress" -> emailAddress, "firstName" -> firstName, "lastName" -> lastName))

      status(result) shouldBe Status.SEE_OTHER
      headers(result).getOrElse(LOCATION, "") shouldBe s"/api-gatekeeper-xml-services/organisations/${organisationId1.value.toString}/team-members"
      verify(mockXmlServiceConnector).addTeamMember(eqTo(organisationId1), eqTo(emailAddress), eqTo(firstName), eqTo(lastName))(*[HeaderCarrier])
    }

    "return 400 and display the create team member page with errors when the form is invalid" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      val result = controller.createTeamMemberAction(organisationId1)(createFakePostRequest("emailAddress" -> emailAddress, "firstName" -> "", "lastName" -> ""))

      status(result) shouldBe Status.BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))

      validateFormErrors(document, Some("Enter a first name"))
      validateFormErrors(document, Some("Enter a last name"))
      validateCreateTeamMemberPage(document, emailAddress)
      verifyZeroInteractions(mockXmlServiceConnector)
    }

    "return 500 when call to add team member fails" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      when(mockXmlServiceConnector.addTeamMember(eqTo(organisationId1), eqTo(emailAddress), eqTo(firstName), eqTo(lastName))(*[HeaderCarrier]))
        .thenReturn(Future.successful(AddCollaboratorFailure(UpstreamErrorResponse("", NOT_FOUND, NOT_FOUND))))

      val result = controller.createTeamMemberAction(organisationId1)(
        createFakePostRequest("emailAddress" -> emailAddress, "firstName" -> firstName, "lastName" -> lastName)
      )

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      verify(mockXmlServiceConnector).addTeamMember(eqTo(organisationId1), eqTo(emailAddress), eqTo(firstName), eqTo(lastName))(*[HeaderCarrier])
    }

    "return forbidden page when not authorised" in new Setup {
      StrideAuthorisationServiceMock.Auth.sessionRecordNotFound

      val result = controller.createTeamMemberAction(organisationId1)(createFakePostRequest("emailAddress" -> emailAddress))

      status(result) shouldBe Status.SEE_OTHER
      verifyZeroInteractions(mockXmlServiceConnector)
    }
  }

  "removeTeamMember" should {

    "return 200 and display the confirmation page when organisation is retrieved and call to remove team member is successful" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      when(mockXmlServiceConnector.getOrganisationByOrganisationId(*[OrganisationId])(*))
        .thenReturn(Future.successful(Right(organisationWithCollaborators)))

      val collaborator = organisationWithCollaborators.collaborators.head
      val result       = controller.removeTeamMember(organisationWithCollaborators.organisationId, collaborator.userId)(
        createFakePostRequest("organisationname" -> organisationWithCollaborators.name)
      )

      status(result) shouldBe Status.OK
      val document = Jsoup.parse(contentAsString(result))

      validateRemoveTeamMemberPage(document)
      validateFormErrors(document)
      verify(mockXmlServiceConnector).getOrganisationByOrganisationId(eqTo(organisationWithCollaborators.organisationId))(*)
    }

    "return 500 when organisation is retrieved but userId does not match any collaborator" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      when(mockXmlServiceConnector.getOrganisationByOrganisationId(*[OrganisationId])(*))
        .thenReturn(Future.successful(Right(organisationWithCollaborators)))

      val result = controller.removeTeamMember(organisationWithCollaborators.organisationId, "unmacthedUserId")(
        createFakePostRequest("email" -> collaborator1.email)
      )

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      verify(mockXmlServiceConnector).getOrganisationByOrganisationId(eqTo(organisationWithCollaborators.organisationId))(*)
      verify(mockXmlServiceConnector, times(0)).removeTeamMember(*[OrganisationId], *, *)(*)
    }

    "return 500 when connector returns 500" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      when(mockXmlServiceConnector.getOrganisationByOrganisationId(*[OrganisationId])(*))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR))))

      val result = controller.removeTeamMember(organisationWithCollaborators.organisationId, "unmacthedUserId")(
        createFakePostRequest("email" -> collaborator1.email)
      )

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      verify(mockXmlServiceConnector).getOrganisationByOrganisationId(eqTo(organisationWithCollaborators.organisationId))(*)
      verify(mockXmlServiceConnector, times(0)).removeTeamMember(*[OrganisationId], *, *)(*)
    }

    "return forbidden view when not authorised" in new Setup {
      StrideAuthorisationServiceMock.Auth.sessionRecordNotFound
      val result = controller.removeTeamMember(org1.organisationId, "")(createFakePostRequest("organisationname" -> org1.name))

      status(result) shouldBe Status.SEE_OTHER
    }
  }

  "removeTeamMemberAction" should {
    "return 303 when form is valid, confirm is yes, organisation is retrieved and call to remove team member is successful" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      when(mockXmlServiceConnector.getOrganisationByOrganisationId(eqTo(organisationWithCollaborators.organisationId))(*))
        .thenReturn(Future.successful(Right(organisationWithCollaborators)))

      when(mockXmlServiceConnector.removeTeamMember(eqTo(organisationWithCollaborators.organisationId), *, *)(*))
        .thenReturn(Future.successful(RemoveCollaboratorSuccess(organisationWithCollaborators)))

      val result = controller.removeTeamMemberAction(organisationWithCollaborators.organisationId, collaborator1.userId)(
        createFakePostRequest("email" -> collaborator1.email, "confirm" -> "Yes")
      )

      status(result) shouldBe Status.SEE_OTHER
      headers(result).getOrElse(LOCATION, "") shouldBe s"/api-gatekeeper-xml-services/organisations/${organisationId1.value.toString}/team-members"
      verify(mockXmlServiceConnector).getOrganisationByOrganisationId(eqTo(organisationWithCollaborators.organisationId))(*)
      verify(mockXmlServiceConnector).removeTeamMember(eqTo(organisationWithCollaborators.organisationId), eqTo(collaborator1.email), *)(*)
    }

    "return 303 when form is valid, confirm is no, organisation is retrieved and call to remove team member is successful" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      val result = controller.removeTeamMemberAction(organisationWithCollaborators.organisationId, collaborator1.userId)(
        createFakePostRequest("email" -> collaborator1.email, "confirm" -> "No")
      )

      status(result) shouldBe Status.SEE_OTHER
      headers(result).getOrElse(LOCATION, "") shouldBe s"/api-gatekeeper-xml-services/organisations/${organisationId1.value.toString}"
      verifyZeroInteractions(mockXmlServiceConnector)
    }

    "return 400 when form is invalid (email missing),  organisation is retrieved and call to remove team member is successful" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      val result = controller.removeTeamMemberAction(organisationWithCollaborators.organisationId, collaborator1.userId)(
        createFakePostRequest("confirm" -> "No")
      )

      status(result) shouldBe BAD_REQUEST
      verifyZeroInteractions(mockXmlServiceConnector)
    }

    "return 400 when form is invalid (email value missing),  organisation is retrieved and call to remove team member is successful" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      val result = controller.removeTeamMemberAction(organisationWithCollaborators.organisationId, collaborator1.userId)(
        createFakePostRequest("email" -> "", "confirm" -> "No")
      )

      status(result) shouldBe BAD_REQUEST
      verifyZeroInteractions(mockXmlServiceConnector)
    }

    "return 400 when form is invalid (confirmation is missing),  organisation is retrieved and call to remove team member is successful" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      val result = controller.removeTeamMemberAction(organisationWithCollaborators.organisationId, collaborator1.userId)(
        createFakePostRequest("email" -> collaborator1.email)
      )

      status(result) shouldBe BAD_REQUEST
      verifyZeroInteractions(mockXmlServiceConnector)
    }

    "return 500 when organisation is retrieved and call to remove team member fails" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      when(mockXmlServiceConnector.getOrganisationByOrganisationId(*[OrganisationId])(*))
        .thenReturn(Future.successful(Right(organisationWithCollaborators)))

      when(mockXmlServiceConnector.removeTeamMember(*[OrganisationId], *, *)(*))
        .thenReturn(Future.successful(RemoveCollaboratorFailure(new RuntimeException("some error"))))

      val result = controller.removeTeamMemberAction(organisationWithCollaborators.organisationId, collaborator1.userId)(
        createFakePostRequest("email" -> collaborator1.email, "confirm" -> "Yes")
      )

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      verify(mockXmlServiceConnector).getOrganisationByOrganisationId(eqTo(organisationWithCollaborators.organisationId))(*)
      verify(mockXmlServiceConnector).removeTeamMember(eqTo(organisationWithCollaborators.organisationId), eqTo(collaborator1.email), *)(*)
    }

    "return 500 when organisation is retrieved but userId does not match any collaborator" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      when(mockXmlServiceConnector.getOrganisationByOrganisationId(*[OrganisationId])(*))
        .thenReturn(Future.successful(Right(organisationWithCollaborators)))

      val result = controller.removeTeamMemberAction(organisationWithCollaborators.organisationId, "someOtherUserId")(
        createFakePostRequest("email" -> collaborator1.email, "confirm" -> "Yes")
      )

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      verify(mockXmlServiceConnector).getOrganisationByOrganisationId(eqTo(organisationWithCollaborators.organisationId))(*)
      verify(mockXmlServiceConnector, times(0)).removeTeamMember(*[OrganisationId], *, *)(*)
    }

    "return 500 when connector returns 500" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      when(mockXmlServiceConnector.getOrganisationByOrganisationId(*[OrganisationId])(*))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR))))

      val result = controller.removeTeamMemberAction(organisationWithCollaborators.organisationId, collaborator1.userId)(
        createFakePostRequest("email" -> collaborator1.email, "confirm" -> "Yes")
      )

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      verify(mockXmlServiceConnector).getOrganisationByOrganisationId(eqTo(organisationWithCollaborators.organisationId))(*)
      verify(mockXmlServiceConnector, times(0)).removeTeamMember(*[OrganisationId], *, *)(*)
    }

    "return forbidden view when not authorised" in new Setup {
      StrideAuthorisationServiceMock.Auth.sessionRecordNotFound
      val result =
        controller.removeTeamMemberAction(organisationWithCollaborators.organisationId, collaborator1.userId)(
          createFakePostRequest("email" -> collaborator1.email, "confirm" -> "Yes")
        )

      status(result) shouldBe Status.SEE_OTHER
    }
  }

}
