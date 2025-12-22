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

import uk.gov.hmrc.domain
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.iossregistration.connectors.{GetVatInfoConnector, RegistrationConnector}
import uk.gov.hmrc.iossregistration.connectors.VatCustomerInfoHttpParser.{VatCustomerInfoReads, VatCustomerInfoResponse}
import uk.gov.hmrc.iossregistration.connectors.RegistrationHttpParser.CreateEtmpRegistrationResponse
import uk.gov.hmrc.iossregistration.logging.Logging
import uk.gov.hmrc.iossregistration.models.{EtmpException, RegistrationWrapper}
import uk.gov.hmrc.iossregistration.models.etmp.{EtmpCustomerIdentificationLegacy, EtmpCustomerIdentificationNew, EtmpDisplayRegistration, EtmpIdType, EtmpRegistrationRequest}
import uk.gov.hmrc.iossregistration.models.etmp.amend.{AmendRegistrationResponse, EtmpAmendRegistrationRequest}
import uk.gov.hmrc.iossregistration.utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RegistrationService @Inject()(
                                     registrationConnector: RegistrationConnector,
                                     getVatInfoConnector: GetVatInfoConnector
                                   )(implicit ec: ExecutionContext) extends Logging {

  def createRegistration(etmpRegistrationRequest: EtmpRegistrationRequest): Future[CreateEtmpRegistrationResponse] =
    registrationConnector.createRegistration(etmpRegistrationRequest)

  def get(iossNumber: String, vrn: Vrn)(implicit headerCarrier: HeaderCarrier): Future[RegistrationWrapper] = {
    for {
      etmpDisplayRegistrationResponse <- registrationConnector.get(iossNumber)
      registrationWrapper <- etmpDisplayRegistrationResponse match
        case Left(etmpDisplayRegistrationError) =>
          val errorMessage = s"There was an error getting Registration from ETMP: ${etmpDisplayRegistrationError.body}"
          logger.error(errorMessage)
          throw EtmpException(errorMessage)

        case Right(etmpDisplayRegistration) =>

          etmpDisplayRegistration.customerIdentification match {
            case EtmpCustomerIdentificationLegacy(vrnLegacy) => getVatInfoForRegistration(vrn, etmpDisplayRegistration)
            case EtmpCustomerIdentificationNew(idType, idValue) if idType == EtmpIdType.VRN => getVatInfoForRegistration(Vrn(idValue), etmpDisplayRegistration)
            case _ =>  RegistrationWrapper(None, etmpDisplayRegistration).toFuture
          }
    } yield registrationWrapper
  }

  def get(iossNumber: String)(implicit headerCarrier: HeaderCarrier): Future[EtmpDisplayRegistration] = {

    registrationConnector.get(iossNumber).map {
      case Right(etmpDisplayRegistration) => etmpDisplayRegistration
      case Left(displayError) =>
        logger.error(s"There was an error getting Registration from ETMP: ${displayError.body}")
        throw EtmpException(s"There was an error getting Registration from ETMP: ${displayError.body}")
    }
  }

  def amendRegistration(etmpRegistrationRequest: EtmpAmendRegistrationRequest): Future[Either[Throwable, AmendRegistrationResponse]] = {
    registrationConnector.amendRegistration(etmpRegistrationRequest).flatMap {
      case Right(amendRegistrationResponse: AmendRegistrationResponse) =>
        logger.info(s"Successfully sent amend registration to ETMP at ${amendRegistrationResponse.processingDateTime} for vrn ${amendRegistrationResponse.vrn} and IOSS number ${amendRegistrationResponse.iossReference}")
        Future.successful(Right(amendRegistrationResponse))
      case Left(error) =>
        logger.error(s"An error occurred while amending registration ${error.getClass} ${error.body}")
        Future.successful(Left(EtmpException(s"There was an error amending Registration from ETMP: ${error.getClass} ${error.body}")))
    }
  }

  def getVatInfoForRegistration(vrn: Vrn, etmpDisplayRegistration: EtmpDisplayRegistration)(implicit headerCarrier: HeaderCarrier): Future[RegistrationWrapper] = {
      val vatCustomerInfoFutureResponse: Future[VatCustomerInfoResponse] = getVatInfoConnector.getVatCustomerDetails(vrn)
      vatCustomerInfoFutureResponse.flatMap {
        case Left(vatCustomerInfoError) =>
          val errorMessage = s"There was an error retrieving the VAT information from ETMP: ${vatCustomerInfoError.body}"
          logger.error(errorMessage)
          throw EtmpException(errorMessage)

        case Right(vatCustomerInfo) =>
          RegistrationWrapper(Some(vatCustomerInfo), etmpDisplayRegistration).toFuture
      }
  }
}
