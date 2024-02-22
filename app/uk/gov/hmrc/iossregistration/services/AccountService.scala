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

package uk.gov.hmrc.iossregistration.services

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.iossregistration.config.AppConfig
import uk.gov.hmrc.iossregistration.connectors.EnrolmentsConnector

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AccountService @Inject()(
                                appConfig: AppConfig,
                                enrolmentsConnector: EnrolmentsConnector
                              )(implicit ec: ExecutionContext) {

  def getLatestAccount(userId: String)(implicit hc: HeaderCarrier): Future[Option[String]] = {
    enrolmentsConnector.es2(userId).map {
      case Right(accounts) =>
        accounts.enrolments
          .filter(_.activationDate.isDefined)
          .maxBy(_.activationDate.get)
          .identifiers
          .find(_.key == appConfig.iossEnrolment)
          .map(_.value)
      case Left(_) => None
    }
  }
}