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

package mocks

import com.github.tomakehurst.wiremock.client.WireMock._

trait XmlServicesStub {

  val baseUrl = "/api-platform-xml-services"

  def findOrganisationByVendorIdUrl(vendorId: String) = s"$baseUrl/organisations?vendorId=$vendorId"

  def findOrganisationByVendorIdReturnsError(vendorId: String, status: Int) = {

    stubFor(get(urlEqualTo(findOrganisationByVendorIdUrl(vendorId)))
      .willReturn(
        aResponse()
          .withStatus(status)
          .withHeader("Content-Type", "application/json")
      ))
  }

  def findOrganisationByVendorIdReturnsResponseWithBody(vendorId: String, status: Int, responseBody: String) = {

    stubFor(get(urlEqualTo(findOrganisationByVendorIdUrl(vendorId)))
      .willReturn(
        aResponse()
          .withStatus(status)
          .withHeader("Content-Type", "application/json")
          .withBody(responseBody)
      ))
  }
}
