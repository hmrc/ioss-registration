/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.iossregistration.models.etmp.amend

import play.api.libs.json.{Json, OFormat, Reads, Writes}

import java.time.LocalDate

trait EtmpExclusionDetails

object EtmpExclusionDetails {

  implicit val reads: Reads[EtmpExclusionDetails] =
    EtmpExclusionDetailsNew.format.widen[EtmpExclusionDetails] orElse
      EtmpExclusionDetailsLegacy.format.widen[EtmpExclusionDetails]

  implicit val writes: Writes[EtmpExclusionDetails] = Writes {
    case ecin: EtmpExclusionDetailsNew => Json.toJson(ecin)(EtmpExclusionDetailsNew.format)
    case ecil: EtmpExclusionDetailsLegacy => Json.toJson(ecil)(EtmpExclusionDetailsLegacy.format)
  }
}


case class EtmpExclusionDetailsNew(
                                 revertExclusion: Boolean,
                                 noLongerSupplyGoods: Boolean,
                                 noLongerEligible: Boolean,
                                 partyType: String = "NETP",
                                 exclusionRequestDate: Option[LocalDate],
                                 identificationValidityDate: Option[LocalDate],
                                 intExclusionRequestDate: Option[LocalDate],
                                 newMemberState: Option[EtmpNewMemberState]
                               ) extends EtmpExclusionDetails

object EtmpExclusionDetailsNew {

  implicit val format: OFormat[EtmpExclusionDetailsNew] = Json.format[EtmpExclusionDetailsNew]

}


case class EtmpExclusionDetailsLegacy(
                                 revertExclusion: Boolean,
                                 noLongerSupplyGoods: Boolean,
                                 partyType: String = "NETP",
                                 exclusionRequestDate: Option[LocalDate],
                                 identificationValidityDate: Option[LocalDate],
                                 intExclusionRequestDate: Option[LocalDate],
                                 newMemberState: Option[EtmpNewMemberState]
                               ) extends EtmpExclusionDetails

object EtmpExclusionDetailsLegacy {

  implicit val format: OFormat[EtmpExclusionDetailsLegacy] = Json.format[EtmpExclusionDetailsLegacy]

}