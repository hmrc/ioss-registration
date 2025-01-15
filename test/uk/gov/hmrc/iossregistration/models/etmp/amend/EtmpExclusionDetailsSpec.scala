package uk.gov.hmrc.iossregistration.models.etmp.amend

import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.iossregistration.base.BaseSpec

import java.time.LocalDate

class EtmpExclusionDetailsSpec extends BaseSpec {

  private val revertExclusion = true
  private val noLongerSupplyGoods = false
  private val partyType = "NETP"
  private val exclusionRequestDate = Some(LocalDate.of(2023, 1, 1))
  private val identificationValidityDate = Some(LocalDate.of(2024, 1, 1))
  private val intExclusionRequestDate = None
  private val newMemberState = Some(EtmpNewMemberState(
    newMemberState = true,
    ceaseSpecialSchemeDate = Some(LocalDate.of(2023, 1, 1)),
    ceaseFixedEstDate = Some(LocalDate.of(2024, 1, 1)),
    movePOBDate = LocalDate.of(2024, 1, 1),
    issuedBy = "HMRC",
    vatNumber = "GB123456789"
  ))

  "EtmpExclusionDetails" - {

    "must deserialise/serialise to and from EtmpExclusionDetails" - {

      "when all optional values are present" in {

        val json = Json.obj(
          "exclusionRequestDate" -> exclusionRequestDate.map(_.toString),
          "newMemberState" -> Json.obj(
            "ceaseFixedEstDate" -> identificationValidityDate.map(_.toString),
            "newMemberState" -> true,
            "vatNumber" -> "GB123456789",
            "ceaseSpecialSchemeDate" -> exclusionRequestDate.map(_.toString),
            "movePOBDate" -> LocalDate.of(2024, 1, 1).toString,
            "issuedBy" -> "HMRC",
          ),
          "identificationValidityDate" -> identificationValidityDate.map(_.toString),
          "noLongerSupplyGoods" -> noLongerSupplyGoods,
          "revertExclusion" -> revertExclusion,
          "partyType" -> partyType,
        )

        val expectedResult = EtmpExclusionDetails(
          revertExclusion = revertExclusion,
          noLongerSupplyGoods = noLongerSupplyGoods,
          partyType = partyType,
          exclusionRequestDate = exclusionRequestDate,
          identificationValidityDate = identificationValidityDate,
          intExclusionRequestDate = intExclusionRequestDate,
          newMemberState = newMemberState
        )

        Json.toJson(expectedResult) mustBe json
        json.validate[EtmpExclusionDetails] mustBe JsSuccess(expectedResult)
      }

      "when all optional values are absent" in {

        val json = Json.obj(
          "revertExclusion" -> revertExclusion,
          "noLongerSupplyGoods" -> noLongerSupplyGoods,
          "partyType" -> partyType
        )

        val expectedResult = EtmpExclusionDetails(
          revertExclusion = revertExclusion,
          noLongerSupplyGoods = noLongerSupplyGoods,
          partyType = partyType,
          exclusionRequestDate = None,
          identificationValidityDate = None,
          intExclusionRequestDate = None,
          newMemberState = None
        )

        Json.toJson(expectedResult) mustBe json
        json.validate[EtmpExclusionDetails] mustBe JsSuccess(expectedResult)
      }
    }
  }
}
