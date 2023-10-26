package uk.gov.hmrc.iossregistration.controllers

import org.scalatest.BeforeAndAfterEach
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.iossregistration.base.BaseSpec
import uk.gov.hmrc.iossregistration.repositories.RegistrationStatusRepository

class EnrolmentsSubscriptionControllerSpec extends BaseSpec with BeforeAndAfterEach {

  private val mockRegistrationStatusRepository = mock[RegistrationStatusRepository]


  override def beforeEach(): Unit = {
    reset(mockRegistrationStatusRepository)

    super.beforeEach()
  }

  "authoriseEnrolment" - {
    "must respond OK and set success when call body is successful" in {

      val subscriptionId = "subid-1"
      val successJson = """{"state": "SUCCEEDED"} """

      val app =
        applicationBuilder
          .overrides(bind[RegistrationStatusRepository].toInstance(mockRegistrationStatusRepository))
          .build()

      running(app) {

        val request =
          FakeRequest(POST, routes.EnrolmentsSubscriptionController.authoriseEnrolment(subscriptionId).url)
            .withJsonBody(Json.parse(successJson))

        val result = route(app, request).value

        status(result) mustEqual NO_CONTENT
      }
    }


  }

}
