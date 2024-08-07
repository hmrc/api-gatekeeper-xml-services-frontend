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

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

import play.api.data.Form
import play.api.data.Forms.{mapping, optional, text}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.config.{AppConfig, ErrorHandler}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.connectors.{ThirdPartyDeveloperConnector, XmlServicesConnector}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.controllers.FormUtils.emailValidator
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.controllers.OrganisationController._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.thirdpartydeveloper.UserResponse
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.organisation._
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.actions.{GatekeeperAuthorisationActions, GatekeeperStrideAuthorisationActions}
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.LoggedInRequest
import uk.gov.hmrc.apiplatform.modules.gkauth.services.{LdapAuthorisationService, StrideAuthorisationService}

object OrganisationController {
  val vendorIdParameterName     = "vendor-id"
  val organisationNameParamName = "organisation-name"

  case class AddOrganisationForm(organisationName: String, emailAddress: String)

  object AddOrganisationForm {

    val form = Form(
      mapping(
        "organisationName" -> text.verifying(error = "organisationname.error.required", x => x.trim.nonEmpty),
        "emailAddress"     -> emailValidator()
      )(AddOrganisationForm.apply)(AddOrganisationForm.unapply)
    )

  }

  case class AddOrganisationWithNewUserForm(organisationName: String, emailAddress: String, firstName: String, lastName: String)

  object AddOrganisationWithNewUserForm {

    val form = Form(
      mapping(
        "organisationName" -> text.verifying(error = "organisationname.error.required", x => x.trim.nonEmpty),
        "emailAddress"     -> emailValidator(),
        "firstName"        -> text.verifying("firstname.error.required", x => x.trim.nonEmpty),
        "lastName"         -> text.verifying("lastname.error.required", x => x.trim.nonEmpty)
      )(AddOrganisationWithNewUserForm.apply)(AddOrganisationWithNewUserForm.unapply)
    )

  }

  case class UpdateOrganisationDetailsForm(organisationName: String)

  object UpdateOrganisationDetailsForm {

    val form = Form(
      mapping(
        "organisationName" -> text.verifying("organisationname.error.required", x => x.trim.nonEmpty)
      )(UpdateOrganisationDetailsForm.apply)(UpdateOrganisationDetailsForm.unapply)
    )
  }

  final case class RemoveOrganisationConfirmationForm(confirm: Option[String] = Some(""))

  object RemoveOrganisationConfirmationForm {

    val form: Form[RemoveOrganisationConfirmationForm] = Form(
      mapping(
        "confirm" -> optional(text).verifying("organisation.error.confirmation.no.choice.field", _.isDefined)
      )(RemoveOrganisationConfirmationForm.apply)(RemoveOrganisationConfirmationForm.unapply)
    )
  }

}

@Singleton
class OrganisationController @Inject() (
    mcc: MessagesControllerComponents,
    organisationSearchView: OrganisationSearchView,
    organisationDetailsView: OrganisationDetailsView,
    organisationAddView: OrganisationAddView,
    organisationAddNewUserView: OrganisationAddNewUserView,
    organisationUpdateView: OrganisationUpdateView,
    organisationRemoveView: OrganisationRemoveView,
    organisationRemoveSuccessView: OrganisationRemoveSuccessView,
    val ldapAuthorisationService: LdapAuthorisationService,
    val strideAuthorisationService: StrideAuthorisationService,
    errorHandler: ErrorHandler,
    xmlServicesConnector: XmlServicesConnector,
    thirdPartyDeveloperConnector: ThirdPartyDeveloperConnector
  )(implicit val ec: ExecutionContext,
    appConfig: AppConfig
  ) extends FrontendController(mcc)
    with GatekeeperStrideAuthorisationActions
    with GatekeeperAuthorisationActions
    with WithUnsafeDefaultFormBinding {

  val addOrganisationForm: Form[AddOrganisationForm]                               = AddOrganisationForm.form
  val addOrganisationWithNewUserForm: Form[AddOrganisationWithNewUserForm]         = AddOrganisationWithNewUserForm.form
  val updateOrganisationDetailsForm: Form[UpdateOrganisationDetailsForm]           = UpdateOrganisationDetailsForm.form
  val removeOrganisationConfirmationForm: Form[RemoveOrganisationConfirmationForm] = RemoveOrganisationConfirmationForm.form

  val organisationsPage: Action[AnyContent] = anyAuthenticatedUserAction {
    implicit request =>
      successful(Ok(organisationSearchView(List.empty, showTable = false)))
  }

  def organisationsSearchAction(searchType: String, searchText: Option[String]): Action[AnyContent] = anyAuthenticatedUserAction {
    implicit request =>
      def toVendorIdOrNone(txtVal: Option[String]): Option[VendorId] = {
        txtVal.flatMap(x => Try(x.toLong).toOption.map(VendorId))
      }

      def isValidVendorId(txtVal: Option[String]): Boolean = {
        if (txtVal.nonEmpty && txtVal.head.isEmpty) true
        else toVendorIdOrNone(txtVal).nonEmpty
      }

      def handleResults(result: Either[Throwable, List[Organisation]], isVendorIdSearch: Boolean): Future[Result] = {
        result match {
          case Right(orgs: List[Organisation])                 => successful(Ok(organisationSearchView(orgs, isVendorIdSearch = isVendorIdSearch)))
          case Left(UpstreamErrorResponse(_, NOT_FOUND, _, _)) => successful(Ok(organisationSearchView(List.empty, isVendorIdSearch = isVendorIdSearch)))
          case Left(_)                                         => errorHandler.internalServerErrorTemplate.map(InternalServerError(_))
        }
      }

      searchType match {
        case x: String if isValidVendorId(searchText) && (x == vendorIdParameterName) =>
          xmlServicesConnector.findOrganisationsByParams(toVendorIdOrNone(searchText), None).flatMap(handleResults(_, isVendorIdSearch = true))
        case x: String if x == organisationNameParamName                              =>
          xmlServicesConnector.findOrganisationsByParams(None, searchText).flatMap(handleResults(_, isVendorIdSearch = false))
        case _                                                                        => successful(Ok(organisationSearchView(List.empty)))
      }
  }

  val organisationsAddPage: Action[AnyContent] = anyStrideUserAction {
    implicit request => successful(Ok(organisationAddView(addOrganisationForm)))
  }

  def organisationsAddAction(): Action[AnyContent] = anyStrideUserAction {
    implicit request =>
      addOrganisationForm.bindFromRequest().fold(
        formWithErrors => successful(BadRequest(organisationAddView(formWithErrors))),
        formData => {
          thirdPartyDeveloperConnector.getByEmails(List(formData.emailAddress)).flatMap {
            case Right(Nil)                       =>
              successful(Ok(organisationAddNewUserView(addOrganisationWithNewUserForm, Some(formData.organisationName), Some(formData.emailAddress))))
            case Right(users: List[UserResponse]) =>
              addOrganisation(formData.organisationName, formData.emailAddress, users.head.firstName, users.head.lastName)
            case Left(_)                          =>
              errorHandler.internalServerErrorTemplate.map(InternalServerError(_))

          }
        }
      )
  }

  def organisationsAddWithNewUserAction(): Action[AnyContent] = anyStrideUserAction {
    implicit request =>
      addOrganisationWithNewUserForm.bindFromRequest().fold(
        formWithErrors => successful(BadRequest(organisationAddNewUserView(formWithErrors, None, None))),
        formData => addOrganisation(formData.organisationName, formData.emailAddress, formData.firstName, formData.lastName)
      )
  }

  private def addOrganisation(
      organisationName: String,
      emailAddress: String,
      firstName: String,
      lastName: String
    )(implicit hc: HeaderCarrier,
      loggedInRequest: LoggedInRequest[_]
    ): Future[Result] = {
    xmlServicesConnector
      .addOrganisation(organisationName, emailAddress, firstName, lastName)
      .flatMap {
        case CreateOrganisationSuccess(x: Organisation) =>
          successful(Redirect(uk.gov.hmrc.apigatekeeperxmlservicesfrontend.controllers.routes.OrganisationController.viewOrganisationPage(x.organisationId)))
        case _                                          => errorHandler.internalServerErrorTemplate.map(InternalServerError(_))
      }
  }

  def viewOrganisationPage(organisationId: OrganisationId): Action[AnyContent] = anyAuthenticatedUserAction {
    implicit request =>
      (for {
        getOrganisationResult <- xmlServicesConnector.getOrganisationByOrganisationId(organisationId)
        getUsersResult        <- xmlServicesConnector.getOrganisationUsersByOrganisationId(organisationId)
      } yield (getOrganisationResult, getUsersResult)).flatMap {
        case (Right(org: Organisation), Right(users: List[OrganisationUser])) => successful(Ok(organisationDetailsView(org, users)))

        case _ => errorHandler.internalServerErrorTemplate.map(InternalServerError(_))
      }
  }

  def updateOrganisationsDetailsPage(organisationId: OrganisationId): Action[AnyContent] = anyStrideUserAction {
    implicit request =>
      xmlServicesConnector.getOrganisationByOrganisationId(organisationId)
        .flatMap {
          case Right(organisation: Organisation) => successful(Ok(organisationUpdateView(updateOrganisationDetailsForm, organisation)))
          case Left(_)                           => errorHandler.internalServerErrorTemplate.map(InternalServerError(_))
        }
  }

  def updateOrganisationsDetailsAction(organisationId: OrganisationId): Action[AnyContent] = anyStrideUserAction {

    def handleFormAction(organisation: Organisation)(implicit request: LoggedInRequest[_]): Future[Result] = {
      updateOrganisationDetailsForm.bindFromRequest().fold(
        formWithErrors => successful(BadRequest(organisationUpdateView(formWithErrors, organisation))),
        formData =>
          xmlServicesConnector.updateOrganisationDetails(organisationId, formData.organisationName).flatMap {
            case UpdateOrganisationDetailsSuccess(_) =>
              successful(Redirect(uk.gov.hmrc.apigatekeeperxmlservicesfrontend.controllers.routes.OrganisationController.viewOrganisationPage(organisationId)))
            case _                                   =>
              errorHandler.internalServerErrorTemplate.map(InternalServerError(_))
          }
      )
    }

    implicit request =>
      xmlServicesConnector.getOrganisationByOrganisationId(organisationId)
        .flatMap {
          case Right(organisation: Organisation) => handleFormAction(organisation)
          case Left(_)                           => errorHandler.internalServerErrorTemplate.map(InternalServerError(_))
        }

  }

  def removeOrganisationPage(organisationId: OrganisationId): Action[AnyContent] = anyStrideUserAction {
    implicit request =>
      xmlServicesConnector.getOrganisationByOrganisationId(organisationId)
        .flatMap {
          case Right(org: Organisation) => successful(Ok(organisationRemoveView(removeOrganisationConfirmationForm, org)))
          case Left(_)                  => errorHandler.internalServerErrorTemplate.map(InternalServerError(_))
        }
  }

  def removeOrganisationAction(organisationId: OrganisationId): Action[AnyContent] = anyStrideUserAction {
    implicit request =>
      def handleRemoveOrganisation(organisation: Organisation): Future[Result] = {
        xmlServicesConnector.removeOrganisation(organisation.organisationId).flatMap {
          case true  => successful(Ok(organisationRemoveSuccessView(organisation)))
          case false => errorHandler.internalServerErrorTemplate.map(InternalServerError(_))
        }

      }

      def handleValidForm(form: RemoveOrganisationConfirmationForm, organisation: Organisation): Future[Result] = {
        form.confirm match {
          case Some("Yes") => handleRemoveOrganisation(organisation)
          case _           => Future.successful(Redirect(routes.OrganisationController.viewOrganisationPage(organisationId).url))
        }
      }

      def handleInvalidForm(formWithErrors: Form[RemoveOrganisationConfirmationForm], organisation: Organisation): Future[Result] =
        Future.successful(BadRequest(organisationRemoveView(formWithErrors, organisation)))

      xmlServicesConnector.getOrganisationByOrganisationId(organisationId)
        .flatMap {
          case Right(org: Organisation) => removeOrganisationConfirmationForm.bindFromRequest().fold(handleInvalidForm(_, org), handleValidForm(_, org))
          case Left(_)                  => errorHandler.internalServerErrorTemplate.map(InternalServerError(_))
        }

  }

}
