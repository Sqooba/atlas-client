package io.sqooba.atlas

import io.sqooba.atlas.model.AtlasStatus
import io.sqooba.atlas.model.AtlasStatus.AtlasStatus
import org.json4s.{DefaultFormats, Formats}
import org.json4s.ext.EnumNameSerializer
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization.write
import org.scalatest.{FlatSpec, Matchers}

class AtlasUtilSpec extends FlatSpec with Matchers {

  case class TestCC(id: Int, atlasStatus: AtlasStatus)
  implicit val jsonFormats: Formats = DefaultFormats + new EnumNameSerializer(AtlasStatus)

  "json ser" should "serialize enum" in {
    val t1 = TestCC(1, AtlasStatus.Active)
    val json = write(t1)
    json.contains("ACTIVE") shouldBe true
  }

  "json" should "work" in {
    val jsonString = """ {"id":1,"atlasStatus":"ACTIVE"} """
    val json = parse(jsonString)
    val cc = json.extract[TestCC]
    cc.atlasStatus shouldBe AtlasStatus.Active
  }
}
