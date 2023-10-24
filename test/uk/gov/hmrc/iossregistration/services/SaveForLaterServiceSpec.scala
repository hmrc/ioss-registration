package uk.gov.hmrc.iossregistration.services

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, verifyNoInteractions, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.iossregistration.connectors.GetVatInfoConnector
import uk.gov.hmrc.iossregistration.generators.Generators
import uk.gov.hmrc.iossregistration.models.{NotFound, SavedUserAnswers}
import uk.gov.hmrc.iossregistration.models.des.VatCustomerInfo
import uk.gov.hmrc.iossregistration.models.requests.{SaveForLaterRequest, SaveForLaterResponse}
import uk.gov.hmrc.iossregistration.repositories.SaveForLaterRepository

import scala.concurrent.ExecutionContext.Implicits.global
import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.Future


class SaveForLaterServiceSpec
  extends AnyFreeSpec
    with Matchers
    with MockitoSugar
    with ScalaCheckPropertyChecks
    with Generators
    with OptionValues
    with ScalaFutures {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  ".saveAnswers" - {

    "must create a SavedUserAnswers, attempt to save it to the repository, and respond with the result of saving" in {

      val now = Instant.now
      val stubClock = Clock.fixed(now, ZoneId.systemDefault())
      val answers = arbitrary[SavedUserAnswers].sample.value
      val insertResult = answers
      val mockSaveForLaterRepository = mock[SaveForLaterRepository]
      val mockGetVatInfoConnector = mock[GetVatInfoConnector]

      when(mockSaveForLaterRepository.set(any())) thenReturn Future.successful(insertResult)

      val request = arbitrary[SaveForLaterRequest].sample.value
      val service = new SaveForLaterService(mockSaveForLaterRepository, mockGetVatInfoConnector, stubClock)

      val result = service.saveAnswers(request).futureValue

      result mustEqual insertResult
      verify(mockSaveForLaterRepository, times(1)).set(any())
      verifyNoInteractions(mockGetVatInfoConnector)
    }
  }

  ".get" - {

    "must retrieve a Saved User Answers record when it is found with a VatCustomerInfo" in {
      val now = Instant.now
      val stubClock = Clock.fixed(now, ZoneId.systemDefault())
      val answers = arbitrary[SavedUserAnswers].sample.value
      val vatInfo = arbitrary[VatCustomerInfo].sample.value
      val mockRepository = mock[SaveForLaterRepository]
      val mockGetVatInfoConnector = mock[GetVatInfoConnector]

      val vrn = arbitrary[Vrn].sample.value

      when(mockRepository.get(any())) thenReturn Future.successful(Some(answers))
      when(mockGetVatInfoConnector.getVatCustomerDetails(any())(any())) thenReturn Future.successful(Right(vatInfo))

      val service = new SaveForLaterService(mockRepository, mockGetVatInfoConnector, stubClock)

      val result = service.get(vrn).futureValue
      result mustBe Some(SaveForLaterResponse(answers, vatInfo))
      verify(mockRepository, times(1)).get(vrn)
      verify(mockGetVatInfoConnector, times(1)).getVatCustomerDetails(answers.vrn)

    }

    "must fail if a VatCustomerInfo cannot be retrieved but a saved answers can" in {
      val now = Instant.now
      val stubClock = Clock.fixed(now, ZoneId.systemDefault())
      val answers = arbitrary[SavedUserAnswers].sample.value
      val mockRepository = mock[SaveForLaterRepository]
      val mockGetVatInfoConnector = mock[GetVatInfoConnector]

      val vrn = arbitrary[Vrn].sample.value

      when(mockRepository.get(any())) thenReturn Future.successful(Some(answers))
      when(mockGetVatInfoConnector.getVatCustomerDetails(any())(any())) thenReturn Future.successful(Left(NotFound))

      val service = new SaveForLaterService(mockRepository, mockGetVatInfoConnector, stubClock)

      val result = service.get(vrn).failed.futureValue
      result mustBe a[RuntimeException]
      verify(mockRepository, times(1)).get(vrn)
      verify(mockGetVatInfoConnector, times(1)).getVatCustomerDetails(answers.vrn)
    }
  }

  ".delete" - {

    "must delete a single Saved User Answers record" in {
      val now = Instant.now
      val stubClock = Clock.fixed(now, ZoneId.systemDefault())
      val mockRepository = mock[SaveForLaterRepository]
      val mockGetVatInfoConnector = mock[GetVatInfoConnector]
      val vrn = arbitrary[Vrn].sample.value

      when(mockRepository.clear(any())) thenReturn Future.successful(true)
      val service = new SaveForLaterService(mockRepository, mockGetVatInfoConnector, stubClock)

      val result = service.delete(vrn).futureValue
      result mustBe true
      verify(mockRepository, times(1)).clear(vrn)
      verifyNoInteractions(mockGetVatInfoConnector)

    }
  }
}

