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

import play.api.libs.json.{Format, Json, OFormat}

import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.connectors.{AddCollaboratorRequest, RemoveCollaboratorRequest}
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.thirdpartydeveloper.UserId

object JsonFormatters {
  implicit val formatOrganisationId: Format[OrganisationId]     = Json.valueFormat[OrganisationId]
  implicit val formatOrganisationName: Format[OrganisationName] = Json.valueFormat[OrganisationName]
  implicit val formatVendorId: Format[VendorId]                 = Json.valueFormat[VendorId]
  implicit val formatServiceName: Format[ServiceName]           = Json.valueFormat[ServiceName]
  implicit val formatCollaborator: OFormat[Collaborator]        = Json.format[Collaborator]
  implicit val formatOrganisation: OFormat[Organisation]        = Json.format[Organisation]

  implicit val formatXmlApi: OFormat[XmlApi] = Json.format[XmlApi]

  implicit val formatCreateOrganisationRequest: OFormat[CreateOrganisationRequest]               = Json.format[CreateOrganisationRequest]
  implicit val formatUpdateOrganisationDetailsRequest: OFormat[UpdateOrganisationDetailsRequest] = Json.format[UpdateOrganisationDetailsRequest]
  implicit val formatAddCollaboratorRequest: OFormat[AddCollaboratorRequest]                     = Json.format[AddCollaboratorRequest]
  implicit val formatRemoveCollaboratorRequest: OFormat[RemoveCollaboratorRequest]               = Json.format[RemoveCollaboratorRequest]

  implicit val formatOrganisationWithNameAndVendorId: OFormat[OrganisationWithNameAndVendorId] = Json.format[OrganisationWithNameAndVendorId]
  implicit val formatBulkUploadOrganisationsRequest: OFormat[BulkUploadOrganisationsRequest]   = Json.format[BulkUploadOrganisationsRequest]
  implicit val formatParsedUserRequest: OFormat[ParsedUser]                                    = Json.format[ParsedUser]
  implicit val formatBulkAddUsersRequest: OFormat[BulkAddUsersRequest]                         = Json.format[BulkAddUsersRequest]
  implicit val formatUserId: Format[UserId]                                                    = Json.valueFormat[UserId]
  implicit val formatOrganisationUser: OFormat[OrganisationUser]                               = Json.format[OrganisationUser]

}
