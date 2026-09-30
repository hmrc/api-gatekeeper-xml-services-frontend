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

object JsonFormatters {
  given formatOrganisationId: Format[OrganisationId]     = Json.valueFormat[OrganisationId]
  given formatOrganisationName: Format[OrganisationName] = Json.valueFormat[OrganisationName]
  given formatVendorId: Format[VendorId]                 = Json.valueFormat[VendorId]
  given formatServiceName: Format[ServiceName]           = Json.valueFormat[ServiceName]
  given formatCollaborator: OFormat[Collaborator]        = Json.format[Collaborator]
  given formatOrganisation: OFormat[Organisation]        = Json.format[Organisation]

  given formatXmlApi: OFormat[XmlApi] = Json.format[XmlApi]

  given formatCreateOrganisationRequest: OFormat[CreateOrganisationRequest]               = Json.format[CreateOrganisationRequest]
  given formatUpdateOrganisationDetailsRequest: OFormat[UpdateOrganisationDetailsRequest] = Json.format[UpdateOrganisationDetailsRequest]
  given formatAddCollaboratorRequest: OFormat[AddCollaboratorRequest]                     = Json.format[AddCollaboratorRequest]
  given formatRemoveCollaboratorRequest: OFormat[RemoveCollaboratorRequest]               = Json.format[RemoveCollaboratorRequest]

  given formatOrganisationWithNameAndVendorId: OFormat[OrganisationWithNameAndVendorId] = Json.format[OrganisationWithNameAndVendorId]
  given formatBulkUploadOrganisationsRequest: OFormat[BulkUploadOrganisationsRequest]   = Json.format[BulkUploadOrganisationsRequest]
  given formatParsedUserRequest: OFormat[ParsedUser]                                    = Json.format[ParsedUser]
  given formatBulkAddUsersRequest: OFormat[BulkAddUsersRequest]                         = Json.format[BulkAddUsersRequest]
  given formatOrganisationUser: OFormat[OrganisationUser]                               = Json.format[OrganisationUser]

}
