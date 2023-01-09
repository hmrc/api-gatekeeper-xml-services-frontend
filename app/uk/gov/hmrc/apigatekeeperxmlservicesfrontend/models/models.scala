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

package uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models

import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.thirdpartydeveloper.UserId

import java.{util => ju}

case class OrganisationId(value: ju.UUID) extends AnyVal

case class VendorId(value: Long) extends AnyVal

case class Collaborator(userId: String, email: String)

case class OrganisationName(value: String) extends AnyVal

case class Organisation(organisationId: OrganisationId, vendorId: VendorId, name: String, collaborators: List[Collaborator] = List.empty)

case class ServiceName(value: String) extends AnyVal

case class XmlApi(name: String, serviceName: ServiceName, context: String, description: String, categories: Option[Seq[ApiCategory]] = None)

case class OrganisationUser(organisationId: OrganisationId, userId: UserId, email: String, firstName:String, lastName: String, xmlApis: List[XmlApi])
