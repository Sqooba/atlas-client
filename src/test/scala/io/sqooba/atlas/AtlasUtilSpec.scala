package io.sqooba.atlas

import scala.concurrent.Future

import dispatch.{Req, url}
import io.sqooba.atlas.model.AtlasStatus
import io.sqooba.atlas.model.AtlasStatus.AtlasStatus
import io.sqooba.CustomMatchers._
import io.sqooba.conf.EnvUtil
import org.json4s.{DefaultFormats, Formats}
import org.json4s.ext.EnumNameSerializer
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization.write
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}

class AtlasUtilSpec extends FlatSpec with Matchers with MockitoSugar {

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

  val mock: AtlasClientWrapper = mock[AtlasClientWrapper]

  "req matcher works with mockito" should "do match correct req" in {
    reset(mock)
    val reqUrl: String = s"http://baseurl/guid"
    val req: Req = url(reqUrl).GET

    when(mock.queryAtlas(argThat(new MockitoReqMatcher(req)))).thenReturn(Future.successful(None))
    mock.queryAtlas(req)
    verify(mock, times(1)).queryAtlas(argThat(new MockitoReqMatcher(req)))
  }

  "req matcher works with mockito" should "not match other req" in {
    reset(mock)
    val reqUrl: String = s"http://baseurl/guid"
    val otherReqUrl: String = s"http://baseurl/otherurl"
    val req: Req = url(reqUrl).GET
    val otherReq: Req = url(otherReqUrl).GET

    when(mock.queryAtlas(argThat(new MockitoReqMatcher(req)))).thenReturn(Future.successful(None))
    mock.queryAtlas(otherReq)
    verify(mock, times(0)).queryAtlas(argThat(new MockitoReqMatcher(req)))
  }

  "req matcher" should "count correct amount of calls" in {
    reset(mock)
    val reqUrl: String = s"http://baseurl/guid"
    val otherReqUrl: String = s"http://baseurl/otherurl"
    val req: Req = url(reqUrl).GET
    val otherReq: Req = url(otherReqUrl).GET

    when(mock.queryAtlas(argThat(new MockitoReqMatcher(req)))).thenReturn(Future.successful(None))
    mock.queryAtlas(otherReq)
    mock.queryAtlas(req)
    mock.queryAtlas(otherReq)
    mock.queryAtlas(req)
    verify(mock, times(2)).queryAtlas(argThat(new MockitoReqMatcher(req)))
  }
}
