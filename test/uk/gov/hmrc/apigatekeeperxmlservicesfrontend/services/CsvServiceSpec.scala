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
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.OrganisationWithNameAndVendorId
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.{ParsedUser, VendorId}
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.connectors.XmlServicesConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.utils.AsyncHmrcSpec
class CsvServiceSpec extends AsyncHmrcSpec with BeforeAndAfterEach {

  trait Setup {
       implicit val hc = HeaderCarrier()
      val mockXmlServiceConnector = mock[XmlServicesConnector]
     val csvService = new CsvService(mockXmlServiceConnector)
  }

  "mapToOrganisationFromCsv" should {

    "return a list of organisations with only one organisation" in new Setup{
      val organisationName = "Organistaion One"
      val vendorId = 1
      val csvTestData = s"""VENDORID,NAME
    $vendorId,$organisationName"""

      val result: Seq[OrganisationWithNameAndVendorId] = csvService.mapToOrganisationFromCsv(csvTestData)
      val actualOrganisation = result.head

      actualOrganisation.name.value shouldBe organisationName
      actualOrganisation.vendorId.value shouldBe vendorId
    }


    "throw an exception when payload has valid headers but no data" in new Setup{
      val csvTestData = s"""VENDORID,NAME"""

      val exception = intercept[RuntimeException] { csvService.mapToOrganisationFromCsv(csvTestData) }
      exception.getMessage() shouldBe "No record(s) found"
    }

    "throw an exception when payload is missing vendorId value" in new Setup{
      val csvTestData = s"""VENDORID,NAME
    testOrganisationName"""

      val exception = intercept[RuntimeException] { csvService.mapToOrganisationFromCsv(csvTestData) }
      exception.getMessage() shouldBe "Expected 2 values on row 1"
    }

    "throw an exception when payload has empty vendorId value" in new Setup{
      val csvTestData = s"""VENDORID,NAME
    ,testOrganisationName"""

      val exception = intercept[RuntimeException] { csvService.mapToOrganisationFromCsv(csvTestData) }
      exception.getMessage() shouldBe "VENDORID cannot be empty on row 1"
    }

    "throw an exception when payload has decimal vendorId value" in new Setup{
      val csvTestData = s"""VENDORID,NAME
    11.01,testOrganisationName"""

      val exception = intercept[NumberFormatException] { csvService.mapToOrganisationFromCsv(csvTestData) }
      exception.getMessage() shouldBe "Invalid VENDORID value on row 1"
    }

    "throw an exception when payload is missing organisation name value" in new Setup{
      val csvTestData = s"""VENDORID,NAME
    1011"""

      val exception = intercept[RuntimeException] { csvService.mapToOrganisationFromCsv(csvTestData) }
      exception.getMessage() shouldBe "Expected 2 values on row 1"
    }
    

    "throw an exception when payload has empty organisation name value" in new Setup{
      val csvTestData = s"""VENDORID,NAME
    1011,"""

      val exception = intercept[RuntimeException] { csvService.mapToOrganisationFromCsv(csvTestData) }
      exception.getMessage() shouldBe "NAME cannot be empty on row 1"
    }

    "throw an exception when payload contains invalid vendorId header(s)" in new Setup{

      val csvTestData = """INVALID_FIRST_HEADER,NAME
    IGNORED,IGNORED"""

      val exception = intercept[IllegalArgumentException] { csvService.mapToOrganisationFromCsv(csvTestData) }
      exception.getMessage() shouldBe "Invalid Header - expected VENDORID"
    }

    "throw an exception when payload contains invalid name header(s)" in new Setup{

      val csvTestData = """VENDORID,INVALIDHEADER
    IGNORED,IGNORED"""

      val exception = intercept[IllegalArgumentException] { csvService.mapToOrganisationFromCsv(csvTestData) }
      exception.getMessage() shouldBe "Invalid Header - expected NAME"
    }

    "throw an exception when payload is missing a column header" in new Setup{

      val csvTestData = """VENDORID"""

      val exception = intercept[RuntimeException] { csvService.mapToOrganisationFromCsv(csvTestData) }
      exception.getMessage() shouldBe "Invalid Header - expected NAME"
    }

    "throw an exception when no payload" in new Setup{

      val csvTestData = """"""

      val exception = intercept[RuntimeException] { csvService.mapToOrganisationFromCsv(csvTestData) }
      exception.getMessage() shouldBe "Invalid Header - expected VENDORID"
    }
  }

  "mapToUsersFromCsv" should {
      val email = "a@b.com"
      val firstName = "Joe"
      val lastName = "Bloggs"
      val servicesString = "service1|service2"
      val vendorIds = "20001|20002"
      val vendorIdsList = List(VendorId(20001), VendorId(20002))

    "return a list of users" in new Setup{

      val csvTestData = s"""EMAIL,FIRSTNAME,LASTNAME,SERVICES,VENDORIDS
    $email, $firstName, $lastName, $servicesString, $vendorIds"""

      val result: Seq[ParsedUser] = await(csvService.mapToUsersFromCsv(csvTestData))
      val actualUser = result.head

      actualUser.email shouldBe email
      actualUser.firstName shouldBe firstName
      actualUser.lastName shouldBe lastName
      actualUser.services shouldBe servicesString
      actualUser.vendorIds shouldBe vendorIdsList
    }

    "throw an exception when invaild vendorIds separator" in new Setup{
      val csvTestData = s"""EMAIL,FIRSTNAME,LASTNAME,SERVICES,VENDORIDS
    $email, $firstName, $lastName, $servicesString, 1000;2000"""

      val exception = intercept[RuntimeException] { csvService.mapToUsersFromCsv(csvTestData) }
      exception.getMessage() shouldBe "Invalid VENDORIDS value on row 1"
    }

    "throw an exception when invaild vendorId value" in new Setup{
      val csvTestData = s"""EMAIL,FIRSTNAME,LASTNAME,SERVICES,VENDORIDS
    $email, $firstName, $lastName, $servicesString, notAnumber"""

      val exception = intercept[RuntimeException] { csvService.mapToUsersFromCsv(csvTestData) }
      exception.getMessage() shouldBe "Invalid VENDORIDS value on row 1"
    }

    "throw an exception when payload has empty vendorIds value" in new Setup{
      val csvTestData = s"""EMAIL,FIRSTNAME,LASTNAME,SERVICES,VENDORIDS
    $email, $firstName, $lastName, $servicesString,"""

      val exception = intercept[RuntimeException] { csvService.mapToUsersFromCsv(csvTestData) }
      exception.getMessage() shouldBe "VENDORIDS cannot be empty on row 1"
    }

    "throw an exception when payload is missing vendorIds value" in new Setup{
      val csvTestData = s"""EMAIL,FIRSTNAME,LASTNAME,SERVICES,VENDORIDS
    $email, $firstName, $lastName, $servicesString"""

      val exception = intercept[RuntimeException] { csvService.mapToUsersFromCsv(csvTestData) }
      exception.getMessage() shouldBe "Expected 5 values on row 1"
    }
  }
}
