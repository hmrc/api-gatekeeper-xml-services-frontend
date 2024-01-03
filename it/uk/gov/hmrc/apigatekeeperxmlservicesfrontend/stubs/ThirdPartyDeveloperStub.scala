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

package uk.gov.hmrc.apigatekeeperxmlservicesfrontend.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping

import play.api.http.Status._
import play.api.libs.json.Json

trait ThirdPartyDeveloperStub {

  def stubGetByEmailsReturnsResponse(emails: List[String], responseAsString: String): StubMapping = {
    val requestAsString = Json.toJson(emails).toString()
    stubFor(
      post(urlEqualTo("/developers/get-by-emails"))
        .withRequestBody(equalTo(requestAsString))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(responseAsString)
            .withHeader("Content-Type", "application/json")
        )
    )
  }

  def stubGetByEmailsReturnsNoResponse(emails: List[String], status: Int): StubMapping = {
    val requestAsString = Json.toJson(emails).toString()

    stubFor(
      post(urlEqualTo("/developers/get-by-emails"))
        .withRequestBody(equalTo(requestAsString))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withHeader("Content-Type", "application/json")
        )
    )
  }

}
