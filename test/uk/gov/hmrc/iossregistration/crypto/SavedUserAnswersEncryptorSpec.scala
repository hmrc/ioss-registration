package uk.gov.hmrc.iossregistration.crypto

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.libs.json.Json
import uk.gov.hmrc.iossregistration.base.BaseSpec
import uk.gov.hmrc.iossregistration.config.AppConfig
import uk.gov.hmrc.iossregistration.models.{LegacyEncryptedSavedUserAnswers, NewEncryptedSavedUserAnswers, SavedUserAnswers}
import uk.gov.hmrc.iossregistration.services.crypto.EncryptionService

class SavedUserAnswersEncryptorSpec extends BaseSpec {

  private val mockAppConfig: AppConfig = mock[AppConfig]
  private val mockEncryptionService: EncryptionService = mock[EncryptionService]
  private val mockSecureGCMCipher: SecureGCMCipher = mock[SecureGCMCipher]

  private val savedUserAnswers: SavedUserAnswers = arbitrarySavedUserAnswers.arbitrary.sample.value
  private val encryptedValue: String = "encryptedValue"

  "SavedUserAnswersEncryptor" - {

    "must encrypt and return a EncryptedSavedUserAnswers" in {

      when(mockEncryptionService.encryptField(any())) thenReturn encryptedValue

      val service = new SavedUserAnswersEncryptor(mockAppConfig, mockEncryptionService, mockSecureGCMCipher)

      val expectedResult = NewEncryptedSavedUserAnswers(savedUserAnswers.vrn, encryptedValue, savedUserAnswers.lastUpdated)
      val result = service.encryptAnswers(savedUserAnswers, savedUserAnswers.vrn)

      result mustBe expectedResult
    }

    "must decrypt and return a SavedUserAnswers" in {

      when(mockEncryptionService.decryptField(any())) thenReturn Json.parse(savedUserAnswers.data.toString()).toString()

      val service = new SavedUserAnswersEncryptor(mockAppConfig, mockEncryptionService, mockSecureGCMCipher)

      val encryptedSavedUserAnswers = NewEncryptedSavedUserAnswers(savedUserAnswers.vrn, encryptedValue, savedUserAnswers.lastUpdated)

      val expectedResult = savedUserAnswers
      val result = service.decryptAnswers(encryptedSavedUserAnswers, encryptedSavedUserAnswers.vrn)

      result mustBe expectedResult
    }

    "must decrypt a LegacyEncryptedSavedUserAnswers and return a SavedUserAnswers" in {

      when(mockSecureGCMCipher.decrypt(any(), any(), any())) thenReturn Json.parse(savedUserAnswers.data.toString()).toString()

      val service = new SavedUserAnswersEncryptor(mockAppConfig, mockEncryptionService, mockSecureGCMCipher)

      val encryptedValue: EncryptedValue = EncryptedValue(value = "testValue", nonce = "testNonce")

      val encryptedSavedUserAnswers = LegacyEncryptedSavedUserAnswers(savedUserAnswers.vrn, encryptedValue, savedUserAnswers.lastUpdated)

      val expectedResult = savedUserAnswers
      val result = service.decryptLegacyAnswers(encryptedSavedUserAnswers, encryptedSavedUserAnswers.vrn)

      result mustBe expectedResult
    }
  }
}
