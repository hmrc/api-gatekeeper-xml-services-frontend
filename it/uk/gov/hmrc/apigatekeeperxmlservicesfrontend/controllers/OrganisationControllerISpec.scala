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

package uk.gov.hmrc.apigatekeeperxmlservicesfrontend.controllers

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NO_CONTENT, SEE_OTHER}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.test.Helpers.{CONTENT_TYPE, FORBIDDEN, NOT_FOUND, OK}
import play.filters.csrf.CSRF.TokenProvider
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.connectors.XmlServicesConnector
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.JsonFormatters._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.thirdpartydeveloper.UserId
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.stubs.XmlServicesStub
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.support.{StrideAuthorisationStub, ServerBaseISpec}
import utils.MockCookies

import java.util.UUID

class OrganisationControllerISpec extends ServerBaseISpec with BeforeAndAfterEach with StrideAuthorisationStub {

  protected override def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.host" -> wireMockHost,
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
    val tokenProvider = app.injector.instanceOf[TokenProvider]
    val validHeaders: List[(String, String)] = List(HeaderNames.AUTHORIZATION -> "Bearer 123")
    val contentTypeHeader = HeaderNames.CONTENT_TYPE -> "application/x-www-form-urlencoded"
    val bypassCsrfTokenHeader = "Csrf-Token" -> "nocheck"

    val objInTest: XmlServicesConnector = app.injector.instanceOf[XmlServicesConnector]
    val vendorId: VendorId = VendorId(12)
    val organisationId = OrganisationId(java.util.UUID.randomUUID())
    val organisation = Organisation(organisationId, vendorId = vendorId, name = "Org name")

    val collaborator = Collaborator("userId", "collaborator1@mail.com")

    val organisationWithTeamMembers = Organisation(
      organisationId = organisationId,
      vendorId = VendorId(14),
      name = "Org name3",
      collaborators = List(collaborator)
    )

    val emailAddress = "a@b.com"
    val firstName = "Joe"
    val lastName = "Bloggs"

    val xmlApi1 = XmlApi(name = "xml api 1",
      serviceName = ServiceName("vat-and-ec-sales-list"),
      context = "/government/collections/vat-and-ec-sales-list-online-support-for-software-developers",
      description = "description",
      categories  = Some(Seq(ApiCategory.CUSTOMS)))

    val xmlApi2 = XmlApi(name = "xml api 3",
      serviceName = ServiceName("customs-import"),
      context = "/government/collections/customs-import",
      description = "description",
      categories  = Some(Seq(ApiCategory.CUSTOMS)))

    val organisationUsers = List(OrganisationUser(organisationId, UserId(UUID.randomUUID()), emailAddress, firstName, lastName, List(xmlApi1, xmlApi2)))

    def callGetEndpoint(url: String, headers: List[(String, String)] = List.empty): WSResponse =
      wsClient
        .url(url)
        .withHttpHeaders(headers: _*)
        .withCookies(MockCookies.makeWsCookie(app))
        .withFollowRedirects(false)
        .get()
        .futureValue

    def callPostEndpoint(url: String, headers: List[(String, String)] = List.empty, request: String): WSResponse =
      wsClient
        .url(url)
        .withHttpHeaders(headers: _*)
        .withCookies(MockCookies.makeWsCookie(app))
        .withFollowRedirects(false)
        .post(request)
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
        strideAuthorisationSucceeds()

        val result = callGetEndpoint(s"$url/organisations", validHeaders)

        result.status mustBe OK

        val content = Jsoup.parse(result.body)
        content.getElementById("page-heading").text() mustBe "Search for XML organisations"
      }

     "respond with 403 and render the Forbidden view" in new Setup {
        strideAuthorisationFails()
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
        strideAuthorisationFails()
        val result = callGetEndpoint(s"$url/organisations/search?searchType=vendorId&searchText=hello")
        result.status mustBe FORBIDDEN
        val content = Jsoup.parse(result.body)
        content.getElementById("page-heading").text() mustBe "You do not have permission to access Gatekeeper"
      }

      "respond with 200 and render organisation search page correctly when no params provided" in new Setup {
        strideAuthorisationSucceeds()
        findOrganisationByParamsReturnsResponseWithBody(None, None, OK, Json.toJson(organisation).toString)
        val result = callGetEndpoint(s"$url/organisations/search")
        result.status mustBe BAD_REQUEST
      }

      "respond with 200 and render organisation search page when searchType is empty" in new Setup {
        strideAuthorisationSucceeds()
        findOrganisationByParamsReturnsResponseWithBody(None, None, OK, Json.toJson(organisation).toString)
        val result = callGetEndpoint(s"$url/organisations/search?searchType=")
        result.status mustBe OK
      }

      "respond with 200 and render organisation search page when searchType query parameter is populated" in new Setup {
        strideAuthorisationSucceeds()
        findOrganisationByParamsReturnsResponseWithBody(None, None, OK, Json.toJson(organisation).toString)
        val result = callGetEndpoint(s"$url/organisations/search?searchType=vendor-id")
        result.status mustBe OK
      }

      "respond with 200 and render organisation search page when organisation-name searchType and searchText query parameters are populated" in new Setup {
        strideAuthorisationSucceeds()

        findOrganisationByParamsReturnsResponseWithBody(None, Some("hello"), OK, Json.toJson(List(organisation)).toString)

        val result = callGetEndpoint(s"$url/organisations/search?searchType=organisation-name&searchText=hello")
        result.status mustBe OK
        val content = Jsoup.parse(result.body)
        content.getElementById("page-heading").text() mustBe "Search for XML organisations"
        Option(content.getElementById("results-table")).isDefined mustBe true

        validateOrganisationRow(0, organisation, content)
      }

      "respond with 400 when searchType query parameter is missing" in new Setup {
        strideAuthorisationSucceeds()

        val result = callGetEndpoint(s"$url/organisations/search?searchText=")
        result.status mustBe BAD_REQUEST
      }

      "respond with 400 when backend returns error" in new Setup {
        strideAuthorisationSucceeds()

        val result = callGetEndpoint(s"$url/organisations/search?searchText=")
        result.status mustBe BAD_REQUEST
      }

      "respond with 200 and render organisation search page when both searchType and searchText query parameters are empty" in new Setup {
        strideAuthorisationSucceeds()

        findOrganisationByParamsReturnsResponseWithBody(None, None, OK, Json.toJson(List(organisation)).toString)

        val result = callGetEndpoint(s"$url/organisations/search?searchType=&searchText=")
        result.status mustBe OK
        val content = Jsoup.parse(result.body)
        content.getElementById("page-heading").text() mustBe "Search for XML organisations"
        Option(content.getElementById("results-table")).isDefined mustBe true
      }

      "respond with 200 and render organisation search page when searchType is populated and searchText query parameters is empty" in new Setup {
        strideAuthorisationSucceeds()
        val jsonToReturn = Json.toJson(List(organisation)).toString

        findOrganisationByParamsReturnsResponseWithBody(None, None, OK, jsonToReturn)

        val result = callGetEndpoint(s"$url/organisations/search?searchType=vendor-id&searchText=")
        result.status mustBe OK
        val content = Jsoup.parse(result.body)
        content.getElementById("page-heading").text() mustBe "Search for XML organisations"
        Option(content.getElementById("results-table")).isDefined mustBe true
      }

      "respond with 200 and render organisation search page when searchType is empty and searchText query parameters is populated" in new Setup {
        strideAuthorisationSucceeds()
        val result = callGetEndpoint(s"$url/organisations/search?searchType=&searchText=hello")
        result.status mustBe OK
        val content = Jsoup.parse(result.body)
        content.getElementById("page-heading").text() mustBe "Search for XML organisations"
        Option(content.getElementById("results-table")).isDefined mustBe true
      }

      "respond with 200 and render organisation search page when vendor-id searchType and searchText query parameters are populated" in new Setup {
        strideAuthorisationSucceeds()
        val result = callGetEndpoint(s"$url/organisations/search?searchType=vendor-id&searchText=hello")
        result.status mustBe OK
        val content = Jsoup.parse(result.body)
        content.getElementById("page-heading").text() mustBe "Search for XML organisations"
        Option(content.getElementById("results-table")).isDefined mustBe true
      }
    }

    "GET /add" should {

      "return 200 and display the add page when authorised " in new Setup {
        strideAuthorisationSucceeds()

        val result = callGetEndpoint(s"$url/organisations/add")
        result.status mustBe OK

        val document = Jsoup.parse(result.body)
        document.getElementById("page-heading").text() mustBe "Add organisation"
        document.getElementById("organisation-name-label").text() mustBe "Organisation name"
        Option(document.getElementById("organisationName")).isDefined mustBe true
        Option(document.getElementById("continue-button")).isDefined mustBe true
      }

      "return 403 when not authorised" in new Setup {
        strideAuthorisationFails()

        val result = callGetEndpoint(s"$url/organisations/add")
        result.status mustBe FORBIDDEN
      }

    }

    "GET  /:organisationId" should {

      "return 200 and display details page when authorised" in new Setup {
        strideAuthorisationSucceeds()
        getOrganisationByOrganisationIdReturnsResponseWithBody(organisationId, OK, Json.toJson(organisationWithTeamMembers).toString())
        getOrganisationUsersByOrganisationIdReturnsResponse(organisationId, OK, organisationUsers)

        val result = callGetEndpoint(s"$url/organisations/${organisationId.value.toString}")

        result.status mustBe OK
        val document = Jsoup.parse(result.body)
        document.getElementById("org-name-heading").text() mustBe "Name"
        document.getElementById("org-name-value").text() mustBe organisationWithTeamMembers.name

        document.getElementById("vendor-id-heading").text() mustBe "Vendor ID"
        document.getElementById("vendor-id-value").text() mustBe organisationWithTeamMembers.vendorId.value.toString

        document.getElementById("team-members-heading").text() mustBe "Team members"
        document.getElementById("user-email-0").text() mustBe "a@b.com"
        document.getElementById("user-services-0").text() mustBe "xml api 1 xml api 3"
      }

      "return 500 when organisation not found" in new Setup {
        strideAuthorisationSucceeds()
        getOrganisationByOrganisationIdReturnsError(organisationId, NOT_FOUND)
        getOrganisationUsersByOrganisationIdReturnsResponse(organisationId, OK, organisationUsers)

        val result = callGetEndpoint(s"$url/organisations/${organisationId.value.toString}")

        result.status mustBe INTERNAL_SERVER_ERROR

      }

      "return 500 when organisation users retrieve fails" in new Setup {
        strideAuthorisationSucceeds()
        getOrganisationByOrganisationIdReturnsResponseWithBody(organisationId, OK, Json.toJson(organisationWithTeamMembers).toString())
        getOrganisationUsersByOrganisationIdReturnsError(organisationId, NOT_FOUND)

        val result = callGetEndpoint(s"$url/organisations/${organisationId.value.toString}")

        result.status mustBe INTERNAL_SERVER_ERROR

      }

      "return 403 when not authorised" in new Setup {
        strideAuthorisationFails()

        val result = callGetEndpoint(s"$url/organisations/${organisationId.value.toString}")

        result.status mustBe FORBIDDEN
      }
    }

    "GET  /:organisationId/update" should {
      "return 200 and organisationDetails EditPage when auth is successful and organisationId exists" in new Setup {
        strideAuthorisationSucceeds()

        getOrganisationByOrganisationIdReturnsResponseWithBody(organisationId, OK, Json.toJson(organisationWithTeamMembers).toString())

        val result = callGetEndpoint(s"$url/organisations/${organisationId.value.toString}/update")

        result.status mustBe OK

        val document = Jsoup.parse(result.body)
        document.getElementById("organisation-name-label").text() mustBe "Change organisation name"
        Option(document.getElementById("organisationName")).isDefined mustBe true
        Option(document.getElementById("continue-button")).isDefined mustBe true
      }

      "return 500 and when auth is successful but organisation cannot be found" in new Setup {
        strideAuthorisationSucceeds()

        getOrganisationByOrganisationIdReturnsError(organisationId, NOT_FOUND)

        val result = callGetEndpoint(s"$url/organisations/${organisationId.value.toString}/update")

        result.status mustBe INTERNAL_SERVER_ERROR
      }

      "return 403 when not authorised" in new Setup {
        strideAuthorisationFails()

        val result = callGetEndpoint(s"$url/organisations/${organisationId.value.toString}/update")

        result.status mustBe FORBIDDEN
      }
    }

    "GET /:organisationId/remove" should {
      "return the removeOrganisationView when organisationId exists" in new Setup {
        strideAuthorisationSucceeds()
        getOrganisationByOrganisationIdReturnsResponseWithBody(organisationId, OK, Json.toJson(organisationWithTeamMembers).toString())

        val result = callGetEndpoint(s"$url/organisations/${organisationId.value.toString}/remove")

        result.status mustBe OK
      }

      "return the error page when organisationId does not exist" in new Setup {
        strideAuthorisationSucceeds()
        getOrganisationByOrganisationIdReturnsError(organisationId, NOT_FOUND)

        val result = callGetEndpoint(s"$url/organisations/${organisationId.value.toString}/remove")

        result.status mustBe INTERNAL_SERVER_ERROR
      }

      "return 403 when not authorised" in new Setup {
        strideAuthorisationFails()

        val result = callGetEndpoint(s"$url/organisations/${organisationId.value.toString}/remove")
        result.status mustBe FORBIDDEN
      }
    }

    "POST /:organisationId/remove" should {
      "return OK when organisation exists, confirm is YES and remove call is successful" in new Setup {
        strideAuthorisationSucceeds()
        getOrganisationByOrganisationIdReturnsResponseWithBody(organisationId, OK, Json.toJson(organisationWithTeamMembers).toString())
        removeOrganisationStub(organisation.organisationId, NO_CONTENT)
        val result = callPostEndpoint(s"$url/organisations/${organisationId.value.toString}/remove", validHeaders:+ bypassCsrfTokenHeader :+ contentTypeHeader, s"confirm=Yes;")

        result.headers.foreach(println)
        result.status mustBe OK
      }

      "return INTERNAL SERVER ERROR when organisation exists, confirm is YES and remove call fails" in new Setup {
        strideAuthorisationSucceeds()
        getOrganisationByOrganisationIdReturnsResponseWithBody(organisationId, OK, Json.toJson(organisationWithTeamMembers).toString())
        removeOrganisationStub(organisation.organisationId, NOT_FOUND)

        val result = callPostEndpoint(s"$url/organisations/${organisationId.value.toString}/remove", validHeaders:+ bypassCsrfTokenHeader :+ contentTypeHeader, s"confirm=Yes;")

        result.status mustBe INTERNAL_SERVER_ERROR
      }

      "return SEE_OTHER when organisation exists and confirm is No" in new Setup {
        strideAuthorisationSucceeds()
        getOrganisationByOrganisationIdReturnsResponseWithBody(organisationId, OK, Json.toJson(organisationWithTeamMembers).toString())

        val result = callPostEndpoint(s"$url/organisations/${organisationId.value.toString}/remove", validHeaders:+ bypassCsrfTokenHeader :+ contentTypeHeader, s"confirm=No;")

        result.status mustBe SEE_OTHER
      }

      "return INTERNAL_SERVER ERROR when organisation does not exists" in new Setup {
        strideAuthorisationSucceeds()
        getOrganisationByOrganisationIdReturnsError(organisationId, NOT_FOUND)

        val result = callPostEndpoint(s"$url/organisations/${organisationId.value.toString}/remove", validHeaders:+ bypassCsrfTokenHeader :+ contentTypeHeader, s"confirm=Yes;")

        result.status mustBe INTERNAL_SERVER_ERROR
      }

      "return FORBIDDEN when auth fails" in new Setup {
        strideAuthorisationFails()

        val result = callPostEndpoint(s"$url/organisations/${organisationId.value.toString}/remove", List(CONTENT_TYPE -> "application/x-www-form-urlencoded"), s"confirm=Yes;")

        result.status mustBe FORBIDDEN
      }
    }
 }
}

