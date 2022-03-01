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

case class CreateOrganisationRequest(organisationName: String, email: String, firstName: Option[String], lastName: Option[String])
case class UpdateOrganisationDetailsRequest(organisationName: String)

case class OrganisationWithNameAndVendorId(name: OrganisationName, vendorId: VendorId)
case class BulkUploadOrganisationsRequest(organisations: Seq[OrganisationWithNameAndVendorId])
case class BulkAddUsersRequest(users: Seq[ParsedUser])

case class ParsedUser(email: String, firstName: String, lastName: String, services: List[ServiceName], vendorIds: List[VendorId])

sealed trait CreateOrganisationResult
case class CreateOrganisationSuccess(organisation: Organisation) extends CreateOrganisationResult
case class CreateOrganisationFailure(error: Throwable) extends CreateOrganisationResult

sealed trait UpdateOrganisationDetailsResult
case class UpdateOrganisationDetailsSuccess(organisation: Organisation) extends UpdateOrganisationDetailsResult
case class UpdateOrganisationDetailsFailure(error: Throwable) extends UpdateOrganisationDetailsResult


sealed trait AddCollaboratorResult
case class AddCollaboratorSuccess(organisation: Organisation) extends AddCollaboratorResult
case class AddCollaboratorFailure(error: Throwable) extends AddCollaboratorResult

sealed trait RemoveCollaboratorResult
case class RemoveCollaboratorSuccess(organisation: Organisation) extends RemoveCollaboratorResult
case class RemoveCollaboratorFailure(error: Throwable) extends RemoveCollaboratorResult

sealed trait ParseOrganisationCsvFailureResult
case class InvalidNumberOfColumnsInCsvResult(error: Throwable) extends ParseOrganisationCsvFailureResult
case class OrganisationCsvParseFailurerResult(error: Throwable) extends ParseOrganisationCsvFailureResult