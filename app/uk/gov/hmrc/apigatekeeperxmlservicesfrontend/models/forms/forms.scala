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

package uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models.forms

import play.api.data.Form
import play.api.data._
import play.api.data.Forms._

object Forms {

  private def emailValidator() = {
    text
      .verifying("teammember.remove.email.error.required", _.nonEmpty)
  }


  case class AddOrganisation(organisationname: Option[String] = Some(""))

  object AddOrganisation {

    val form = Form(
      mapping( //organisation-name-input
        "organisationname" -> optional(nonEmptyText).verifying("organisationname.error.required", x => x.isDefined)
      )(AddOrganisation.apply)(AddOrganisation.unapply)
    )

  }

  case class AddTeamMemberForm(emailAddress: Option[String] = Some(""))

  object AddTeamMemberForm {

    val form = Form(
      mapping(
        "emailAddress" -> optional(nonEmptyText).verifying("teammember.add.email.error.required", x => x.isDefined)
      )(AddTeamMemberForm.apply)(AddTeamMemberForm.unapply)
    )

  }

  final case class RemoveTeamMemberConfirmationForm(email: String, confirm: Option[String] = Some(""))

  object RemoveTeamMemberConfirmationForm {
    val form: Form[RemoveTeamMemberConfirmationForm] = Form(
      mapping(
        "email" -> emailValidator,
        "confirm" -> optional(text).verifying("team.member.error.confirmation.no.choice.field", _.isDefined)
      )(RemoveTeamMemberConfirmationForm.apply)(RemoveTeamMemberConfirmationForm.unapply)
    )
  }
}