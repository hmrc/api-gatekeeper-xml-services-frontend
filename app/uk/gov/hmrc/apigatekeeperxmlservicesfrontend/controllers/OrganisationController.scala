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

import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.utils.GatekeeperAuthWrapper
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.connectors.AuthConnector
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.ForbiddenView
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.GatekeeperRole

import scala.concurrent.ExecutionContext
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.config.AppConfig
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.organisation.OrganisationSearchView
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.Organisation

import java.{util => ju}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.OrganisationId
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.VendorId
@Singleton
class OrganisationController @Inject() (
    mcc: MessagesControllerComponents,
    organisationSearchView: OrganisationSearchView,
    override val authConnector: AuthConnector,
    val forbiddenView: ForbiddenView
  )(implicit val ec: ExecutionContext,
    appConfig: AppConfig)
    extends FrontendController(mcc)
    with GatekeeperAuthWrapper {

  val organisationsPage: Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request =>
      Future.successful(Ok(organisationSearchView(List.empty)))
  }

  def organisationsSearchAction(searchType: String, searchText: String): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request =>
      val org1 = Organisation(organisationId = OrganisationId(ju.UUID.randomUUID()), vendorId = VendorId(1), name = "Org 1")
      val org2 = org1.copy(vendorId = VendorId(2), name = "Org 2")
      val org3 = org1.copy(vendorId = VendorId(3), name = "Org 3")
      Future.successful(Ok(organisationSearchView(List(org1, org2, org3))))
  }

}
