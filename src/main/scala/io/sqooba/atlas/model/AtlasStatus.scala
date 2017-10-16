package io.sqooba.atlas.model

object AtlasStatus extends Enumeration {
  type AtlasStatus = Value
  val Active = Value("ACTIVE")
  val Disabled = Value("DISABLED")
}
