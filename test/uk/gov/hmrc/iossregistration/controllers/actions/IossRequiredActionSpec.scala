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

package uk.gov.hmrc.iossregistration.controllers.actions

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.Result
import play.api.mvc.Results.Unauthorized
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.iossregistration.base.BaseSpec
import uk.gov.hmrc.iossregistration.config.AppConfig
import uk.gov.hmrc.iossregistration.connectors.IntermediaryRegistrationConnector
import uk.gov.hmrc.iossregistration.models.etmp.intermediary.IntermediaryRegistrationWrapper
import uk.gov.hmrc.iossregistration.utils.FutureSyntax.FutureOps

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class IossRequiredActionSpec extends BaseSpec with BeforeAndAfterEach {

  val mockIntermediaryRegistrationConnector: IntermediaryRegistrationConnector = mock[IntermediaryRegistrationConnector]
  val mockAppConfig: AppConfig = mock[AppConfig]

  class Harness(
                 maybeIossNumber: Option[String],
                 intermediaryRegistrationConnector: IntermediaryRegistrationConnector,
                 appConfig: AppConfig
               ) extends IossRequiredAction(
    intermediaryRegistrationConnector,
    appConfig,
    maybeIossNumber
  ) {

    def callRefine[A](request: AuthorisedMandatoryVrnRequest[A]): Future[Either[Result, AuthorisedMandatoryIossRequest[A]]] = refine(request)
  }

  def intermediaryRegistrationWithClients(iossNumber: Seq[String]): IntermediaryRegistrationWrapper = {
    arbitraryIntermediaryRegistrationWrapper.arbitrary.sample.value.copy(
      etmpDisplayRegistration = arbitraryEtmpIntermediaryDisplayRegistration.arbitrary.sample.value.copy(
        clientDetails = iossNumber.map { ioss =>
          arbitraryEtmpClientDetails.arbitrary.sample.value.copy(clientIossID = ioss)
        }
      )
    )
  }

  def enrolmentsWithIntermediaries(intermediaryNumbers: Seq[String]): Enrolments = {
    Enrolments(intermediaryNumbers.map { int =>
      Enrolment(
        key = "HMRC-IOSS-INT",
        identifiers = Seq(EnrolmentIdentifier("IntNumber", int)),
        state = "Activated"
      )
    }.toSet
    )
  }

  override def beforeEach(): Unit = {
    Mockito.reset(mockIntermediaryRegistrationConnector, mockAppConfig)
  }

  def buildRegistrationRequest(iossNumber: Option[String], intermediaryNumber: Option[String], enrolments: Enrolments): AuthorisedMandatoryVrnRequest[_] = {
    AuthorisedMandatoryVrnRequest(FakeRequest(), testCredentials, userId, vrn, iossNumber, intermediaryNumber, enrolments)
  }

  "Ioss Required Action" - {

    "when the user has logged in as an Organisation Admin with strong credentials but ioss enrolment" - {

      "must return Unauthorized" in {

        val action = new Harness(None, mockIntermediaryRegistrationConnector, mockAppConfig)
        val request = FakeRequest(GET, "/test/url?k=session-id")
        val result = action.callRefine(AuthorisedMandatoryVrnRequest(request,
          testCredentials,
          userId,
          vrn,
          None,
          None,
          enrolments
        )).futureValue

        result mustBe Left(Unauthorized)
      }

      "must return Right" in {

        val action = new Harness(None, mockIntermediaryRegistrationConnector, mockAppConfig)
        val request = FakeRequest(GET, "/test/url?k=session-id")
        val result = action.callRefine(AuthorisedMandatoryVrnRequest(request,
          testCredentials,
          userId,
          vrn,
          Some(iossNumber),
          None,
          enrolments
        )).futureValue

        val expectResult = AuthorisedMandatoryIossRequest(request, testCredentials, userId, vrn, iossNumber)

        result mustBe Right(expectResult)
      }
    }

    "must build a Registration Request" - {

      "when an IOSS exists" in {

        val application = applicationBuilder.build()

        running(application) {

          val request = buildRegistrationRequest(
            iossNumber = Some(iossNumber),
            intermediaryNumber = None,
            enrolments = enrolments
          )

          val action = new Harness(None, mockIntermediaryRegistrationConnector, mockAppConfig)

          val result = action.callRefine(AuthorisedMandatoryVrnRequest(request,
            testCredentials,
            userId,
            vrn,
            Some(iossNumber),
            None,
            enrolments
          )).futureValue

          val expectedResult = AuthorisedMandatoryIossRequest(request, testCredentials, userId, vrn, iossNumber)

          result mustBe Right(expectedResult)
        }
      }

      "when an active intermediary has access to an IOSS client" in {

        val currentIntermediary = intermediaryNumber

        when(mockAppConfig.intermediaryEnabled) thenReturn true
        when(mockIntermediaryRegistrationConnector.get(any())(any())) thenReturn
          intermediaryRegistrationWithClients(Seq(iossNumber)).toFuture

        val application = applicationBuilder.build()

        running(application) {

          val request = buildRegistrationRequest(
            iossNumber = Some(iossNumber),
            intermediaryNumber = Some(currentIntermediary),
            enrolments = enrolmentsWithIntermediaries(Seq(currentIntermediary))
          )

          val action = new Harness(Some(iossNumber), mockIntermediaryRegistrationConnector, mockAppConfig)

          val result = action.callRefine(request).futureValue

          val expectedResult = AuthorisedMandatoryIossRequest(
            request.request,
            request.credentials,
            request.userId,
            request.vrn,
            iossNumber
          )

          result mustBe Right(expectedResult)
        }
      }

      "when intermediary has access via previous enrolments" in {

        val currentIntermediary = intermediaryNumber
        val previousIntermediary = "IN9007654322"

        when(mockAppConfig.intermediaryEnabled) thenReturn true
        when(mockIntermediaryRegistrationConnector.get(any())(any())) thenReturn
          intermediaryRegistrationWithClients(Seq(iossNumber)).toFuture

        val application = applicationBuilder.build()

        running(application) {

          val request = buildRegistrationRequest(
            iossNumber = Some(iossNumber),
            intermediaryNumber = Some(previousIntermediary),
            enrolments = enrolmentsWithIntermediaries(Seq(currentIntermediary, previousIntermediary))
          )

          val action = new Harness(None, mockIntermediaryRegistrationConnector, mockAppConfig)

          val result = action.callRefine(request).futureValue

          val expectedResult = AuthorisedMandatoryIossRequest(
            request.request,
            request.credentials,
            request.userId,
            request.vrn,
            iossNumber
          )

          result mustBe Right(expectedResult)
        }
      }
    }

    "must return Unauthorised" - {

      "when intermediary has no access to any of the clients" in {

        val currentIntermediary = intermediaryNumber

        when(mockIntermediaryRegistrationConnector.get(any())(any())) thenReturn
          intermediaryRegistrationWithClients(Nil).toFuture

        val application = applicationBuilder.build()

        running(application) {

          val request = buildRegistrationRequest(
            iossNumber = None,
            intermediaryNumber = Some(currentIntermediary),
            enrolments = enrolmentsWithIntermediaries(Seq(currentIntermediary))
          )

          val action = new Harness(None, mockIntermediaryRegistrationConnector, mockAppConfig)

          val result = action.callRefine(request).futureValue

          result mustBe Left(Unauthorized)
        }
      }

      "when both intermediary and ioss does not exist" in {

        when(mockIntermediaryRegistrationConnector.get(any())(any())) thenReturn
          intermediaryRegistrationWithClients(Nil).toFuture

        val application = applicationBuilder.build()

        running(application) {

          val request = buildRegistrationRequest(
            iossNumber = None,
            intermediaryNumber = None,
            enrolments = enrolmentsWithIntermediaries(Seq.empty)
          )

          val action = new Harness(None, mockIntermediaryRegistrationConnector, mockAppConfig)

          val result = action.callRefine(request).futureValue

          result mustBe Left(Unauthorized)
        }
      }
    }
  }
}