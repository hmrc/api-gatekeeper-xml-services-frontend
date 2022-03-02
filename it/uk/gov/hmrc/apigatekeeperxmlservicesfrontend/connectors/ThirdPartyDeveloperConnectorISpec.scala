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

package uk.gov.hmrc.apigatekeeperxmlservicesfrontend.connectors

import org.scalatest.BeforeAndAfterEach
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.thirdpartydeveloper.JsonFormatters._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.thirdpartydeveloper.{UserId, UserResponse}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.stubs.ThirdPartyDeveloperStub
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.support.ServerBaseISpec
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException, Upstream5xxResponse}

import java.{util => ju}

class ThirdPartyDeveloperConnectorISpec extends ServerBaseISpec with BeforeAndAfterEach with ThirdPartyDeveloperStub {

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  implicit def mat: akka.stream.Materializer = app.injector.instanceOf[akka.stream.Materializer]

  protected override def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.third-party-developer.host" -> wireMockHost,
        "microservice.services.third-party-developer.port" -> wireMockPort,
        "metrics.enabled" -> true,
        "auditing.enabled" -> false,
        "auditing.consumer.baseUri.host" -> wireMockHost,
        "auditing.consumer.baseUri.port" -> wireMockPort
      )

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val email = "foo@bar.com"
    val userId: UserId = UserId(ju.UUID.randomUUID())
    val firstName = "Joe"
    val lastName = "Bloggs"

    val userResponse: UserResponse = UserResponse(
      email = email,
      firstName = firstName,
      lastName = lastName,
      verified = true,
      userId = userId
    )

    val underTest: ThirdPartyDeveloperConnector = app.injector.instanceOf[ThirdPartyDeveloperConnector]

  }

  "getByEmail" should {
    val emails = List("a@b.com", "b@c.com")

    val validResponseString = Json.toJson(List(UserResponse("a@b.com", "firstname", "lastName", verified = true, UserId(ju.UUID.randomUUID)))).toString

    "return Right with users when users are returned" in new Setup {
     stubGetByEmailsReturnsResponse(emails, validResponseString)

      val result: Either[Throwable, List[UserResponse]] = await(underTest.getByEmails(emails))

      result match {
        case Right(_: List[UserResponse]) => succeed
        case _                            => fail
      }
    }

    "return Right when no users are returned" in new Setup {
      stubGetByEmailsReturnsResponse(emails, "[]")

      val result: Either[Throwable, List[UserResponse]] = await(underTest.getByEmails(emails))

      result match {
        case Right(_: List[UserResponse]) => succeed
        case _                            => fail
      }
    }

    "return Left when not found returned" in new Setup {
      stubGetByEmailsReturnsNoResponse(emails, NOT_FOUND)
      val result: Either[Throwable, List[UserResponse]] = await(underTest.getByEmails(emails))

      result match {
        case Left(_: NotFoundException) => succeed
        case _                          => fail
      }
    }

    "return Left when internal server error returned" in new Setup {
       stubGetByEmailsReturnsNoResponse(emails, INTERNAL_SERVER_ERROR)

      val result: Either[Throwable, List[UserResponse]] = await(underTest.getByEmails(emails))

      result match {
        case Left(_: Upstream5xxResponse) => succeed
        case _                            => fail
      }
    }

  }


}
