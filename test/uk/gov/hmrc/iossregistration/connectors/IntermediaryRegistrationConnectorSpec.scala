/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.iossregistration.connectors

import com.github.tomakehurst.wiremock.client.WireMock.*
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.Application
import play.api.libs.json.Json
import play.api.test.Helpers.running
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.iossregistration.base.BaseSpec
import uk.gov.hmrc.iossregistration.models.etmp.intermediary.IntermediaryRegistrationWrapper

class IntermediaryRegistrationConnectorSpec
  extends BaseSpec
    with WireMockHelper
    with ScalaCheckPropertyChecks {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private def application: Application = {
    applicationBuilder
      .configure("microservice.services.ioss-intermediary-registration.port" -> server.port)
      .build()
  }

  ".get" - {
    def url(intermediaryNumber: String) = s"/ioss-intermediary-registration/get-registration/$intermediaryNumber"

    "must return a registration when the server provides one" in {

      val app = application

      running(app) {
        val connector = app.injector.instanceOf[IntermediaryRegistrationConnector]
        val registration = arbitrary[IntermediaryRegistrationWrapper].sample.value

        val responseBody = Json.toJson(registration).toString

        server.stubFor(get(urlEqualTo(url(intermediaryNumber))).willReturn(ok().withBody(responseBody)))

        val result = connector.get(intermediaryNumber).futureValue

        result mustEqual registration
      }
    }

  }

}
