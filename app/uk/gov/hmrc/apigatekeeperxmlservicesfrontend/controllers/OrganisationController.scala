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

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.config.AppConfig
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.connectors.{AuthConnector, XmlServicesConnector}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.controllers.OrganisationController._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.{GatekeeperRole, Organisation, OrganisationId, VendorId}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.utils.GatekeeperAuthWrapper
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.organisation._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.{ErrorTemplate, ForbiddenView}
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController


import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.forms.AddOrganisation
import play.api.data.Form
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.CreateOrganisationSuccessResult

@Singleton
class OrganisationController @Inject() (
    mcc: MessagesControllerComponents,
    organisationSearchView: OrganisationSearchView,
    organisationDetailsView: OrganisationDetailsView,
    organisationAddView: OrganisationAddView,
    override val authConnector: AuthConnector,
    val forbiddenView: ForbiddenView,
    errorTemplate: ErrorTemplate,
    xmlServicesConnector: XmlServicesConnector
  )(implicit val ec: ExecutionContext,
    appConfig: AppConfig)
    extends FrontendController(mcc)
    with GatekeeperAuthWrapper {

        val addOrganisationForm: Form[AddOrganisation] = AddOrganisation.form

  val organisationsPage: Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request => Future.successful(Ok(organisationSearchView(List.empty, showTable = false)))
  }

  val organisationsAddPage: Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request => Future.successful(Ok(organisationAddView(addOrganisationForm)))
  }

  def organisationsAddAction(): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request =>
      addOrganisationForm.bindFromRequest.fold(
        formWithErrors => {
           Future.successful(BadRequest(organisationAddView(formWithErrors)))
        }, organisationAddData => {
           xmlServicesConnector
           .addOrganisation(organisationAddData.organisationname.getOrElse(""))
           .map{
             case CreateOrganisationSuccessResult(x: Organisation) =>  Redirect(uk.gov.hmrc.apigatekeeperxmlservicesfrontend.controllers.routes.OrganisationController.manageOrganisation(x.organisationId))
             case _ =>  InternalServerError(errorTemplate("Internal Server Error", "Internal Server Error", "Internal Server Error"))
           }          
        })
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
          case Right(orgs: List[Organisation])                 => Ok(organisationSearchView(orgs, isVendorIdSearch = isVendorIdSearch))
          case Left(UpstreamErrorResponse(_, NOT_FOUND, _, _)) => Ok(organisationSearchView(List.empty, isVendorIdSearch = isVendorIdSearch))
          case Left(_)                                         => InternalServerError(errorTemplate("Internal Server Error", "Internal Server Error", "Internal Server Error"))
        }
      }

      searchType match {
        case x: String if isValidVendorId(searchText) && (x == vendorIdParameterName) =>
          xmlServicesConnector.findOrganisationsByParams(toVendorIdOrNone(searchText), None).map(handleResults(_, isVendorIdSearch = true))
        case x: String if x == organisationNameParamName                              =>
          xmlServicesConnector.findOrganisationsByParams(None, searchText).map(handleResults(_, isVendorIdSearch = false))
        case _                                                                        => Future.successful(Ok(organisationSearchView(List.empty)))
      }
  }

  def manageOrganisation(organisationId: OrganisationId): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request =>
      xmlServicesConnector.getOrganisationByOrganisationId(organisationId)
        .map {
          case Right(org: Organisation) => Ok(organisationDetailsView(org, getEmailString(org)))
          // in theory this error
          case Left(_)                  => InternalServerError(errorTemplate("Internal Server Error", "Internal Server Error", "Internal Server Error"))
        }
  }

  private def getEmailString(org: Organisation): String = {
    if(org.collaborators.isEmpty) ""
    else org.collaborators.map(_.email).mkString("", ";", ";")
  }
}

object OrganisationController {
  val vendorIdParameterName = "vendor-id"
  val organisationNameParamName = "organisation-name"
}
