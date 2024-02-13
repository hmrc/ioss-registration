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

package uk.gov.hmrc.iossregistration.models.etmp

import uk.gov.hmrc.iossregistration.models.{Enumerable, WithName}

sealed trait EtmpMessageType

object EtmpMessageType extends Enumerable.Implicits {

  case object IOSSSubscriptionCreate extends WithName("IOSSSubscriptionCreate") with EtmpMessageType

  case object IOSSSubscriptionAmend extends WithName("IOSSSubscriptionAmend") with EtmpMessageType

  val values: Seq[EtmpMessageType] = Seq(
    IOSSSubscriptionCreate, IOSSSubscriptionAmend
  )

  implicit val enumerable: Enumerable[EtmpMessageType] =
    Enumerable(values.map(v => v.toString -> v): _*)
}
