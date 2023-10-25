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

import play.api.libs.json.{Json, JsValue}
import uk.gov.hmrc.iossregistration.controllers.actions.AuthorisedRequest

case class BTAExternalEntryAuditModel(
                                       userId: String,
                                       userAgent: String,
                                       vrn: Option[String],
                                       redirectUrl: String
                                     ) extends JsonAuditModel {

  override val auditType: String = "BTAExternalEntry"
  override val transactionName: String = "bta-external-entry"

  override val detail: JsValue = Json.obj(
    "userId" -> userId,
    "browserUserAgent" -> userAgent,
    "vrn" -> vrn,
    "redirectUrl" -> redirectUrl
  )
}

object BTAExternalEntryAuditModel {

  def build(redirectUrl: String)(implicit request: AuthorisedRequest[_]): BTAExternalEntryAuditModel =
    BTAExternalEntryAuditModel(
      userId = request.userId,
      userAgent = request.headers.get("user-agent").getOrElse(""),
      vrn = request.vrn.map(_.vrn),
      redirectUrl = redirectUrl
    )
}