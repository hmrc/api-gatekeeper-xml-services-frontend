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
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.connectors.XmlServicesConnector
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.utils.{OrganisationTestData, ViewSpecHelpers}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.helper.WithCSRFAddToken
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.teammembers.{AddTeamMemberView, ManageTeamMembersView, RemoveTeamMemberView}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.{ErrorTemplate, ForbiddenView}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class TeamMembersControllerSpec extends ControllerBaseSpec with WithCSRFAddToken with ViewSpecHelpers {

  trait Setup extends ControllerSetupBase with OrganisationTestData {
    val fakeRequest = FakeRequest("GET", "/organisations")
    val organisationSearchRequest = FakeRequest("GET", "/organisations-search")
    private lazy val forbiddenView = app.injector.instanceOf[ForbiddenView]
    private lazy val errorTemplate = app.injector.instanceOf[ErrorTemplate]
    private lazy val manageTeamMembersView = app.injector.instanceOf[ManageTeamMembersView]
    private lazy val addTeamMembersView = app.injector.instanceOf[AddTeamMemberView]
    private lazy val removeTeamMembersView = app.injector.instanceOf[RemoveTeamMemberView]

    val mockXmlServiceConnector = mock[XmlServicesConnector]

    val controller = new TeamMembersController(
      mcc,
      manageTeamMembersView,
      addTeamMembersView,
      removeTeamMembersView,
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

  "manageTeamMembers" should {

    "return 200 and render the manage team member view when organisation exists" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()

      when(mockXmlServiceConnector.getOrganisationByOrganisationId(*[OrganisationId])(*))
        .thenReturn(Future.successful(Right(organisationWithCollaborators)))

      val result = controller.manageTeamMembers(
        organisationWithCollaborators.organisationId)(fakeRequest.withCSRFToken.withFormUrlEncodedBody("organisationname" -> org1.name))

      status(result) shouldBe OK

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("org-name-caption").text() shouldBe org1.name
      document.getElementById("team-member-heading").text() shouldBe "Manage team members"

      verify(mockXmlServiceConnector).getOrganisationByOrganisationId(eqTo(organisationWithCollaborators.organisationId))(*)
    }

    "return 500 and render the error page when organisation doesn't exist" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()

      when(mockXmlServiceConnector.getOrganisationByOrganisationId(*[OrganisationId])(*))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR))))

      val result = controller.manageTeamMembers(
        organisationWithCollaborators.organisationId)(fakeRequest.withCSRFToken.withFormUrlEncodedBody("organisationname" -> org1.name))

      status(result) shouldBe INTERNAL_SERVER_ERROR

      verify(mockXmlServiceConnector).getOrganisationByOrganisationId(eqTo(organisationWithCollaborators.organisationId))(*)
    }

    "return forbidden view when not authorised" in new Setup {
      givenAUnsuccessfulLogin()
      val result = controller.manageTeamMembers(org1.organisationId)(fakeRequest.withCSRFToken.withFormUrlEncodedBody("organisationname" -> org1.name))

      status(result) shouldBe Status.SEE_OTHER

    }
  }

  "addTeamMemberPage" should {

    "return 200 and display the add team member page" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()

      val result = controller.addTeamMemberPage(org1.organisationId)(fakeRequest.withCSRFToken)

      status(result) shouldBe Status.OK

      val document = Jsoup.parse(contentAsString(result))

      validateFormErrors(document)
      validateAddTeamMemberPage(document)
    }

    "return forbidden view when not authorised" in new Setup {
      givenAUnsuccessfulLogin()
      val result = controller.addTeamMemberPage(organisationId1)(fakeRequest.withCSRFToken)

      status(result) shouldBe Status.SEE_OTHER

    }

  }
  "addTeamMemberAction" should {
    "call add team member via connector, then redirect to the manage team members page when call is successful" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()

      val emailAddress = "a@b.com"

      when(mockXmlServiceConnector.addTeamMember(eqTo(organisationId1), eqTo(emailAddress))(*[HeaderCarrier]))
        .thenReturn(Future.successful(AddCollaboratorSuccess(org1)))

      val result = controller.addTeamMemberAction(organisationId1)(fakeRequest
        .withCSRFToken.withFormUrlEncodedBody("emailAddress" -> emailAddress))

      status(result) shouldBe Status.SEE_OTHER
      headers(result).getOrElse(LOCATION, "") shouldBe s"/api-gatekeeper-xml-services/organisations/${organisationId1.value.toString}/team-members"

      verify(mockXmlServiceConnector).addTeamMember(eqTo(organisationId1), eqTo(emailAddress))(*[HeaderCarrier])

    }

    "call add team member via connector, then show Internal server error page when call fails" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()

      val emailAddress = "a@b.com"

      when(mockXmlServiceConnector.addTeamMember(eqTo(organisationId1), eqTo(emailAddress))(*[HeaderCarrier]))
        .thenReturn(Future.successful(AddCollaboratorFailure(UpstreamErrorResponse("", NOT_FOUND, NOT_FOUND))))

      val result = controller.addTeamMemberAction(organisationId1)(fakeRequest
        .withCSRFToken.withFormUrlEncodedBody("emailAddress" -> emailAddress))

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR

      verify(mockXmlServiceConnector).addTeamMember(eqTo(organisationId1), eqTo(emailAddress))(*[HeaderCarrier])
    }

    "return 400 and display the add team member page with errors when the form is invalid" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()

      val result = controller.addTeamMemberAction(organisationId1)(fakeRequest
        .withCSRFToken.withFormUrlEncodedBody("emailAddress" -> ""))

      status(result) shouldBe Status.BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))

      validateFormErrors(document, Some("Enter an email address"))
      validateAddTeamMemberPage(document)
      verifyZeroInteractions(mockXmlServiceConnector)

    }


    "return forbidden view when not authorised" in new Setup {
      givenAUnsuccessfulLogin()
      val result = controller.addTeamMemberAction(organisationId1)(fakeRequest
        .withCSRFToken.withFormUrlEncodedBody("emailAddress" -> "dontcareAbout@this"))

      status(result) shouldBe Status.SEE_OTHER

      verifyZeroInteractions(mockXmlServiceConnector)
    }


  }

  "removeTeamMember" should {

    "return 200 and display the confirmation page when organisation is retrieved and call to remove team member is successful" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()

      when(mockXmlServiceConnector.getOrganisationByOrganisationId(*[OrganisationId])(*))
        .thenReturn(Future.successful(Right(organisationWithCollaborators)))

      val collaborator = organisationWithCollaborators.collaborators.head
      val result = controller.removeTeamMember(organisationWithCollaborators.organisationId, collaborator.userId)(
        fakeRequest.withCSRFToken.withFormUrlEncodedBody("organisationname" -> organisationWithCollaborators.name)
      )

      status(result) shouldBe Status.OK
      val document = Jsoup.parse(contentAsString(result))

      validateRemoveTeamMemberPage(document)
      validateFormErrors(document)
      verify(mockXmlServiceConnector).getOrganisationByOrganisationId(eqTo(organisationWithCollaborators.organisationId))(*)


    }


    "return 500 when organisation is retrieved but userId does not match any collaborator" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()

      when(mockXmlServiceConnector.getOrganisationByOrganisationId(*[OrganisationId])(*))
        .thenReturn(Future.successful(Right(organisationWithCollaborators)))

      val result = controller.removeTeamMember(organisationWithCollaborators.organisationId, "unmacthedUserId")(
        fakeRequest.withCSRFToken.withFormUrlEncodedBody("email" -> collaborator1.email)
      )

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      verify(mockXmlServiceConnector).getOrganisationByOrganisationId(eqTo(organisationWithCollaborators.organisationId))(*)
      verify(mockXmlServiceConnector, times(0)).removeTeamMember(*[OrganisationId], *, *)(*)

    }

    "return 500 when connector returns 500" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()

      when(mockXmlServiceConnector.getOrganisationByOrganisationId(*[OrganisationId])(*))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR))))

      val result = controller.removeTeamMember(organisationWithCollaborators.organisationId, "unmacthedUserId")(
        fakeRequest.withCSRFToken.withFormUrlEncodedBody("email" -> collaborator1.email)
      )

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      verify(mockXmlServiceConnector).getOrganisationByOrganisationId(eqTo(organisationWithCollaborators.organisationId))(*)
      verify(mockXmlServiceConnector, times(0)).removeTeamMember(*[OrganisationId], *, *)(*)

    }

    "return forbidden view when not authorised" in new Setup {
      givenAUnsuccessfulLogin()
      val result = controller.removeTeamMember(org1.organisationId, "")(fakeRequest.withCSRFToken.withFormUrlEncodedBody("organisationname" -> org1.name))

      status(result) shouldBe Status.SEE_OTHER

    }

  }

  "removeTeamMemberAction" should {

    "return 303 when form is valid, confirm is yes, organisation is retrieved and call to remove team member is successful" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()

      when(mockXmlServiceConnector.getOrganisationByOrganisationId(eqTo(organisationWithCollaborators.organisationId))(*))
        .thenReturn(Future.successful(Right(organisationWithCollaborators)))

      when(mockXmlServiceConnector.removeTeamMember(eqTo(organisationWithCollaborators.organisationId), *, *)(*))
        .thenReturn(Future.successful(RemoveCollaboratorSuccess(organisationWithCollaborators)))


      val result = controller.removeTeamMemberAction(organisationWithCollaborators.organisationId, collaborator1.userId)(
        fakeRequest.withCSRFToken.withFormUrlEncodedBody("email" -> organisationWithCollaborators.name, "confirm" -> "Yes")
      )

      status(result) shouldBe Status.SEE_OTHER
      headers(result).getOrElse(LOCATION, "") shouldBe s"/api-gatekeeper-xml-services/organisations/${organisationId1.value.toString}/team-members"

      verify(mockXmlServiceConnector).getOrganisationByOrganisationId(eqTo(organisationWithCollaborators.organisationId))(*)
      verify(mockXmlServiceConnector).removeTeamMember(eqTo(organisationWithCollaborators.organisationId), eqTo(collaborator1.email), *)(*)

    }


    "return 303 when form is valid, confirm is no, organisation is retrieved and call to remove team member is successful" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()

      val result = controller.removeTeamMemberAction(organisationWithCollaborators.organisationId, collaborator1.userId)(
        fakeRequest.withCSRFToken.withFormUrlEncodedBody("email" -> organisationWithCollaborators.name, "confirm" -> "No")
      )

      status(result) shouldBe Status.SEE_OTHER
      headers(result).getOrElse(LOCATION, "") shouldBe s"/api-gatekeeper-xml-services/organisations/${organisationId1.value.toString}"

      verifyZeroInteractions(mockXmlServiceConnector)

    }

    "return 400 when form is invalid (email missing),  organisation is retrieved and call to remove team member is successful" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()

      val result = controller.removeTeamMemberAction(organisationWithCollaborators.organisationId, collaborator1.userId)(
        fakeRequest.withCSRFToken.withFormUrlEncodedBody("confirm" -> "No")
      )

      status(result) shouldBe BAD_REQUEST

      verifyZeroInteractions(mockXmlServiceConnector)
    }


    "return 400 when form is invalid (email value missing),  organisation is retrieved and call to remove team member is successful" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()

      val result = controller.removeTeamMemberAction(organisationWithCollaborators.organisationId, collaborator1.userId)(
        fakeRequest.withCSRFToken.withFormUrlEncodedBody("email" -> "", "confirm" -> "No")
      )

      status(result) shouldBe BAD_REQUEST

      verifyZeroInteractions(mockXmlServiceConnector)
    }


    "return 400 when form is invalid (confirmation is missing),  organisation is retrieved and call to remove team member is successful" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()

      val result = controller.removeTeamMemberAction(organisationWithCollaborators.organisationId, collaborator1.userId)(
        fakeRequest.withCSRFToken.withFormUrlEncodedBody("email" -> organisationWithCollaborators.name)
      )

      status(result) shouldBe BAD_REQUEST

      verifyZeroInteractions(mockXmlServiceConnector)
    }


    "return 500 when organisation is retrieved and call to remove team member fails" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()

      when(mockXmlServiceConnector.getOrganisationByOrganisationId(*[OrganisationId])(*))
        .thenReturn(Future.successful(Right(organisationWithCollaborators)))

      when(mockXmlServiceConnector.removeTeamMember(*[OrganisationId], *, *)(*))
        .thenReturn(Future.successful(RemoveCollaboratorFailure(new RuntimeException("some error"))))

      val result = controller.removeTeamMemberAction(organisationWithCollaborators.organisationId, collaborator1.userId)(
        fakeRequest.withCSRFToken.withFormUrlEncodedBody("email" -> organisationWithCollaborators.name, "confirm" -> "Yes")
      )

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR

      verify(mockXmlServiceConnector).getOrganisationByOrganisationId(eqTo(organisationWithCollaborators.organisationId))(*)
      verify(mockXmlServiceConnector).removeTeamMember(eqTo(organisationWithCollaborators.organisationId), eqTo(collaborator1.email), *)(*)

    }

    "return 500 when organisation is retrieved but userId does not match any collaborator" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()

      when(mockXmlServiceConnector.getOrganisationByOrganisationId(*[OrganisationId])(*))
        .thenReturn(Future.successful(Right(organisationWithCollaborators)))

      val result = controller.removeTeamMemberAction(organisationWithCollaborators.organisationId, "someOtherUserId")(
        fakeRequest.withCSRFToken.withFormUrlEncodedBody("email" -> organisationWithCollaborators.name, "confirm" -> "Yes")
      )

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      verify(mockXmlServiceConnector).getOrganisationByOrganisationId(eqTo(organisationWithCollaborators.organisationId))(*)
      verify(mockXmlServiceConnector, times(0)).removeTeamMember(*[OrganisationId], *, *)(*)

    }

    "return 500 when connector returns 500" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()

      when(mockXmlServiceConnector.getOrganisationByOrganisationId(*[OrganisationId])(*))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR))))

      val result = controller.removeTeamMemberAction(organisationWithCollaborators.organisationId, collaborator1.userId)(
        fakeRequest.withCSRFToken.withFormUrlEncodedBody("email" -> organisationWithCollaborators.name, "confirm" -> "Yes")
      )

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      verify(mockXmlServiceConnector).getOrganisationByOrganisationId(eqTo(organisationWithCollaborators.organisationId))(*)
      verify(mockXmlServiceConnector, times(0)).removeTeamMember(*[OrganisationId], *, *)(*)

    }

    "return forbidden view when not authorised" in new Setup {
      givenAUnsuccessfulLogin()
      val result = controller.removeTeamMemberAction(organisationWithCollaborators.organisationId, collaborator1.userId)(
        fakeRequest.withCSRFToken.withFormUrlEncodedBody("email" -> organisationWithCollaborators.name, "confirm" -> "Yes")
      )
      status(result) shouldBe Status.SEE_OTHER

    }

  }
}
