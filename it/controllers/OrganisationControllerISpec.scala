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

package controllers

import mocks.XmlServicesStub
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.test.Helpers.{BAD_REQUEST, FORBIDDEN, NOT_FOUND, OK, INTERNAL_SERVER_ERROR}
import support.AuthServiceStub
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.connectors.XmlServicesConnector
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.JsonFormatters._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.{Organisation, OrganisationId, VendorId}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.support.ServerBaseISpec
import java.util.UUID
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.Collaborator

class OrganisationControllerISpec extends ServerBaseISpec with BeforeAndAfterEach with AuthServiceStub {

  protected override def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.port" -> wireMockPort,
        "metrics.enabled" -> true,
        "auditing.enabled" -> false,
        "auditing.consumer.baseUri.host" -> wireMockHost,
        "auditing.consumer.baseUri.port" -> wireMockPort,
        "microservice.services.api-platform-xml-services.host" -> wireMockHost,
        "microservice.services.api-platform-xml-services.port" -> wireMockPort
      )

  val url = s"http://localhost:$port/api-gatekeeper-xml-services"

  trait Setup extends XmlServicesStub {
    val wsClient: WSClient = app.injector.instanceOf[WSClient]

    val objInTest: XmlServicesConnector = app.injector.instanceOf[XmlServicesConnector]
    val vendorId: VendorId = VendorId(12)
    val organisation = Organisation(organisationId = OrganisationId(java.util.UUID.randomUUID()), vendorId = vendorId, name = "Org name")

    val organisationWithTeamMembers = Organisation(
      organisationId = OrganisationId(UUID.randomUUID()),
      vendorId = VendorId(14),
      name = "Org name3",
      collaborators = List(Collaborator("userId", "collaborator1@mail.com"))
    )

    def callGetEndpoint(url: String, headers: List[(String, String)] = List.empty): WSResponse =
      wsClient
        .url(url)
        .withHttpHeaders(headers: _*)
        .withFollowRedirects(false)
        .get()
        .futureValue

    def validateOrganisationRow(rowId: Int, org: Organisation, document: Document) = {
      document.getElementById(s"vendor-id-$rowId").text() mustBe org.vendorId.value.toString
      document.getElementById(s"name-$rowId").text() mustBe org.name
      document.getElementById(s"manage-org-$rowId-link").attr("href") mustBe s"/api-gatekeeper-xml-services/organisations/${org.organisationId.value.toString}"
    }
  }

  "OrganisationController" when {

    "GET /organisations" should {
      "respond with 200 and render organisation search page" in new Setup {
        primeAuthServiceSuccess()
        val result = callGetEndpoint(s"$url/organisations")
        result.status mustBe OK
        val content = Jsoup.parse(result.body)
        content.getElementById("page-heading").text() mustBe "Search for XML organisations"
      }

      "respond with 403 and render the Forbidden view" in new Setup {
        primeAuthServiceFail()
        val result = callGetEndpoint(s"$url/organisations")
        result.status mustBe FORBIDDEN
        val content = Jsoup.parse(result.body)
        content.getElementById("page-heading").text() mustBe "You do not have permission to access Gatekeeper"
      }

      "respond with 404 and render errorTemplate Correctly when path invalid" in new Setup {
        val result = callGetEndpoint(s"$url/unknown-path")
        result.status mustBe NOT_FOUND
      }
    }

    "GET /organisations-search" should {

      "respond with 403 and render the Forbidden view when auth fails" in new Setup {
        primeAuthServiceFail()
        val result = callGetEndpoint(s"$url/organisations-search?searchType=vendorId&searchText=hello")
        result.status mustBe FORBIDDEN
        val content = Jsoup.parse(result.body)
        content.getElementById("page-heading").text() mustBe "You do not have permission to access Gatekeeper"
      }

      "respond with 200 and render organisation search page correctly when no params provided" in new Setup {
        primeAuthServiceSuccess()
        val result = callGetEndpoint(s"$url/organisations-search")
        result.status mustBe BAD_REQUEST
      }

      "respond with 200 and render organisation search page when searchType is empty" in new Setup {
        primeAuthServiceSuccess()
        findOrganisationByParamsReturnsResponseWithBody(None, None, OK, Json.toJson(organisation).toString)
        val result = callGetEndpoint(s"$url/organisations-search?searchType=")
        result.status mustBe OK
      }

      "respond with 200 and render organisation search page when searchType query parameter is populated" in new Setup {
        primeAuthServiceSuccess()
        findOrganisationByParamsReturnsResponseWithBody(None, None, OK, Json.toJson(organisation).toString)
        val result = callGetEndpoint(s"$url/organisations-search?searchType=vendor-id")
        result.status mustBe OK
      }

      "respond with 200 and render organisation search page when organisation-name searchType and searchText query parameters are populated" in new Setup {
        primeAuthServiceSuccess()
        val jsonToReturn = Json.toJson(List(organisation)).toString

        findOrganisationByParamsReturnsResponseWithBody(None, Some("hello"), OK, jsonToReturn)

        val result = callGetEndpoint(s"$url/organisations-search?searchType=organisation-name&searchText=hello")
        result.status mustBe OK
        val content = Jsoup.parse(result.body)
        content.getElementById("page-heading").text() mustBe "Search for XML organisations"
        Option(content.getElementById("results-table")).isDefined mustBe true

        validateOrganisationRow(0, organisation, content)
      }

      "respond with 400 when searchType query parameter is missing" in new Setup {
        primeAuthServiceSuccess()

        val result = callGetEndpoint(s"$url/organisations-search?searchText=")
        result.status mustBe BAD_REQUEST
      }

      "respond with 200 and render organisation search page when both searchType and searchText query parameters are empty" in new Setup {
        primeAuthServiceSuccess()
        val result = callGetEndpoint(s"$url/organisations-search?searchType=&searchText=")
        result.status mustBe OK
        val content = Jsoup.parse(result.body)
        content.getElementById("page-heading").text() mustBe "Search for XML organisations"
        Option(content.getElementById("results-table")).isDefined mustBe true
      }

      "respond with 200 and render organisation search page when searchType is populated and searchText query parameters is empty" in new Setup {
        primeAuthServiceSuccess()
        val result = callGetEndpoint(s"$url/organisations-search?searchType=vendor-id&searchText=")
        result.status mustBe OK
        val content = Jsoup.parse(result.body)
        content.getElementById("page-heading").text() mustBe "Search for XML organisations"
        Option(content.getElementById("results-table")).isDefined mustBe true
      }

      "respond with 200 and render organisation search page when searchType is empty and searchText query parameters is populated" in new Setup {
        primeAuthServiceSuccess()
        val result = callGetEndpoint(s"$url/organisations-search?searchType=&searchText=hello")
        result.status mustBe OK
        val content = Jsoup.parse(result.body)
        content.getElementById("page-heading").text() mustBe "Search for XML organisations"
        Option(content.getElementById("results-table")).isDefined mustBe true
      }

      "respond with 200 and render organisation search page when vendor-id searchType and searchText query parameters are populated" in new Setup {
        primeAuthServiceSuccess()
        val result = callGetEndpoint(s"$url/organisations-search?searchType=vendor-id&searchText=hello")
        result.status mustBe OK
        val content = Jsoup.parse(result.body)
        content.getElementById("page-heading").text() mustBe "Search for XML organisations"
        Option(content.getElementById("results-table")).isDefined mustBe true
      }
    }

    "GET /organisations/:organisationId/team-members" should {
      "respond with 400 if invalid OrganisationId" in new Setup {
        primeAuthServiceSuccess()
        val result = callGetEndpoint(s"$url/organisations/aldskjflaskjdf/team-members")
        result.status mustBe BAD_REQUEST

      }

      "respond with 403 if auth fails" in new Setup {
        primeAuthServiceFail()
        val result = callGetEndpoint(s"$url/organisations/${UUID.randomUUID.toString}/team-members")
        result.status mustBe FORBIDDEN

      }

      "respond with 200 and render manage team members page" in new Setup {
        primeAuthServiceSuccess()
        val orgId = organisationWithTeamMembers.organisationId
        getOrganisationByOrganisationIdReturnsResponseWithBody(orgId, 200, Json.toJson(organisationWithTeamMembers).toString())
        val result = callGetEndpoint(s"$url/organisations/${orgId.value.toString}/team-members")
        result.status mustBe OK

        val document = Jsoup.parse(result.body)
        document.getElementById("org-name-caption").text() mustBe organisationWithTeamMembers.name
        document.getElementById("team-member-heading").text() mustBe "Manage team members"

      }

      "respond with 500 and render error template" in new Setup {
        primeAuthServiceSuccess()
        val orgId = organisationWithTeamMembers.organisationId
        getOrganisationByOrganisationIdReturnsError(orgId, 500)
        val result = callGetEndpoint(s"$url/organisations/${orgId.value.toString}/team-members")
        result.status mustBe INTERNAL_SERVER_ERROR

        val document = Jsoup.parse(result.body)
        document.getElementById("page-heading").text() mustBe "Internal Server Error"

      }
    }

  }
}
