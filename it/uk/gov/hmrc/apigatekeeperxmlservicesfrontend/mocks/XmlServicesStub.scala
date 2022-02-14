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

package uk.gov.hmrc.apigatekeeperxmlservicesfrontend.mocks

import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.JsonFormatters._
import play.api.libs.json.Json
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.connectors.{AddCollaboratorRequest, RemoveCollaboratorRequest}

trait XmlServicesStub {

  val baseUrl = "/api-platform-xml-services"

  val organisationUrl = s"$baseUrl/organisations"

  val csvuploadUrl = s"$baseUrl/csvupload"

  def  updateOrganistionDetailsUrl(organisationId: OrganisationId) ={
    s"$organisationUrl/${organisationId.value.toString}"
  }

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

  def createOrganisationRequestAsString(organisationName: String, email: String): String = {
    Json.toJson(CreateOrganisationRequest(organisationName, email)).toString
  }

  def updateOrganisationDetailsRequestAsString(organisationName: String): String = {
    Json.toJson(UpdateOrganisationDetailsRequest(organisationName)).toString
  }

  def addCollaboratorRequestAsString(email: String): String = {
    Json.toJson(AddCollaboratorRequest(email)).toString
  }

  def bulkUploadOrganisationsRequestAsString(organisationsWithNameAndVendorIds: Seq[OrganisationWithNameAndVendorId]): String = {
    Json.toJson(BulkUploadOrganisationsRequest(organisationsWithNameAndVendorIds)).toString
  }

  def bulAddUsersRequestAsString(users: Seq[ParsedUser]): String ={
    Json.toJson((BulkAddUsersRequest(users))).toString()
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

  def getAllApisResponseWithBody(status: Int, responseBody: String) = {

    stubFor(get(urlEqualTo(s"$baseUrl/xml/apis"))
      .willReturn(
        aResponse()
          .withStatus(status)
          .withHeader("Content-Type", "application/json")
          .withBody(responseBody)
      ))
  }

  def getAllApisReturnsError(status: Int) = {

    stubFor(get(urlEqualTo(s"$baseUrl/xml/apis"))
      .willReturn(
        aResponse()
          .withStatus(status)
          .withHeader("Content-Type", "application/json")
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

  def addOrganisationReturnsResponse(organisationName: String, email: String, status: Int, response: Organisation) = {

    stubFor(post(urlEqualTo(organisationUrl))
      .withRequestBody(equalToJson(createOrganisationRequestAsString(organisationName, email)))
      .willReturn(
        aResponse()
          .withStatus(status)
          .withBody(Json.toJson(response).toString)
      ))
  }

  def addOrganisationReturnsError(organisationName: String, email: String, status: Int) = {

    stubFor(post(urlEqualTo(organisationUrl))
      .withRequestBody(equalToJson(createOrganisationRequestAsString(organisationName, email)))
      .willReturn(
        aResponse()
          .withStatus(status)
      ))
  }

  def updateOrganisationDetailsReturnsResponse(organisationName: String, organisationId: OrganisationId,  status: Int, response: Organisation) = {

    stubFor(post(urlEqualTo(updateOrganistionDetailsUrl(organisationId)))
      .withRequestBody(equalToJson(updateOrganisationDetailsRequestAsString(organisationName)))
      .willReturn(
        aResponse()
          .withStatus(status)
          .withBody(Json.toJson(response).toString)
      ))
  }

  def updateOrganisationDetailsReturnsError(organisationName: String, organisationId: OrganisationId, status: Int) = {
    stubFor(post(urlEqualTo(updateOrganistionDetailsUrl(organisationId)))
      .withRequestBody(equalToJson(updateOrganisationDetailsRequestAsString(organisationName)))
      .willReturn(
        aResponse()
          .withStatus(status)
      ))
  }

    def removeOrganisationStub(organisationId: OrganisationId,  status: Int) = {

    stubFor(delete(urlEqualTo(updateOrganistionDetailsUrl(organisationId)))
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

    stubFor(post(urlEqualTo(s"$csvuploadUrl/bulkorganisations"))
      .withRequestBody(equalToJson(bulkUploadOrganisationsRequestAsString(organisationsWithNameAndVendorIds)))
      .willReturn(
        aResponse()
          .withStatus(status)
      ))
  }

    def bulkAddUsersReturnsResponse(users: Seq[ParsedUser], status: Int) = {

    stubFor(post(urlEqualTo(s"$csvuploadUrl/bulkusers"))
      .withRequestBody(equalToJson(bulAddUsersRequestAsString(users)))
      .willReturn(
        aResponse()
          .withStatus(status)
      ))
  }

}
