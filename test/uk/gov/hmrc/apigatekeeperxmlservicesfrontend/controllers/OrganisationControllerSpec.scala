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

import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.ForbiddenView
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.organisation.OrganisationSearchView

import scala.concurrent.Future
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.VendorId
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.ErrorTemplate
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.connectors.XmlServicesConnector

class OrganisationControllerSpec extends ControllerBaseSpec {

  trait Setup extends ControllerSetupBase {
    val fakeRequest = FakeRequest("GET", "/organisations")
    val organisationSearchRequest = FakeRequest("GET", "/organisations-search")
    private lazy val forbiddenView = app.injector.instanceOf[ForbiddenView]
    private lazy val errorTemplate = app.injector.instanceOf[ErrorTemplate]
    private lazy val organisationSearchView = app.injector.instanceOf[OrganisationSearchView]
    val mockXmlServiceConnector = mock[XmlServicesConnector]

    val controller = new OrganisationController(
      mcc,
      organisationSearchView,
      mockAuthConnector,
      forbiddenView,
      errorTemplate,
      mockXmlServiceConnector
    )
  }

  "GET /organisations" should {
    "return 200" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()
      val result = controller.organisationsPage(fakeRequest)
      status(result) shouldBe Status.OK
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
      contentAsString(result) contains "Search for XML organisations"
    }

    "return forbidden view" in new Setup {
      givenAUnsuccessfulLogin()
      val result = controller.organisationsPage(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
    }
  }

  "GET /organisations-search" should {
    // to test :-
    // invalid search type
    // vendor id search type empty search text
    /// vendor id  search type non long vendor
    // vendor id search type with valid vendor id

    "return 200 when vendor-id search type and valid vendor id" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()
      val vendorId = 9001L
      when(mockXmlServiceConnector.findOrganisationsByParams(eqTo(Some(VendorId(vendorId)))))
      .thenReturn(Future.successful(Right(List.empty)))

      val result = controller.organisationsSearchAction("vendor-id", Some(vendorId.toString))(organisationSearchRequest)
      status(result) shouldBe Status.OK
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
      contentAsString(result) contains "Search for XML organisations"
    }
  }
}
