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

package uk.gov.hmrc.apigatekeeperxmlservicesfrontend.utils

import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.{Organisation, OrganisationId, VendorId}

import java.util.UUID
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.Collaborator


trait OrganisationTestData {
    val vendorId = 9001L
    val organisationId1 = OrganisationId(UUID.randomUUID())
    val organisationId2 = OrganisationId(UUID.randomUUID())
    val organisationId3 = OrganisationId(UUID.randomUUID())
    val org1 = Organisation(organisationId1, VendorId(1),  "Org 1")
    val org2 = Organisation(organisationId2, VendorId(2), "Org 2")
    val org3 = Organisation(organisationId3, VendorId(3), "Org 3")

    val emailAddress = "a@b.com"
    val firstName = "bob"
    val lastName = "hope"
    val collaborator1 = Collaborator("userId1", "email1")
    val collaborator2 =  Collaborator("userId2", "email2")
    val organisationWithCollaborators = org1.copy(collaborators = List(collaborator1, collaborator2))

    val organisations = List(org1, org2, org3)

}
