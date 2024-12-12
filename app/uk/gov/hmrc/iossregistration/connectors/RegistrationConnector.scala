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
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpException, StringContextOps}
import uk.gov.hmrc.iossregistration.config.{AmendRegistrationConfig, CreateRegistrationConfig, DisplayRegistrationConfig}
import uk.gov.hmrc.iossregistration.connectors.RegistrationHttpParser._
import uk.gov.hmrc.iossregistration.logging.Logging
import uk.gov.hmrc.iossregistration.models.UnexpectedResponseStatus
import uk.gov.hmrc.iossregistration.models.etmp.EtmpRegistrationRequest
import uk.gov.hmrc.iossregistration.models.etmp.amend.EtmpAmendRegistrationRequest

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class RegistrationConnector @Inject()(
                                            httpClientV2: HttpClientV2,
                                            createRegistrationConfig: CreateRegistrationConfig,
                                            displayRegistrationConfig: DisplayRegistrationConfig,
                                            amendRegistrationConfig: AmendRegistrationConfig
                                          )(implicit ec: ExecutionContext) extends Logging {

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private def getHeaders(correlationId: String): Seq[(String, String)] = displayRegistrationConfig.eisEtmpGetHeaders(correlationId)

  private def createHeaders(correlationId: String): Seq[(String, String)] = createRegistrationConfig.eisEtmpCreateHeaders(correlationId)

  private def amendHeaders(correlationId: String): Seq[(String, String)] = amendRegistrationConfig.eisEtmpAmendHeaders(correlationId)

  def get(iossNumber: String): Future[DisplayRegistrationResponse] = {

    val correlationId = UUID.randomUUID.toString
    val headersWithCorrelationId = getHeaders(correlationId)
    val url = url"${displayRegistrationConfig.baseUrl}vec/iossregistration/viewreg/v1/$iossNumber"
    httpClientV2.get(url)
      .setHeader(headersWithCorrelationId: _*)
      .execute[DisplayRegistrationResponse].recover {
      case e: HttpException =>
        logger.error(s"Unexpected response from etmp registration ${e.getMessage}", e)
        Left(UnexpectedResponseStatus(e.responseCode, s"Unexpected response from ${serviceName}, received status ${e.responseCode}"))
    }
  }

  def createRegistration(registration: EtmpRegistrationRequest): Future[CreateEtmpRegistrationResponse] = {

    val correlationId = UUID.randomUUID.toString
    val headersWithCorrelationId = createHeaders(correlationId)
    val headersWithoutAuth = headersWithCorrelationId.filterNot {
      case (key, _) => key.matches(AUTHORIZATION)
    }

    logger.info(s"Sending create request to etmp with headers $headersWithoutAuth")

    httpClientV2.post(url"${createRegistrationConfig.baseUrl}vec/iosssubscription/subdatatransfer/v1")
      .withBody(Json.toJson(registration))
      .setHeader(headersWithCorrelationId: _*)
      .execute[CreateEtmpRegistrationResponse].recover {
      case e: HttpException =>
        logger.error(s"Unexpected response from etmp registration ${e.getMessage}", e)
        Left(UnexpectedResponseStatus(e.responseCode, s"Unexpected response from ${serviceName}, received status ${e.responseCode}"))
    }
  }

  def amendRegistration(registration: EtmpAmendRegistrationRequest): Future[CreateAmendRegistrationResponse] = {

    val correlationId: String = UUID.randomUUID.toString
    val headersWithCorrelationId = amendHeaders(correlationId)
    val headersWithoutAuth = headersWithCorrelationId.filterNot {
      case (key, _) => key.matches(AUTHORIZATION)
    }

    logger.info(s"Sending amend request to etmp with headers $headersWithoutAuth")

    httpClientV2.put(url"${amendRegistrationConfig.baseUrl}vec/iossregistration/amendregistration/v1")
      .withBody(Json.toJson(registration))
      .setHeader(headersWithCorrelationId: _*)
      .execute[CreateAmendRegistrationResponse]
      .recover {
        case e: HttpException =>
          logger.error(s"Unexpected response from etmp registration ${e.getMessage}", e)
          Left(UnexpectedResponseStatus(e.responseCode, s"Unexpected response from ${serviceName}, received status ${e.responseCode}"))
      }
  }
}
