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

import uk.gov.hmrc.iossregistration.models.{Enumerable, WithName}

sealed trait EtmpRegistrationStatus

object EtmpRegistrationStatus extends Enumerable.Implicits {
  case object Success extends WithName("Success") with EtmpRegistrationStatus
  case object Pending extends WithName("Pending") with EtmpRegistrationStatus
  case object Error extends WithName("Error") with EtmpRegistrationStatus

  val values: Seq[EtmpRegistrationStatus] = Seq(
    Success,
    Pending,
    Error
  )

  implicit val enumerable: Enumerable[EtmpRegistrationStatus] =
    Enumerable(values.map(v => v.toString -> v): _*)
}
