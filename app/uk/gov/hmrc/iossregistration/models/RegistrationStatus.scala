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

package uk.gov.hmrc.iossregistration.models

import uk.gov.hmrc.iossregistration.models.etmp.EtmpRegistrationStatus
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import play.api.libs.json._

import java.time.Instant

case class RegistrationStatus(
                               subscriptionId: String,
                               status: EtmpRegistrationStatus,
                               lastUpdated: Instant = Instant.now
                             )

object RegistrationStatus {

  val reads: Reads[RegistrationStatus] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "subscriptionId").read[String] and
        (__ \ "status").read[EtmpRegistrationStatus] and
        (__ \ "lastUpdated").readWithDefault[Instant](Instant.now())(MongoJavatimeFormats.instantFormat)
      )(RegistrationStatus.apply _)
  }

  val writes: OWrites[RegistrationStatus] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "subscriptionId").write[String] and
        (__ \ "status").write[EtmpRegistrationStatus] and
        (__ \ "lastUpdated").write(MongoJavatimeFormats.instantFormat)
      )(registrationStatus => Tuple.fromProductTyped(registrationStatus))
  }

  implicit val format: OFormat[RegistrationStatus] = OFormat(reads, writes)

}
