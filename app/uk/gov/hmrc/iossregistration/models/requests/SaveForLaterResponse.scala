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

package uk.gov.hmrc.iossregistration.models.requests

import play.api.libs.json.{JsObject, JsValue, Json, OFormat}
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.iossregistration.models.SavedUserAnswers
import uk.gov.hmrc.iossregistration.models.des.VatCustomerInfo

import java.time.Instant

case class SaveForLaterResponse(vrn: Vrn,
                                data: JsValue,
                                vatInfo: VatCustomerInfo,
                                lastUpdated: Instant)

object SaveForLaterResponse {
  def apply(savedUserAnswers: SavedUserAnswers, vatCustomerInfo: VatCustomerInfo): SaveForLaterResponse = {
    SaveForLaterResponse(savedUserAnswers.vrn, savedUserAnswers.data, vatCustomerInfo, savedUserAnswers.lastUpdated)
  }

  implicit val format: OFormat[SaveForLaterResponse] = Json.format[SaveForLaterResponse]
}