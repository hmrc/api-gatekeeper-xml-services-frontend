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

case class CreateOrganisationRequest(organisationName: String)

case class OrganisationWithNameAndVendorId(name: OrganisationName, vendorId: VendorId)
case class BulkFindAndCreateOrUpdateRequest(organisations: Seq[OrganisationWithNameAndVendorId])

sealed trait CreateOrganisationResult
case class CreateOrganisationSuccessResult(organisation: Organisation) extends CreateOrganisationResult
case class CreateOrganisationFailureResult(error: Throwable) extends CreateOrganisationResult

sealed trait AddCollaboratorResult
case class AddCollaboratorSuccessResult(organisation: Organisation) extends AddCollaboratorResult
case class AddCollaboratorFailureResult(error: Throwable) extends AddCollaboratorResult

sealed trait RemoveCollaboratorResult
case class RemoveCollaboratorSuccessResult(organisation: Organisation) extends RemoveCollaboratorResult
case class RemoveCollaboratorFailureResult(error: Throwable) extends RemoveCollaboratorResult

sealed trait ParseOrganisationCsvFailureResult
case class InvalidNumberOfColumnsInCsvResult(error: Throwable) extends ParseOrganisationCsvFailureResult
case class OrganisationCsvParseFailurerResult(error: Throwable) extends ParseOrganisationCsvFailureResult