package uk.gov.hmrc.iossregistration.models.etmp

import org.scalacheck.Arbitrary.arbitrary
import play.api.libs.json.{Json, JsSuccess}
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.iossregistration.base.BaseSpec

import java.time.LocalDateTime

class EtmpEnrolmentResponseSpec extends BaseSpec {

  private val processingDateTime = LocalDateTime.now()
  private val formBundleNumber = arbitrary[String].sample.value
  private val genVrn = arbitrary[Vrn].sample.value
  private val iossReference = arbitrary[TaxRefTraderID].sample.value
  private val businessPartner = arbitrary[String].sample.value

  "EtmpEnrolmentResponse" - {

    "must deserialise/serialise to and from EtmpEnrolmentResponse" - {

      "when all values are present" in {

        val json = Json.obj(
          "processingDateTime" -> processingDateTime,
          "formBundleNumber" -> formBundleNumber,
          "vrn" -> genVrn.vrn,
          "iossReference" -> iossReference.taxReferenceNumber,
          "businessPartner" -> businessPartner
        )

        val expectedResult = EtmpEnrolmentResponse(
          processingDateTime = processingDateTime,
          formBundleNumber = formBundleNumber,
          vrn = genVrn.vrn,
          iossReference = iossReference.taxReferenceNumber,
          businessPartner = businessPartner
        )

        Json.toJson(expectedResult) mustBe json
        json.validate[EtmpEnrolmentResponse] mustBe JsSuccess(expectedResult)
      }

    }
  }
}
