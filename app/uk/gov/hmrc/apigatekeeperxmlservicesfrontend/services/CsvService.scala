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

@Singleton
class CsvService @Inject() () extends Logging {

  def mapToOrganisationFromCsv(csvData: String): Seq[OrganisationWithNameAndVendorId] = {
    val reader = new InputStreamReader(IOUtils.toInputStream(csvData, StandardCharsets.UTF_8))

    val records = org.apache.commons.csv.CSVFormat.EXCEL
      .withFirstRecordAsHeader()
      .parse(reader).getRecords.asScala.toList

    if (records.size == 0) throw new RuntimeException("No record(s) found")

    records.map(parseOrganisation)
  }

  def parseOrganisation(record: CSVRecord): OrganisationWithNameAndVendorId = {
    val expectedValues = 2
    if (record.size() < expectedValues) throw new RuntimeException(s"Expected $expectedValues values on row ${record.getRecordNumber}")

    def parseString(s: String): String = {
      Option(s) match {
        case Some(s: String) if s.trim.nonEmpty => s.trim()
        case _                             => throw new RuntimeException(s"Organisation name cannot be empty on row ${record.getRecordNumber}")
      }
    }

    def parseLong(s: String): Long = {
      Option(s) match {
        case Some(s: String) if s.trim.nonEmpty => 
          try{
             s.trim().toLong
          }catch{
            case e: NumberFormatException =>  throw new NumberFormatException(s"Invalid VendorId value on row ${record.getRecordNumber}")
          }
        case _                             => throw new RuntimeException(s"VendorId cannot be empty on row ${record.getRecordNumber}")
      }
    }

    OrganisationWithNameAndVendorId(
      vendorId = VendorId(parseLong(record.get("VENDORID"))),
      name = OrganisationName(parseString(record.get("NAME")))
    )
  }
}
