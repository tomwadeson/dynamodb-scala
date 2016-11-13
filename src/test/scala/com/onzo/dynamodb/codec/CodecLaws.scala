package com.onzo.dynamodb.codec

import cats.laws._
import com.onzo.dynamodb.{Encoder, Decoder}

trait CodecLaws[A] {
  def decoder: Decoder[A]

  def encoder: Encoder[A]

  val name = "name"

  def codecRoundTrip(a: A): IsEq[A] =
    decoder.decode(name, encoder.encode(name, a)) <-> a
}

object CodecLaws {
  def apply[A](implicit d: Decoder[A], e: Encoder[A]): CodecLaws[A] = new CodecLaws[A] {
    val decoder: Decoder[A] = d
    val encoder: Encoder[A] = e
  }
}
