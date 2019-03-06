---
author: Jacob Wang
title: Scala Implicits 101
---

## This talk

- What are implicits? Why are they useful?
- How does the compiler resolve implicits
- Best practices & tips

## Why Implicits?

- Reduce boilerplate in everyday code
- Typeclasses
- Extension methods
- Clean **Domain Specific Languages** (DSL)

## What are implicits

Filling in parameters and function calls for you,
implicitly!

> - Two types
>   - Implicit Parameters
>   - Implicit Conversions
- Everything resolved at compile time

## Implicit Parameters

Parameters the compiler finds and fills in for you

```tut:silent
def addThem(a: Int)(implicit b: Int): Int = {
  a + b
}

implicit val i: Int = 3
addThem(1) // 4

```

At runtime, the JVM sees `addThem(a: Int, b: Int)`

## Implicit Conversions

Functions the compiler applies for you

```tut:silent
def needString(str: String): String = str

implicit def intToString(i: Int): String = i.toString

val s = needString(2) // "2"

// equivilent to...

needString(intToString(2))
```

## Implicit Conversions - Ex2

```tut:silent
implicit def intToString(i: Int): String = i.toString

// Note: startsWith(String) is a method on String type

2.startsWith("someString") // returns false
2.startsWith("2") // returns true

// compiles down to...

intToString(2).startsWith("someString")
intToString(2).startsWith("2")
```

## Implicit Conversions - Ex3

```tut:silent
implicit def anyToString[T](obj: T): String = obj.toString

val a: String = 1 // "1"
val b: String = List(1,2) // "List(1,2)"

// compiles down to...

val a: String = anyToString(1) 
val b: String = anyToString(List(1,2))
```

# Understanding Implicit resolution

## Implicit Resolution...

...is a puzzle we ask the compiler to solve

- `implicit val`s are **facts**
- `implicit def`s are **rules**
  - Given `X`, Obtain `Y`
- Compiler use the available implicits to solve the puzzle (satisfy the type system)

## Two parts of implicit resolution

- Finding the implicits available at the current location
- Use the available implicits to arrive at a solution that satisfies the type system

## Back to example 2..
```tut:silent
implicit def intToString(i: Int): String = i.toString

2.startsWith("someString")
```

- `Int` doesn't have a `startsWith` method
- Through implicit search, we have Int => String conversion (`intToString`)
- String has a `startsWith(x: String)` method
- Scala compiler generates code, with resolved implicit def applied

<div class="fragment">
```tut:silent
intToString(2).startsWith("someString")
```
</div>

---

![](./images/cat_fit_single.jpg)

## JSON Conversion (example)

```tut:invisible
type Json = String
```

```tut:silent
trait Encoder[A] {
  def encode(obj: A): Json
}

implicit val encodeInt: Encoder[Int] = (i: Int) => i.toString
```

## JSON Conversion (example)

- Don't want to write manually `Encoder` instances for `List[Int]`, `List[String]` and more...

- If we know how to convert a type `A` into JSON, then we know how to turn a list of `A` into JSON!

<div class="fragment">
```tut:silent
implicit def encodeList[A](implicit encodeA: Encoder[A]):
  Encoder[List[A]] = new Encoder[List[A]] {
      def encode(list: List[A]) = {
        "[" +
        list.map(encodeA.encode(_)).mkString(",") +
        "]"
      }
  }
```
</div>

## JSON Conversion (example)

```tut:silent
def printJson[T](obj: T)(implicit en: Encoder[T]) = {
  println(en.encode(obj))
}
```

```tut:silent
printJson(1) // "1"
printJson(List(1,2,3)) // "[1,2,3]"
```

## How does it work?

```tut:silent
printJson(List(1,2,3))   // def printJson[T](obj: T)(implicit en: Encoder[T])
```

- printJson is given a `List[Int]`, therefore it needs an `Encoder[List[Int]]`
- **Fact**: `Encoder[Int]`
- **Rule**: Given `Encoder[A]`, we can get `Encoder[List[A]]`
- By combining the fact and the rule, we have `Encoder[List[Int]]`

<div class="fragment">
```tut:silent
printJson(List(1,2,3))(encodeList(encodeInt))
```
</div>

---

![](./images/cat_fit_many.jpg)

# Finding Implicits

## Where does Scala find implicits?

> - Lexical Scope
>   - Any implicits defined in the current code block
> - Implicit Scope

## Implicits in lexical scope

> - Anything defined or imported in the current code block

```tut:silent
object Implicits {
  implicit val int: Int = 1
}

object Lexical {
  import Implicits.int // or Implicits._
  implicit val str: String = "asdf"

  // Implicits of type String and Int are available in this scope
}
```

## "Implicit Scope"

- Class body and parent class bodies
  - Intuition: Anything you can directly reference by name
- Companion object of the types involved (and their parent classes)
- package objects of the types involved\*

## Implicit Scope Example 1

```tut:silent
trait ParentClass {
  implicit val encodeDouble: Encoder[Double] = ???
}
```

```tut:silent
object UseSite extends ParentClass { // Implicit from parent class's body

  // def implicitly[I](implicit imp: I): I = imp
  implicitly[Encoder[Double]] // compiles, implicit found

}
```

## Implicit Scope Example 2

```scala
object Encoder { // Companion object of Encoder
  implicit def encodeMap[A](implicit encoder: Encoder[A])
    : Encoder[Map[String, A]] = ???
}

final case class Dog(name: String)

object Dog {
  implicit val encodeDog: Encoder[Dog] = (d: Dog) => d.toString
}

implicitly[Encoder[Map[String, Dog]]] // compiles, no explicit imports needed!
```

# Best Practices

## Best Practices

1. Maybe you don't need implicits
1. Put implicits in companion objects
1. Avoid implicit conversions
1. Canonicalness of typeclass instances

## 1. Maybe you don't need implicits

```scala
def spawnActors(implicit actorSystem: ActorSystem) = ???
```

```scala
spawnActors

// vs.

spawnActors(myActorSystem)
```

- Trade-off between correctness, readability, compile time, and ergonomics. 
- Is it worth the convenience?

## 2. Put implicits in companion objects

- No explicit import required!
- The default location to look for implicits especially typeclass instances

--- 

- Defining a new data type (e.g. `Dog`)?
  - Put it in `Dog` companion object
- Defining a new typeclass (e.g. `Encoder`)?
  - Add instances for basic data types (Int, Double, String, etc) if it makes sense

## 3. Avoid implicit conversions

- Avoid using implicit conversions (`implicit def`) for automatically converting between concrete types
  - Code is harder to follow and diagnose
  - Use extension methods instead as it is more explicit

## 4. Canonicalness of typeclass instances

- Avoid creating multiple instances of the same typeclass for the same type
  - A canonoical location for type instances (the companion object) helps enforce it
- If you need multiple typeclass instances (e.g. Different JSON serialization to different clients)
  - Ideally, remove the `implicit` keyword to force the user to choose one explicitly
  - Write tests to catch any refactoring mistake causing a different implicit to be picked up

# Tips and Tricks

## IntelliJ

Intellij has helpful functionality to show you the implicits being resolved

- **Show Implicit Hint** action (recent version of Intellij Scala plugin)
- Also...
  - **Implicit Arguments** (show implicit arguments)
  - **Implicit Conversions** (show implicit conversions)
- Hint: In IntelliJ you can use **Cmd + Shift + A** to search for an action by name

---

![](./images/intellij_show_implicit_hints.png)

## Use `implicitly`!

> - "Summon" (resolve) an implicit, giving you an evaluated instance of the given type

```tut:invisible
implicit val encodeInts: Encoder[List[Int]] = (xs: List[Int]) => xs.toString
```

```tut:silent
val encodeIntList: Encoder[List[Int]] = implicitly[Encoder[List[Int]]]
encodeIntList.encode(List(1))
```

## `implicitly` for resolving compile errors

- Useful for debugging typeclass derivation errors (e.g. circe-generics or doobie) since you know one of the case class fields 
  is missing the typeclass instance, but don't know which one.

<div class="fragment">
```scala
import io.circe.generic.semiauto.deriveEncoder
import java.time.Year

final case class Antique(
  name: String,
  creationYear: Year,
  ...many other fields
)

object Antique {
  implicit val encode: Encoder[Antique] = deriveEncoder[Antique]
  // Compiler error: could not find Lazy implicit value of type
  //                 io.circe.generic.encoding.DerivedObjectEncoder[Antique]
}
```
</div>

---

```scala
object Antique {
  implicitly[Encoder[String]] // no error on this line
  // Compile Error: could not find implicit value for
  //                parameter e: io.circe.Encoder[java.time.Year]
  implicitly[Encoder[Year]]

  implicit val encode: Encoder[Antique] = deriveEncoder[Antique]
}
```

## Other information

- Implicit resolution has precedence rules
  - Can be used to provide fallback implementation 
- Implicit classes 
  - Simpler syntax to define extension methods
  - Allocation-free if implicit class extends `AnyVal`
- [https://github.com/tek/splain](https://github.com/tek/splain)
  - Compiler plugin which logs the implicit search path and gives you more precisely what implicit is missing

## Further Learning

- Learn by example
  - Circe, circe-generics, doobie
- [Profiling compile times](https://www.scala-lang.org/blog/2018/06/04/scalac-profiling.html)
  - Help diagnose compile time issues which are often caused by heavy use of implicits
