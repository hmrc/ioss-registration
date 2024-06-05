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

package uk.gov.hmrc.iossregistration.connectors

import play.api.http.HeaderNames
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{GatewayTimeoutException, HeaderCarrier, HttpErrorFunctions, StringContextOps}
import uk.gov.hmrc.iossregistration.config.GetVatInfoConfig
import uk.gov.hmrc.iossregistration.connectors.VatCustomerInfoHttpParser.{VatCustomerInfoReads, VatCustomerInfoResponse}
import uk.gov.hmrc.iossregistration.logging.Logging
import uk.gov.hmrc.iossregistration.models.GatewayTimeout

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GetVatInfoConnector @Inject()(
                                     getVatInfoConfig: GetVatInfoConfig,
                                     httpClientV2: HttpClientV2
                                   )(implicit ec: ExecutionContext)
  extends HttpErrorFunctions with Logging {

  private val XCorrelationId = "X-Correlation-Id"

  private def headers(correlationId: String): Seq[(String, String)] = Seq(
    HeaderNames.AUTHORIZATION -> s"Bearer ${getVatInfoConfig.authorizationToken}",
    "Environment" -> getVatInfoConfig.environment,
    XCorrelationId -> correlationId
  )

  def getVatCustomerDetails(vrn: Vrn)(implicit headerCarrier: HeaderCarrier): Future[VatCustomerInfoResponse] = {
    httpClientV2
      .get(url"${getVatInfoConfig.baseUrl}vat/customer/vrn/${vrn.value}/information")
      .setHeader(headers(UUID.randomUUID.toString): _*)
      .execute[VatCustomerInfoResponse]
      .recover {
      case e: GatewayTimeoutException =>
        logger.error(s"Request timeout from Get vat info: $e", e)
        Left(GatewayTimeout)
    }
  }
}
