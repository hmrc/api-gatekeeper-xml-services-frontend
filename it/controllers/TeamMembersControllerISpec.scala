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
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.http.Status.SEE_OTHER
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.test.Helpers.{BAD_REQUEST, FORBIDDEN, INTERNAL_SERVER_ERROR, OK}
import support.AuthServiceStub
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.connectors.XmlServicesConnector
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.JsonFormatters._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.{Collaborator, Organisation, OrganisationId, VendorId}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.support.ServerBaseISpec

import java.util.UUID

class TeamMembersControllerISpec extends ServerBaseISpec with BeforeAndAfterEach with AuthServiceStub {

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
    val organisationId = OrganisationId(java.util.UUID.randomUUID())
    val organisation = Organisation(organisationId , vendorId = vendorId, name = "Org name")

    val collaborator = Collaborator("userId", "collaborator1@mail.com")
    val organisationWithTeamMembers = Organisation(
      organisationId = organisationId,
      vendorId = VendorId(14),
      name = "Org name3",
      collaborators = List(collaborator)
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

    def validateOrganisationRow(rowId: Int, org: Organisation, document: Document) = {
      document.getElementById(s"vendor-id-$rowId").text() mustBe org.vendorId.value.toString
      document.getElementById(s"name-$rowId").text() mustBe org.name
      document.getElementById(s"manage-org-$rowId-link").attr("href") mustBe s"/api-gatekeeper-xml-services/organisations/${org.organisationId.value.toString}"
    }
  }

  "TeamMembersController" when {

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
        getOrganisationByOrganisationIdReturnsResponseWithBody(orgId, OK, Json.toJson(organisationWithTeamMembers).toString())
        val result = callGetEndpoint(s"$url/organisations/${orgId.value.toString}/team-members")
        result.status mustBe OK

        val document = Jsoup.parse(result.body)
        document.getElementById("org-name-caption").text() mustBe organisationWithTeamMembers.name
        document.getElementById("team-member-heading").text() mustBe "Manage team members"

      }

      "respond with 500 and render error template" in new Setup {
        primeAuthServiceSuccess()
        val orgId = organisationWithTeamMembers.organisationId
        getOrganisationByOrganisationIdReturnsError(orgId, INTERNAL_SERVER_ERROR)
        val result = callGetEndpoint(s"$url/organisations/${orgId.value.toString}/team-members")
        result.status mustBe INTERNAL_SERVER_ERROR

        val document = Jsoup.parse(result.body)
        document.getElementById("page-heading").text() mustBe "Internal Server Error"

      }
    }

    "GET /organisations/:organisationId/team-members/:userId/remove" should {

      "respond with 200 when remove team member is successful" in new Setup {
        primeAuthServiceSuccess()

        getOrganisationByOrganisationIdReturnsResponseWithBody(organisationId, OK, Json.toJson(organisationWithTeamMembers).toString())

        val result = callGetEndpoint(s"$url/organisations/${organisationId.value.toString}/team-members/${organisationWithTeamMembers.collaborators.head.userId}/remove")

        result.status mustBe OK

        val document = Jsoup.parse(result.body)

        document.getElementById("page-heading").text() mustBe s"Are you sure you want to remove ${collaborator.email}?"
        Option(document.getElementById("yes")).isDefined mustBe true
        Option(document.getElementById("no")).isDefined mustBe true
        Option(document.getElementById("continue-button")).isDefined mustBe true

      }


      "respond with 400 if invalid OrganisationId" in new Setup {
        primeAuthServiceSuccess()

        val result = callGetEndpoint(s"$url/organisations/aldskjflaskjdf/team-members/${organisationWithTeamMembers.collaborators.head.userId}/remove")

        result.status mustBe BAD_REQUEST

      }

      "respond with 403 if auth fails" in new Setup {
        primeAuthServiceFail()
        val orgId = organisationWithTeamMembers.organisationId

        val result = callGetEndpoint(s"$url/organisations/${orgId.value.toString}/team-members/${organisationWithTeamMembers.collaborators.head.userId}/remove")


        result.status mustBe FORBIDDEN

      }
    }

    "POST /organisations/:organisationId/remove-team-member/:userId" should {

      "respond with 200 when remove team member action is successful" in new Setup {
        primeAuthServiceSuccess()

        getOrganisationByOrganisationIdReturnsResponseWithBody(organisationId, 200, Json.toJson(organisationWithTeamMembers).toString())

        removeTeamMemberReturnsResponse(organisationId, organisationWithTeamMembers.collaborators.head.email, "bob", OK, organisationWithTeamMembers.copy(collaborators = List.empty))

        val result = callPostEndpoint(s"$url/organisations/${organisationId.value.toString}/team-members/${organisationWithTeamMembers.collaborators.head.userId}/remove",
         List(CONTENT_TYPE -> "application/x-www-form-urlencoded"), s"email=${collaborator.email};confirm=Yes;")

        result.status mustBe SEE_OTHER

      }

      "respond with 500 when remove team member fails" in new Setup {
        primeAuthServiceSuccess()

        getOrganisationByOrganisationIdReturnsResponseWithBody(organisationWithTeamMembers.organisationId, 200, Json.toJson(organisationWithTeamMembers).toString())

        removeTeamMemberReturnsError(organisationWithTeamMembers.organisationId, organisationWithTeamMembers.collaborators.head.email, "bob", INTERNAL_SERVER_ERROR)

        val result = callPostEndpoint(s"$url/organisations/${organisationId.value.toString}/team-members/${organisationWithTeamMembers.collaborators.head.userId}/remove",
          List(CONTENT_TYPE -> "application/x-www-form-urlencoded"), s"email=${collaborator.email};confirm=Yes;")

        result.status mustBe INTERNAL_SERVER_ERROR

      }


      "respond with 400 if invalid OrganisationId" in new Setup {
        primeAuthServiceSuccess()

        val result = callPostEndpoint(s"$url/organisations/someInvalidOrg/team-members/${organisationWithTeamMembers.collaborators.head.userId}/remove",
          List(CONTENT_TYPE -> "application/x-www-form-urlencoded"), s"email=${collaborator.email};confirm=Yes;")

        result.status mustBe BAD_REQUEST

      }

      "respond with 403 if auth fails" in new Setup {
        primeAuthServiceFail()

        val result = callPostEndpoint(s"$url/organisations/${organisationId.value.toString}/team-members/${organisationWithTeamMembers.collaborators.head.userId}/remove",
          List(CONTENT_TYPE -> "application/x-www-form-urlencoded"), s"email=${collaborator.email};confirm=Yes;")

        result.status mustBe FORBIDDEN

      }
    }
  }
}
