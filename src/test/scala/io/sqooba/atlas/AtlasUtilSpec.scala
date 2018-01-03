package io.sqooba.atlas

import dispatch.{url, Req}
import io.sqooba.atlas.model.AtlasStatus
import io.sqooba.atlas.model.AtlasStatus.AtlasStatus
import io.sqooba.CustomMatchers._
import io.sqooba.conf.EnvUtil
import org.json4s.{DefaultFormats, Formats}
import org.json4s.ext.EnumNameSerializer
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization.write
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
    val json = parse(jsonString, true)
    val cc = json.extract[TestCC]
    cc.atlasStatus shouldBe AtlasStatus.Active
  }

  "test reading env" should "give value from env" in {
    val testUrl = "http://somenonsense.url"
    EnvUtil.setEnv("ATLAS_BASEURL", testUrl)

    val client = new AtlasClient()
    client.atlasBaseUrl shouldBe testUrl

    EnvUtil.removeEnv("ATLAS_BASEURL")
  }

  "req matcher" should "match successfully" in {
    val reqUrl: String = s"http://baseurl/guid"
    val req: Req = url(reqUrl).GET
    req should matchUrl (reqUrl)
  }

  "req matcher" should "not match different url" in {
    val reqUrl: String = s"http://baseurl/guid"
    val failedUrl: String = s"http://baseurl/guid/other"
    val req: Req = url(reqUrl).GET
    req should not (matchUrl (failedUrl))
  }
}
