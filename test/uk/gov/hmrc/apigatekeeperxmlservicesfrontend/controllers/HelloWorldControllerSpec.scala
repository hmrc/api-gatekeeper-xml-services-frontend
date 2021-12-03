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
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.HelloWorldPage
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.views.html.ForbiddenView
import scala.concurrent.ExecutionContext.Implicits.global

class HelloWorldControllerSpec extends ControllerBaseSpec {

  trait Setup extends ControllerSetupBase {
    val fakeRequest = FakeRequest("GET", "/hello-world")
    private lazy val forbiddenView = app.injector.instanceOf[ForbiddenView]
    private lazy val helloWorldPage = app.injector.instanceOf[HelloWorldPage]

    val controller = new HelloWorldController(
      mcc,
      helloWorldPage,
      mockAuthConnector,
      forbiddenView
    )
  }

  "GET /" should {
    "return 200" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()
      val result = controller.helloWorld(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return forbidden view" in new Setup {
      givenAUnsuccessfulLogin()
      val result = controller.helloWorld(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
    }

    "return HTML" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()
      val result = controller.helloWorld(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }
  }
}
