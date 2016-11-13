package com.onzo.dynamodb

import java.util.UUID
import cats.functor.Contravariant
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import scala.collection.generic.IsTraversableOnce
import collection.JavaConverters._

trait Encoder[A] {
  //self =>
  def apply(a: A): AttributeValue

  def apply(name: String, a: A): Map[String, AttributeValue] = {
    Map(name -> apply(a))
  }

  def contramap[B](f: B => A): Encoder[B] = Encoder.createEncoder(b => apply(f(b)))
}

object Encoder {
  def apply[A](implicit e: Encoder[A]): Encoder[A] = e

  // `instance` is more idomatic here, but `createEncoder` is more readable for those not familiar with the type class pattern
  def createEncoder[A](f: A => AttributeValue): Encoder[A] = new Encoder[A] {
    def apply(a: A): AttributeValue = f(a)
  }

  implicit def encodeTraversableOnce[A0, C[_]](implicit
                                               e: Encoder[A0],
                                               is: IsTraversableOnce[C[A0]] {type A = A0}
                                              ): Encoder[C[A0]] =
    createEncoder { list =>
      val items = new java.util.ArrayList[AttributeValue]()

      is.conversion(list).foreach { a =>
        items add e(a)
      }

      new AttributeValue().withL(items)
    }

  implicit val encodeAttributeValue: Encoder[AttributeValue] = createEncoder(identity)
  implicit val encodeChar: Encoder[Char] = createEncoder(char => new AttributeValue().withS(char.toString))
  implicit val encodeString: Encoder[String] = createEncoder(new AttributeValue().withS(_))
  implicit val encodeBoolean: Encoder[Boolean] = createEncoder(new AttributeValue().withBOOL(_))
  implicit val encodeFloat: Encoder[Float] = createEncoder(a => new AttributeValue().withN(a.toString))
  implicit val encodeDouble: Encoder[Double] = createEncoder(a => new AttributeValue().withN(a.toString))
  implicit val encodeByte: Encoder[Byte] = createEncoder(a => new AttributeValue().withN(a.toString))
  implicit val encodeShort: Encoder[Short] = createEncoder(a => new AttributeValue().withN(a.toString))
  implicit val encodeInt: Encoder[Int] = createEncoder(a => new AttributeValue().withN(a.toString))
  implicit val encodeLong: Encoder[Long] = createEncoder(a => new AttributeValue().withN(a.toString))
  implicit val encodeBigInt: Encoder[BigInt] = createEncoder(a => new AttributeValue().withN(a.toString))
  implicit val encodeBigDecimal: Encoder[BigDecimal] = createEncoder(a => new AttributeValue().withN(a.toString))
  implicit val encodeUUID: Encoder[UUID] = createEncoder(uuid => new AttributeValue().withS(uuid.toString))

  implicit def encodeOption[A](implicit e: Encoder[A]): Encoder[Option[A]] = new Encoder[Option[A]] {
    // `get` is an unsafe operation; it may throw at runtime.  Here we use `withNULL` to represent the absence of a value instead.
    override def apply(a: Option[A]): AttributeValue =
      a.map(e(_))
       .getOrElse(new AttributeValue().withNULL(true))

    override def apply(name: String, a: Option[A]): Map[String, AttributeValue] =
      a.fold(Map.empty[String, AttributeValue])(x => Map(name -> e(x)))
  }


  implicit def encodeMapLike[M[K, +V] <: Map[K, V], V](implicit
                                                       e: Encoder[V]
                                                      ): Encoder[M[String, V]] = Encoder.createEncoder { m =>
    new AttributeValue().withM(m.mapValues(e(_)).asJava)
  }

  def encodeEither[A, B](leftKey: String, rightKey: String)(implicit
                                                            ea: Encoder[A],
                                                            eb: Encoder[B]
  ): Encoder[Either[A, B]] = createEncoder { a =>
    val map = new java.util.HashMap[String, AttributeValue]()
    a.fold(
      a => map.put(leftKey, ea(a)),
      b => map.put(rightKey, eb(b)))
    new AttributeValue().withM(map)
  }

  implicit val contravariantEncode: Contravariant[Encoder] = new Contravariant[Encoder] {
    def contramap[A, B](e: Encoder[A])(f: B => A): Encoder[B] = e.contramap(f)
  }

}
