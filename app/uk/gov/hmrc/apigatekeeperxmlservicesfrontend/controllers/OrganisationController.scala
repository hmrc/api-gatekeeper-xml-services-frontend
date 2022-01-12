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

import controllers.routes
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.config.AppConfig
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.connectors.{AuthConnector, XmlServicesConnector}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.controllers.OrganisationController._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.{Collaborator, CreateOrganisationSuccessResult, GatekeeperRole, Organisation, OrganisationId, RemoveCollaboratorSuccessResult, VendorId}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.utils.GatekeeperAuthWrapper
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.organisation._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.{ErrorTemplate, ForbiddenView}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import play.api.data.Form
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.forms.Forms.{AddOrganisation, RemoveTeamMemberConfirmationForm}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.teammembers.{RemoveTeamMemberView, ManageTeamMembersView}

import scala.concurrent.Future.successful

@Singleton
class OrganisationController @Inject()(
                                        mcc: MessagesControllerComponents,
                                        organisationSearchView: OrganisationSearchView,
                                        organisationDetailsView: OrganisationDetailsView,
                                        organisationAddView: OrganisationAddView,
                                        manageTeamMembersView: ManageTeamMembersView,
                                        removeTeamMemberView: RemoveTeamMemberView,
                                        override val authConnector: AuthConnector,
                                        val forbiddenView: ForbiddenView,
                                        errorTemplate: ErrorTemplate,
                                        xmlServicesConnector: XmlServicesConnector
                                      )(implicit val ec: ExecutionContext,
                                        appConfig: AppConfig)
  extends FrontendController(mcc)
    with GatekeeperAuthWrapper {

  val addOrganisationForm: Form[AddOrganisation] = AddOrganisation.form
  val confirmRemoveForm: Form[RemoveTeamMemberConfirmationForm] = RemoveTeamMemberConfirmationForm.form

  val organisationsPage: Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request => successful(Ok(organisationSearchView(List.empty, showTable = false)))
  }

  val organisationsAddPage: Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request => successful(Ok(organisationAddView(addOrganisationForm)))
  }

  def organisationsAddAction(): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request =>
      addOrganisationForm.bindFromRequest.fold(
        formWithErrors => {
          successful(BadRequest(organisationAddView(formWithErrors)))
        },
        organisationAddData => {
          xmlServicesConnector
            .addOrganisation(organisationAddData.organisationname.getOrElse(""))
            .map {
              case CreateOrganisationSuccessResult(x: Organisation) =>
                Redirect(uk.gov.hmrc.apigatekeeperxmlservicesfrontend.controllers.routes.OrganisationController.manageOrganisation(x.organisationId))
              case _ => InternalServerError(errorTemplate("Internal Server Error", "Internal Server Error", "Internal Server Error"))
            }
        }
      )
  }

  def organisationsSearchAction(searchType: String, searchText: Option[String]): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request =>
      def toVendorIdOrNone(txtVal: Option[String]): Option[VendorId] = {
        txtVal.flatMap(x => Try(x.toLong).toOption.map(VendorId))
      }

      def isValidVendorId(txtVal: Option[String]): Boolean = {
        if (txtVal.nonEmpty && txtVal.head.isEmpty) true
        else toVendorIdOrNone(txtVal).nonEmpty
      }

      def handleResults(result: Either[Throwable, List[Organisation]], isVendorIdSearch: Boolean) = {
        result match {
          case Right(orgs: List[Organisation]) => Ok(organisationSearchView(orgs, isVendorIdSearch = isVendorIdSearch))
          case Left(UpstreamErrorResponse(_, NOT_FOUND, _, _)) => Ok(organisationSearchView(List.empty, isVendorIdSearch = isVendorIdSearch))
          case Left(_) => InternalServerError(errorTemplate("Internal Server Error", "Internal Server Error", "Internal Server Error"))
        }
      }

      searchType match {
        case x: String if isValidVendorId(searchText) && (x == vendorIdParameterName) =>
          xmlServicesConnector.findOrganisationsByParams(toVendorIdOrNone(searchText), None).map(handleResults(_, isVendorIdSearch = true))
        case x: String if x == organisationNameParamName =>
          xmlServicesConnector.findOrganisationsByParams(None, searchText).map(handleResults(_, isVendorIdSearch = false))
        case _ => successful(Ok(organisationSearchView(List.empty)))
      }
  }

  def manageOrganisation(organisationId: OrganisationId): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request =>
      xmlServicesConnector.getOrganisationByOrganisationId(organisationId)
        .map {
          case Right(org: Organisation) => Ok(organisationDetailsView(org, getEmailString(org)))
          // in theory this error
          case Left(_) => InternalServerError(errorTemplate("Internal Server Error", "Internal Server Error", "Internal Server Error"))
        }
  }

  def manageTeamMembers(organisationId: OrganisationId): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request => {
      xmlServicesConnector.getOrganisationByOrganisationId(organisationId)
        .map {
          case Right(org: Organisation) => Ok(manageTeamMembersView(org))
          case Left(_) => InternalServerError(errorTemplate("Internal Server Error", "Internal Server Error", "Internal Server Error"))
        }
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
                case RemoveCollaboratorSuccessResult(_) => Redirect(routes.OrganisationController.manageOrganisation(organisationId).url, SEE_OTHER)
                case _ => InternalServerError(errorTemplate("Internal Server Error", "Internal Server Error", "Internal Server Error"))
              }
          case _ => successful(InternalServerError(errorTemplate("Internal Server Error", "Internal Server Error", "Internal Server Error")))
        }
      }

      def handleValidForm(form: RemoveTeamMemberConfirmationForm): Future[Result] = {
        form.confirm match {
          case Some("Yes") => handleRemoveTeamMember()
          case _ => successful(Redirect(routes.OrganisationController.manageOrganisation(organisationId).url))
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

  private def getEmailString(org: Organisation): String = {
    if (org.collaborators.isEmpty) ""
    else org.collaborators.map(_.email).mkString("", ";", ";")
  }
}

object OrganisationController {
  val vendorIdParameterName = "vendor-id"
  val organisationNameParamName = "organisation-name"
}
