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

package uk.gov.hmrc.iossregistration.config

import play.api.Configuration
import play.api.http.HeaderNames

import javax.inject.Inject

case class DisplayRegistrationConfig @Inject()(
                               config: Configuration,
                               genericConfig: EisGenericConfig
                             ) {

  val baseUrl: Service = config.get[Service]("microservice.services.display-registration")
  private val authorizationToken: String = config.get[String]("microservice.services.display-registration.authorizationToken")

  def eisEtmpGetHeaders(correlationId: String): Seq[(String, String)] = genericConfig.eisEtmpGenericHeaders(correlationId) ++ Seq(
    HeaderNames.AUTHORIZATION -> s"Bearer $authorizationToken"
  )
}
