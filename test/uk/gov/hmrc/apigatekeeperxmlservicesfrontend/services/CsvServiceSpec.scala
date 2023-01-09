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

package uk.gov.hmrc.apigatekeeperxmlservicesfrontend.services

import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models._
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.connectors.XmlServicesConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.utils.AsyncHmrcSpec
import scala.concurrent.Future

class CsvServiceSpec extends AsyncHmrcSpec with BeforeAndAfterEach {

  trait Setup {
    implicit val hc = HeaderCarrier()
    val mockXmlServiceConnector = mock[XmlServicesConnector]
    val csvService = new CsvService(mockXmlServiceConnector)

  }

  "mapToOrganisationFromCsv" should {

    "return a list of organisations with only one organisation" in new Setup {
      val organisationName = "Organistaion One"
      val vendorId = 1
      val csvTestData = s"""VENDORID,NAME
    $vendorId,$organisationName"""

      val result: Seq[OrganisationWithNameAndVendorId] = csvService.mapToOrganisationFromCsv(csvTestData)
      val actualOrganisation = result.head

      actualOrganisation.name.value shouldBe organisationName
      actualOrganisation.vendorId.value shouldBe vendorId
    }

    "throw an exception when payload has valid headers but no data" in new Setup {
      val csvTestData = s"""VENDORID,NAME"""

      val exception = intercept[RuntimeException] { csvService.mapToOrganisationFromCsv(csvTestData) }
      exception.getMessage() shouldBe "No record(s) found"
    }

    "throw an exception when payload is missing vendorId value" in new Setup {
      val csvTestData = s"""VENDORID,NAME
    testOrganisationName"""

      val exception = intercept[RuntimeException] { csvService.mapToOrganisationFromCsv(csvTestData) }
      exception.getMessage() shouldBe "Expected 2 values on row 1"
    }

    "throw an exception when payload has empty vendorId value" in new Setup {
      val csvTestData = s"""VENDORID,NAME
    ,testOrganisationName"""

      val exception = intercept[RuntimeException] { csvService.mapToOrganisationFromCsv(csvTestData) }
      exception.getMessage() shouldBe "VENDORID cannot be empty on row 1"
    }

    "throw an exception when payload has decimal vendorId value" in new Setup {
      val csvTestData = s"""VENDORID,NAME
    11.01,testOrganisationName"""

      val exception = intercept[NumberFormatException] { csvService.mapToOrganisationFromCsv(csvTestData) }
      exception.getMessage() shouldBe "Invalid VENDORID value on row 1"
    }

    "throw an exception when payload is missing organisation name value" in new Setup {
      val csvTestData = s"""VENDORID,NAME
    1011"""

      val exception = intercept[RuntimeException] { csvService.mapToOrganisationFromCsv(csvTestData) }
      exception.getMessage() shouldBe "Expected 2 values on row 1"
    }

    "throw an exception when payload has empty organisation name value" in new Setup {
      val csvTestData = s"""VENDORID,NAME
    1011,"""

      val exception = intercept[RuntimeException] { csvService.mapToOrganisationFromCsv(csvTestData) }
      exception.getMessage() shouldBe "NAME cannot be empty on row 1"
    }

    "throw an exception when payload contains invalid vendorId header(s)" in new Setup {

      val csvTestData = """INVALID_FIRST_HEADER,NAME
    IGNORED,IGNORED"""

      val exception = intercept[IllegalArgumentException] { csvService.mapToOrganisationFromCsv(csvTestData) }
      exception.getMessage() shouldBe "Invalid Header - expected VENDORID"
    }

    "throw an exception when payload contains invalid name header(s)" in new Setup {

      val csvTestData = """VENDORID,INVALIDHEADER
    IGNORED,IGNORED"""

      val exception = intercept[IllegalArgumentException] { csvService.mapToOrganisationFromCsv(csvTestData) }
      exception.getMessage() shouldBe "Invalid Header - expected NAME"
    }

    "throw an exception when payload is missing a column header" in new Setup {

      val csvTestData = """VENDORID"""

      val exception = intercept[RuntimeException] { csvService.mapToOrganisationFromCsv(csvTestData) }
      exception.getMessage() shouldBe "Invalid Header - expected NAME"
    }

    "throw an exception when no payload" in new Setup {

      val csvTestData = """"""

      val exception = intercept[RuntimeException] { csvService.mapToOrganisationFromCsv(csvTestData) }
      exception.getMessage() shouldBe "Invalid Header - expected VENDORID"
    }
  }

  "mapToUsersFromCsv" should {
    val email = "a@b.com"
    val firstName = "Joe"
    val lastName = "Bloggs"
    val servicesString = "vat-and-ec-sales-list|stamp-taxes-online"
    val vendorIds = "20001|20002"
    val vendorIdsList = List(VendorId(20001), VendorId(20002))
    val serviceName1 = ServiceName("vat-and-ec-sales-list")
    val serviceName2 = ServiceName("stamp-taxes-online")

    val xmlApi1 = XmlApi(name = "xml api",
      serviceName = serviceName1,
      context = "context",
      description = "description",
      categories  = Some(Seq(ApiCategory.CUSTOMS)))
    val xmlApi2 = xmlApi1.copy(serviceName = serviceName2)
    val xmlApis = Seq(xmlApi1, xmlApi2)
    

    "return a list of users" in new Setup {
      when(mockXmlServiceConnector.getAllApis).thenReturn(Future.successful(Right(xmlApis)))

      val csvTestData = s"""EMAIL,FIRSTNAME,LASTNAME,SERVICES,VENDORIDS,DUPLICATENAMES
    $email, $firstName, $lastName, $servicesString, $vendorIds,"""

      val result: Seq[ParsedUser] = await(csvService.mapToUsersFromCsv(csvTestData))
      val actualUser = result.head

      actualUser.email shouldBe email
      actualUser.firstName shouldBe firstName
      actualUser.lastName shouldBe lastName
      actualUser.services shouldBe List(serviceName1, serviceName2)
      actualUser.vendorIds shouldBe vendorIdsList
    }

    "return a list of users when empty (no white space) services specified" in new Setup {
      when(mockXmlServiceConnector.getAllApis).thenReturn(Future.successful(Right(xmlApis)))

      val csvTestData = s"""EMAIL,FIRSTNAME,LASTNAME,SERVICES,VENDORIDS,DUPLICATENAMES
    $email, $firstName, $lastName,, $vendorIds,"""

      val result: Seq[ParsedUser] = await(csvService.mapToUsersFromCsv(csvTestData))
      val actualUser = result.head

      actualUser.email shouldBe email
      actualUser.firstName shouldBe firstName
      actualUser.lastName shouldBe lastName
      actualUser.services shouldBe List.empty
      actualUser.vendorIds shouldBe vendorIdsList
    }

    "return a list of users when blank (with white space) services specified" in new Setup {
      when(mockXmlServiceConnector.getAllApis).thenReturn(Future.successful(Right(xmlApis)))

      val csvTestData = s"""EMAIL,FIRSTNAME,LASTNAME,SERVICES,VENDORIDS,DUPLICATENAMES
    $email, $firstName, $lastName, , $vendorIds, somevalue """

      val result: Seq[ParsedUser] = await(csvService.mapToUsersFromCsv(csvTestData))
      val actualUser = result.head

      actualUser.email shouldBe email
      actualUser.firstName shouldBe firstName
      actualUser.lastName shouldBe lastName
      actualUser.services shouldBe List.empty
      actualUser.vendorIds shouldBe vendorIdsList
    }

    "throw an exception when xml connector returns an empty list of xml apis" in new Setup {
      when(mockXmlServiceConnector.getAllApis).thenReturn(Future.successful(Right(Nil)))

      val csvTestData = s"""EMAIL,FIRSTNAME,LASTNAME,SERVICES,VENDORIDS,DUPLICATENAMES
    $email, $firstName, $lastName, $servicesString, 1000;2000"""

      val exception = intercept[RuntimeException] { await(csvService.mapToUsersFromCsv(csvTestData)) }
      exception.getMessage() shouldBe "No XML APIs found"
    }


    "throw an exception when xml connector returns error" in new Setup {
      when(mockXmlServiceConnector.getAllApis).thenReturn(Future.successful(Left(new RuntimeException("error"))))

      val csvTestData = s"""EMAIL,FIRSTNAME,LASTNAME,SERVICES,VENDORIDS,DUPLICATENAMES
    $email, $firstName, $lastName, $servicesString, 1000;2000"""

      val exception = intercept[RuntimeException] { await(csvService.mapToUsersFromCsv(csvTestData)) }
      exception.getMessage() shouldBe "Error getting XML APIs from backend - error"
    }

    "throw an exception when invaild services separator" in new Setup {
      when(mockXmlServiceConnector.getAllApis).thenReturn(Future.successful(Right(xmlApis)))

      val csvTestData = s"""EMAIL,FIRSTNAME,LASTNAME,SERVICES,VENDORIDS,DUPLICATENAMES
    $email, $firstName, $lastName, service1;service2, 1000|2000"""

      val exception = intercept[RuntimeException] { await(csvService.mapToUsersFromCsv(csvTestData)) }
      exception.getMessage() shouldBe "Invalid service [service1;service2] on row 1"
    }

    "throw an exception when invaild serviceName" in new Setup {
      when(mockXmlServiceConnector.getAllApis).thenReturn(Future.successful(Right(xmlApis)))

      val csvTestData = s"""EMAIL,FIRSTNAME,LASTNAME,SERVICES,VENDORIDS,DUPLICATENAMES
    $email, $firstName, $lastName, service1|vat-and-ec-sales-list, 1000|2000"""

      val exception = intercept[RuntimeException] { await(csvService.mapToUsersFromCsv(csvTestData)) }
      exception.getMessage() shouldBe "Invalid service [service1] on row 1"
    }

    "throw an exception when invaild vendorIds separator" in new Setup {
      when(mockXmlServiceConnector.getAllApis).thenReturn(Future.successful(Right(xmlApis)))

      val csvTestData = s"""EMAIL,FIRSTNAME,LASTNAME,SERVICES,VENDORIDS,DUPLICATENAMES
    $email, $firstName, $lastName, $servicesString, 1000;2000"""

      val exception = intercept[RuntimeException] { await(csvService.mapToUsersFromCsv(csvTestData)) }
      exception.getMessage() shouldBe "Invalid VENDORIDS value on row 1"
    }

    "throw an exception when invaild vendorId value" in new Setup {
      when(mockXmlServiceConnector.getAllApis).thenReturn(Future.successful(Right(xmlApis)))

      val csvTestData = s"""EMAIL,FIRSTNAME,LASTNAME,SERVICES,VENDORIDS,DUPLICATENAMES
    $email, $firstName, $lastName, $servicesString, notAnumber"""

      val exception = intercept[RuntimeException] { await(csvService.mapToUsersFromCsv(csvTestData)) }
      exception.getMessage() shouldBe "Invalid VENDORIDS value on row 1"
    }

    "throw an exception when payload has empty vendorIds value" in new Setup {
      when(mockXmlServiceConnector.getAllApis).thenReturn(Future.successful(Right(xmlApis)))

      val csvTestData = s"""EMAIL,FIRSTNAME,LASTNAME,SERVICES,VENDORIDS,DUPLICATENAMES
    $email, $firstName, $lastName, $servicesString,"""

      val exception = intercept[RuntimeException] { await(csvService.mapToUsersFromCsv(csvTestData)) }
      exception.getMessage() shouldBe "VENDORIDS cannot be empty on row 1"
    }

    "throw an exception when payload is missing vendorIds value" in new Setup {
      when(mockXmlServiceConnector.getAllApis).thenReturn(Future.successful(Right(xmlApis)))

      val csvTestData = s"""EMAIL,FIRSTNAME,LASTNAME,SERVICES,VENDORIDS,DUPLICATENAMES
    $email, $firstName, $lastName, $servicesString"""

      val exception = intercept[RuntimeException] { await(csvService.mapToUsersFromCsv(csvTestData)) }
      exception.getMessage() shouldBe "Expected at least 5 values on row 1"
    }
  }
}
