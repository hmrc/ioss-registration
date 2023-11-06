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

import play.api.http.HeaderNames._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpException}
import uk.gov.hmrc.iossregistration.config.{AmendRegistrationConfig, CreateRegistrationConfig, DisplayRegistrationConfig}
import uk.gov.hmrc.iossregistration.connectors.RegistrationHttpParser._
import uk.gov.hmrc.iossregistration.logging.Logging
import uk.gov.hmrc.iossregistration.metrics.{MetricsEnum, ServiceMetrics}
import uk.gov.hmrc.iossregistration.models.UnexpectedResponseStatus
import uk.gov.hmrc.iossregistration.models.etmp.EtmpRegistrationRequest

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class RegistrationConnector @Inject()(
                                            httpClient: HttpClient,
                                            createRegistrationConfig: CreateRegistrationConfig,
                                            displayRegistrationConfig: DisplayRegistrationConfig,
                                            amendRegistrationConfig: AmendRegistrationConfig,
                                            metrics: ServiceMetrics
                                          )(implicit ec: ExecutionContext) extends Logging {

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private def getHeaders(correlationId: String): Seq[(String, String)] = displayRegistrationConfig.eisEtmpGetHeaders(correlationId)
  private def createHeaders(correlationId: String): Seq[(String, String)] = createRegistrationConfig.eisEtmpCreateHeaders(correlationId)
  private def amendHeaders(correlationId: String): Seq[(String, String)] = amendRegistrationConfig.eisEtmpAmendHeaders(correlationId)

  def get(iossNumber: String): Future[DisplayRegistrationResponse] = {

    val correlationId = UUID.randomUUID().toString
    val headersWithCorrelationId = getHeaders(correlationId)
    val timerContext = metrics.startTimer(MetricsEnum.GetRegistration)
    val url = s"${displayRegistrationConfig.baseUrl}vec/iossregistration/viewreg/v1/$iossNumber"
    httpClient.GET[DisplayRegistrationResponse](url = url, headers = headersWithCorrelationId).map { result =>
      timerContext.stop()
      result
    }.recover {
      case e: HttpException =>
        timerContext.stop()
        logger.error(s"Unexpected response from etmp registration ${e.getMessage}", e)
        Left(UnexpectedResponseStatus(e.responseCode, s"Unexpected response from ${serviceName}, received status ${e.responseCode}"))
    }
  }

  def createRegistration(registration: EtmpRegistrationRequest): Future[CreateEtmpRegistrationResponse] = {

    val correlationId = UUID.randomUUID().toString
    val headersWithCorrelationId = createHeaders(correlationId)
    val headersWithoutAuth = headersWithCorrelationId.filterNot {
      case (key, _) => key.matches(AUTHORIZATION)
    }

    logger.info(s"Sending create request to etmp with headers $headersWithoutAuth")

    httpClient.POST[EtmpRegistrationRequest, CreateEtmpRegistrationResponse](
      s"${createRegistrationConfig.baseUrl}vec/iosssubscription/subdatatransfer/v1",
      registration,
      headers = headersWithCorrelationId
    ).map {
      result =>
        result
    }.recover {
      case e: HttpException =>
        logger.error(s"Unexpected response from etmp registration ${e.getMessage}", e)
        Left(UnexpectedResponseStatus(e.responseCode, s"Unexpected response from ${serviceName}, received status ${e.responseCode}"))
    }
  }

  def amendRegistration(registration: EtmpRegistrationRequest): Future[CreateAmendRegistrationResponse] = {

    val correlationId: String = UUID.randomUUID().toString
    val headersWithCorrelationId = amendHeaders(correlationId)
    val timerContext = metrics.startTimer(MetricsEnum.AmendRegistration)
    val headersWithoutAuth = headersWithCorrelationId.filterNot {
      case (key, _) => key.matches(AUTHORIZATION)
    }

    logger.info(s"Sending amend request to etmp with headers $headersWithoutAuth")

    httpClient.PUT[EtmpRegistrationRequest, CreateAmendRegistrationResponse](
      s"${amendRegistrationConfig.baseUrl}vec/iosssubscription/amendreg/v1",
      registration,
      headers = headersWithCorrelationId
    ).map { result =>
      timerContext.stop()
      result
    }.recover {
      case e: HttpException =>
        timerContext.stop()
        logger.error(s"Unexpected response from etmp registration ${e.getMessage}", e)
        Left(UnexpectedResponseStatus(e.responseCode, s"Unexpected response from ${serviceName}, received status ${e.responseCode}"))
    }
  }
}
