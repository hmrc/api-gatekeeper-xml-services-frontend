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

package controllers.csvupload

import org.jsoup.Jsoup
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.test.Helpers.{FORBIDDEN, INTERNAL_SERVER_ERROR, NOT_FOUND, OK, SEE_OTHER}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.mocks.XmlServicesStub
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.{OrganisationName, OrganisationWithNameAndVendorId, VendorId}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.support.{AuthServiceStub, ServerBaseISpec}

class CsvUploadControllerISpec extends ServerBaseISpec with BeforeAndAfterEach with AuthServiceStub {

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

    val invalidCsvPayload = """,NAME
    1110,TestOrganisation101"""

    val validCsvPayloadWithOneRow = """VENDORID,NAME
    1110,TestOrganisation101"""

    val validCsvPayloadWithTwoRows = """VENDORID,NAME
    1110,TestOrganisation101
    1111,TestOrganisation102"""

    val organisationsWithNameAndVendorIds = Seq(
      OrganisationWithNameAndVendorId(OrganisationName("TestOrganisation101"), VendorId(1110)),
      OrganisationWithNameAndVendorId(OrganisationName("TestOrganisation102"), VendorId(1111))
    )

    def callGetEndpoint(url: String, headers: List[(String, String)] = List.empty): WSResponse =
      wsClient
        .url(url)
        .withHttpHeaders(headers: _*)
        .withFollowRedirects(false)
        .get()
        .futureValue

    def callPostEndpoint(url: String, headers: List[(String, String)] = List.empty, request: String): WSResponse =
      wsClient
        .url(url)
        .withHttpHeaders(headers: _*)
        .withFollowRedirects(false)
        .post(request)
        .futureValue

    def validatePageIsRendered(result: WSResponse, status: Int) = {
      result.status mustBe status
      val content = Jsoup.parse(result.body)
      content.getElementById("page-heading").text() mustBe "Upload organisations as CSV"
    }
  }

  "CsvUploadController" when {

    "GET /csvupload/organisation-page" should {
      "respond with 200 and render organisation page" in new Setup {
        primeAuthServiceSuccess()
        val result = callGetEndpoint(s"$url/csvupload/organisation-page")
        result.status mustBe OK
        val content = Jsoup.parse(result.body)
        content.getElementById("page-heading").text() mustBe "Upload organisations as CSV"
      }

      "respond with 403 and render the Forbidden view" in new Setup {
        primeAuthServiceFail()
        val result = callGetEndpoint(s"$url/csvupload/organisation-page")
        result.status mustBe FORBIDDEN
        val content = Jsoup.parse(result.body)
        content.getElementById("page-heading").text() mustBe "You do not have permission to access Gatekeeper"
      }

      "respond with 404 and render errorTemplate Correctly when path invalid" in new Setup {
        val result = callGetEndpoint(s"$url/unknown-path")
        result.status mustBe NOT_FOUND
      }
    }

    "POST /csvupload/organisation-action" should {

      "redirect to organisation page when valid form provided and connector returns 200" in new Setup {
        primeAuthServiceSuccess()
        bulkFindAndCreateOrUpdateReturnsResponse(organisationsWithNameAndVendorIds, OK)

        val result = callPostEndpoint(
          url = s"$url/csvupload/organisation-action",
          List(CONTENT_TYPE -> "application/x-www-form-urlencoded"),
          s"csv-data-input=${validCsvPayloadWithTwoRows};"
        )
        result.status mustBe SEE_OTHER
      }

      "show error page when valid form provided but connector returns error" in new Setup {
        primeAuthServiceSuccess()
        bulkFindAndCreateOrUpdateReturnsResponse(organisationsWithNameAndVendorIds, INTERNAL_SERVER_ERROR)

        val result = callPostEndpoint(
          url = s"$url/csvupload/organisation-action",
          List(CONTENT_TYPE -> "application/x-www-form-urlencoded"),
          s"csv-data-input=${validCsvPayloadWithTwoRows};"
        )
        result.status mustBe INTERNAL_SERVER_ERROR
      }

      "show error page when invalid form provided" in new Setup {
        primeAuthServiceSuccess()

        val result = callPostEndpoint(
          url = s"$url/csvupload/organisation-action",
          List(CONTENT_TYPE -> "application/x-www-form-urlencoded"),
          s"csv-data-input=${invalidCsvPayload};"
        )

        result.status mustBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
