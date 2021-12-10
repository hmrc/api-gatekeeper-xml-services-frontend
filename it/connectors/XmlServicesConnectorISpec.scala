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

import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.support.ServerBaseISpec
import org.scalatest.BeforeAndAfterEach
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.HeaderCarrier
import play.api.libs.ws.WSClient
import mocks.XmlServicesStub
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.connectors.XmlServicesConnector
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.VendorId
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.JsonFormatters._
import play.api.test.Helpers._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.Organisation
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.http.BadRequestException
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.OrganisationId
import java.{util => ju}
import play.api.libs.json.Json
import uk.gov.hmrc.http.NotFoundException

class XmlServicesConnectorISpec extends ServerBaseISpec with BeforeAndAfterEach {

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
  }

  "findOrganisationByVendorId" should {

    "return Left when back end returns Bad Request" in new Setup {
      findOrganisationByVendorIdReturnsError(Some(vendorId.value.toString), BAD_REQUEST)
      val result = await(objInTest.findOrganisationsByParams(Some(vendorId)))

      result match {
        case Left(e: BadRequestException) => e.responseCode mustBe BAD_REQUEST
        case _ => fail
      }

    }

    "return Left when back end returns Not Found" in new Setup {
      findOrganisationByVendorIdReturnsError(Some(vendorId.value.toString), NOT_FOUND)
      val result = await(objInTest.findOrganisationsByParams(Some(vendorId)))

      result match {
        case Left(e: NotFoundException) => e.responseCode mustBe NOT_FOUND
        case _ => fail
      }
    }

    "return Left when back end returns Internal Server Error" in new Setup {
      findOrganisationByVendorIdReturnsError(Some(vendorId.value.toString), INTERNAL_SERVER_ERROR)
      val result = await(objInTest.findOrganisationsByParams(Some(vendorId)))

      result match {
        case Left(e: UpstreamErrorResponse) => e.statusCode mustBe INTERNAL_SERVER_ERROR
        case _ => fail
      }
    }

    "return Right(List(Organisation)) when back end returns a List of Organisations" in new Setup {
      findOrganisationByVendorIdReturnsResponseWithBody(Some(vendorId.value.toString), OK, Json.toJson(List(organisation)).toString)
      val result = await(objInTest.findOrganisationsByParams(Some(vendorId)))

      result match {
        case Right(org) => org mustBe List(organisation)
        case _ => fail
      }
    }

    "return Right(List(Organisation)) when no vendorId is passed in and the back end returns a List of Organisations" in new Setup {
      findOrganisationByVendorIdReturnsResponseWithBody(None, OK, Json.toJson(List(organisation)).toString)
      val result = await(objInTest.findOrganisationsByParams(None))

      result match {
        case Right(org) => org mustBe List(organisation)
        case _ => fail
      }
    }
  }

}
