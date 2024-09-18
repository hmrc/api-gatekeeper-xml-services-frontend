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

import org.apache.commons.validator.routines.EmailValidator

import play.api.data.{Forms, Mapping}

object FormUtils {
  lazy val DefaultMaxLength: Int = 320
  val emailValidator             = EmailValidator.getInstance

  def emailValidator(maxLength: Int = DefaultMaxLength): Mapping[String] = {
    Forms.text
      .verifying("emailAddress.error.not.valid.field", email => emailValidator.isValid(email) || email.isEmpty)
      .verifying("emailAddress.error.maxLength.field", email => email.length <= maxLength)
      .verifying("emailAddress.error.required.field", email => email.nonEmpty)
  }

}
