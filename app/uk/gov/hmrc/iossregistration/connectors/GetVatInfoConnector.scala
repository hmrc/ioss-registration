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
import uk.gov.hmrc.iossregistration.logging.Logging
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.{GatewayTimeoutException, HeaderCarrier, HttpClient, HttpErrorFunctions}
import uk.gov.hmrc.iossregistration.config.GetVatInfoConfig
import uk.gov.hmrc.iossregistration.connectors.VatCustomerInfoHttpParser.{VatCustomerInfoReads, VatCustomerInfoResponse}
import uk.gov.hmrc.iossregistration.models.GatewayTimeout

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GetVatInfoConnector @Inject()(
                                     getVatInfoConfig: GetVatInfoConfig,
                                     httpClient: HttpClient
                                   )(implicit ec: ExecutionContext)
  extends HttpErrorFunctions with Logging {

  private val XCorrelationId = "X-Correlation-Id"

  private def headers(correlationId: String): Seq[(String, String)] = Seq(
    HeaderNames.AUTHORIZATION -> s"Bearer ${getVatInfoConfig.authorizationToken}",
    "Environment" -> getVatInfoConfig.environment,
    XCorrelationId -> correlationId
  )

  def getVatCustomerDetails(vrn: Vrn)(implicit headerCarrier: HeaderCarrier): Future[VatCustomerInfoResponse] = {
    val url = s"${getVatInfoConfig.baseUrl}vat/customer/vrn/${vrn.value}/information"
    val correlationId = UUID.randomUUID().toString
    httpClient.GET[VatCustomerInfoResponse](
      url = url,
      headers = headers(correlationId)
    ).recover {
      case e: GatewayTimeoutException =>
        logger.error(s"Request timeout from Get vat info: $e", e)
        Left(GatewayTimeout)
    }
  }
}
