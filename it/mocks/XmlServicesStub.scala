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

package mocks

import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.JsonFormatters._
import play.api.libs.json.Json

trait XmlServicesStub {

  val baseUrl = "/api-platform-xml-services"

  val organisationUrl = s"$baseUrl/organisations"

  def findOrganisationByParamsUrl(vendorId: Option[String]) = vendorId match {
    case None    => s"$baseUrl/organisations"
    case Some(v) => s"$baseUrl/organisations?vendorId=$v"
  }

  def createOrganisationRequestAsString(organisationName: String): String = {
    Json.toJson(CreateOrganisationRequest(organisationName = organisationName)).toString
  }

  def findOrganisationByParamsReturnsError(vendorId: Option[String], status: Int) = {

    stubFor(get(urlEqualTo(findOrganisationByParamsUrl(vendorId)))
      .willReturn(
        aResponse()
          .withStatus(status)
          .withHeader("Content-Type", "application/json")
      ))
  }

  def findOrganisationByParamsReturnsResponseWithBody(vendorId: Option[String], status: Int, responseBody: String) = {

    stubFor(get(urlEqualTo(findOrganisationByParamsUrl(vendorId)))
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
}
