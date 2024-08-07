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

package uk.gov.hmrc.apigatekeeperxmlservicesfrontend.connectors

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

import play.api.Logging
import play.api.http.Status.NO_CONTENT
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}

import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.connectors.XmlServicesConnector._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.JsonFormatters._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models._

@Singleton
class XmlServicesConnector @Inject() (val http: HttpClientV2, val config: Config)(implicit ec: ExecutionContext) extends Logging {

  val baseUrl: String = s"${config.serviceBaseUrl}/api-platform-xml-services"

  def findOrganisationsByParams(
      vendorId: Option[VendorId],
      organisationName: Option[String]
    )(implicit hc: HeaderCarrier
    ): Future[Either[Throwable, List[Organisation]]] = {

    val vendorIdParams = vendorId.map(v => Seq("vendorId" -> v.value.toString)).getOrElse(Seq.empty)
    val orgNameParams  = organisationName.map(o => Seq("organisationName" -> o)).getOrElse(Seq.empty)
    val sortByParams   = (vendorId, organisationName) match {
      case (Some(_), None) => Seq("sortBy" -> "VENDOR_ID")
      case (None, Some(_)) => Seq("sortBy" -> "ORGANISATION_NAME")
      case _               => Seq.empty
    }

    val params = vendorIdParams ++ orgNameParams ++ sortByParams

    handleResult(http.get(url"$baseUrl/organisations?$params").execute[List[Organisation]])
  }

  def getOrganisationByOrganisationId(organisationId: OrganisationId)(implicit hc: HeaderCarrier): Future[Either[Throwable, Organisation]] = {
    handleResult(http.get(url"$baseUrl/organisations/${organisationId.value}").execute[Organisation])
  }

  def addOrganisation(organisationName: String, email: String, firstName: String, lastName: String)(implicit hc: HeaderCarrier): Future[CreateOrganisationResult] = {
    val createOrganisationRequest: CreateOrganisationRequest = CreateOrganisationRequest(organisationName, email, firstName, lastName)

    http.post(url"$baseUrl/organisations")
      .withBody(Json.toJson(createOrganisationRequest))
      .execute[Either[UpstreamErrorResponse, Organisation]]
      .map {
        case Right(x: Organisation) => CreateOrganisationSuccess(x)
        case Left(err)              => CreateOrganisationFailure(err)
      }

  }

  def updateOrganisationDetails(
      organisationId: OrganisationId,
      organisationName: String
    )(implicit hc: HeaderCarrier
    ): Future[UpdateOrganisationDetailsResult] = {
    val updateOrganisationDetailsRequest: UpdateOrganisationDetailsRequest = UpdateOrganisationDetailsRequest(organisationName)

    http.post(url"$baseUrl/organisations/${organisationId.value}")
      .withBody(Json.toJson(updateOrganisationDetailsRequest))
      .execute[Either[UpstreamErrorResponse, Organisation]]
      .map {
        case Right(x: Organisation) => UpdateOrganisationDetailsSuccess(x)
        case Left(err)              => UpdateOrganisationDetailsFailure(err)
      }

  }

  def removeOrganisation(organisationId: OrganisationId)(implicit hc: HeaderCarrier): Future[Boolean] = {
    http.delete(url"$baseUrl/organisations/${organisationId.value}")
      .execute[HttpResponse]
      .map(_.status == NO_CONTENT)
      .recover {
        case NonFatal(e) =>
          logger.error(e.getMessage)
          false
      }
  }

  def addTeamMember(
      organisationId: OrganisationId,
      email: String,
      firstname: String,
      lastname: String
    )(implicit hc: HeaderCarrier
    ): Future[AddCollaboratorResult] = {

    http.post(url"$baseUrl/organisations/${organisationId.value}/add-collaborator")
      .withBody(Json.toJson(AddCollaboratorRequest(email, firstname, lastname)))
      .execute[Either[UpstreamErrorResponse, Organisation]]
      .map {
        case Right(x: Organisation) => AddCollaboratorSuccess(x)
        case Left(err)              => AddCollaboratorFailure(err)
      }

  }

  def removeTeamMember(organisationId: OrganisationId, email: String, gateKeeperUserId: String)(implicit hc: HeaderCarrier): Future[RemoveCollaboratorResult] = {

    http.post(url"$baseUrl/organisations/${organisationId.value}/remove-collaborator")
      .withBody(Json.toJson(RemoveCollaboratorRequest(email, gateKeeperUserId)))
      .execute[Either[UpstreamErrorResponse, Organisation]]
      .map {
        case Right(x: Organisation) => RemoveCollaboratorSuccess(x)
        case Left(err)              => RemoveCollaboratorFailure(err)
      }

  }

  def getAllApis(implicit hc: HeaderCarrier): Future[Either[Throwable, Seq[XmlApi]]] = {
    handleResult(http.get(url"$baseUrl/xml/apis").execute[Seq[XmlApi]])
  }

  def getOrganisationUsersByOrganisationId(
      organisationId: OrganisationId
    )(implicit hc: HeaderCarrier
    ): Future[Either[Throwable, List[OrganisationUser]]] = {
    handleResult(http.get(url"$baseUrl/organisations/${organisationId.value}/get-users").execute[List[OrganisationUser]])
  }

  private def handleResult[A](result: Future[A]): Future[Either[Throwable, A]] = {
    result.map(x => Right(x))
      .recover {
        case NonFatal(e) =>
          logger.error(e.getMessage)
          Left(e)
      }
  }

}

object XmlServicesConnector {
  case class Config(serviceBaseUrl: String)
}

case class AddCollaboratorRequest(email: String, firstName: String, lastName: String)

case class RemoveCollaboratorRequest(email: String, gatekeeperUserId: String)
