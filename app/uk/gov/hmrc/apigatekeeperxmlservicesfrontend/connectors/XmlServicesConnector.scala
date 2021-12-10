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

package uk.gov.hmrc.apigatekeeperxmlservicesfrontend.connectors

import javax.inject.{Inject, Singleton}

import uk.gov.hmrc.http.HttpClient
import scala.concurrent.ExecutionContext
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.VendorId
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.Organisation
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.JsonFormatters._
import scala.concurrent.Future
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.connectors.XmlServicesConnector._
import uk.gov.hmrc.http.HeaderCarrier

@Singleton
class XmlServicesConnector @Inject()(val http: HttpClient,  val config: Config)(implicit ec: ExecutionContext) {

   val serviceBaseUrl: String = config.serviceBaseUrl

  def findOrganisationByVendorId(vendorId: VendorId)(implicit hc: HeaderCarrier): Future[Option[Organisation]] = {

    http.GET[Option[Organisation]](url = s"${serviceBaseUrl}/organisations" , queryParams = Seq(("vendorId" -> vendorId.value.toString)))

  }
  
} 

object XmlServicesConnector{
    case class Config(
      serviceBaseUrl: String
  )
}