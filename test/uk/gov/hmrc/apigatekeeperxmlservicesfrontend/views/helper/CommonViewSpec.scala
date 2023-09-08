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

package uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.helper

import org.jsoup.nodes.Document

import java.util.Locale
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.{Lang, MessagesImpl, MessagesProvider}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.config.AppConfig
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.utils.AsyncHmrcSpec
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.LoggedInUser
import uk.gov.hmrc.apiplatform.modules.gkauth.services.{LdapAuthorisationServiceMockModule, StrideAuthorisationServiceMockModule}
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.LoggedInRequest
import play.api.i18n.MessagesApi
import play.api.mvc.MessagesRequest
import play.api.test.FakeRequest

trait CommonViewSpec extends AsyncHmrcSpec with GuiceOneAppPerSuite {
  val mcc                                         = app.injector.instanceOf[MessagesControllerComponents]
  val messagesApi                                 = mcc.messagesApi
  implicit val messagesProvider: MessagesProvider = MessagesImpl(Lang(Locale.ENGLISH), messagesApi)
  implicit val appConfig: AppConfig               = mock[AppConfig]

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(("metrics.jvm", false))
      .build()

  def hasBackLink(document: Document): Boolean = {
    !document.getElementsByAttributeValue("data-module", "hmrc-back-link").isEmpty
  }

  trait BaseSetup {
    def loggedInRequest: LoggedInRequest[_]
    lazy val loggedInUser: LoggedInUser = LoggedInUser.fromRequest(loggedInRequest)
  }

  trait LdapAuth {
    self: BaseSetup =>

    val loggedInRequest = new LoggedInRequest(
      name = Some(LdapAuthorisationServiceMockModule.LdapUserName),
      role = GatekeeperRoles.READ_ONLY,
      request = new MessagesRequest(FakeRequest("GET", "/"), mock[MessagesApi])
    )
  }

  trait StrideAuth {
    self: BaseSetup =>

    val loggedInRequest = new LoggedInRequest(
      name = Some(StrideAuthorisationServiceMockModule.StrideUserName),
      role = GatekeeperRoles.USER,
      request = new MessagesRequest(FakeRequest("GET", "/"), mock[MessagesApi])
    )
  }
}
