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

package uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models

import play.api.libs.json.Json
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.connectors.{AddCollaboratorRequest, RemoveCollaboratorRequest}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.thirdpartydeveloper.UserId

object JsonFormatters {
  implicit val formatOrganisationId = Json.valueFormat[OrganisationId]
  implicit val formatOrganisationName = Json.valueFormat[OrganisationName]
  implicit val formatVendorId = Json.valueFormat[VendorId]
  implicit val formatServiceName = Json.valueFormat[ServiceName]
  implicit val formatCollaborator = Json.format[Collaborator]
  implicit val formatOrganisation = Json.format[Organisation]

  implicit val formatXmlApi = Json.format[XmlApi]

  implicit val formatCreateOrganisationRequest = Json.format[CreateOrganisationRequest]
  implicit val formatUpdateOrganisationDetailsRequest = Json.format[UpdateOrganisationDetailsRequest]
  implicit val formatAddCollaboratorRequest = Json.format[AddCollaboratorRequest]
  implicit val formatRemoveCollaboratorRequest = Json.format[RemoveCollaboratorRequest]

  implicit val formatOrganisationWithNameAndVendorId = Json.format[OrganisationWithNameAndVendorId]
  implicit val formatBulkUploadOrganisationsRequest = Json.format[BulkUploadOrganisationsRequest]
  implicit val formatParsedUserRequest = Json.format[ParsedUser]
  implicit val formatBulkAddUsersRequest = Json.format[BulkAddUsersRequest]
  implicit val formatUserId = Json.valueFormat[UserId]
  implicit val formatOrganisationUser = Json.format[OrganisationUser]


}
