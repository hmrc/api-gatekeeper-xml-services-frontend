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

package mocks

import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.JsonFormatters._
import play.api.libs.json.Json
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.connectors.{AddCollaboratorRequest, RemoveCollaboratorRequest}

trait XmlServicesStub {

  val baseUrl = "/api-platform-xml-services"

  val organisationUrl = s"$baseUrl/organisations"
  
  def  removeTeamMemberUrl(organisationId: OrganisationId) ={
    s"$organisationUrl/${organisationId.value.toString}/remove-collaborator"
  }

  def  addTeamMemberUrl(organisationId: OrganisationId) ={
    s"$organisationUrl/${organisationId.value.toString}/add-collaborator"
  }
  
  def findOrganisationByParamsUrl(vendorId: Option[String], organisationName: Option[String]) =
    (vendorId, organisationName) match {
      case (Some(v), None)       => s"$baseUrl/organisations?vendorId=$v&sortBy=VENDOR_ID"
      case (None, Some(orgName)) => s"$baseUrl/organisations?organisationName=${java.net.URLEncoder.encode(orgName, "UTF-8")}&sortBy=ORGANISATION_NAME"
      case _                     => s"$baseUrl/organisations"
    }

  def createOrganisationRequestAsString(organisationName: String): String = {
    Json.toJson(CreateOrganisationRequest(organisationName)).toString
  }

  def addCollaboratorRequestAsString(email: String): String = {
    Json.toJson(AddCollaboratorRequest(email)).toString
  }

  def bulkFindAndCreateOrUpdateRequestAsString(organisationsWithNameAndVendorIds: Seq[OrganisationWithNameAndVendorId]): String = {
    Json.toJson(BulkFindAndCreateOrUpdateRequest(organisationsWithNameAndVendorIds)).toString
  }

  def removeCollaboratorRequestAsString(email: String, gateKeeperId: String): String = {
    Json.toJson(RemoveCollaboratorRequest(email, gateKeeperId)).toString
  }

  def findOrganisationByParamsReturnsError(vendorId: Option[String], organisationName: Option[String], status: Int) = {

    stubFor(get(urlEqualTo(findOrganisationByParamsUrl(vendorId, organisationName)))
      .willReturn(
        aResponse()
          .withStatus(status)
          .withHeader("Content-Type", "application/json")
      ))
  }

  def findOrganisationByParamsReturnsResponseWithBody(vendorId: Option[String], organisationName: Option[String], status: Int, responseBody: String) = {

    stubFor(get(urlEqualTo(findOrganisationByParamsUrl(vendorId, organisationName)))
      .willReturn(
        aResponse()
          .withStatus(status)
          .withHeader("Content-Type", "application/json")
          .withBody(responseBody)
      ))
  }

  def getOrganisationByOrganisationIdReturnsResponseWithBody(orgId: OrganisationId, status: Int, responseBody: String) = {

    stubFor(get(urlEqualTo(s"$baseUrl/organisations/${orgId.value.toString}"))
      .willReturn(
        aResponse()
          .withStatus(status)
          .withHeader("Content-Type", "application/json")
          .withBody(responseBody)
      ))
  }

  def getOrganisationByOrganisationIdReturnsError(orgId: OrganisationId, status: Int) = {

    stubFor(get(urlEqualTo(s"$baseUrl/organisations/${orgId.value.toString}"))
      .willReturn(
        aResponse()
          .withStatus(status)
          .withHeader("Content-Type", "application/json")
      ))
  }

  def addOrganisationReturnsResponse(organisationName: String, status: Int, response: Organisation) = {

    stubFor(post(urlEqualTo(organisationUrl))
      .withRequestBody(equalToJson(createOrganisationRequestAsString(organisationName)))
      .willReturn(
        aResponse()
          .withStatus(status)
          .withBody(Json.toJson(response).toString)
      ))
  }

  def addOrganisationReturnsError(organisationName: String, status: Int) = {

    stubFor(post(urlEqualTo(organisationUrl))
      .withRequestBody(equalToJson(createOrganisationRequestAsString(organisationName)))
      .willReturn(
        aResponse()
          .withStatus(status)
      ))
  }

  def addTeamMemberReturnsResponse(organisationId: OrganisationId, email: String, status: Int, response: Organisation) = {

    stubFor(post(urlEqualTo(addTeamMemberUrl(organisationId)))
      .withRequestBody(equalToJson(addCollaboratorRequestAsString(email)))
      .willReturn(
        aResponse()
          .withStatus(status)
          .withBody(Json.toJson(response).toString)
      ))
  }

  def addTeamMemberReturnsError(organisationId: OrganisationId, email: String, status: Int) = {

    stubFor(post(urlEqualTo(addTeamMemberUrl(organisationId)))
      .withRequestBody(equalToJson(addCollaboratorRequestAsString(email)))
      .willReturn(
        aResponse()
          .withStatus(status)
      ))
  }

  def removeTeamMemberReturnsResponse(organisationId: OrganisationId, email: String, gatekeeperId: String, status: Int, response: Organisation) = {

    stubFor(post(urlEqualTo(removeTeamMemberUrl(organisationId)))
      .withRequestBody(equalToJson(removeCollaboratorRequestAsString(email, gatekeeperId)))
      .willReturn(
        aResponse()
          .withStatus(status)
          .withBody(Json.toJson(response).toString)
      ))
  }

  def removeTeamMemberReturnsError(organisationId: OrganisationId, email: String, gatekeeperId: String, status: Int) = {

    stubFor(post(urlEqualTo(removeTeamMemberUrl(organisationId)))
      .withRequestBody(equalToJson(removeCollaboratorRequestAsString(email, gatekeeperId)))
      .willReturn(
        aResponse()
          .withStatus(status)
      ))
  }

  def bulkFindAndCreateOrUpdateReturnsResponse(organisationsWithNameAndVendorIds: Seq[OrganisationWithNameAndVendorId], status: Int) = {

    stubFor(post(urlEqualTo(s"$organisationUrl/bulk"))
      .withRequestBody(equalToJson(bulkFindAndCreateOrUpdateRequestAsString(organisationsWithNameAndVendorIds)))
      .willReturn(
        aResponse()
          .withStatus(status)
      ))
  }

}
