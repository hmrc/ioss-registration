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

package uk.gov.hmrc.iossregistration.models.audit

import play.api.libs.json.{JsObject, Json, JsValue}
import uk.gov.hmrc.iossregistration.controllers.actions.AuthorisedMandatoryVrnRequest
import uk.gov.hmrc.iossregistration.models.etmp.EtmpEnrolmentResponse
import uk.gov.hmrc.iossregistration.models.etmp.amend.EtmpAmendRegistrationRequest

case class EtmpAmendRegistrationRequestAuditModel(
                                                   etmpRegistrationAuditType: EtmpRegistrationAuditType,
                                                   userId: String,
                                                   userAgent: String,
                                                   vrn: String,
                                                   etmpAmendRegistrationRequest: EtmpAmendRegistrationRequest,
                                                   etmpEnrolmentResponse: Option[EtmpEnrolmentResponse],
                                                   errorResponse: Option[String],
                                                   submissionResult: SubmissionResult
                                            ) extends JsonAuditModel {

  override val auditType: String = etmpRegistrationAuditType.auditType

  override val transactionName: String = etmpRegistrationAuditType.transactionName

  private val etmpEnrolmentResponseObj: JsObject =
    if (etmpEnrolmentResponse.isDefined) {
      Json.obj("etmpEnrolmentResponse" -> etmpEnrolmentResponse)
    } else {
      Json.obj()
    }

  private val errorResponseObj: JsObject =
    if (errorResponse.isDefined) {
      Json.obj("errorResponse" -> errorResponse)
    } else {
      Json.obj()
    }

  override val detail: JsValue = Json.obj(
    "userId" -> userId,
    "browserUserAgent" -> userAgent,
    "requestersVrn" -> vrn,
    "etmpAmendRegistrationRequest" -> Json.toJson(etmpAmendRegistrationRequest),
    "submissionResult" -> Json.toJson(submissionResult)
  ) ++ etmpEnrolmentResponseObj ++
    errorResponseObj
}

object EtmpAmendRegistrationRequestAuditModel {

  def build(
             etmpRegistrationAuditType: EtmpRegistrationAuditType,
             etmpRegistrationRequest: EtmpAmendRegistrationRequest,
             etmpEnrolmentResponse: Option[EtmpEnrolmentResponse],
             errorResponse: Option[String],
             submissionResult: SubmissionResult
           )(implicit request: AuthorisedMandatoryVrnRequest[_]): EtmpAmendRegistrationRequestAuditModel =
    EtmpAmendRegistrationRequestAuditModel(
      etmpRegistrationAuditType = etmpRegistrationAuditType,
      userId = request.userId,
      userAgent = request.headers.get("user-agent").getOrElse(""),
      vrn = request.vrn.vrn,
      etmpAmendRegistrationRequest = etmpRegistrationRequest,
      etmpEnrolmentResponse = etmpEnrolmentResponse,
      errorResponse = errorResponse,
      submissionResult = submissionResult
    )
}
