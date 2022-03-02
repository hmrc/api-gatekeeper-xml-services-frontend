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

package uk.gov.hmrc.apigatekeeperxmlservicesfrontend.connectors

import play.api.Logging
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.connectors.XmlServicesConnector._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.JsonFormatters._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, UpstreamErrorResponse}
import play.api.http.Status.NO_CONTENT

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import uk.gov.hmrc.http.HttpResponse

@Singleton
class XmlServicesConnector @Inject()(val http: HttpClient, val config: Config)(implicit ec: ExecutionContext) extends Logging {

  val baseUrl: String = s"${config.serviceBaseUrl}/api-platform-xml-services"

  def findOrganisationsByParams(vendorId: Option[VendorId], organisationName: Option[String])
                               (implicit hc: HeaderCarrier): Future[Either[Throwable, List[Organisation]]] = {
    val vendorIdParams = vendorId.map(v => Seq("vendorId" -> v.value.toString)).getOrElse(Seq.empty)
    val orgNameParams = organisationName.map(o => Seq("organisationName" -> o)).getOrElse(Seq.empty)
    val sortByParams = (vendorId, organisationName) match {
      case (Some(_), None) => Seq("sortBy" -> "VENDOR_ID")
      case (None, Some(_)) => Seq("sortBy" -> "ORGANISATION_NAME")
      case _ => Seq.empty
    }

    val params = vendorIdParams ++ orgNameParams ++ sortByParams

    handleResult(http.GET[List[Organisation]](url = s"$baseUrl/organisations", queryParams = params))
  }

  def getOrganisationByOrganisationId(organisationId: OrganisationId)(implicit hc: HeaderCarrier): Future[Either[Throwable, Organisation]] = {
    handleResult(http.GET[Organisation](url = s"$baseUrl/organisations/${organisationId.value}"))
  }

  def addOrganisation(organisationName: String, email: String, firstName: String, lastName: String)
                     (implicit hc: HeaderCarrier): Future[CreateOrganisationResult] = {
    val createOrganisationRequest: CreateOrganisationRequest = CreateOrganisationRequest(organisationName, email, firstName, lastName)

    http.POST[CreateOrganisationRequest, Either[UpstreamErrorResponse, Organisation]](
      url = s"$baseUrl/organisations",
      body = createOrganisationRequest
    ).map {
      case Right(x: Organisation) => CreateOrganisationSuccess(x)
      case Left(err) => CreateOrganisationFailure(err)
    }

  }

  def updateOrganisationDetails(organisationId: OrganisationId, organisationName: String)
                               (implicit hc: HeaderCarrier): Future[UpdateOrganisationDetailsResult] = {
    val updateOrganisationDetailsRequest: UpdateOrganisationDetailsRequest = UpdateOrganisationDetailsRequest(organisationName)

    http.POST[UpdateOrganisationDetailsRequest, Either[UpstreamErrorResponse, Organisation]](
      url = s"$baseUrl/organisations/${organisationId.value}",
      body = updateOrganisationDetailsRequest
    ).map {
      case Right(x: Organisation) => UpdateOrganisationDetailsSuccess(x)
      case Left(err) => UpdateOrganisationDetailsFailure(err)
    }

  }

  def removeOrganisation(organisationId: OrganisationId)(implicit hc: HeaderCarrier): Future[Boolean] = {
    http.DELETE[HttpResponse](
      url = s"$baseUrl/organisations/${organisationId.value}")
      .map(_.status == NO_CONTENT)
      .recover {
        case NonFatal(e) =>
          logger.error(e.getMessage)
          false
      }
  }

  def addTeamMember(organisationId: OrganisationId, email: String, firstname: String, lastname: String)
                   (implicit hc: HeaderCarrier): Future[AddCollaboratorResult] = {
    http.POST[AddCollaboratorRequest, Either[UpstreamErrorResponse, Organisation]](
      url = s"$baseUrl/organisations/${organisationId.value}/add-collaborator",
      AddCollaboratorRequest(email, firstname, lastname)
    ).map {
      case Right(x: Organisation) => AddCollaboratorSuccess(x)
      case Left(err) => AddCollaboratorFailure(err)
    }

  }

  def bulkAddOrganisations(organisations: Seq[OrganisationWithNameAndVendorId])(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, Unit]] = {
    http.POST[BulkUploadOrganisationsRequest, Either[UpstreamErrorResponse, Unit]](
      url = s"$baseUrl/csvupload/bulkorganisations", BulkUploadOrganisationsRequest(organisations)
    )
  }

  def bulkAddUsers(users: Seq[ParsedUser])(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, Unit]] = {
    http.POST[BulkAddUsersRequest, Either[UpstreamErrorResponse, Unit]](
      url = s"$baseUrl/csvupload/bulkusers", BulkAddUsersRequest(users)
    )
  }

  def removeTeamMember(organisationId: OrganisationId, email: String, gateKeeperUserId: String)
                      (implicit hc: HeaderCarrier): Future[RemoveCollaboratorResult] = {
    http.POST[RemoveCollaboratorRequest, Either[UpstreamErrorResponse, Organisation]](
      url = s"$baseUrl/organisations/${organisationId.value}/remove-collaborator",
      RemoveCollaboratorRequest(email, gateKeeperUserId)
    ).map {
      case Right(x: Organisation) => RemoveCollaboratorSuccess(x)
      case Left(err) => RemoveCollaboratorFailure(err)
    }

  }

  def getAllApis()(implicit hc: HeaderCarrier): Future[Either[Throwable, Seq[XmlApi]]] = {
    handleResult(http.GET[Seq[XmlApi]](url = s"$baseUrl/xml/apis"))
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
  case class Config(serviceBaseUrl: String)
}

case class AddCollaboratorRequest(email: String, firstName: String, lastName: String)

case class RemoveCollaboratorRequest(email: String, gatekeeperUserId: String)
