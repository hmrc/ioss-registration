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

package uk.gov.hmrc.iossregistration.repositories

import com.typesafe.config.Config
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.Configuration
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.iossregistration.config.AppConfig
import uk.gov.hmrc.iossregistration.crypto.{SavedUserAnswersEncryptor, AesGCMCrypto}
import uk.gov.hmrc.iossregistration.models.{EncryptedSavedUserAnswers, LegacyEncryptedSavedUserAnswers, NewEncryptedSavedUserAnswers, SavedUserAnswers}
import uk.gov.hmrc.iossregistration.services.crypto.EncryptionService
import uk.gov.hmrc.mongo.test.{CleanMongoCollectionSupport, DefaultPlayMongoRepositorySupport}

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.ExecutionContext.Implicits.global

class SaveForLaterRepositorySpec
  extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[EncryptedSavedUserAnswers]
    with CleanMongoCollectionSupport
    with ScalaFutures
    with IntegrationPatience
    with OptionValues {

  private val mockAppConfig = mock[AppConfig]
  private val mockConfiguration = mock[Configuration]
  private val mockConfig = mock[Config]
  private val secureGcmCipher: AesGCMCrypto = new AesGCMCrypto
  private val encryptionService: EncryptionService = new EncryptionService(mockConfiguration)
  private val encryptor = new SavedUserAnswersEncryptor(mockAppConfig, encryptionService, secureGcmCipher)
  private val secretKey: String = "VqmXp7yigDFxbCUdDdNZVIvbW6RgPNJsliv6swQNCL8="

  private val instant = Instant.now
  private val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

  implicit val arbitraryVrn: Arbitrary[Vrn] =
    Arbitrary {
      Gen.listOfN(9, Gen.numChar).map(_.mkString).map(Vrn)
    }

  implicit val arbitrarySavedUserAnswers: Arbitrary[SavedUserAnswers] =
    Arbitrary {
      for {
        vrn <- arbitrary[Vrn]
        data = JsObject(Seq(
          "test" -> Json.toJson("test")
        ))
        now = instant
      } yield SavedUserAnswers(
        vrn = vrn,
        data = data,
        lastUpdated = now
      )
    }

  override protected val repository: SaveForLaterRepository =
    new SaveForLaterRepository(
      mongoComponent = mongoComponent,
      encryptor = encryptor,
      appConfig = mockAppConfig
    )

  when(mockConfiguration.underlying) thenReturn mockConfig
  when(mockConfig.getString(any())) thenReturn secretKey
  when(mockAppConfig.encryptionKey) thenReturn secretKey

  ".set savedAnswers" - {

    "must insert saved answers for different VRNs" in {

      def rotateDigitsInString(chars: String): String =
        chars.map {
          char =>
            (char.asDigit + 1) % 10
        }.mkString

      val answers = arbitrary[SavedUserAnswers].sample.value
      val answers1 = answers copy (lastUpdated = Instant.now(stubClock).truncatedTo(ChronoUnit.MILLIS))
      val vrn2 = Vrn(rotateDigitsInString(answers1.vrn.vrn).mkString)
      val answers2 = answers1.copy(
        vrn = vrn2,
        lastUpdated = Instant.now(stubClock).truncatedTo(ChronoUnit.MILLIS)
      )

      val insertResult1 = repository.set(answers1).futureValue
      val insertReturn2 = repository.set(answers2).futureValue
      val databaseRecords = findAll().futureValue
      val decryptedDatabaseRecords =
        databaseRecords.map(e => determineEncryptionType(e))

      insertResult1 mustBe (answers1)
      insertReturn2 mustBe (answers2)
      decryptedDatabaseRecords must contain theSameElementsAs Seq(answers1, answers2)
    }

    "must replace saved answers with the same VRN" in {

      val answers = arbitrary[SavedUserAnswers].sample.value
      val answers2 = answers.copy(lastUpdated = Instant.now(stubClock).truncatedTo(ChronoUnit.MILLIS))
      val insertResult1 = repository.set(answers).futureValue
      val insertResult2 = repository.set(answers2).futureValue

      insertResult1 mustBe answers
      insertResult2 mustBe answers2

      val decryptedDatabaseRecords =
        findAll().futureValue.map(e => determineEncryptionType(e))

      decryptedDatabaseRecords must contain only answers2
    }
  }

  ".get one" - {

    "must return Saved answers record when one exists for this VRN" in {

      val answers1 = arbitrary[SavedUserAnswers].sample.value

      val answers = answers1.copy(lastUpdated = Instant.now(stubClock).truncatedTo(ChronoUnit.MILLIS))

      insert(encryptor.encryptAnswers(answers, answers.vrn)).futureValue

      val result = repository.get(answers.vrn).futureValue

      result.value mustEqual answers
    }

    "must return None when a return does not exist for this VRN" in {

      val vrn = arbitrary[Vrn].sample.value

      val result = repository.get(vrn).futureValue

      result must not be defined
    }
  }

  ".clear" - {

    "must return true when Saved Answers Record is deleted" in {

      val answers = arbitrary[SavedUserAnswers].sample.value

      insert(encryptor.encryptAnswers(answers, answers.vrn)).futureValue

      val result = repository.clear(answers.vrn).futureValue

      result mustEqual true

      val currentAnswers = repository.get(answers.vrn).futureValue
      currentAnswers must not be defined
    }
  }

  private def determineEncryptionType(answers: EncryptedSavedUserAnswers): SavedUserAnswers = {
    answers match {
      case l: LegacyEncryptedSavedUserAnswers =>
        encryptor.decryptLegacyAnswers(l, l.vrn)
      case n: NewEncryptedSavedUserAnswers =>
        encryptor.decryptAnswers(n, n.vrn)
      case _ => throw new IllegalArgumentException("Not a valid EncryptedSavedUserAnswers type.")
    }
  }
}



