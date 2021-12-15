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

package uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models

import uk.gov.hmrc.http.SessionKeys
import java.{util => ju}

object GatekeeperRole extends Enumeration {
  type GatekeeperRole = Value
  val USER,SUPERUSER,ADMIN = Value
}

object GatekeeperSessionKeys {
  val LoggedInUser = "LoggedInUser"
  val AuthToken = SessionKeys.authToken
}

case class OrganisationId(value: ju.UUID) extends AnyVal

case class VendorId(value: Long) extends AnyVal

case class Organisation(organisationId: OrganisationId, vendorId: VendorId, name: String)