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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.iossregistration.config.AppConfig
import uk.gov.hmrc.iossregistration.models.RegistrationStatus
import uk.gov.hmrc.iossregistration.models.etmp.EtmpRegistrationStatus
import uk.gov.hmrc.iossregistration.repositories.InsertResult.{AlreadyExists, InsertSucceeded}
import uk.gov.hmrc.mongo.test.{CleanMongoCollectionSupport, PlayMongoRepositorySupport}

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext.Implicits.global

class RegistrationStatusRepositorySpec extends AnyFreeSpec
  with Matchers
  with CleanMongoCollectionSupport
  with PlayMongoRepositorySupport[RegistrationStatus]
  with ScalaFutures
  with IntegrationPatience
  with OptionValues
  with MockitoSugar {

  private val appConfig = mock[AppConfig]
  protected val repository: RegistrationStatusRepository =
    new RegistrationStatusRepository(
      mongoComponent = mongoComponent,
      appConfig = appConfig
    )

  val registrationStatus: RegistrationStatus = RegistrationStatus("100000001-id",
    EtmpRegistrationStatus.Success, Instant.now.truncatedTo(ChronoUnit.MILLIS))

  ".insert" - {

    "must insert data" in {

      val insertResult = repository.insert(registrationStatus).futureValue
      val databaseRecord = findAll().futureValue.headOption.value

      insertResult mustEqual InsertSucceeded
      databaseRecord mustEqual registrationStatus
    }

    "must not allow duplicate data to be inserted" in {

      repository.insert(registrationStatus).futureValue
      val secondResult = repository.insert(registrationStatus).futureValue

      secondResult mustEqual AlreadyExists
    }
  }

  ".set registration status" - {

    "must update the existing status" in {

      repository.insert(registrationStatus).futureValue
      val updatedRegistrationStatus = registrationStatus.copy(status = EtmpRegistrationStatus.Pending)

      val updatedResult = repository.set(updatedRegistrationStatus).futureValue

      updatedResult mustEqual updatedRegistrationStatus
    }
  }

  ".get one" - {

    "must return Saved record when one exists for this subscription id" in {

      insert(registrationStatus).futureValue

      val result = repository.get(registrationStatus.subscriptionId).futureValue

      result.value mustEqual registrationStatus
    }

    "must return None when no data exists" in {

      val result = repository.get("100000002-id").futureValue

      result must not be defined
    }
  }

  ".delete" - {

    "must return true when saved record is deleted" in {

      insert(registrationStatus).futureValue

      val result = repository.delete(registrationStatus.subscriptionId).futureValue

      result mustEqual true

      val currentAnswers = repository.get(registrationStatus.subscriptionId).futureValue
      currentAnswers must not be defined
    }
  }
}
