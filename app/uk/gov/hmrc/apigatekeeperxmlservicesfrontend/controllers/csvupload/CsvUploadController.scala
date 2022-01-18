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

import org.apache.commons.csv.CSVRecord
import org.apache.commons.io.IOUtils
import play.api.Logging
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.config.AppConfig
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.connectors.AuthConnector
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.{GatekeeperRole, Organisation, OrganisationId, VendorId}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.forms.Forms.CsvData
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.utils.GatekeeperAuthWrapper
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.csvupload.OrganisationCsvUploadView
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.{ErrorTemplate, ForbiddenView}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import java.io.InputStreamReader
import javax.inject.{Inject, Singleton}
import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class CsvUploadController @Inject() (
    mcc: MessagesControllerComponents,
    organisationCsvUploadView: OrganisationCsvUploadView,
    errorTemplate: ErrorTemplate,
    override val authConnector: AuthConnector,
    val forbiddenView: ForbiddenView
  )(implicit val ec: ExecutionContext, appConfig: AppConfig) extends FrontendController(mcc) with GatekeeperAuthWrapper  with Logging {

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
          try{
          val organisations: Seq[Organisation] = mapToOrganisationFromCsv(csvData.csv)

          logger.info(s"***** Number of Organisations successfully parsed: ${organisations.size}")

          Future.successful(Ok(organisationCsvUploadView(csvDataForm)))
        } catch {
            case exception: Throwable => Future.successful(InternalServerError(errorTemplate("Internal Server Error", "Internal Server Error", exception.getMessage)))
        }
  }
      )
  }

  private def mapToOrganisationFromCsv(csvData: String): Seq[Organisation] = {
    val reader = new InputStreamReader(IOUtils.toInputStream(csvData))

    org.apache.commons.csv.CSVFormat.EXCEL
      .withFirstRecordAsHeader()
      .parse(reader).getRecords.asScala
      .map(createOrganisation)
  }

  private def createOrganisation(record: CSVRecord): Organisation = {
    val expectedValues = 2
    if (record.size() < expectedValues) throw new RuntimeException(s"Expected $expectedValues values on row ${record.getRecordNumber}")

    def parseString(s: String): String = {
      Option(s) match {
        case Some(s: String) if s.nonEmpty => s.trim()
        case _ => throw new RuntimeException(s"Organisation name cannot be empty")
      }
    }

    Organisation(
      organisationId = OrganisationId(java.util.UUID.randomUUID()),
      vendorId = VendorId(record.get("VENDORID").trim().toLong),
      name = parseString(record.get("NAME"))
    )
  }

}