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

package uk.gov.hmrc.iossregistration.models.etmp

import play.api.libs.json.{Json, Reads, Writes}
import uk.gov.hmrc.iossregistration.models.etmp.EtmpSchemeDetails.displayReads

case class EtmpDisplayRegistration(
                                    tradingNames: Seq[EtmpTradingName],
                                    schemeDetails: EtmpSchemeDetails,
                                    bankDetails: EtmpBankDetails,
                                    exclusions: Seq[EtmpExclusion],
                                    adminUse: EtmpAdminUse
                                  )

object EtmpDisplayRegistration {

  implicit private val etmpDetailsReads: Reads[EtmpSchemeDetails] = displayReads

  implicit val reads: Reads[EtmpDisplayRegistration] = Json.reads[EtmpDisplayRegistration]
  implicit val writes: Writes[EtmpDisplayRegistration] = Json.writes[EtmpDisplayRegistration]
}