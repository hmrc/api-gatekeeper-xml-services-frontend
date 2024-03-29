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

package uk.gov.hmrc.apigatekeeperxmlservicesfrontend.config

import play.api.inject.Module
import play.api.{Configuration, Environment}

import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.connectors.{ThirdPartyDeveloperConnector, XmlServicesConnector}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.controllers.HandleForbiddenWithView
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.actions.ForbiddenHandler

class ConfigurationModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration) = {
    Seq(
      bind[XmlServicesConnector.Config].toProvider[XmlServicesConnectorProvider],
      bind[ThirdPartyDeveloperConnector.Config].toProvider[ThirdPartyDeveloperConnectorProvider],
      bind[ForbiddenHandler].to(classOf[HandleForbiddenWithView])
    )
  }
}
