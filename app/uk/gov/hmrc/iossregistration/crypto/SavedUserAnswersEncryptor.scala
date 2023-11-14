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
import uk.gov.hmrc.iossregistration.models._

import javax.inject.Inject


class SavedUserAnswersEncryptor @Inject()(
                                           crypto: SecureGCMCipher
                                         ) {

  def encryptAnswers(answers: SavedUserAnswers, vrn: Vrn, key: String): EncryptedSavedUserAnswers = {
    def encryptAnswerValue(answerValue: String): EncryptedValue =
      crypto.encrypt(answerValue, vrn.vrn, key)

    EncryptedSavedUserAnswers(
      vrn = vrn,
      data = encryptAnswerValue(answers.data.toString),
      lastUpdated = answers.lastUpdated
    )
  }

  def decryptAnswers(encryptedAnswers: EncryptedSavedUserAnswers, vrn: Vrn, key: String): SavedUserAnswers = {
    def decryptValue(encryptedValue: EncryptedValue): String = crypto.decrypt(encryptedValue, vrn.vrn, key)

    SavedUserAnswers(
      vrn = vrn,
      data = Json.parse(decryptValue(encryptedAnswers.data)),
      lastUpdated = encryptedAnswers.lastUpdated
    )
  }
}