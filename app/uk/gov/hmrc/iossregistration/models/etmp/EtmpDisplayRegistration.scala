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

import play.api.libs.functional.syntax.*
import play.api.libs.json.{Json, Reads, Writes, __}
import uk.gov.hmrc.iossregistration.models.etmp.EtmpDisplaySchemeDetails.displayReads
import uk.gov.hmrc.iossregistration.models.etmp.intermediary.EtmpOtherAddress

case class EtmpDisplayRegistration(
                                    customerIdentification: EtmpCustomerIdentification,
                                    tradingNames: Seq[EtmpTradingName],
                                    schemeDetails: EtmpDisplaySchemeDetails,
                                    bankDetails: Option[EtmpBankDetails],
                                    otherAddress: Option[EtmpOtherAddress],
                                    exclusions: Seq[EtmpExclusion],
                                    adminUse: EtmpAdminUse
                                  )

object EtmpDisplayRegistration {

  implicit private val etmpDetailsReads: Reads[EtmpDisplaySchemeDetails] = displayReads

  implicit val reads: Reads[EtmpDisplayRegistration] =
    (
      (__ \ "customerIdentification").read[EtmpCustomerIdentification] and
        (__ \ "tradingNames").readNullable[Seq[EtmpTradingName]].map(_.getOrElse(List.empty)) and
        (__ \ "schemeDetails").read[EtmpDisplaySchemeDetails] and
        (__ \ "bankDetails").readNullable[EtmpBankDetails] and
        (__ \ "otherAddress").readNullable[EtmpOtherAddress] and
        (__ \ "exclusions").readNullable[Seq[EtmpExclusion]].map(_.getOrElse(List.empty)) and
        (__ \ "adminUse").read[EtmpAdminUse]
      )(EtmpDisplayRegistration.apply _)
  implicit val writes: Writes[EtmpDisplayRegistration] = Json.writes[EtmpDisplayRegistration]
}
