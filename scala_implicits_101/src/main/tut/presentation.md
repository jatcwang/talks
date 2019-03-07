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

## Implicit Resolution - What's that?

Automatically filling in parameters and function calls for you!

<div class="fragment">
> - Two types
>   - Implicit Parameters
>   - Implicit Conversions
- All resolved at compile time
</div>

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
def wantString(str: String): String = str

implicit def intToString(i: Int): String = i.toString

val s = wantString(2) // "2"

// compiles down to the following at runtime...

wantString(intToString(2))
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

With generic parameters!

```tut:silent
implicit def anyToString[T](obj: T): String = obj.toString

wantString(Map("k" -> "v")) // "Map(k -> v)"
wantString(List(1,2)) // "List(1,2)"

// compiles down to...

wantString(anyToString(Map("k" -> "v")))
wantString(anyToString(List(1,2)))
```

# Understanding Implicit Resolution

## Implicit Resolution...

...is a puzzle we ask the compiler to solve

- `implicit val`s are **facts**
- `implicit def`s are **rules**
  - Given some existing facts, derive a new fact
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
- We got `Int => String` implicit conversion
- String has a `startsWith(x: String)` method
- The types match. Scala compiler generates code, with resolved implicit conversions applied

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

implicit val encodeInt: Encoder[Int] = new Encoder[Int] { 
  def encode(i: Int) = i.toString
}
```

## JSON Conversion (example)

- Let's encode `List` of things!
- Don't want to manually write `Encoder` instances for `List[Int]`, `List[String]`, etc etc

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
- By combining the fact and the rule, we get `Encoder[List[Int]]`

<div class="fragment">
```tut:silent
printJson(List(1,2,3))(encodeList(encodeInt))
```
</div>

---

![](./images/cat_fit_many.jpg)

# Searching for implicits

## Where does Scala search for implicits?

> - Lexical Scope
>   - Any implicits defined/imported in the current code block
> - Implicit Scope

## Implicits in lexical scope

> - Anything defined or imported in the current code block

```tut:silent
object Implicits {
  implicit val implicitDouble: Double = 1.0
}

object Lexical {
  import Implicits.implicitDouble // or Implicits._
  implicit val str: String = "asdf"

  // def implicitly[I](implicit imp: I): I = imp
  implicitly[Double] // resolves to 1.0
  implicitly[String] // resolves to "asdf"
}
```

## "Implicit Scope"

- Class body and parent class bodies
  - Anything you can directly reference by name
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

  implicitly[Encoder[Double]] // compiles - found encodeDouble

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
```

```scala
// compiles, no explicit imports needed!
// Resolved using implicits found in companion objects
implicitly[Encoder[Map[String, Dog]]] 
```

# Best Practices

## Guiding Principles

> - Make code easy to reason about
> - Make it hard to do the wrong thing

## Best Practices

1. Maybe you don't need implicits?
1. Put implicits in companion objects
1. Avoid implicit conversions
1. Canonicalness of typeclass instances

## 1. Maybe you don't need implicits?

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
- The default location to look for implicits, especially for typeclass instances

## 2. Put implicits in companion objects

- Defining a new data type (e.g. `Dog`)?
  - Put typeclass instances in `Dog` companion object
- Defining a new typeclass (e.g. `Encoder`)?
  - Add instances for basic data types (Int, Double, String, etc) if it makes sense

## 3. Avoid implicit conversions

- Avoid using implicit conversions (`implicit def`) for automatically converting between concrete types
  - Code is harder to follow and diagnose
  - Use extension methods instead as it is more explicit
- Use it for deriving typeclass instances generically (like `Encoder[List[A]]`)

## 4. Canonicalness of typeclass instances

- Avoid creating multiple instances of the same typeclass for the same type
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
- [https://github.com/tek/splain](https://github.com/tek/splain)
  - Compiler plugin which logs the implicit search path and gives you more precisely what implicit is missing

## Further Learning

- Learn by example
  - Circe, circe-generics, doobie
- [Profiling compile times](https://www.scala-lang.org/blog/2018/06/04/scalac-profiling.html)
  - Help diagnose compile time issues which are often caused by heavy use of implicits
  
## Questions?
