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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.config.AppConfig
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.connectors.{AuthConnector, XmlServicesConnector}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.controllers.OrganisationController._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.forms.Forms.{AddOrganisationForm, UpdateOrganisationDetailsForm}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.utils.GatekeeperAuthWrapper
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.organisation._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.{ErrorTemplate, ForbiddenView}
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.concurrent.Future.successful
import scala.util.Try

@Singleton
class OrganisationController @Inject()(
                                        mcc: MessagesControllerComponents,
                                        organisationSearchView: OrganisationSearchView,
                                        organisationDetailsView: OrganisationDetailsView,
                                        organisationAddView: OrganisationAddView,
                                        organisationUpdateView: OrganisationUpdateView,
                                        override val authConnector: AuthConnector,
                                        val forbiddenView: ForbiddenView,
                                        errorTemplate: ErrorTemplate,
                                        xmlServicesConnector: XmlServicesConnector
                                      )(implicit val ec: ExecutionContext,
                                        appConfig: AppConfig)
  extends FrontendController(mcc) with GatekeeperAuthWrapper {

  val addOrganisationForm: Form[AddOrganisationForm] = AddOrganisationForm.form
  val updateOrganisationDetailsForm: Form[UpdateOrganisationDetailsForm] = UpdateOrganisationDetailsForm.form

  val organisationsPage: Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request => successful(Ok(organisationSearchView(List.empty, showTable = false)))
  }

  val organisationsAddPage: Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request => successful(Ok(organisationAddView(addOrganisationForm)))
  }

  def organisationsAddAction(): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request =>
      addOrganisationForm.bindFromRequest.fold(
        formWithErrors => successful(BadRequest(organisationAddView(formWithErrors))),
        organisationAddData => {
          xmlServicesConnector
            .addOrganisation(organisationAddData.organisationname.getOrElse(""))
            .map {
              case CreateOrganisationSuccess(x: Organisation) =>
                Redirect(uk.gov.hmrc.apigatekeeperxmlservicesfrontend.controllers.routes.OrganisationController.viewOrganisationPage(x.organisationId))
              case _ => InternalServerError(errorTemplate("Internal Server Error", "Internal Server Error", "Internal Server Error"))
            }
        }
      )
  }

  def viewOrganisationPage(organisationId: OrganisationId): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request =>
      xmlServicesConnector.getOrganisationByOrganisationId(organisationId)
        .map {
          case Right(org: Organisation) => Ok(organisationDetailsView(org, getEmailString(org)))
          // in theory this error
          case Left(_) => InternalServerError(errorTemplate("Internal Server Error", "Internal Server Error", "Internal Server Error"))
        }
  }


  def updateOrganisationsDetailsPage(organisationId: OrganisationId): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request =>
      xmlServicesConnector.getOrganisationByOrganisationId(organisationId)
        .map {
          case Right(org: Organisation) =>Ok(organisationUpdateView(updateOrganisationDetailsForm, organisationId))
          // in theory this error
          case Left(_) => InternalServerError(errorTemplate("Internal Server Error", "Internal Server Error", "Internal Server Error"))
        }
  }




  def updateOrganisationsDetailsAction(organisationId: OrganisationId): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request =>
      updateOrganisationDetailsForm.bindFromRequest.fold(
        formWithErrors => successful(BadRequest(organisationUpdateView(formWithErrors, organisationId))),
        formData =>
          formData.organisationname match{
            case Some(organisationName: String) =>  xmlServicesConnector
                                          .updateOrganisationDetails(organisationId, organisationName).map{
              case UpdateOrganisationDetailsSuccess(_) =>
                Redirect(uk.gov.hmrc.apigatekeeperxmlservicesfrontend.controllers.routes.OrganisationController.viewOrganisationPage(organisationId))
              case _ =>
                Redirect(uk.gov.hmrc.apigatekeeperxmlservicesfrontend.controllers.routes.OrganisationController.viewOrganisationPage(organisationId))
            }
            case _  => successful(InternalServerError(errorTemplate("Internal Server Error", "Internal Server Error", "Internal Server Error")))
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



  private def getEmailString(org: Organisation): String = {
    if (org.collaborators.isEmpty) ""
    else org.collaborators.map(_.email).mkString("", ";", ";")
  }
}

object OrganisationController {
  val vendorIdParameterName = "vendor-id"
  val organisationNameParamName = "organisation-name"
}
