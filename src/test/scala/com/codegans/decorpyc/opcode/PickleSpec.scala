package com.codegans.decorpyc.opcode

import com.codegans.decorpyc.util.ByteSource
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class PickleSpec extends AnyFlatSpec with Matchers {

  behavior of "Pickle"

  it should "parse an empty dict into OpcodeRoot" in {
    // PROTO(0x80, 5), EMPTY_DICT(0x7d), STOP(0x2e)
    val bytes = Array[Byte](0x80.toByte, 0x05.toByte, 0x7d.toByte, 0x2e.toByte)

    val root = Pickle(ByteSource(bytes))

    root.attributes shouldBe Map.empty
    root.children shouldBe Nil
  }

  it should "fail on unknown opcode" in {
    // 0x00 is not a valid opcode in the table
    val bytes = Array[Byte](0x00.toByte)

    an[IllegalArgumentException] shouldBe thrownBy {
      Pickle(ByteSource(bytes))
    }
  }

  it should "fail on opcode incompatible with the declared protocol" in {
    // PROTO(0x80, 1), NEWOBJ(0x81) - NEWOBJ requires protocol >= 2
    val bytes = Array[Byte](0x80.toByte, 0x01.toByte, 0x81.toByte)

    an[IllegalArgumentException] shouldBe thrownBy {
      Pickle(ByteSource(bytes))
    }
  }

  it should "fail on unexpected root format when top-level is not a dict" in {
    // PROTO(0x80, 5), SHORT_BINUNICODE(0x8c, len=3, 'a','b','c'), STOP(0x2e)
    val bytes = Array[Byte](
      0x80.toByte, 0x05.toByte,
      0x8c.toByte, 0x03.toByte, 'a'.toByte, 'b'.toByte, 'c'.toByte,
      0x2e.toByte
    )

    an[IllegalArgumentException] shouldBe thrownBy {
      Pickle(ByteSource(bytes))
    }
  }
}
