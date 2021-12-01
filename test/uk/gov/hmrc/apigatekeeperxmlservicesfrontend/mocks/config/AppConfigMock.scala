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

package uk.gov.hmrc.apigatekeeperxmlservicesfrontend.mocks.config

import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.mocks.TestRoles._
import org.mockito.MockitoSugar
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.config.AppConfig

trait AppConfigMock extends MockitoSugar {
  implicit val mockConfig = mock[AppConfig]

  when(mockConfig.title).thenReturn("Unit Test Title")

  when(mockConfig.userRole).thenReturn(userRole)
  when(mockConfig.adminRole).thenReturn(adminRole)
  when(mockConfig.superUserRole).thenReturn(superUserRole)
  when(mockConfig.superUsers).thenReturn(Seq("superUserName"))

  when(mockConfig.gatekeeperSuccessUrl).thenReturn("http://mock-gatekeeper-frontend/api-gatekeeper/applications")
  when(mockConfig.strideLoginUrl).thenReturn("https://loginUri")
  when(mockConfig.appName).thenReturn("Gatekeeper app name")

}
