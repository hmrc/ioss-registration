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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.iossregistration.connectors.EnrolmentsConnector
import uk.gov.hmrc.iossregistration.models.enrolments.{EACDEnrolment, EACDEnrolments, EACDIdentifiers}

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AccountServiceSpec extends AnyFreeSpec with MockitoSugar with ScalaFutures with Matchers with BeforeAndAfterEach {
  private val mockEnrolmentsConnector = mock[EnrolmentsConnector]
  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  override def beforeEach() = {
    reset(mockEnrolmentsConnector)
  }

  ".AccountService" - {

    "return the latest account IOSS number when enrolments exist and activation dates are available" in {

      val enrolments = Seq(
        EACDEnrolment(
          service = "HMRC-IOSS-ORG",
          state = "Activated",
          activationDate = Some(LocalDateTime.parse("2023-01-01T10:00:00")),
          identifiers = Seq(EACDIdentifiers("IOSSNumber", "123456"))
        ),
        EACDEnrolment(
          service = "HMRC-IOSS-ORG",
          state = "Activated",
          activationDate = Some(LocalDateTime.parse("2023-02-01T10:00:00")),
          identifiers = Seq(EACDIdentifiers("IOSSNumber", "654321"))
        )
      )

      when(mockEnrolmentsConnector.es2(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(EACDEnrolments(enrolments))))

      val service = new AccountService(mockEnrolmentsConnector)

      service.getLatestAccount("userId").map { result =>
        result mustBe Some("654321")
      }
    }

    "return None when no enrolments are available" in {

      when(mockEnrolmentsConnector.es2(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(EACDEnrolments(Seq.empty))))

      val service = new AccountService(mockEnrolmentsConnector)

      service.getLatestAccount("userId").map { result =>
        result mustBe None
      }
    }

    "return None when activation dates are not defined" in {

      val enrolments = Seq(
        EACDEnrolment(
          service = "HMRC-IOSS-ORG",
          state = "Activated",
          activationDate = None,
          identifiers = Seq(EACDIdentifiers("IOSSNumber", "123456"))
        )
      )

      when(mockEnrolmentsConnector.es2(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(EACDEnrolments(enrolments))))

      val service = new AccountService(mockEnrolmentsConnector)

      service.getLatestAccount("userId").map { result =>
        result mustBe None
      }
    }

    "return None when ES2 call fails" in {

      when(mockEnrolmentsConnector.es2(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left("Error")))

      val service = new AccountService(mockEnrolmentsConnector)

      service.getLatestAccount("userId").map { result =>
        result mustBe None
      }
    }

    "return None if no identifier with key 'IOSSNumber' is found" in {

      val enrolments = Seq(
        EACDEnrolment(
          service = "HMRC-IOSS-ORG",
          state = "Activated",
          activationDate = Some(LocalDateTime.parse("2023-01-01T10:00:00")),
          identifiers = Seq(EACDIdentifiers("SomeOtherKey", "999999"))
        )
      )

      when(mockEnrolmentsConnector.es2(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(EACDEnrolments(enrolments))))

      val service = new AccountService(mockEnrolmentsConnector)

      service.getLatestAccount("userId").map { result =>
        result mustBe None
      }
    }
  }
}
