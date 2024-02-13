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

package uk.gov.hmrc.iossregistration.config

import play.api.http.HeaderNames.{ACCEPT, CONTENT_TYPE, DATE, X_FORWARDED_HOST}
import play.api.http.MimeTypes
import uk.gov.hmrc.iossregistration.models.binders.Format.eisDateTimeFormatter

import java.time.{Clock, LocalDateTime}
import javax.inject.Inject

class EisGenericConfig @Inject()(clock: Clock) {

  private val XCorrelationId = "X-Correlation-Id"

  def eisEtmpGenericHeaders(correlationId: String): Seq[(String, String)] = Seq(
    CONTENT_TYPE -> MimeTypes.JSON,
    ACCEPT -> MimeTypes.JSON,
    DATE -> eisDateTimeFormatter.format(LocalDateTime.now(clock)),
    XCorrelationId -> correlationId,
    X_FORWARDED_HOST -> "MDTP"
  )

}
