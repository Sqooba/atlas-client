package io.sqooba.atlas

import io.sqooba.ExternalSpec
import org.scalatest.{AsyncFlatSpec, Matchers}

class AtlasClientE2eSpec extends AsyncFlatSpec with Matchers {

  val aClient = new AtlasClient(new AtlasClientWrapper(), "http://dopintambari03.node.pmiint.ocean:21000")

  "search entities" should "return SearchResult" taggedAs (ExternalSpec) in {
    aClient.searchEntities("kafka_topic", "name=\"toptoptop\"").map(res => {
      println(res)
      1 shouldBe 1
    })
  }
}
