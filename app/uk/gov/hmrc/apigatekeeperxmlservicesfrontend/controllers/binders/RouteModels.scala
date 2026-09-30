/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.apigatekeeperxmlservicesfrontend.controllers.binders

import java.util.UUID

import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.OrganisationId

object RouteModels {

  case class SimpleOrganisationId(value: UUID) extends AnyVal {
    override def toString: String = value.toString
  }

  given Conversion[SimpleOrganisationId, OrganisationId] = oRt => OrganisationId(oRt.value)

  given Conversion[OrganisationId, SimpleOrganisationId] = oId => SimpleOrganisationId(oId.value)
}
