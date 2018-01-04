package io.sqooba

import dispatch.Req
import org.mockito.ArgumentMatcher
import org.scalatest.matchers.{MatchResult, Matcher}

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

  class MockitoReqMatcher(reqLeft: Req) extends ArgumentMatcher[Req] {
    override def matches(argument: scala.Any): Boolean = {
      if (argument.isInstanceOf[Req]) {
        val req = argument.asInstanceOf[Req]
        req.url.equals(reqLeft.url)
      } else {
        false
      }
    }
  }
}

object CustomMatchers extends CustomMatchers
