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

package uk.gov.hmrc.apigatekeeperxmlservicesfrontend.models

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

sealed abstract class ApiCategory(displayName: String, filter: String) extends EnumEntry

object ApiCategory extends Enum[ApiCategory] with PlayJsonEnum[ApiCategory] {
  val values = findValues

  case object EXAMPLE                      extends ApiCategory("Example", "example")
  case object AGENTS                       extends ApiCategory("Agents", "agents")
  case object BUSINESS_RATES               extends ApiCategory("Business Rates", "business-rates")
  case object CHARITIES                    extends ApiCategory("Charities", "charities")
  case object CONSTRUCTION_INDUSTRY_SCHEME extends ApiCategory("Construction Industry Scheme", "construction-industry-scheme")
  case object CORPORATION_TAX              extends ApiCategory("Corporation Tax", "corporation-tax")
  case object CUSTOMS                      extends ApiCategory("Customs", "customs")
  case object ESTATES                      extends ApiCategory("Estates", "estates")
  case object HELP_TO_SAVE                 extends ApiCategory("Help to Save", "help-to-save")
  case object INCOME_TAX_MTD               extends ApiCategory("Income Tax (Making Tax Digital)", "income-tax")
  case object LIFETIME_ISA                 extends ApiCategory("Lifetime ISA", "lifetime-isa")
  case object MARRIAGE_ALLOWANCE           extends ApiCategory("Marriage Allowance", "marriage-allowance")
  case object NATIONAL_INSURANCE           extends ApiCategory("National Insurance", "national-insurance")
  case object PAYE                         extends ApiCategory("PAYE", "paye")
  case object PENSIONS                     extends ApiCategory("Pensions", "pensions")
  case object PRIVATE_GOVERNMENT           extends ApiCategory("Private Government", "private-government")
  case object RELIEF_AT_SOURCE             extends ApiCategory("Relief at Source", "relief-at-source")
  case object SELF_ASSESSMENT              extends ApiCategory("Self Assessment", "self-assessment")
  case object STAMP_DUTY                   extends ApiCategory("Stamp Duty", "stamp-duty")
  case object TRUSTS                       extends ApiCategory("Trusts", "trusts")
  case object VAT_MTD                      extends ApiCategory("VAT (Making Tax Digital)", "vat")
  case object VAT                          extends ApiCategory("VAT", "vat")
  case object OTHER                        extends ApiCategory("Other", "other")

}
