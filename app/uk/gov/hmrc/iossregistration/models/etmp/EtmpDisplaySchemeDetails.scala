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

import play.api.libs.functional.syntax._
import play.api.libs.json.{__, Json, OFormat, Reads}

case class EtmpDisplaySchemeDetails(
                              commencementDate: String,
                              euRegistrationDetails: Seq[EtmpDisplayEuRegistrationDetails],
                              previousEURegistrationDetails: Seq[EtmpPreviousEuRegistrationDetails],
                              websites: Seq[EtmpWebsite],
                              contactName: String,
                              businessTelephoneNumber: String,
                              businessEmailId: String,
                              unusableStatus: Boolean,
                              nonCompliantReturns: Option[String],
                              nonCompliantPayments: Option[String]
                            )

object EtmpDisplaySchemeDetails {

  private def fromDisplayRegistrationPayload(
                                              commencementDate: String,
                                              euRegistrationDetails: Option[Seq[EtmpDisplayEuRegistrationDetails]],
                                              previousEURegistrationDetails: Option[Seq[EtmpPreviousEuRegistrationDetails]],
                                              websites: Seq[EtmpWebsite],
                                              contactNameOrBusinessAddress: String,
                                              businessTelephoneNumber: String,
                                              businessEmailAddress: String,
                                              unusableStatus: Boolean,
                                              nonCompliantReturns: Option[String],
                                              nonCompliantPayments: Option[String]
                                            ): EtmpDisplaySchemeDetails =
    EtmpDisplaySchemeDetails(
      commencementDate = commencementDate,
      euRegistrationDetails = euRegistrationDetails.fold(Seq.empty[EtmpDisplayEuRegistrationDetails])(a => a),
      previousEURegistrationDetails = previousEURegistrationDetails.fold(Seq.empty[EtmpPreviousEuRegistrationDetails])(a => a),
      websites = websites,
      contactName = contactNameOrBusinessAddress,
      businessTelephoneNumber = businessTelephoneNumber,
      businessEmailId = businessEmailAddress,
      unusableStatus = unusableStatus,
      nonCompliantReturns = nonCompliantReturns,
      nonCompliantPayments = nonCompliantPayments
    )

  val displayReads: Reads[EtmpDisplaySchemeDetails] =
    (
      (__ \ "commencementDate").read[String] and
        (__ \ "euRegistrationDetails").readNullable[Seq[EtmpDisplayEuRegistrationDetails]] and
        (__ \ "previousEURegistrationDetails").readNullable[Seq[EtmpPreviousEuRegistrationDetails]] and
        (__ \ "websites").read[Seq[EtmpWebsite]] and
        (__ \ "contactDetails" \ "contactNameOrBusinessAddress").read[String] and
        (__ \ "contactDetails" \ "businessTelephoneNumber").read[String] and
        (__ \ "contactDetails" \ "businessEmailAddress").read[String] and
        (__ \ "contactDetails" \ "unusableStatus").read[Boolean] and
        (__ \ "nonCompliantReturns").readNullable[String] and
        (__ \ "nonCompliantPayments").readNullable[String]
      )(EtmpDisplaySchemeDetails.fromDisplayRegistrationPayload _)

  implicit val format: OFormat[EtmpDisplaySchemeDetails] =
    Json.format[EtmpDisplaySchemeDetails]
}
