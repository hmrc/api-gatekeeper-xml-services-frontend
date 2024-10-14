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

import java.util.UUID
import java.{util => ju}

import org.scalatest.BeforeAndAfterEach

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.test.Helpers._
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ApiCategory
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.JsonFormatters._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.thirdpartydeveloper.UserId
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.stubs.XmlServicesStub
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.support.ServerBaseISpec

class XmlServicesConnectorISpec extends ServerBaseISpec with BeforeAndAfterEach {

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  protected override def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.port"                      -> wireMockPort,
        "metrics.enabled"                                      -> true,
        "auditing.enabled"                                     -> false,
        "auditing.consumer.baseUri.host"                       -> wireMockHost,
        "auditing.consumer.baseUri.port"                       -> wireMockPort,
        "microservice.services.api-platform-xml-services.host" -> wireMockHost,
        "microservice.services.api-platform-xml-services.port" -> wireMockPort
      )

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val wsClient: WSClient = app.injector.instanceOf[WSClient]

  trait Setup extends XmlServicesStub {
    val objInTest: XmlServicesConnector = app.injector.instanceOf[XmlServicesConnector]
    val vendorId: VendorId              = VendorId(12)
    val emailAddress                    = "email@email.com"
    val organisation                    = Organisation(organisationId = OrganisationId(ju.UUID.randomUUID()), vendorId = vendorId, name = "Org name")
    val organisation2                   = Organisation(organisationId = OrganisationId(ju.UUID.randomUUID()), vendorId = VendorId(13), name = "Org name2")

    val collaboratorList = List(Collaborator("userId", "collaborator1@mail.com"))

    val organisationId = OrganisationId(ju.UUID.randomUUID())

    val organisationWithTeamMembers = Organisation(
      organisationId = OrganisationId(ju.UUID.randomUUID()),
      vendorId = VendorId(14),
      name = "Org name3",
      collaborators = collaboratorList
    )

    val organisationsWithNameAndVendorIds = Seq(
      OrganisationWithNameAndVendorId(OrganisationName("Test Organsation One"), VendorId(101)),
      OrganisationWithNameAndVendorId(OrganisationName("Test Organsation Two"), VendorId(102))
    )

    val services       = List(ServiceName("service1"), ServiceName("service2"))
    val email          = "a@b.com"
    val firstName      = "Joe"
    val lastName       = "Bloggs"
    val servicesString = "service1;service2;"
    val vendorIds      = "20001|20002"
    val vendorIdsList  = List(VendorId(20001), VendorId(20002))

    val parsedUser = ParsedUser(email, firstName, lastName, services, vendorIdsList)

    val users = Seq(
      ParsedUser(email, firstName, lastName, services, vendorIdsList),
      ParsedUser("b@b.com", firstName + 1, lastName + 1, services, vendorIdsList)
    )

    val xmlApi1 = XmlApi(
      name = "xml api 1",
      serviceName = ServiceName("vat-and-ec-sales-list"),
      context = "/government/collections/vat-and-ec-sales-list-online-support-for-software-developers",
      description = "description",
      categories = Some(Seq(ApiCategory.CUSTOMS))
    )

    val xmlApi2 = XmlApi(
      name = "xml api 3",
      serviceName = ServiceName("customs-import"),
      context = "/government/collections/customs-import",
      description = "description",
      categories = Some(Seq(ApiCategory.CUSTOMS))
    )

    val organisationUsers = List(OrganisationUser(organisationId, Some(UserId(UUID.randomUUID())), emailAddress, firstName, lastName, List(xmlApi1, xmlApi2)))

  }

  "findOrganisationsByParams" should {

    "return Left when back end returns Bad Request" in new Setup {
      findOrganisationByParamsReturnsError(Some(vendorId.value.toString), None, BAD_REQUEST)
      val result = await(objInTest.findOrganisationsByParams(Some(vendorId), None))

      result match {
        case Left(UpstreamErrorResponse(_, BAD_REQUEST, _, _)) => succeed
        case _                                                 => fail()
      }

    }

    "return Right(List(Organisation)) when backend called with vendor id and organisations returned " in new Setup {
      findOrganisationByParamsReturnsResponseWithBody(Some(vendorId.value.toString), None, OK, Json.toJson(List(organisation)).toString)
      val result = await(objInTest.findOrganisationsByParams(Some(vendorId), None))

      result match {
        case Right(org) => org mustBe List(organisation)
        case _          => fail()
      }
    }

    "return Right(List(Organisation)) when backend called with organisationName and organisations returned " in new Setup {
      findOrganisationByParamsReturnsResponseWithBody(None, Some("I am a org name"), OK, Json.toJson(List(organisation)).toString)
      val result = await(objInTest.findOrganisationsByParams(None, Some("I am a org name")))

      result match {
        case Right(org) => org mustBe List(organisation)
        case _          => fail()
      }
    }

    "return Right(List(Organisation)) when no query parameters provided and the back end returns a List of Organisations" in new Setup {
      findOrganisationByParamsReturnsResponseWithBody(None, None, OK, Json.toJson(List(organisation, organisation2)).toString)
      val result = await(objInTest.findOrganisationsByParams(None, None))

      result match {
        case Right(org) => org must contain.allOf(organisation, organisation2)
        case _          => fail()
      }
    }
  }

  "getOrganisationByOrganisationId" should {
    "return right with some organisation when back end call successful" in new Setup {
      val orgId  = organisation.organisationId
      getOrganisationByOrganisationIdReturnsResponseWithBody(orgId, 200, Json.toJson(organisation).toString())
      val result = await(objInTest.getOrganisationByOrganisationId(orgId))
      result match {
        case Right(org) => org mustBe organisation
        case _          => fail()
      }
    }

    "return Right None with when back end returns 404" in new Setup {
      val orgId  = organisation.organisationId
      getOrganisationByOrganisationIdReturnsError(orgId, 404)
      val result = await(objInTest.getOrganisationByOrganisationId(orgId))
      result match {
        case Left(e: UpstreamErrorResponse) => e.statusCode mustBe 404
        case _                              => fail()
      }
    }

    "return Left with when back end returns 404" in new Setup {
      val orgId  = organisation.organisationId
      getOrganisationByOrganisationIdReturnsError(orgId, 500)
      val result = await(objInTest.getOrganisationByOrganisationId(orgId))
      result match {
        case Left(e: UpstreamErrorResponse) => e.statusCode mustBe INTERNAL_SERVER_ERROR
        case _                              => fail()
      }
    }

  }

  "addOrganisation" should {

    "return CreateOrganisationSuccess when back end returns Organisation" in new Setup {

      addOrganisationReturnsResponse(organisation.name, emailAddress, firstName, lastName, OK, organisation)
      val result = await(objInTest.addOrganisation(organisation.name, emailAddress, firstName, lastName))

      result mustBe CreateOrganisationSuccess(organisation)

    }

    "return CreateOrganisationFailure when back end returns error" in new Setup {
      addOrganisationReturnsError(organisation.name, emailAddress, firstName, lastName, INTERNAL_SERVER_ERROR)
      val result = await(objInTest.addOrganisation(organisation.name, emailAddress, firstName, lastName))

      result match {
        case CreateOrganisationFailure(UpstreamErrorResponse(_, INTERNAL_SERVER_ERROR, _, _)) => succeed
        case _                                                                                => fail()
      }
    }
  }

  "updateOrganisationDetails" should {

    "return UpdateOrganisationSuccess when back end returns Organisation" in new Setup {
      updateOrganisationDetailsReturnsResponse(organisation.name, organisation.organisationId, OK, organisation)
      val result = await(objInTest.updateOrganisationDetails(organisation.organisationId, organisation.name))

      result mustBe UpdateOrganisationDetailsSuccess(organisation)

    }

    "return UpdateOrganisationDetailsFailure when back end returns error" in new Setup {
      updateOrganisationDetailsReturnsError(organisation.name, organisation.organisationId, INTERNAL_SERVER_ERROR)
      val result = await(objInTest.updateOrganisationDetails(organisation.organisationId, organisation.name))

      result match {
        case UpdateOrganisationDetailsFailure(UpstreamErrorResponse(_, INTERNAL_SERVER_ERROR, _, _)) => succeed
        case _                                                                                       => fail()
      }
    }
  }

  "removeOrganisation" should {
    "return true when backend returns NO CONTENT" in new Setup {
      removeOrganisationStub(organisation.organisationId, NO_CONTENT)

      val result = await(objInTest.removeOrganisation(organisation.organisationId))

      result mustBe true

    }

    "return false when backend returns NOT FOUND" in new Setup {
      removeOrganisationStub(organisation.organisationId, NOT_FOUND)

      val result = await(objInTest.removeOrganisation(organisation.organisationId))

      result mustBe false

    }
  }

  "AddTeamMember" should {
    "return AddCollaboratorSuccessfulResult when add collaborator call is successful" in new Setup {

      val updatedCollaboratorList = collaboratorList ++ List(Collaborator(emailAddress, "someUserId"))

      addTeamMemberReturnsResponse(
        organisationWithTeamMembers.organisationId,
        emailAddress,
        firstName,
        lastName,
        OK,
        organisationWithTeamMembers.copy(collaborators = updatedCollaboratorList)
      )

      val result = await(objInTest.addTeamMember(organisationWithTeamMembers.organisationId, emailAddress, firstName, lastName))

      result mustBe AddCollaboratorSuccess(organisationWithTeamMembers.copy(collaborators = updatedCollaboratorList))
    }

    "return AddCollaboratorFailure when add collaborator call is successful" in new Setup {

      val expectedErrorMessage = s"POST of 'http://localhost:$wireMockPort/api-platform-xml-services/organisations/" +
        s"${organisationWithTeamMembers.organisationId.value.toString}/add-collaborator' returned 404. Response body: ''"

      addTeamMemberReturnsError(
        organisationWithTeamMembers.organisationId,
        emailAddress,
        firstName,
        lastName,
        NOT_FOUND
      )

      val result = await(objInTest.addTeamMember(organisationWithTeamMembers.organisationId, emailAddress, firstName, lastName))

      result match {
        case AddCollaboratorFailure(UpstreamErrorResponse(message, NOT_FOUND, _, _)) => message mustBe expectedErrorMessage
        case _                                                                       => fail()
      }
    }

  }

  "removeTeamMember" should {
    "return RemoveCollaboratorSuccess when remove collaborator call is successful " in new Setup {
      removeTeamMemberReturnsResponse(
        organisationWithTeamMembers.organisationId,
        organisationWithTeamMembers.collaborators.head.email,
        "somegatekeeperId",
        OK,
        organisationWithTeamMembers.copy(collaborators = List.empty)
      )
      val result = await(objInTest.removeTeamMember(organisationWithTeamMembers.organisationId, organisationWithTeamMembers.collaborators.head.email, "somegatekeeperId"))

      result mustBe RemoveCollaboratorSuccess(organisationWithTeamMembers.copy(collaborators = List.empty))

    }

    "return RemoveCollaboratorFailure when remove collaborator call is unsuccessful " in new Setup {
      removeTeamMemberReturnsResponse(
        organisationWithTeamMembers.organisationId,
        organisationWithTeamMembers.collaborators.head.email,
        "somegatekeeperId",
        NOT_FOUND,
        organisationWithTeamMembers.copy(collaborators = List.empty)
      )
      val result = await(objInTest.removeTeamMember(organisationWithTeamMembers.organisationId, organisationWithTeamMembers.collaborators.head.email, "somegatekeeperId"))

      result match {
        case RemoveCollaboratorFailure(e) => e.getMessage
        case _                            => fail()
      }

    }

    "return RemoveCollaboratorFailure when remove collaborator call is unsuccessful (500 Error)" in new Setup {
      removeTeamMemberReturnsResponse(
        organisationWithTeamMembers.organisationId,
        organisationWithTeamMembers.collaborators.head.email,
        "somegatekeeperId",
        INTERNAL_SERVER_ERROR,
        organisationWithTeamMembers.copy(collaborators = List.empty)
      )
      val result = await(objInTest.removeTeamMember(organisationWithTeamMembers.organisationId, organisationWithTeamMembers.collaborators.head.email, "somegatekeeperId"))

      result match {
        case RemoveCollaboratorFailure(e) => e.getMessage
        case _                            => fail()
      }
    }

    "getAllApis" should {

      val xmlApi = XmlApi(
        name = "xml api 1",
        serviceName = ServiceName("vat-and-ec-sales-list"),
        context = "/government/collections/vat-and-ec-sales-list-online-support-for-software-developers",
        description = "description",
        categories = Some(Seq(ApiCategory.CUSTOMS))
      )

      "return Right when getAllApis call is successful" in new Setup {

        getAllApisResponseWithBody(OK, Json.toJson(Seq(xmlApi)).toString)

        val result = await(objInTest.getAllApis)

        result mustBe Right((Seq(xmlApi)))
      }

      "return Left when getAllApis call is unsuccessful" in new Setup {

        getAllApisReturnsError(INTERNAL_SERVER_ERROR)

        val result = await(objInTest.getAllApis)

        result match {
          case Left(UpstreamErrorResponse(_, INTERNAL_SERVER_ERROR, _, _)) => succeed
          case _                                                           => fail()
        }
      }
    }

    "getOrganisationUsersByOrganisationId" should {
      "return Right with list of users when connector receives 200 and list of users " in new Setup {

        val organisationUser = OrganisationUser(organisationId, Some(UserId(UUID.randomUUID())), email, firstName, lastName, List(xmlApi1, xmlApi2))
        getOrganisationUsersByOrganisationIdReturnsResponse(organisationId, OK, List(organisationUser))
        val result           = await(objInTest.getOrganisationUsersByOrganisationId(organisationId))

        result match {
          case Right(users: List[OrganisationUser]) => users mustBe List(organisationUser)
          case _                                    => fail()
        }
      }
    }

    "return Left and error when connector receives 500 " in new Setup {

      getOrganisationUsersByOrganisationIdReturnsError(organisationId, INTERNAL_SERVER_ERROR)
      val result = await(objInTest.getOrganisationUsersByOrganisationId(organisationId))

      result match {
        case Left(_: UpstreamErrorResponse) => succeed
        case _                              => fail()
      }
    }

  }
}
