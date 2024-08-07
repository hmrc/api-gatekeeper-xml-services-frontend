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

package uk.gov.hmrc.apigatekeeperxmlservicesfrontend.connectors

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

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

import play.api.Logging
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.connectors.ThirdPartyDeveloperConnector.Config
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.thirdpartydeveloper.JsonFormatters._
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.thirdpartydeveloper.UserResponse

@Singleton
class ThirdPartyDeveloperConnector @Inject() (http: HttpClientV2, config: Config)(implicit val ec: ExecutionContext) extends Logging {

  def getByEmails(emails: List[String])(implicit hc: HeaderCarrier): Future[Either[Throwable, List[UserResponse]]] = {
    http.post(url"${config.thirdPartyDeveloperUrl}/developers/get-by-emails")
      .withBody(Json.toJson(emails))
      .execute[List[UserResponse]]
      .map(x => Right(x))
      .recover {
        case NonFatal(e) =>
          logger.error(e.getMessage)
          Left(e)
      }
  }

}

object ThirdPartyDeveloperConnector {
  case class Config(thirdPartyDeveloperUrl: String)
}
