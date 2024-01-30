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

package uk.gov.hmrc.iossregistration.controllers.external

import play.api.libs.json.JsValue
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.iossregistration.models.external.Event
import uk.gov.hmrc.iossregistration.services.ChannelPreferenceService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class EventController @Inject()(
                                 cc: ControllerComponents,
                                 preferenceChannelService: ChannelPreferenceService
                               )(implicit ec: ExecutionContext) extends BackendController(cc) {

  def processBouncedEmailEvent(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[Event](event =>
      preferenceChannelService.updatePreferences(event).map {
        case true => NoContent
        case _ => InternalServerError("Error downstream on preference channel service")
      }
    )

  }

}
