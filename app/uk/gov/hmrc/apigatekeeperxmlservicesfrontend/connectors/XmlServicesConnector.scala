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

package uk.gov.hmrc.apigatekeeperxmlservicesfrontend.connectors

import javax.inject.{Inject, Singleton}

import uk.gov.hmrc.http.HttpClient
import scala.concurrent.ExecutionContext

import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.JsonFormatters._
import scala.concurrent.Future
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.connectors.XmlServicesConnector._
import uk.gov.hmrc.http.HeaderCarrier
import scala.util.control.NonFatal
import play.api.Logging
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.http.HttpReads.Implicits._

@Singleton
class XmlServicesConnector @Inject() (val http: HttpClient, val config: Config)(implicit ec: ExecutionContext) extends Logging {

  val baseUrl: String = s"${config.serviceBaseUrl}/api-platform-xml-services"

  def findOrganisationsByParams(vendorId: Option[VendorId])(implicit hc: HeaderCarrier): Future[Either[Throwable, List[Organisation]]] = {
    val params = vendorId.map(v => Seq(("vendorId" -> v.value.toString))).getOrElse(Seq.empty)

    handleResult(http.GET[List[Organisation]](url = s"${baseUrl}/organisations", queryParams = params))
  }
  
  def addOrganisation(organisationName: String)(implicit hc: HeaderCarrier): Future[Either[Throwable, CreateOrganisationResult]] = {
    val createOrganisationRequest: CreateOrganisationRequest = CreateOrganisationRequest(organisationName = organisationName)

    http.POST[CreateOrganisationRequest, Either[UpstreamErrorResponse, Unit]](
      url = s"${baseUrl}/organisations",
      body = createOrganisationRequest
    )
    .map(_ match {
      case Right(_) => Right(CreateOrganisationSuccessResult)
      case Left(err) => throw err
    })
  }

  private def handleResult[A](result: Future[A]): Future[Either[Throwable, A]] = {
    result.map(x => Right(x))
      .recover {
        case NonFatal(e) => logger.error(e.getMessage)
          Left(e)
      }
  }

}

object XmlServicesConnector {

  case class Config(
      serviceBaseUrl: String)
}
