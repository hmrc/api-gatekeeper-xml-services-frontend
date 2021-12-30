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

import play.api.test.FakeRequest
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.GatekeeperSessionKeys
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.mocks.connectors.AuthConnectorMock
import uk.gov.hmrc.http.HeaderCarrier

trait ControllerSetupBase extends AuthConnectorMock {
  
  implicit val hc = HeaderCarrier()

  val authToken = GatekeeperSessionKeys.AuthToken -> "some-bearer-token"
  val userToken = GatekeeperSessionKeys.LoggedInUser -> userName
  val superUserToken = GatekeeperSessionKeys.LoggedInUser -> superUserName
  val adminToken = GatekeeperSessionKeys.LoggedInUser -> adminName
  val aLoggedInRequest = FakeRequest().withSession(authToken, userToken)
  val aSuperUserLoggedInRequest = FakeRequest().withSession(authToken, superUserToken)
  val anAdminLoggedInRequest = FakeRequest().withSession(authToken, adminToken)
  val aLoggedOutRequest = FakeRequest().withSession()

}
