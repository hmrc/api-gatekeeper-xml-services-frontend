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

package utils

import org.openqa.selenium.{Cookie => SeleniumCookie}
import play.api.Application
import play.api.libs.ws.WSCookie
import play.api.mvc.{Cookie, Session, SessionCookieBaker}
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCrypto

object MockCookies {

  private val sessionCookieName = "mdtp"

  val mockSession = Session(Map(
    SessionKeys.lastRequestTimestamp -> System.currentTimeMillis().toString,
    SessionKeys.authToken -> "mock-bearer-token",
    SessionKeys.sessionId -> "mock-sessionid"
  ))

  def makeSessionCookie(session: Session, app: Application): Cookie = {
    val cookieCrypto = app.injector.instanceOf[SessionCookieCrypto]
    val cookieBaker = app.injector.instanceOf[SessionCookieBaker]
    val sessionCookie = cookieBaker.encodeAsCookie(session)
    val encryptedValue = cookieCrypto.crypto.encrypt(PlainText(sessionCookie.value))
    sessionCookie.copy(value = encryptedValue.value)
  }

  def makeWsCookie(application: Application) = {
    createWsCookie(sessionCookieName, mockSession, application)
  }

  def createWsCookie(cookieName: String, session: Session, application: Application) = {
    val cookie = makeSessionCookie(session, application)
    new WSCookie() {
      override def name: String = cookieName
      override def value: String = cookie.value
      override def domain: Option[String] = cookie.domain
      override def path: Option[String] = Some(cookie.path)
      override def maxAge: Option[Long] = cookie.maxAge.map(_.toLong)
      override def secure: Boolean = cookie.secure
      override def httpOnly: Boolean = cookie.httpOnly
    }
  }

  def makeSeleniumCookie(session: Session, application: Application) = {
    val playCookie = makeSessionCookie(session, application)

    new SeleniumCookie(sessionCookieName, playCookie.value)
  }

}
