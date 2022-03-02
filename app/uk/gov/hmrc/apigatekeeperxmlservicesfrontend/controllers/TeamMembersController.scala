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

import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.config.AppConfig
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.connectors.{AuthConnector, ThirdPartyDeveloperConnector, XmlServicesConnector}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.forms.Forms.{AddTeamMemberForm, CreateAndAddTeamMemberForm, RemoveTeamMemberConfirmationForm}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.thirdpartydeveloper.UserResponse
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.utils.GatekeeperAuthWrapper
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.teammembers._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.{ErrorTemplate, ForbiddenView}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

class TeamMembersController @Inject()(mcc: MessagesControllerComponents,
                                      manageTeamMembersView: ManageTeamMembersView,
                                      addTeamMemberView: AddTeamMemberView,
                                      createTeamMemberView: CreateTeamMemberView,
                                      removeTeamMemberView: RemoveTeamMemberView,
                                      override val authConnector: AuthConnector,
                                      val forbiddenView: ForbiddenView,
                                      errorTemplate: ErrorTemplate,
                                      xmlServicesConnector: XmlServicesConnector,
                                      thirdPartyDeveloperConnector: ThirdPartyDeveloperConnector
                                    )(implicit val ec: ExecutionContext,
                                      appConfig: AppConfig)
  extends FrontendController(mcc) with GatekeeperAuthWrapper {

  val addTeamMemberForm: Form[AddTeamMemberForm] = AddTeamMemberForm.form
  val createAndAddTeamMemberForm: Form[CreateAndAddTeamMemberForm] = CreateAndAddTeamMemberForm.form
  val confirmRemoveForm: Form[RemoveTeamMemberConfirmationForm] = RemoveTeamMemberConfirmationForm.form

  def manageTeamMembers(organisationId: OrganisationId): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request => {
      xmlServicesConnector.getOrganisationByOrganisationId(organisationId).map {
          case Right(org: Organisation) => Ok(manageTeamMembersView(org))
          case Left(_) => InternalServerError(errorTemplate("Internal Server Error", "Internal Server Error", "Internal Server Error"))
        }
    }
  }

  def addTeamMemberPage(organisationId: OrganisationId): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request => successful(Ok(addTeamMemberView(addTeamMemberForm, organisationId)))
  }

  def addTeamMemberAction(organisationId: OrganisationId): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request =>
      addTeamMemberForm.bindFromRequest.fold(
        formWithErrors => successful(BadRequest(addTeamMemberView(formWithErrors, organisationId))),
        teamMemberAddData => {
         thirdPartyDeveloperConnector.getByEmails(List(teamMemberAddData.emailAddress.getOrElse(""))).flatMap {
           case Right(Nil) => successful(Ok(createTeamMemberView(createAndAddTeamMemberForm, organisationId, teamMemberAddData.emailAddress)))
           case Right(users: List[UserResponse]) => addOrCreateTeamMember(organisationId, users.head.email,  users.head.firstName,  users.head.lastName)
           case Left(_) => successful(InternalServerError(errorTemplate("Internal Server Error", "Internal Server Error", "Internal Server Error")))
         }
        }
      )
  }

  def createTeamMemberAction(organisationId: OrganisationId): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request => createAndAddTeamMemberForm.bindFromRequest.fold(
        formWithErrors => successful(BadRequest(createTeamMemberView(formWithErrors, organisationId, None))),
        formData => addOrCreateTeamMember(organisationId, formData.emailAddress,  formData.firstName,  formData.lastName)
      )
  }

  private def addOrCreateTeamMember(organisationId: OrganisationId, emailAddress: String, firstname: String, lastname: String )
                                   (implicit hc: HeaderCarrier, request: LoggedInRequest[_]): Future[Result] ={
    xmlServicesConnector
      .addTeamMember(organisationId, emailAddress, firstname, lastname)
      .map {
        case AddCollaboratorSuccess(x: Organisation) =>
          Redirect(uk.gov.hmrc.apigatekeeperxmlservicesfrontend.controllers.routes.TeamMembersController.manageTeamMembers(x.organisationId))
        case _ => InternalServerError(errorTemplate("Internal Server Error", "Internal Server Error", "Internal Server Error"))
      }
  }


  def removeTeamMember(organisationId: OrganisationId, userId: String): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request => {
      getCollaboratorByUserIdAndOrganisationId(organisationId, userId).flatMap {
        case Some(collaborator: Collaborator) =>
          successful(Ok(removeTeamMemberView(RemoveTeamMemberConfirmationForm.form, organisationId, userId, collaborator.email)))
        case _ => successful(InternalServerError(errorTemplate("Internal Server Error", "Internal Server Error", "Internal Server Error")))
      }
    }

  }


  def removeTeamMemberAction(organisationId: OrganisationId, userId: String): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request =>

      def handleRemoveTeamMember(): Future[Result] = {
        getCollaboratorByUserIdAndOrganisationId(organisationId, userId).flatMap {
          case Some(collaborator: Collaborator) =>
            xmlServicesConnector.removeTeamMember(organisationId, collaborator.email, request.name.getOrElse("Unknown Name"))
              .map {
                case RemoveCollaboratorSuccess(_) => Redirect(routes.TeamMembersController.manageTeamMembers(organisationId).url, SEE_OTHER)
                case _ => InternalServerError(errorTemplate("Internal Server Error", "Internal Server Error", "Internal Server Error"))
              }
          case _ => successful(InternalServerError(errorTemplate("Internal Server Error", "Internal Server Error", "Internal Server Error")))
        }
      }

      def handleValidForm(form: RemoveTeamMemberConfirmationForm): Future[Result] = {
        form.confirm match {
          case Some("Yes") => handleRemoveTeamMember()
          case _ => successful(Redirect(routes.OrganisationController.viewOrganisationPage(organisationId).url))
        }
      }

      def handleInvalidForm(formWithErrors: Form[RemoveTeamMemberConfirmationForm]) =
        successful(BadRequest(removeTeamMemberView(formWithErrors, organisationId, userId, formWithErrors("email").value.getOrElse(""))))

      RemoveTeamMemberConfirmationForm.form.bindFromRequest.fold(handleInvalidForm, handleValidForm)
  }

  private def getCollaboratorByUserIdAndOrganisationId(organisationId: OrganisationId, userId: String)
                                                      (implicit hc: HeaderCarrier): Future[Option[Collaborator]] = {
    xmlServicesConnector.getOrganisationByOrganisationId(organisationId).map {
      case Right(organisation: Organisation) =>
        organisation.collaborators.find(_.userId.equalsIgnoreCase(userId))
      case _ => None
    }
  }

}
