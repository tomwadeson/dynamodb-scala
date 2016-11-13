package com.onzo.dynamodb

import java.math.MathContext
import java.util.UUID

import cats.Monad
import com.amazonaws.services.dynamodbv2.model.AttributeValue

import scala.collection.generic.CanBuildFrom
import scala.util.Try

trait Decoder[A] {
  self =>
  def apply(c: AttributeValue): A

  def apply(name: String, items: Map[String, AttributeValue]): A = {
    val vOpt = items.get(name)
    vOpt.fold(
      throw new Exception(s"Attribute '$name' not found in '$items'")
    )(
      v => apply(v)
    )
  }

  def map[B](f: A => B): Decoder[B] = new Decoder[B] {
    def apply(c: AttributeValue): B = f(self(c))
  }

  def flatMap[B](f: A => Decoder[B]): Decoder[B] = new Decoder[B] {
    def apply(c: AttributeValue): B = {
      f(self(c))(c)
    }
  }
}

object Decoder {
  def apply[A](implicit d: Decoder[A]): Decoder[A] = d

  // `instance` is more idomatic here, but `createDecoder` is more readable for those not familiar with the type class pattern
  def createDecoder[A](f: AttributeValue => A): Decoder[A] = new Decoder[A] {
    def apply(c: AttributeValue): A = f(c)
  }

  implicit val decodeAttributeValue: Decoder[AttributeValue] = createDecoder(identity)
  implicit val decodeChar: Decoder[Char] = createDecoder(_.getS.charAt(0))
  implicit val decodeString: Decoder[String] = createDecoder(_.getS)
  implicit val decodeBoolean: Decoder[Boolean] = createDecoder(_.getBOOL)
  implicit val decodeFloat: Decoder[Float] = createDecoder(_.getN.toFloat)
  implicit val decodeDouble: Decoder[Double] = createDecoder(_.getN.toDouble)
  implicit val decodeByte: Decoder[Byte] = createDecoder(_.getN.toByte)
  implicit val decodeShort: Decoder[Short] = createDecoder(_.getN.toShort)
  implicit val decodeInt: Decoder[Int] = createDecoder(_.getN.toInt)
  implicit val decodeLong: Decoder[Long] = createDecoder(_.getN.toLong)
  implicit val decodeBigInt: Decoder[BigInt] = createDecoder(a => BigDecimal(a.getN, MathContext.UNLIMITED).toBigInt())
  implicit val decodeBigDecimal: Decoder[BigDecimal] = createDecoder(a => BigDecimal(a.getN, MathContext.UNLIMITED))
  implicit val decodeUUID: Decoder[UUID] = createDecoder(a => UUID.fromString(a.getS))

  implicit def decodeCanBuildFrom[A, C[_]](implicit
                                           d: Decoder[A],
                                           cbf: CanBuildFrom[Nothing, A, C[A]]
                                          ): Decoder[C[A]] = createDecoder { c =>
    import scala.collection.JavaConversions._

    val list = c.getL
    val builder = cbf()
    for {
      e <- list
    } yield {
      builder += d(e)
    }
    builder.result()
  }

  implicit def decodeOption[A](implicit d: Decoder[A]): Decoder[Option[A]] = new Decoder[Option[A]] {
    override def apply(c: AttributeValue): Option[A] = Try(d(c)).toOption
    override def apply(name: String, items: Map[String, AttributeValue]): Option[A] = {
      items.get(name).flatMap(apply)
    }
  }

  /**
    * @group Decoding
    */
  implicit def decodeMap[M[K, +V] <: Map[K, V], V](implicit
                                                   d: Decoder[V],
                                                   cbf: CanBuildFrom[Nothing, (String, V), M[String, V]]
                                                  ): Decoder[M[String, V]] = createDecoder { c =>
    import scala.collection.JavaConversions._
    val map = c.getM
    val builder = cbf()
    for {
      (k, v) <- map
    } yield {
      builder += k -> d(v)
    }
    builder.result()
  }

  implicit val monadDecode: Monad[Decoder] = new Monad[Decoder] {
    def pure[A](a: A): Decoder[A] = createDecoder(_ => a)

    def flatMap[A, B](fa: Decoder[A])(f: A => Decoder[B]): Decoder[B] = fa.flatMap(f)
  }
}