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

import javax.inject.Inject
import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

import play.api.Logging
import play.api.data.Form
import play.api.data.Forms.{mapping, optional, text}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.config.{AppConfig, ErrorHandler}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.connectors.{ThirdPartyDeveloperConnector, XmlServicesConnector}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.controllers.FormUtils.emailValidator
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.controllers.TeamMembersController._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.thirdpartydeveloper.UserResponse
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.teammembers._
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.actions.GatekeeperStrideAuthorisationActions
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.LoggedInRequest
import uk.gov.hmrc.apiplatform.modules.gkauth.services.{LdapAuthorisationService, StrideAuthorisationService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

object TeamMembersController {

  case class AddTeamMemberForm(emailAddress: String)

  object AddTeamMemberForm {

    val form = Form(
      mapping(
        "emailAddress" -> emailValidator()
      )(AddTeamMemberForm.apply)(AddTeamMemberForm.unapply)
    )

  }

  case class CreateAndAddTeamMemberForm(emailAddress: String, firstName: String, lastName: String)

  object CreateAndAddTeamMemberForm {

    val form = Form(
      mapping(
        "emailAddress" -> emailValidator(),
        "firstName"    -> text.verifying("firstname.error.required", x => x.trim.nonEmpty),
        "lastName"     -> text.verifying("lastname.error.required", x => x.trim.nonEmpty)
      )(CreateAndAddTeamMemberForm.apply)(CreateAndAddTeamMemberForm.unapply)
    )
  }

  final case class RemoveTeamMemberConfirmationForm(email: String, confirm: Option[String] = Some(""))

  object RemoveTeamMemberConfirmationForm {

    val form: Form[RemoveTeamMemberConfirmationForm] = Form(
      mapping(
        "email"   -> emailValidator(),
        "confirm" -> optional(text).verifying("team.member.error.confirmation.no.choice.field", _.isDefined)
      )(RemoveTeamMemberConfirmationForm.apply)(RemoveTeamMemberConfirmationForm.unapply)
    )
  }

}

class TeamMembersController @Inject() (
    mcc: MessagesControllerComponents,
    manageTeamMembersView: ManageTeamMembersView,
    addTeamMemberView: AddTeamMemberView,
    createTeamMemberView: CreateTeamMemberView,
    removeTeamMemberView: RemoveTeamMemberView,
    errorHandler: ErrorHandler,
    xmlServicesConnector: XmlServicesConnector,
    thirdPartyDeveloperConnector: ThirdPartyDeveloperConnector,
    ldapAuthorisationService: LdapAuthorisationService,
    val strideAuthorisationService: StrideAuthorisationService
  )(implicit val ec: ExecutionContext,
    appConfig: AppConfig
  ) extends FrontendController(mcc) with GatekeeperStrideAuthorisationActions with Logging {

  val addTeamMemberForm: Form[AddTeamMemberForm]                   = AddTeamMemberForm.form
  val createAndAddTeamMemberForm: Form[CreateAndAddTeamMemberForm] = CreateAndAddTeamMemberForm.form
  val confirmRemoveForm: Form[RemoveTeamMemberConfirmationForm]    = RemoveTeamMemberConfirmationForm.form

  def manageTeamMembers(organisationId: OrganisationId): Action[AnyContent] = anyStrideUserAction {
    implicit request =>
      {
        xmlServicesConnector.getOrganisationByOrganisationId(organisationId).map {
          case Right(org: Organisation) => Ok(manageTeamMembersView(org))
          case Left(error: Throwable)   =>
            logger.info(s"manageTeamMembers failed getting organisation for ${organisationId.value}", error)
            InternalServerError(errorHandler.internalServerErrorTemplate)
        }
      }
  }

  def addTeamMemberPage(organisationId: OrganisationId): Action[AnyContent] = anyStrideUserAction {
    implicit request => successful(Ok(addTeamMemberView(addTeamMemberForm, organisationId)))
  }

  def addTeamMemberAction(organisationId: OrganisationId): Action[AnyContent] = anyStrideUserAction {
    implicit request =>
      def handleGetUsersFromTPD(teamMemberAddData: AddTeamMemberForm) = {
        thirdPartyDeveloperConnector.getByEmails(List(teamMemberAddData.emailAddress)).flatMap {
          case Right(Nil)                       =>
            successful(Ok(createTeamMemberView(createAndAddTeamMemberForm, organisationId, teamMemberAddData.emailAddress)))
          case Right(users: List[UserResponse]) =>
            addOrCreateTeamMember(organisationId, users.head.email, users.head.firstName, users.head.lastName)
          case Left(error: Throwable)           =>
            logger.info(s"addTeamMemberAction failed for ${organisationId.value}", error)
            successful(InternalServerError(errorHandler.internalServerErrorTemplate))
        }
      }

      addTeamMemberForm.bindFromRequest.fold(
        formWithErrors => successful(BadRequest(addTeamMemberView(formWithErrors, organisationId))),
        teamMemberAddData => {
          getCollaboratorByEmailAddressAndOrganisationId(organisationId, teamMemberAddData.emailAddress).flatMap {
            case Some(user: Collaborator) =>
              logger.info(s"error in addOrCreateTeamMember for organisation ${organisationId.value} duplicate collaborator ${user.userId}")
              val formWithError = AddTeamMemberForm.form.fill(teamMemberAddData)
                .withError("emailAddress", "team.member.error.emailAddress.already.exists.field")
              successful(BadRequest(addTeamMemberView(formWithError, organisationId)))
            case None                     => handleGetUsersFromTPD(teamMemberAddData)
          }
        }
      )

  }

  def createTeamMemberAction(organisationId: OrganisationId): Action[AnyContent] = anyStrideUserAction {
    implicit request =>
      createAndAddTeamMemberForm.bindFromRequest.fold(
        formWithErrors => {
          logger.info(s"createTeamMemberAction invalid form provided for ${organisationId.value}")
          successful(BadRequest(createTeamMemberView(formWithErrors, organisationId, "")))
        },
        formData => addOrCreateTeamMember(organisationId, formData.emailAddress, formData.firstName, formData.lastName)
      )
  }

  private def addOrCreateTeamMember(
      organisationId: OrganisationId,
      emailAddress: String,
      firstname: String,
      lastname: String
    )(implicit hc: HeaderCarrier,
      request: LoggedInRequest[_]
    ): Future[Result] = {
    xmlServicesConnector
      .addTeamMember(organisationId, emailAddress, firstname, lastname)
      .map {
        case AddCollaboratorSuccess(x: Organisation)  =>
          Redirect(uk.gov.hmrc.apigatekeeperxmlservicesfrontend.controllers.routes.TeamMembersController.manageTeamMembers(x.organisationId))
        case AddCollaboratorFailure(error: Throwable) =>
          logger.info(s"error in addOrCreateTeamMember attempting to add team member to ${organisationId.value}", error)
          InternalServerError(errorHandler.internalServerErrorTemplate)
      }
  }

  def removeTeamMember(organisationId: OrganisationId, userId: String): Action[AnyContent] = anyStrideUserAction {
    implicit request =>
      {
        getCollaboratorByUserIdAndOrganisationId(organisationId, userId).flatMap {
          case Some(collaborator: Collaborator) =>
            successful(Ok(removeTeamMemberView(RemoveTeamMemberConfirmationForm.form, organisationId, userId, collaborator.email)))
          case _                                =>
            logger.info(s"getCollaboratorByUserIdAndOrganisationId failed for orgId:${organisationId.value} & userId: $userId ")
            successful(InternalServerError(errorHandler.internalServerErrorTemplate))
        }
      }
  }

  def removeTeamMemberAction(organisationId: OrganisationId, userId: String): Action[AnyContent] = anyStrideUserAction {
    implicit request =>
      def handleRemoveTeamMember(): Future[Result] = {
        getCollaboratorByUserIdAndOrganisationId(organisationId, userId).flatMap {
          case Some(collaborator: Collaborator) =>
            xmlServicesConnector.removeTeamMember(organisationId, collaborator.email, request.name.getOrElse("Unknown Name"))
              .map {
                case RemoveCollaboratorSuccess(_) => Redirect(routes.TeamMembersController.manageTeamMembers(organisationId).url, SEE_OTHER)
                case _                            =>
                  logger.info(s"removeTeamMemberAction connector failed for  orgId:${organisationId.value} & userId: $userId ")
                  InternalServerError(errorHandler.internalServerErrorTemplate)
              }
          case _                                =>
            logger.info(s"removeTeamMemberAction: getCollaboratorByUserIdAndOrganisationId failed for  orgId:${organisationId.value} & userId: $userId ")
            successful(InternalServerError(errorHandler.internalServerErrorTemplate))
        }
      }

      def handleValidForm(form: RemoveTeamMemberConfirmationForm): Future[Result] = {
        form.confirm match {
          case Some("Yes") => handleRemoveTeamMember()
          case _           => successful(Redirect(routes.OrganisationController.viewOrganisationPage(organisationId).url))
        }
      }

      def handleInvalidForm(formWithErrors: Form[RemoveTeamMemberConfirmationForm]) =
        successful(BadRequest(removeTeamMemberView(formWithErrors, organisationId, userId, formWithErrors("email").value.getOrElse(""))))

      RemoveTeamMemberConfirmationForm.form.bindFromRequest.fold(handleInvalidForm, handleValidForm)
  }

  private def getCollaboratorByUserIdAndOrganisationId(organisationId: OrganisationId,
                                                       userId: String)(
    implicit hc: HeaderCarrier): Future[Option[Collaborator]] = {

    xmlServicesConnector.getOrganisationByOrganisationId(organisationId).map {
      case Right(organisation: Organisation) =>
        organisation.collaborators.find(_.userId.equalsIgnoreCase(userId))
      case Left(error: Throwable)            =>
        logger.error(s"getOrganisationByOrganisationId failed for orgId:${organisationId.value}", error)
        None
    }
  }

  private def getCollaboratorByEmailAddressAndOrganisationId(organisationId: OrganisationId,
                                                             emailAddress: String)(
    implicit hc: HeaderCarrier): Future[Option[Collaborator]] = {

    xmlServicesConnector.getOrganisationByOrganisationId(organisationId).map {
      case Right(organisation: Organisation) =>
        organisation.collaborators.find(_.email.equalsIgnoreCase(emailAddress.trim))
      case Left(error: Throwable)            =>
        logger.error(s"getCollaboratorByEmailAddressAndOrganisationId failed for orgId:${organisationId.value}", error)
        None
    }
  }

}
