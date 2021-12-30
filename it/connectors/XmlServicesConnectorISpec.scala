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

package connectors

import mocks.XmlServicesStub
import org.scalatest.BeforeAndAfterEach
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.test.Helpers._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.connectors.XmlServicesConnector
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.JsonFormatters._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.support.ServerBaseISpec
import uk.gov.hmrc.http.{HeaderCarrier, Upstream4xxResponse, UpstreamErrorResponse}

import java.{util => ju}

class XmlServicesConnectorISpec extends ServerBaseISpec with BeforeAndAfterEach {

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

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

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val wsClient: WSClient = app.injector.instanceOf[WSClient]

  trait Setup extends XmlServicesStub {
    val objInTest: XmlServicesConnector = app.injector.instanceOf[XmlServicesConnector]
    val vendorId: VendorId = VendorId(12)

    val organisation = Organisation(organisationId = OrganisationId(ju.UUID.randomUUID()), vendorId = vendorId, name = "Org name")
    val organisation2 = Organisation(organisationId = OrganisationId(ju.UUID.randomUUID()), vendorId = vendorId, name = "Org name2")
  }

  "findOrganisationsByParams" should {

    "return Left when back end returns Bad Request" in new Setup {
      findOrganisationByParamsReturnsError(Some(vendorId.value.toString), None, BAD_REQUEST)
      val result = await(objInTest.findOrganisationsByParams(Some(vendorId), None))

      result match {
        case Left(e: Upstream4xxResponse) => e.statusCode mustBe BAD_REQUEST
        case _                            => fail
      }

    }

    "return Right(List(Organisation)) when backend called with vendor id and organisations returned " in new Setup {
      findOrganisationByParamsReturnsResponseWithBody(Some(vendorId.value.toString), None, OK, Json.toJson(List(organisation)).toString)
      val result = await(objInTest.findOrganisationsByParams(Some(vendorId), None))

      result match {
        case Right(org) => org mustBe List(organisation)
        case _          => fail
      }
    }

    "return Right(List(Organisation)) when backend called with organisationName and organisations returned " in new Setup {
      findOrganisationByParamsReturnsResponseWithBody(None, Some("I am a org name"), OK, Json.toJson(List(organisation)).toString)
      val result = await(objInTest.findOrganisationsByParams(None, Some("I am a org name")))

      result match {
        case Right(org) => org mustBe List(organisation)
        case _          => fail
      }
    }

    "return Right(List(Organisation)) when no query parameters provided and the back end returns a List of Organisations" in new Setup {
      findOrganisationByParamsReturnsResponseWithBody(None, None, OK, Json.toJson(List(organisation, organisation2)).toString)
      val result = await(objInTest.findOrganisationsByParams(None, None))

      result match {
        case Right(org) => org must contain allOf(organisation, organisation2)
        case _          => fail
      }
    }
  }

  "getOrganisationByOrganisationId" should {
    "return right with some organisation when back end call successful" in new Setup {
      val orgId = organisation.organisationId
      getOrganisationByOrganisationIdReturnsResponseWithBody(orgId, 200, Json.toJson(organisation).toString())
      val result = await(objInTest.getOrganisationByOrganisationId(orgId))
      result match {
        case Right(org) => org mustBe organisation
        case _          => fail()
      }
    }

    "return Right None with when back end returns 404" in new Setup {
      val orgId = organisation.organisationId
      getOrganisationByOrganisationIdReturnsError(orgId, 404)
      val result = await(objInTest.getOrganisationByOrganisationId(orgId))
      result match {
        case Left(e: UpstreamErrorResponse) => e.statusCode mustBe 404
        case _                              => fail()
      }
    }

    "return Left with when back end returns 404" in new Setup {
      val orgId = organisation.organisationId
      getOrganisationByOrganisationIdReturnsError(orgId, 500)
      val result = await(objInTest.getOrganisationByOrganisationId(orgId))
      result match {
        case Left(e: UpstreamErrorResponse) => e.statusCode mustBe INTERNAL_SERVER_ERROR
        case _                              => fail()
      }
    }

  }
  "addOrganisation" should {

    "return CreateOrganisationSuccessResult when back end returns Organisation" in new Setup {
      addOrganisationReturnsResponse(organisation.name, OK, organisation)
      val result = await(objInTest.addOrganisation(organisation.name))

      result mustBe CreateOrganisationSuccessResult(organisation)

    }

    "return CreateOrganisationFailureResult when back end returns error" in new Setup {
      addOrganisationReturnsError(organisation.name, INTERNAL_SERVER_ERROR)
      val result = await(objInTest.addOrganisation(organisation.name))

      result match {
        case CreateOrganisationFailureResult(UpstreamErrorResponse(_, INTERNAL_SERVER_ERROR, _, _)) => succeed
        case _                                                                                      => fail()
      }
    }
  }
}
