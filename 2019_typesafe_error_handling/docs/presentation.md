# Adventures in type-safe error handling

--- 

How do we handle errors in Scala today?

## Either[+E, +A]

* Use any error type you want!

<div class="fragment">

```scala mdoc:silent
sealed trait AllErrors extends Throwable
final case class E1() extends AllErrors
final case class E2() extends AllErrors
final case class E3() extends AllErrors
```

```scala mdoc:compile-only
def maybeError1: Either[E1, Unit] = ???
def maybeError2: Either[E2, Unit] = ???

val result: Either[AllErrors, Unit] = 
  for {
    _ <- maybeError1
    _ <- maybeError2
  } yield ()
```

</div>

* Covariant - Compiler will find **Least Upper Bound** (LUB) to reconcile the error type 

## Cats IO[A]

* Any **IO[A]** can fail with a **Throwable** using **IO.raiseError**
* Have to rely on documentation, not types

<div class="fragment">

```scala mdoc:invisible
import cats.effect.IO
```
```scala mdoc:compile-only
def io1: IO[Unit] = ???
def io2: IO[Unit] = ???

val ioResult: IO[Unit] = 
  for {
    _ <- io1
    _ <- io2
  } yield ()
```

</div>

--- 

### Either in IO

> * Need to check the **Either** result from the previous step
> * Error prone and verbose - Not recommended

```scala mdoc:compile-only
def io1: IO[Either[E1, Unit]] = ???
def io2: IO[Either[E2, Unit]] = ???

for {
  result <- io1
  result2 <- result match {
    case Left(e1) => IO.pure(Left(e1))
    case Right(_) => io2
  }
} yield {
  result2 match {
    case Left(e2) => Left(e2)
    case Right(_) => Right(())
  }
}
```

## EitherT[IO, E, A]

Similar to **IO[Either[E, A]]** but "short-circuits" when you have a `Left`

<div class="fragment">

```scala mdoc:invisible
import cats.data.EitherT
import cats.implicits._
```

```scala mdoc:compile-only
def et1: EitherT[IO, E1, Unit] = ???
def et2: EitherT[IO, E2, Unit] = ???

val eitherTResult: EitherT[IO, AllErrors, Unit] = 
  for {
    _ <- et1.leftWiden[AllErrors]
    _ <- et2.leftWiden[AllErrors]
  } yield ()
```

</div>

* Invariant - no auto upcasting but you can use `leftWiden`
* **IO.raiseError** reserved for defects or unhandleable errors

## Bifunctor IO[+E, +A] (ZIO)

> * Similar to EitherT, but better ergonomic

<div class="fragment">

```scala mdoc:reset:invisible
import zio._
import com.example.Example._
```

```scala mdoc:compile-only
def zio1: IO[E1, Unit] = ???
def zio2: IO[E2, Unit] = ???

val eitherTResult: IO[AllErrors, Unit] = 
  for {
    _ <- zio1
    _ <- zio2
  } yield ()
```

</div>

* Can terminate the execution chain with a `Throwable` (**IO.die**)

---

---

## Java Checked Exceptions!

---

```java
void method1() throws E1 { ... }

void method2() throws E2 { ... }

void onlyE1Handled() throws E2 { // E2 not handled, must declare!
  try {
    method1();
    method2();
  } 
  catch (E1 e1) { ... }
}

void allHandled() { // All errors handled!
  try {
    method1();
    method2();
  } 
  catch (E1 e1) { ... }
  catch (E2 e2) { ... }
}

```

## The trouble with checked exceptions

* Not available in Scala 🙃
* Errors are not values. Doesn't work well with many newer language features such as anonymous functions
* Type system special case - no abstraction or reuse

## But it has many cool ideas too

- Exhaustive handling 
- Partial handling
- Open union of errors
- Can we have these in Scala?

```scala mdoc:reset:invisible
val v = 1
```

## Shapeless Coproduct!

<div class="fragment">

```scala mdoc:invisible
sealed trait MyError
final case class E1() extends MyError
final case class E2() extends MyError
final case class E3() extends MyError
```

```scala mdoc
import shapeless._
type E123 = E1 :+: E2 :+: E3 :+: CNil
// Similar to Either[E1, Either[E2, Either[E3, CNil]]

import shapeless.syntax.inject._

val e1InCoproduct: E1 :+: E2 :+: E3 :+: CNil = E1().inject[E1 :+: E2 :+: E3 :+: CNil]
val e2InCoproduct: E1 :+: E2 :+: E3 :+: CNil = E2().inject[E1 :+: E2 :+: E3 :+: CNil]

e2InCoproduct match {
  case Inl(E1()) => println("it's E1!")
  case Inr(Inl(E2())) => println("it's E2!")
  case Inr(Inr(Inl(E3()))) => println("it's E3!")
  case Inr(Inr(Inr(cnil))) => cnil.impossible // To satisfy exhaustiveness check
}
```

</div>

## Coproducts are flexible!

Let's extract a particular cases from a coproduct!

```scala mdoc
import shapeless.ops.coproduct._

// Returns a Left(E1()) if we have an E1
Remove[E1 :+: E2 :+: E3 :+: CNil, E1].apply(e1InCoproduct)

// Otherwise return the rest in Right(..)
Remove[E1 :+: E2 :+: E3 :+: CNil, E2].apply(e1InCoproduct)
```

<div class="fragment">
...and you can do many, many things with Coproducts!
</div>

---

Using **Coproducts** directly feels cumbersome

Can we make it nicer?

## Hotpotato

A library for type-safe, ergonomic and readable error handling!

* Based on Shapeless coproducts
* Integrates with ZIO and Cats

## First, a bit of simplification

Coproducts can be a bit tedious to read and write, so Hotpotato provides some type aliases for coproducts

```scala mdoc
import hotpotato._

type ErrorsSimple = OneOf3[E1, E2, E3] // is equivalent to E1 :+: E2 :+: E3 :+: CNil
```

## Handling errors - exhaustive

> * Convert all errors into one single type 
> * **OR** each to its own type

```scala mdoc:invisible
case class X1()
case class X2()
```

```scala mdoc:compile-only
import hotpotato._
import shapeless.syntax.inject._
import zio._

val io: IO[OneOf3[E1, E2, E3], Unit] = IO.fail(E1().inject[OneOf3[E1, E2, E3]])

// Turn every error into String
val resString: IO[String, Unit] = io.mapErrorAllInto(
  (e1: E1) => "e1",
  (e2: E2) => "e2",
  (e3: E3) => "e3",
)

// Turn every error into some other type
val result: IO[OneOf2[X2, X1], Unit] = io.mapErrorAll(
  (e1: E1) => X1(),
  (e2: E2) => X2(),
  (e3: E3) => X1(),
)
```

## Handling errors - partial

```scala mdoc:reset:invisible
import com.example.Example._
import zio._
```

```scala mdoc:compile-only
import hotpotato._

val ioE123: IO[OneOf3[E1, E2, E3], String] = ???

// Turn some error into String
val result: IO[OneOf3[String, Int, E3], String] = ioE123.mapErrorSome(
  (e1: E1) => "e1",
  (e2: E2) => 12,
)
```

## Error handling with side-effects

Very often error recovery/handling requires side-effect (e.g. logging)

```scala mdoc:compile-only
import hotpotato._

val ioE123: IO[OneOf3[E1, E2, E3], String] = ???
val fallbackIO: E1 => IO[Int, String] = ???

val result: IO[OneOf3[Int, E2, E3], String] = ioE123.flatMapErrorSome(
  (e1: E1) => fallbackIO(e1),
)
```

<div class="fragment">
**flatMapErrorAll**, **flatMapErrorAllInto** are provided for exhaustive handling too
</div>

## Combining errors

We often have a series of steps and each step may have different errors

```scala mdoc:compile-only
import hotpotato._

val ioE1: IO[E1, Unit] = ???
val ioE23: IO[OneOf2[E2, E3], Unit] = ???

// An embedder tells the compiler what types we want all errors to embed to
implicit val embedder: Embedder[OneOf3[E1, E2, E3]] = Embedder.make

val result: IO[OneOf3[E1, E2, E3], Unit] = for {
  _ <- ioE1.embedError
  _ <- ioE23.embedError
} yield ()
```

## Interfacing with sealed trait errors

Easy conversion from/to sealed traits

```scala mdoc:compile-only
import hotpotato._

// Recall that E1, E2 and E3 all extends AllErrors
val ioAllErrors: IO[AllErrors, String] = ???

val ioE123: IO[OneOf3[E1, E2, E3], String] = ioAllErrors.errorAsCoproduct

val ioAllErrorsAgain: IO[AllErrors, String] = ioE123.unifyError
```

## Summary

<style>
  .dark {
    background-color: #6baaf7;
  }
  
  .bold {
    font-weight: bold;
  }
  
  .text-centered td {
    text-align: center!important;
  }
  
  .yes:after {
    content: '✓';
    font-weight: bold;
    color: green;
  }
  
  .no:after {
    content: '⨯';
    color: red;
  }
  
  .hmm {
    background-image: url("assets/images/emoji_sweat.svg");
    background-position: center;
    background-size: 27px;
    background-repeat: no-repeat;
  }
  
  .emb {
    background-image: url("assets/images/emoji_embarrased.svg");
    background-position: center;
    background-size: 27px;
    background-repeat: no-repeat;
  }
  
  .no-box .sourceCode {
    box-shadow: none!important;
  }
  
  .no-box img {
    box-shadow: none!important;
  }
  
  .figure img {
    margin: 0!important;
    overflow: hidden;
  }
  
  .figure p {
    margin: 0!important;;
  }
  
</style>

<table class="text-centered" style="border-radius: 6px;overflow: hidden;">
    <thead>
        <tr>
          <td></td>
          <td class="dark">ChckEx</td>
          <td class="dark">EitherT</td>
          <td class="dark">ZIO</td>
          <td class="dark">With Hotpotato</td>
        </tr>
    </thead>
    <tbody>
        <tr>
          <td class="dark">Composable</td>
          <td class="no"></td>
          <td class="yes"></td>
          <td class="yes"></td>
          <td class="yes"></td>
        </tr>
        <tr>
          <td class="dark">Error type unification</td>
          <td class="yes"></td>
          <td class="emb"></td>
          <td class="yes"></td>
          <td class="hmm"></td>
        </tr>
        <tr>
          <td class="dark">Open error union</td>
          <td class="yes"></td>
          <td class="no"></td>
          <td class="no"></td>
          <td class="yes"></td>
        </tr>
        <tr>
          <td class="dark">Handling - Exhaustive</td>
          <td class="yes"></td>
          <td class="yes"></td>
          <td class="yes"></td>
          <td class="yes"></td>
        </tr>
        <tr>
          <td class="dark">Handling - Partial</td>
          <td class="yes"></td>
          <td class="no"></td>
          <td class="no"></td>
          <td class="yes"></td>
        </tr>
    </tbody>
</table>


## It's just the beginning!

::: nonincremental

* Hotpotato is available now!
* Your ideas, feedback and use cases are welcome!
* Docs: [jatcwang.github.io/hotpotato/]([https://jatcwang.github.io/hotpotato/])
* Gitter: [jatcwang.github.io/hotpotato/]([https://jatcwang.github.io/hotpotato/])

:::

## Thank you!

> * Twitter / Github: @jatcwang

## Why sealed trait isn't enough

Let's look at an example

<div class="fragment">
    <div class="figure no-box" style="height: 335px; ">
        <img class="no-box" width="600px" src="assets/images/callgraph_simple.svg" />
    </div>
</div>

<div class="fragment">
How should we model the errors for **B1** and **B2**?
</div>

---

<div class="figure no-box" style="height: 280px; ">
    <img class="no-box" width="600px" src="assets/images/callgraph_simple.svg" />
</div>

<div class="no-box">

```scala mdoc:compile-only
sealed trait B1Errors
sealed trait B2Errors

case class Conflict() extends B1Errors with B2Errors
case class NotFound() extends B1Errors
case class Unauthorized() extends B2Errors
```
</div>

---

```scala mdoc:compile-only
sealed trait B1Errors
sealed trait B2Errors

case class Conflict() extends B1Errors with B2Errors
case class NotFound() extends B1Errors
case class Unauthorized() extends B2Errors
```

::: nonincremental

<ul>
  <li class="fragment">Error class declaration now need to be in the same file</li>
  <li class="fragment">You cannot use these error classes in another error hierarchy</li>
  <li class="fragment">We want:
    <ul>
      <li>Exhaustive matching</li>
      <li>Partial elimination</li>
      <li>Use types we don't own</li>
    </ul>
  </li>
</ul>

:::
