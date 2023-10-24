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

import play.api.http.Status.CREATED
import play.api.libs.json.JsSuccess
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import uk.gov.hmrc.iossregistration.logging.Logging
import uk.gov.hmrc.iossregistration.models.TaxEnrolmentErrorResponse

object EnrolmentsHttpParser extends Logging {

  type EnrolmentResultsResponse = Either[TaxEnrolmentErrorResponse, Unit]

  implicit object EnrolmentsResponseReads extends HttpReads[EnrolmentResultsResponse] {
    override def read(method: String, url: String, response: HttpResponse): EnrolmentResultsResponse = {
      response.status match {
        case CREATED => Right(())
        case status =>
          logger.info(s"Response received from enrolments: ${response.status} with body ${response.body}")
          if (response.body.isEmpty) {
            Left(
              TaxEnrolmentErrorResponse(s"UNEXPECTED_$status", "The response body was empty")
            )
          } else {
            response.json.validateOpt[TaxEnrolmentErrorResponse] match {
              case JsSuccess(Some(value), _) =>
                logger.error(s"Error response from enrolments $url, received status $status, body of response was: ${response.body}")
                Left(value)
              case _ =>
                logger.error(s"Unexpected error response from enrolments $url, received status $status, body of response was: ${response.body}")
                Left(

                  TaxEnrolmentErrorResponse(s"UNEXPECTED_$status", response.body)
                )
            }
          }
      }
    }
  }
}
