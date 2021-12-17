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

import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.config.AppConfig
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.connectors.AuthConnector
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.connectors.XmlServicesConnector
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.GatekeeperRole
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.Organisation
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.OrganisationId
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.VendorId
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.utils.GatekeeperAuthWrapper
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.ErrorTemplate
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.ForbiddenView
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.organisation._
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Try

@Singleton
class OrganisationController @Inject() (
    mcc: MessagesControllerComponents,
    organisationSearchView: OrganisationSearchView,
    organisationDetailsView: OrganisationDetailsView,
    override val authConnector: AuthConnector,
    val forbiddenView: ForbiddenView,
    errorTemplate: ErrorTemplate,
    xmlServicesConnector: XmlServicesConnector
  )(implicit val ec: ExecutionContext,
    appConfig: AppConfig)
    extends FrontendController(mcc)
    with GatekeeperAuthWrapper {

  val organisationsPage: Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request =>
      Future.successful(Ok(organisationSearchView(List.empty, false)))
  }

  private def toVendorIdOrNone(txtVal: Option[String]): Option[VendorId] = {
    txtVal.flatMap(x => Try(x.toLong).toOption.map(VendorId(_)))
  }

  private def isValidVendorId(txtVal: Option[String]): Boolean = {
    if (txtVal.nonEmpty && txtVal.head.isEmpty) true
    else toVendorIdOrNone(txtVal).nonEmpty
  }

  def organisationsSearchAction(searchType: String, maybeSearchText: Option[String]): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request =>
      (searchType, maybeSearchText) match {
        case (vendorIdParameterName, _) if isValidVendorId(maybeSearchText) => xmlServicesConnector.findOrganisationsByParams(toVendorIdOrNone(maybeSearchText), None).map {
            case Right(orgs: List[Organisation])                 => Ok(organisationSearchView(orgs))
            case Left(UpstreamErrorResponse(_, NOT_FOUND, _, _)) => Ok(organisationSearchView(List.empty))
            case Left(_)                                         => {
              InternalServerError(errorTemplate("Internal Server Error", "Internal Server Error", "Internal Server Error"))
            }
          }

        case _                                                              => {
          Future.successful(Ok(organisationSearchView(List.empty)))
        }
      }

  }

  def manageOrganisation(organisationId: OrganisationId): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request =>

      xmlServicesConnector.getOrganisationByOrganisationId(organisationId)
      .map {
        case Right(org: Organisation) => Ok(organisationDetailsView(org))
        // in theory this error
        case Left(_)  => InternalServerError(errorTemplate("Internal Server Error", "Internal Server Error", "Internal Server Error"))

      }
  }

}

object OrganisationController {
  val vendorIdParameterName = "vendor-id"
}
