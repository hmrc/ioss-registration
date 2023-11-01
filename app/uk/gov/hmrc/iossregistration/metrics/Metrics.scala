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

package uk.gov.hmrc.iossregistration.metrics

import com.codahale.metrics.{MetricRegistry, Timer}
import com.codahale.metrics.Timer.Context
import com.kenshoo.play.metrics.Metrics
import uk.gov.hmrc.iossregistration.metrics.MetricsEnum.MetricsEnum

import javax.inject.Inject

class DefaultServiceMetrics @Inject()(val metrics: Metrics) extends ServiceMetrics

trait ServiceMetrics {
  val metrics: Metrics

  def startTimer(api: MetricsEnum): Context = timers(api).time()

  val registry: MetricRegistry = metrics.defaultRegistry
  val timers: Map[MetricsEnum, Timer] = Map(
    MetricsEnum.GetRegistration -> registry.timer("get-registration-response-timer"),
    MetricsEnum.ValidateCoreRegistration -> registry.timer("validate-core-registration-response-timer"),
    MetricsEnum.GetVatCustomerDetails -> registry.timer("get-vat-customer-details-response-timer"),
    MetricsEnum.ConfirmEnrolment -> registry.timer("confirm-enrolment-response-timer")
  )
}