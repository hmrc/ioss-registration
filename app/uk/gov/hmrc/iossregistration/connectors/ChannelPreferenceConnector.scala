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

package uk.gov.hmrc.iossregistration.connectors

import play.api.http.HeaderNames
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpErrorFunctions, HttpResponse, StringContextOps}
import uk.gov.hmrc.iossregistration.config.ChannelPreferenceConfig
import uk.gov.hmrc.iossregistration.logging.Logging
import uk.gov.hmrc.iossregistration.models.etmp.channelPreference.ChannelPreferenceRequest

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ChannelPreferenceConnector @Inject()(
                                            channelPreferenceConfig: ChannelPreferenceConfig,
                                            httpClientV2: HttpClientV2
                                          )(implicit ec: ExecutionContext) extends HttpErrorFunctions with Logging {
  private val XCorrelationId = "X-Correlation-Id"

  private def headers(correlationId: String): Seq[(String, String)] = Seq(
    HeaderNames.AUTHORIZATION -> s"Bearer ${channelPreferenceConfig.authorizationToken}",
    "Environment" -> channelPreferenceConfig.environment,
    XCorrelationId -> correlationId
  )

  def updatePreferences(channelPreference: ChannelPreferenceRequest)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val correlationId = UUID.randomUUID.toString
    httpClientV2.put(url"${channelPreferenceConfig.baseUrl}income-tax/customer/IOSS/contact-preference")
      .withBody(Json.toJson(channelPreference))
      .setHeader(headers(correlationId): _*)
      .execute[HttpResponse]
  }

}
