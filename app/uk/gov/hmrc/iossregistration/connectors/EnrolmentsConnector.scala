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

import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpErrorFunctions, HttpResponse, StringContextOps}
import uk.gov.hmrc.iossregistration.config.{EnrolmentProxyConfig, TaxEnrolmentsConfig}
import uk.gov.hmrc.iossregistration.connectors.EnrolmentsHttpParser.{ES2EnrolmentResultsResponse, QueryEnrolmentResultsResponseReads}
import uk.gov.hmrc.iossregistration.controllers.routes
import uk.gov.hmrc.iossregistration.logging.Logging
import uk.gov.hmrc.iossregistration.models.enrolments.SubscriberRequest

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EnrolmentsConnector @Inject()(
                                     taxEnrolmentsConfig: TaxEnrolmentsConfig,
                                     enrolmentProxyConfig: EnrolmentProxyConfig,
                                     httpClientV2: HttpClientV2
                                   )
                                   (implicit ec: ExecutionContext) extends HttpErrorFunctions with Logging {

  def confirmEnrolment(subscriptionId: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val etmpId = UUID.randomUUID.toString

    httpClientV2.put(url"${taxEnrolmentsConfig.baseUrl}subscriptions/$subscriptionId/subscriber")
      .withBody(Json.toJson(
        SubscriberRequest(taxEnrolmentsConfig.iossEnrolmentKey,
          s"${taxEnrolmentsConfig.callbackBaseUrl}${routes.EnrolmentsSubscriptionController.authoriseEnrolment(subscriptionId).url}",
          etmpId
        )
      )).execute[HttpResponse]
  }

  def es2(userId: String)(implicit hc: HeaderCarrier): Future[ES2EnrolmentResultsResponse] = {
    httpClientV2.get(
      url"${enrolmentProxyConfig.baseUrl}enrolment-store/users/$userId/enrolments?service=HMRC-IOSS-ORG"
    ).execute[ES2EnrolmentResultsResponse]
  }
}
