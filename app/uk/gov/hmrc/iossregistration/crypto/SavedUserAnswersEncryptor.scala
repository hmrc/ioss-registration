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

package uk.gov.hmrc.iossregistration.crypto

import play.api.libs.json.Json
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.iossregistration.config.AppConfig
import uk.gov.hmrc.iossregistration.models._
import uk.gov.hmrc.iossregistration.services.crypto.EncryptionService

import javax.inject.Inject


class SavedUserAnswersEncryptor @Inject()(
                                           appConfig: AppConfig,
                                           encryptionService: EncryptionService,
                                           secureGCMCipher: AesGCMCrypto
                                         ) {

  protected val key: String = appConfig.encryptionKey

  def encryptAnswers(answers: SavedUserAnswers, vrn: Vrn): NewEncryptedSavedUserAnswers = {
    def encryptAnswerValue(answerValue: String): String = encryptionService.encryptField(answerValue)

    NewEncryptedSavedUserAnswers(
      vrn = vrn,
      data = encryptAnswerValue(answers.data.toString),
      lastUpdated = answers.lastUpdated
    )
  }

  def decryptAnswers(encryptedAnswers: NewEncryptedSavedUserAnswers, vrn: Vrn): SavedUserAnswers = {
    def decryptValue(encryptedValue: String): String = encryptionService.decryptField(encryptedValue)

    SavedUserAnswers(
      vrn = vrn,
      data = Json.parse(decryptValue(encryptedAnswers.data)),
      lastUpdated = encryptedAnswers.lastUpdated
    )
  }

  def decryptLegacyAnswers(encryptedAnswers: LegacyEncryptedSavedUserAnswers, vrn: Vrn): SavedUserAnswers = {
    def decryptValue(encryptedValue: EncryptedValue): String = secureGCMCipher.decrypt(encryptedValue, vrn.vrn, key)

    SavedUserAnswers(
      vrn = vrn,
      data = Json.parse(decryptValue(encryptedAnswers.data)),
      lastUpdated = encryptedAnswers.lastUpdated
    )
  }
}