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

package uk.gov.hmrc.apigatekeeperxmlservicesfrontend.config

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import com.google.inject.ImplementedBy

@ImplementedBy(classOf[AppConfigImpl])
trait AppConfig {

  def welshLanguageSupportEnabled: Boolean

  def title: String

  def appName: String
  def authBaseUrl: String
  def strideLoginUrl: String

  def gatekeeperSuccessUrl: String

  def superUserRole: String
  def userRole: String
  def adminRole: String
  def superUsers: Seq[String]

}

@Singleton
class AppConfigImpl @Inject() (config: Configuration) extends ServicesConfig(config) with AppConfig {

  def title = "HMRC API Gatekeeper"

  val welshLanguageSupportEnabled: Boolean = config.getOptional[Boolean]("features.welsh-language-support").getOrElse(false)

  val appName = getString("appName")
  val authBaseUrl: String = baseUrl("auth")
  val strideLoginUrl = s"${baseUrl("stride-auth-frontend")}/stride/sign-in"

  val gatekeeperSuccessUrl = getString("api-gatekeeper-frontend-success-url")

  val superUserRole = getString("roles.super-user")
  val userRole = getString("roles.user")
  val adminRole = getString("roles.admin")

    def superUsers: Seq[String] = {
    config
      .getOptional[Seq[String]]("superUsers")
      .getOrElse(Seq.empty)
  }

}
