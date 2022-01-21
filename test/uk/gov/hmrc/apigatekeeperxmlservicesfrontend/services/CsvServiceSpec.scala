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

package uk.gov.hmrc.apigatekeeperxmlservicesfrontend.services

import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.Organisation
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.utils.HmrcSpec
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.OrganisationWithNameAndVendorId

class CsvServiceSpec extends HmrcSpec with BeforeAndAfterEach {

  val csvService = new CsvService()

  "mapToOrganisationFromCsv" should {

    "return a list of organisations with only one organisation" in {
      val organisationName = "Organistaion One"
      val vendorId = 1
      val csvTestData = s"""VENDORID,NAME
    $vendorId,$organisationName"""

      val result: Seq[OrganisationWithNameAndVendorId] = csvService.mapToOrganisationFromCsv(csvTestData)
      val actualOrganisation = result.head

      actualOrganisation.name shouldBe organisationName
      actualOrganisation.vendorId.value shouldBe vendorId
    }

    "throw an exception when payload is missing vendorId value" in {
      val csvTestData = s"""VENDORID,NAME
    testOrganisationName"""

      val exception = intercept[RuntimeException] { csvService.mapToOrganisationFromCsv(csvTestData) }
      exception.getMessage() shouldBe "Expected 2 values on row 1"
    }

    "throw an exception when payload has empty vendorId value" in {
      val csvTestData = s"""VENDORID,NAME
    ,testOrganisationName"""

      val exception = intercept[NumberFormatException] { csvService.mapToOrganisationFromCsv(csvTestData) }
      exception.getMessage() shouldBe "For input string: \"\""
    }

    "throw an exception when payload has decimal vendorId value" in {
      val csvTestData = s"""VENDORID,NAME
    11.01,testOrganisationName"""

      val exception = intercept[NumberFormatException] { csvService.mapToOrganisationFromCsv(csvTestData) }
      exception.getMessage() shouldBe "For input string: \"11.01\""
    }

    "throw an exception when payload is missing organisation name value" in {
      val csvTestData = s"""VENDORID,NAME
    1011"""

      val exception = intercept[RuntimeException] { csvService.mapToOrganisationFromCsv(csvTestData) }
      exception.getMessage() shouldBe "Expected 2 values on row 1"
    }

    "throw an exception when payload has empty organisation name value" in {
      val csvTestData = s"""VENDORID,NAME
    1011,"""

      val exception = intercept[RuntimeException] { csvService.mapToOrganisationFromCsv(csvTestData) }
      exception.getMessage() shouldBe "Organisation name cannot be empty"
    }

    "throw an exception when payload contains invalid column header(s)" in {

      val csvTestData = """INVALID_FIRST_HEADER,INVALID_SECOND_HEADER
    SomeInvalidData,SomeInvalidData"""

      val exception = intercept[IllegalArgumentException] { csvService.mapToOrganisationFromCsv(csvTestData) }
      exception.getMessage() shouldBe "Mapping for VENDORID not found, expected one of [INVALID_FIRST_HEADER, INVALID_SECOND_HEADER]"
    }

    "throw an exception when payload is missing a column header" in {

      val csvTestData = """VENDORID"""

      val exception = intercept[RuntimeException] { csvService.mapToOrganisationFromCsv(csvTestData) }
      exception.getMessage() shouldBe "No record(s) found"
    }
  }
}
