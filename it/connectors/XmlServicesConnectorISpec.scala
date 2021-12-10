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
      findOrganisationByVendorIdReturnsError(vendorId.value.toString, BAD_REQUEST)
      val result = await(objInTest.findOrganisationByVendorId(vendorId))

      result match {
        case Left(e: BadRequestException) => e.responseCode mustBe BAD_REQUEST
        case _ => fail
      }

    }

    "return Right(None) when back end returns Not Found" in new Setup {
      findOrganisationByVendorIdReturnsError(vendorId.value.toString, NOT_FOUND)
      val result = await(objInTest.findOrganisationByVendorId(vendorId))

      result match {
        case Right(organisation) => organisation mustBe None
        case _ => fail
      }
    }

    "return Right(Organisation) when back end returns Organisation" in new Setup {
      findOrganisationByVendorIdReturnsResponseWithBody(vendorId.value.toString, OK, Json.toJson(organisation).toString)
      val result = await(objInTest.findOrganisationByVendorId(vendorId))

      result match {
        case Right(org) => org mustBe Some(organisation)
        case _ => fail
      }
    }
  }

}
