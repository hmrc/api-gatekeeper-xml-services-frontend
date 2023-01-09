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

package uk.gov.hmrc.apigatekeeperxmlservicesfrontend.controllers

import play.api.mvc.PathBindable
import java.util.UUID
import scala.util.Try
import uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.OrganisationId

package object binders {

  private def organisationIdFromString(text: String): Either[String, OrganisationId] = {
    Try(UUID.fromString(text))
      .toOption
      .toRight(s"Cannot accept $text as OrganisationId")
      .map(OrganisationId(_))
  }

  implicit def organisationIdPathBinder(implicit textBinder: PathBindable[String]): PathBindable[OrganisationId] = new PathBindable[OrganisationId] {

    override def bind(key: String, value: String): Either[String, OrganisationId] = {
      textBinder.bind(key, value).flatMap(organisationIdFromString)
    }

    override def unbind(key: String, organisationId: OrganisationId): String = {
      textBinder.unbind(key, organisationId.value.toString)
    }
  }

}
