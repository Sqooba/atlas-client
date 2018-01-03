package io.sqooba

import dispatch.Req
import org.scalatest.matchers.{Matcher, MatchResult}

trait CustomMatchers {

  class ReqUrlMatcher(expectedUrl: String) extends Matcher[Req] {

    def apply(left: Req): MatchResult = {
      val url = left.url
      MatchResult(
        url.equals(expectedUrl),
        s"""Url: $url did not end match "$expectedUrl"""",
        s"""Url: $url matched "$expectedUrl""""
      )
    }
  }

  def matchUrl(expectedUrl: String): ReqUrlMatcher = new ReqUrlMatcher(expectedUrl)
}

object CustomMatchers extends CustomMatchers
