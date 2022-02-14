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

import org.apache.commons.csv.CSVRecord
import org.apache.commons.io.IOUtils
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models._

import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import javax.inject.{Inject, Singleton}
import scala.collection.JavaConverters._
import play.api.Logging
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.connectors.XmlServicesConnector
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CsvService @Inject()
(xmlServicesConnector: XmlServicesConnector)
(implicit val ec: ExecutionContext) extends Logging {

  object OrganisationHeader extends Enumeration {
    type OrganisationHeader = Value

    val VENDORID, NAME = Value
  }

  object UsersHeader extends Enumeration {
    type UsersHeader = Value

    val EMAIL, FIRSTNAME, LASTNAME, SERVICES, VENDORIDS = Value
  }

  def mapToUsersFromCsv(csvData: String)(implicit hc: HeaderCarrier): Future[Seq[ParsedUser]] = {
    val expectedHeaders = UsersHeader.values.toList.map(_.toString())

    def validateUsersHeaders(headers: List[String]) =
      validateHeaders(headers, expectedHeaders)

    def parseUser(record: CSVRecord, xmlApis: Seq[String]): ParsedUser = {
      val expectedValues = 5
      if (record.size() < expectedValues) throw new RuntimeException(s"Expected $expectedValues values on row ${record.getRecordNumber}")

      ParsedUser(
        email = parseStringFromCsv(record, s"${UsersHeader.EMAIL}"),
        firstName = parseStringFromCsv(record, s"${UsersHeader.FIRSTNAME}"),
        lastName = parseStringFromCsv(record, s"${UsersHeader.LASTNAME}"),
        services = parseServiceNames(record, xmlApis),
        vendorIds = parseVendorIds(record)
      )
    }

    val records = extractCsvRecords(csvData, expectedHeaders, validateUsersHeaders)

    handleGetAllApis.map( allServices =>
      records.map(x => parseUser(x, allServices))
    )
  }

  def mapToOrganisationFromCsv(csvData: String): Seq[OrganisationWithNameAndVendorId] = {
    val expectedHeaders = OrganisationHeader.values.toList.map(_.toString())

    def validateOrganisationHeaders(headers: List[String]) =
      validateHeaders(headers, expectedHeaders)

    def parseOrganisation(record: CSVRecord): OrganisationWithNameAndVendorId = {
      val expectedValues = 2
      if (record.size() < expectedValues) throw new RuntimeException(s"Expected $expectedValues values on row ${record.getRecordNumber}")

      OrganisationWithNameAndVendorId(
        vendorId = VendorId(parseLongFromCsv(record, s"${OrganisationHeader.VENDORID}")),
        name = OrganisationName(parseStringFromCsv(record, s"${OrganisationHeader.NAME}"))
      )

    }

    val records = extractCsvRecords(csvData, expectedHeaders, validateOrganisationHeaders)
    records.map(parseOrganisation)
  }

  private def extractCsvRecords(csvData: String, headerList: List[String], validateHeaderFunc: List[String] => Unit) = {

    val reader = new InputStreamReader(IOUtils.toInputStream(csvData, StandardCharsets.UTF_8))

    val csvFormat = org.apache.commons.csv.CSVFormat.DEFAULT
      .withHeader(headerList: _*)
      .withIgnoreSurroundingSpaces()
      .withFirstRecordAsHeader()
      .parse(reader)

    validateHeaderFunc.apply(csvFormat.getHeaderNames.asScala.toList)

    val records = csvFormat.getRecords.asScala.toList

    validateRecords(records)
    records
  }

  private def parseStringFromCsv(record: CSVRecord, columnKey: String): String = {
    Option(record.get(columnKey)) match {
      case Some(s: String) if s.trim.nonEmpty => s.trim()
      case _ => throw new RuntimeException(s"$columnKey cannot be empty on row ${record.getRecordNumber}")
    }
  }

  private def parseLongFromCsv(record: CSVRecord, columnKey: String): Long = {
    try {
      parseStringFromCsv(record, columnKey).toLong
    } catch {
      case _: NumberFormatException => throw new NumberFormatException(s"Invalid $columnKey value on row ${record.getRecordNumber}")
    }
  }

  private def parseVendorIds(record: CSVRecord): List[VendorId] = {
    val vendorIds = parseStringFromCsv(record, s"${UsersHeader.VENDORIDS}")
    vendorIds.split('|').map(x =>
      try {
        VendorId(x.toLong)
      } catch {
        case _: NumberFormatException => throw new NumberFormatException(s"Invalid ${UsersHeader.VENDORIDS} value on row ${record.getRecordNumber}")
      }).toList
  }

  private def parseServiceNames(record: CSVRecord, allServices: Seq[String]): List[ServiceName] = {
    val serviceNames = parseStringFromCsv(record, s"${UsersHeader.SERVICES}")
    serviceNames.split('|').toList.map(
      service => if (!allServices.contains(service)) {
        throw new RuntimeException(s"Invalid service [$service] on row ${record.getRecordNumber}")
      }
      else {
        ServiceName(service)
      }
    )
  }

  private def handleGetAllApis()(implicit hc: HeaderCarrier): Future[Seq[String]] = {
    xmlServicesConnector.getAllApis.map {
      case Right(Nil) => throw new RuntimeException("No XML APIs found")
      case Right(allApis: Seq[XmlApi]) => allApis.map(x => x.serviceName.value)
      case Left(error: Throwable) => throw new RuntimeException(s"Error getting XML APIs from backend - ${error.getMessage}")
    }
  }

  private def validateHeaders(headers: List[String], expectedHeaders: List[String]): Unit = {
    expectedHeaders.foreach(x => {
      if (!headers.contains(x)) throw new IllegalArgumentException(s"Invalid Header - expected $x")
      else ()
    })
  }

  private def validateRecords(records: List[CSVRecord]): Unit = {
    if (records.isEmpty) throw new RuntimeException("No record(s) found")
  }
}
