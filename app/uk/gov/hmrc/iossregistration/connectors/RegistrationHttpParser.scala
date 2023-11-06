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

import play.api.http.Status.{CREATED, NOT_FOUND, OK}
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import uk.gov.hmrc.iossregistration.models.etmp.{AmendRegistrationResponse, EtmpDisplayRegistration, EtmpEnrolmentErrorResponse, EtmpEnrolmentResponse}
import uk.gov.hmrc.iossregistration.models.{ErrorResponse, EtmpEnrolmentError, InvalidJson, NotFound, ServerError, UnexpectedResponseStatus}

object RegistrationHttpParser extends BaseHttpParser {

  override val serviceName: String = "etmp registration"

  type CreateEtmpRegistrationResponse = Either[ErrorResponse, EtmpEnrolmentResponse]

  type DisplayRegistrationResponse = Either[ErrorResponse, EtmpDisplayRegistration]

  type CreateAmendRegistrationResponse = Either[ErrorResponse, AmendRegistrationResponse]

  implicit object CreateRegistrationReads extends HttpReads[CreateEtmpRegistrationResponse] {
    override def read(method: String, url: String, response: HttpResponse): CreateEtmpRegistrationResponse =
      response.status match {
        case CREATED => response.json.validate[EtmpEnrolmentResponse] match {
          case JsSuccess(enrolmentResponse, _) => Right(enrolmentResponse)
          case JsError(errors) =>
            logger.error(s"Failed trying to parse JSON, but was successfully created ${response.body} $errors")
            Left(InvalidJson)
        }
        case status =>
          if (response.body.nonEmpty) {
            response.json.validate[EtmpEnrolmentErrorResponse] match {
              case JsSuccess(enrolmentErrorResponse, _) =>
                Left(EtmpEnrolmentError(enrolmentErrorResponse.errors.code, enrolmentErrorResponse.errors.text))
              case JsError(errors) =>
                logger.error(s"Failed trying to parse JSON with status $status and body ${response.body} json parse error: $errors")
                Left(UnexpectedResponseStatus(status, s"Unexpected response from ${serviceName}, received status $status"))
            }
          } else {
            logger.error(s"Failed trying to parse empty JSON with status ${response.status} and body ${response.body}")
            logger.warn(s"Unexpected response from core registration, received status $status")
            Left(UnexpectedResponseStatus(status, s": Unexpected response from ${serviceName}, received status $status"))
          }
      }
  }

  implicit object DisplayRegistrationReads extends HttpReads[DisplayRegistrationResponse] {
    override def read(method: String, url: String, response: HttpResponse): DisplayRegistrationResponse =
      response.status match {
        case OK => response.json.validate[EtmpDisplayRegistration] match {
          case JsSuccess(displayRegistrationResponse, _) => Right(displayRegistrationResponse)
          case JsError(errors) =>
            logger.error(s"Failed trying to parse display registration response JSON with status ${response.status} with errors: $errors")
            Left(InvalidJson)
        }
        case status =>
          logger.error(s"Unknown error happened on display registration $status with body ${response.body}")
          Left(ServerError)
      }
  }

  implicit object CreateAmendRegistrationResponseReads extends HttpReads[CreateAmendRegistrationResponse] {
    override def read(method: String, url: String, response: HttpResponse): CreateAmendRegistrationResponse =
      response.status match {
        case OK => response.json.validate[AmendRegistrationResponse] match {
          case JsSuccess(amendRegistrationResponse, _) => Right(amendRegistrationResponse)
          case JsError(errors) =>
            logger.error(s"Failed trying to parse JSON with status ${response.status} and body ${response.body}", errors)
            Left(InvalidJson)
        }
        case NOT_FOUND =>
          logger.warn(s"url not reachable")
          Left(NotFound)
        case status =>
          logger.error(s"Unknown error happened on amend registration $status with body ${response.body}")
          Left(ServerError)
      }
  }
}
