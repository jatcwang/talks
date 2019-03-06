package com.example
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

final case class Person(
  id: PersonId,
  name: String,
  hobbies: List[Hobby]
)

object Person {
//  implicitly[Encoder[PersonId]]
//  implicitly[Encoder[String]]
//  implicitly[Encoder[List[Hobby]]]
  implicit val encoder: Encoder[Person] = deriveEncoder
}

final case class PersonId(str: String)

object PersonId {
  implicit val encoder: Encoder[PersonId] = deriveEncoder
}

final case class Hobby(name: String, level: Int)

object Hobby {
  implicit val encoder: Encoder[Hobby] = deriveEncoder
}
