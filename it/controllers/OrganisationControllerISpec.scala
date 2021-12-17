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

package controllers

import mocks.XmlServicesStub
import org.scalatest.BeforeAndAfterEach
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.{WSClient, WSResponse}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.support.ServerBaseISpec
import play.api.test.Helpers.{BAD_REQUEST, FORBIDDEN, NOT_FOUND, OK}
import support.AuthServiceStub
import org.jsoup.Jsoup
import play.api.libs.json.Json
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.connectors.XmlServicesConnector
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.{Organisation, OrganisationId, VendorId}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.JsonFormatters._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.OrganisationName

class OrganisationControllerISpec extends ServerBaseISpec with BeforeAndAfterEach with AuthServiceStub {

  protected override def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.port" -> wireMockPort,
        "metrics.enabled" -> true,
        "auditing.enabled" -> false,
        "auditing.consumer.baseUri.host" -> wireMockHost,
        "auditing.consumer.baseUri.port" -> wireMockPort
      )

  val url = s"http://localhost:$port/api-gatekeeper-xml-services"

  trait Setup extends XmlServicesStub {
    val wsClient: WSClient = app.injector.instanceOf[WSClient]

    val objInTest: XmlServicesConnector = app.injector.instanceOf[XmlServicesConnector]
    val vendorId: VendorId = VendorId(12)

    val organisation = Organisation(organisationId = OrganisationId(java.util.UUID.randomUUID()), vendorId = vendorId, name = OrganisationName("Org name"))

    def callGetEndpoint(url: String, headers: List[(String, String)] = List.empty): WSResponse =
      wsClient
        .url(url)
        .withHttpHeaders(headers: _*)
        .withFollowRedirects(false)
        .get()
        .futureValue
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

      "respond with 403 and render the Forbidden view" in new Setup {
        primeAuthServiceFail()
        val result = callGetEndpoint(s"$url/organisations-search?searchType=vendorId&searchText=hello")
        result.status mustBe FORBIDDEN
        val content = Jsoup.parse(result.body)
        content.getElementById("page-heading").text() mustBe "You do not have permission to access Gatekeeper"

      }

      "respond with 200 and render organisation search page without query parameters" in new Setup {
        primeAuthServiceSuccess()
        val result = callGetEndpoint(s"$url/organisations-search")
        result.status mustBe BAD_REQUEST
      }

      "respond with 200 and render organisation search page when searchType query parameter is empty" in new Setup {
        primeAuthServiceSuccess()
        findOrganisationByParamsReturnsResponseWithBody(None, None, OK, Json.toJson(organisation).toString)
        val result = callGetEndpoint(s"$url/organisations-search?searchType=")
        result.status mustBe OK
      }

      "respond with 200 and render organisation search page when searchType query parameter is populated" in new Setup {
        primeAuthServiceSuccess()
        findOrganisationByParamsReturnsResponseWithBody(None, None,  OK, Json.toJson(organisation).toString)
        val result = callGetEndpoint(s"$url/organisations-search?searchType=vendor-id")
        result.status mustBe OK
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

      "respond with 200 and render organisation search page when both searchType and searchText query parameters are populated" in new Setup {
        primeAuthServiceSuccess()
        val result = callGetEndpoint(s"$url/organisations-search?searchType=vendor-id&searchText=hello")
        result.status mustBe OK
        val content = Jsoup.parse(result.body)
        content.getElementById("page-heading").text() mustBe "Search for XML organisations"
        Option(content.getElementById("results-table")).isDefined mustBe true
      }
    }
  }
}
