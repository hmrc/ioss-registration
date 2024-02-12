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

package uk.gov.hmrc.iossregistration.services

import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.iossregistration.connectors.GetVatInfoConnector
import uk.gov.hmrc.iossregistration.connectors.VatCustomerInfoHttpParser.VatCustomerInfoResponse
import uk.gov.hmrc.iossregistration.models.{ErrorResponse, SavedUserAnswers}
import uk.gov.hmrc.iossregistration.models.des.VatCustomerInfo
import uk.gov.hmrc.iossregistration.models.requests.{SaveForLaterRequest, SaveForLaterResponse}
import uk.gov.hmrc.iossregistration.repositories.SaveForLaterRepository

import java.time.{Clock, Instant}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class SaveForLaterService @Inject()(
                                     repository: SaveForLaterRepository,
                                     getVatInfoConnector: GetVatInfoConnector,
                                     clock: Clock
                                   )(implicit ec: ExecutionContext) {

  def saveAnswers(request: SaveForLaterRequest): Future[SavedUserAnswers] = {
    val answers = SavedUserAnswers(
      vrn = request.vrn,
      data = request.data,
      lastUpdated = Instant.now(clock)
    )
    repository.set(answers)
  }

  def get(vrn: Vrn)(implicit hc: HeaderCarrier): Future[Option[SaveForLaterResponse]] = {
    repository.get(vrn).flatMap(mapMaybeUserAnswers)
  }

  private def mapMaybeUserAnswers(maybeSavedUserAnswers: Option[SavedUserAnswers])(implicit hc: HeaderCarrier): Future[Option[SaveForLaterResponse]] = {
    val maybeEventualLaterResponse = maybeSavedUserAnswers.map { savedUserAnswers =>
      getVatCustomerInfo(savedUserAnswers.vrn).map { vatCustomerInfo =>
        SaveForLaterResponse(savedUserAnswers, vatCustomerInfo)
      }
    }

    maybeEventualLaterResponse match {
      case Some(eventualResponse) => eventualResponse.map(Option.apply)
      case None => Future.successful(None)
    }
  }

  private def getVatCustomerInfo(vrn: Vrn)(implicit hc: HeaderCarrier): Future[VatCustomerInfo] = {
    getVatInfoConnector.getVatCustomerDetails(vrn).flatMap { errorOrResult: VatCustomerInfoResponse =>
      errorOrResult match {
        case Right(vatCustomerInfo) => Future.successful(vatCustomerInfo)
        case Left(errorResponse: ErrorResponse) => Future.failed(new RuntimeException(s"$errorResponse was returned for $vrn"))
      }
    }
  }

  def delete(vrn: Vrn): Future[Boolean] =
    repository.clear(vrn)

}
