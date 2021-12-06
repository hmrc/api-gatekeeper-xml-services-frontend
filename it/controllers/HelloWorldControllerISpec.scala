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

package uk.gov.hmrc.apigatekeeperxmlservicesfrontend.controllers

import org.scalatest.BeforeAndAfterEach
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.{WSClient, WSResponse}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.support.ServerBaseISpec
import play.api.test.Helpers.{NOT_FOUND, OK, FORBIDDEN}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.support.AuthServiceStub
import org.jsoup.Jsoup

class HelloWorldControllerISpec extends ServerBaseISpec with BeforeAndAfterEach with AuthServiceStub {


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

  val wsClient: WSClient = app.injector.instanceOf[WSClient]


  def callGetEndpoint(url: String, headers: List[(String, String)]): WSResponse =
    wsClient
      .url(url)
      .withHttpHeaders(headers: _*)
      .withFollowRedirects(false)
      .get()
      .futureValue


  "HelloWorldController" when {

    "GET /" should {
      "respond with 200 and render hello world page" in {
        primeAuthServiceSuccess
        val result = callGetEndpoint(s"$url/hello-world", List.empty)
        result.status mustBe OK
        val content = Jsoup.parse(result.body)
        content.getElementById("page-heading").text() mustBe "api-gatekeeper-xml-services-frontend"

      }

      "respond with 403 and render the Forbidden view" in {
        primeAuthServiceFail
        val result = callGetEndpoint(s"$url/hello-world", List.empty)
        result.status mustBe FORBIDDEN
        val content = Jsoup.parse(result.body)
        content.getElementById("page-heading").text() mustBe "You do not have permission to access Gatekeeper"

      }

      "respond with 404 and render errorTemplate Correctly when path invalid" in {

        val result = callGetEndpoint(s"$url/unknown-path", List.empty)
        result.status mustBe NOT_FOUND

      }

    }
  }
}
