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

package uk.gov.hmrc.apigatekeeperxmlservicesfrontend.utils

import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.{Organisation, OrganisationId, VendorId}

import java.util.UUID


trait OrganisationTestData {
    val vendorId = 9001L
    val org1 = Organisation(organisationId = OrganisationId(UUID.randomUUID()), vendorId = VendorId(1), name = "Org 1")
    val org2 = org1.copy(vendorId = VendorId(2), name = "Org 2")
    val org3 = org1.copy(vendorId = VendorId(3), name = "Org 3")

    val organisations = List(org1, org2, org3)

}
