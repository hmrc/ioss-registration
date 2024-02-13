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

import play.api.Configuration
import play.api.http.HeaderNames._
import play.api.http.MimeTypes
import uk.gov.hmrc.iossregistration.models.binders.Format

import java.time.{Clock, LocalDateTime}
import javax.inject.Inject

class CoreValidationConfig @Inject()(config: Configuration, clock: Clock) {

  val coreValidationUrl: Service = config.get[Service]("microservice.services.core-validation")

  private val XCorrelationId = "X-Correlation-Id"
  private val authorizationToken: String = config.get[String]("microservice.services.core-validation.authorizationToken")

  def eisCoreHeaders(correlationId: String): Seq[(String, String)] = Seq(
    XCorrelationId -> correlationId,
    X_FORWARDED_HOST -> "MDTP",
    CONTENT_TYPE -> MimeTypes.JSON,
    ACCEPT -> MimeTypes.JSON,
    DATE -> Format.eisDateTimeFormatter.format(LocalDateTime.now(clock)),
    AUTHORIZATION -> s"Bearer $authorizationToken"
  )

}
