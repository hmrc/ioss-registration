package uk.gov.hmrc.iossregistration.services.crypto

import play.api.Configuration
import play.api.test.Helpers.running
import uk.gov.hmrc.iossregistration.base.BaseSpec

class EncryptionServiceSpec extends BaseSpec {

  "EncryptionService" - {

    "must encrypt text value" in {

      val textToEncrypt: String = "Test String"

      val application = applicationBuilder.build()

      running(application) {

        val configuration: Configuration = application.configuration

        val service: EncryptionService = new EncryptionService(configuration)

        val result = service.encryptField(textToEncrypt)

        result mustBe a[String]
        result mustNot be(textToEncrypt)
      }
    }

    "must decrypt text value" in {

      val textToEncrypt: String = "Test String"

      val application = applicationBuilder.build()

      running(application) {

        val configuration: Configuration = application.configuration

        val service: EncryptionService = new EncryptionService(configuration)

        val encryptedValue = service.encryptField(textToEncrypt)

        val result = service.decryptField(encryptedValue)

        result mustBe a[String]
        result mustBe textToEncrypt
      }
    }

    "must throw a Security Exception if text value can't be decrypted" in {

      val textToEncrypt: String = "Test String"

      val application = applicationBuilder.build()

      running(application) {

        val configuration: Configuration = application.configuration

        val service: EncryptionService = new EncryptionService(configuration)

        val invalidEncryptedValue = service.encryptField(textToEncrypt) + "any"

        val result = intercept[SecurityException](service.decryptField(invalidEncryptedValue))
        result.getMessage mustBe "Unable to decrypt value"
      }
    }
  }
}
