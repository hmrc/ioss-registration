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

package uk.gov.hmrc.iossregistration.controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.iossregistration.base.BaseSpec
import uk.gov.hmrc.iossregistration.connectors.ValidateCoreRegistrationConnector
import uk.gov.hmrc.iossregistration.models.core.*

import java.time.LocalDate
import scala.concurrent.Future

class ValidateCoreRegistrationControllerSpec extends BaseSpec {

  "post" - {

    val coreValidationRequest = CoreRegistrationRequest(SourceType.VATNumber.toString, None, vrn.vrn, None, "GB")

    val coreRegistrationValidationResult: CoreRegistrationValidationResult =
      CoreRegistrationValidationResult(
        "IM2344433220",
        Some("IN4747493822"),
        "FR",
        true,
        Seq(Match(
          TraderId("IM0987654321"),
          Some("444444444"),
          "DE",
          Some(3),
          Some(LocalDate.now().format(Match.dateFormatter)),
          Some(LocalDate.now().format(Match.dateFormatter)),
          Some(1),
          Some(2)
        ))
      )
    "must return 200 when returning a match" in {

      val mockConnector = mock[ValidateCoreRegistrationConnector]
      when(mockConnector.validateCoreRegistration(any())) thenReturn Future.successful(Right(coreRegistrationValidationResult))

      val app =
        applicationBuilder
          .overrides(bind[ValidateCoreRegistrationConnector].toInstance(mockConnector))
          .build()

      running(app) {

        val request =
          FakeRequest(POST, routes.ValidateCoreRegistrationController.post().url)
            .withJsonBody(Json.toJson(coreValidationRequest))

        val result = route(app, request).value

        status(result) mustEqual OK
      }
    }

    "must return 400 when the JSON request payload is not valid" in {

      val invalidJson = "{}"

      val app = applicationBuilder.build()

      running(app) {

        val request =
          FakeRequest(POST, routes.ValidateCoreRegistrationController.post().url)
            .withJsonBody(Json.toJson(invalidJson))

        val result = route(app, request).value

        status(result) mustEqual BAD_REQUEST
      }
    }

  }
}
