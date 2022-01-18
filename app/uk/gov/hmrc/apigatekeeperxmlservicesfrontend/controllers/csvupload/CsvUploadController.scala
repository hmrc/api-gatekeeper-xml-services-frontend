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

package uk.gov.hmrc.apigatekeeperxmlservicesfrontend.controllers.csvupload

import play.api.Logging
import play.api.data.Form
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.config.AppConfig
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.connectors.AuthConnector
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.GatekeeperRole
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.Organisation
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.forms.Forms.CsvData
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.services.CsvUploadService
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.utils.GatekeeperAuthWrapper
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.ErrorTemplate
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.ForbiddenView
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.csvupload.OrganisationCsvUploadView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class CsvUploadController @Inject() (
    mcc: MessagesControllerComponents,
    organisationCsvUploadView: OrganisationCsvUploadView,
    errorTemplate: ErrorTemplate,
    override val authConnector: AuthConnector,
    val forbiddenView: ForbiddenView,
    val csvUploadService: CsvUploadService
  )(implicit val ec: ExecutionContext,
    appConfig: AppConfig)
    extends FrontendController(mcc)
    with GatekeeperAuthWrapper
    with Logging {

  val csvDataForm: Form[CsvData] = CsvData.form

  def organisationPage: Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request => Future.successful(Ok(organisationCsvUploadView(csvDataForm)))
  }

  def uploadOrganisationsCsvAction(): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request =>
      csvDataForm.bindFromRequest.fold(
        formWithErrors => {
          Future.successful(BadRequest(organisationCsvUploadView(formWithErrors)))
        },
        csvData => {
          try {
            val organisations: Seq[Organisation] = csvUploadService.mapToOrganisationFromCsv(csvData.csv)

            logger.info(s"Number of Organisations successfully parsed: ${organisations.size}")

            Future.successful(Ok(organisationCsvUploadView(csvDataForm)))
          } catch {
            case exception: Throwable => Future.successful(InternalServerError(errorTemplate("Internal Server Error", "Internal Server Error", exception.getMessage)))
          }
        }
      )
  }
}
