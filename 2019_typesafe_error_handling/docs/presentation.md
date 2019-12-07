
# Adventures in type-safe error handling

## About me

- Scala Developer at <img style="box-shadow: none; margin: 0 0 3px 5px; vertical-align: sub;" src="./assets/images/medidata.png"/>
- Typed FP and other useful tools
- @jatcwang

## Error handling - A whirlwind tour

## Either[+E, +A]

* Use any error type you want!
* Covariant - Compiler will find **Least Upper Bound** (LUB) to make the error type converge

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

## IO[A] / Future[A]

<span style="font-style: italic">"Errors? What errors? **Explodes**"</span>

* Any **IO[A]** can fail with a **Throwable**
* Have to rely on documentation, not types

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

## EitherT

* Similar to **IO[Either[E, A]]** but "short-circuits" when you have a `Left`
* Invariant - no auto upcasting but you can use `leftWiden`

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

## ZIO[-R, +E, +A]

* Similar to EitherT, but better ergonomic
* Can terminate the fibre with a `Throwable` ("Die")

## Java Checked Exceptions!

* Compiler enforced error handling
* Exhaustive handling (like sealed traits)
* **Partial Handling** - handle what you can, let the rest bubble up

---

```java
void doA() throws Err1 { ... }

void doB() throws Err2 { ... }

void doAThenBPartial() throws Err2 { // Err2 not handled, must declare!
  try {
    doA();
    doB();
  } 
  catch (Err1 e1) { ... }
}

void doAThenB() { // All errors handled!
  try {
    doA();
    doB();
  } 
  catch (Err1 e1) { ... }
  catch (Err2 e2) { ... }
}

```

```scala mdoc:reset:invisible
val v = 1
```

## The trouble with checked exceptions

* Errors are not return values. Doesn't compose well with libraries (Java 8 streams / lambdas)
* Type system special case - no abstraction or reuse
* Not available in Scala 🙃
* Still, **partial handling** can be great!

## Summary

<style>
  .dark {
    background-color: darkgrey;
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
    background-image: url("assets/images/sweat_emoji.svg");
    background-position: center;
    background-size: 23px;
    background-repeat: no-repeat;
  }
</style>

<table class="text-centered">
    <thead>
        <tr>
          <td></td>
          <td class="dark">ChckEx</td>
          <td class="dark">EitherT</td>
          <td class="dark">ZIO</td>
        </tr>
    </thead>
    <tbody>
        <tr>
          <td class="dark">Composable</td>
          <td class="no"></td>
          <td class="yes"></td>
          <td class="yes"></td>
        </tr>
        <tr>
          <td class="dark">Error Type Unification</td>
          <td class="yes"></td>
          <td class="hmm"></td>
          <td class="yes"></td>
        </tr>
        <tr>
          <td class="dark">Handling - Exhaustive</td>
          <td class="yes"></td>
          <td class="yes"></td>
          <td class="yes"></td>
        </tr>
        <tr>
          <td class="dark">Handling - Partial</td>
          <td class="yes"></td>
          <td class="no"></td>
          <td class="no"></td>
        </tr>
    </tbody>
</table>









## What is a good error interface?

- "Just enough errors, but not too much"
- Right answer depends on context!

## What does our API consumers want?

- "What can go wrong that I need to think about"?
- Show them errors they might want to handle
- Don't bother them with anything they cannot deal with
- **Semantic** vs **Operational/Implementation**

## What do we want from our error handling technique?

- Exhaustiveness
- Partial handling
- Good ergonomics and error message
- Share code / error definitions
- Performance

## Our Example Scenario

- A service that can create and fetch users (via network calls)
- Failure cases:
  - **User creation**: Clashing user name or unauthorized/not found
  - **Fetch user**: User not found, or unauthorized/not found

```scala mdoc:invisible
type UserId = String
case class User(id: UserId, name: String)
case class UserData(name: String)
```

```scala mdoc:silent
sealed trait UserServiceError
final case class Unauthorized() extends UserServiceError
final case class UserNotFound() extends UserServiceError
final case class UserAlreadyExists() extends UserServiceError
 ```

```scala mdoc:silent
import zio._
trait UserService {
  def createUser(userData: UserData): IO[UserServiceError, UserId]
  def getUser: IO[UserServiceError, User]
}
```

```scala
def createUser(userData: UserData): IO[ServiceError, UserId] = {
  for {
    _ <- checkPermissions // IO[Unauthorized, Unit]
    newUser <- insertUserIntoDb // IO[UserAlreadyExists, User]
  } yield newUser
}
```

## The problem with sealed traits

- Choose one:
  - Mispresent whaterrors can actually happen
  - Code repetition ==> Boilerplate, hard to reuse code
- No partial handling

```scala mdoc:silent
sealed trait CreateUserErrors

object CreateUserErrors {
  final case class Unauthorized() extends CreateUserErrors
  final case class UserAlreadyExists() extends CreateUserErrors
}

sealed trait FetchUserErrors
object FetchUserErrors {
  final case class Unauthorized() extends FetchUserErrors
  final case class UserNotFound() extends FetchUserErrors
}
```

## Java checked exceptions

- Handle the error, or let it bubble up (enforced)
- First class, but non-composeable
- Akward interactions with closures leads to unwieldly code
  when using with future, streams and others

## What we want

- Freely combine existing error classes
- Exhaustiveness
- Allow handling only a subset of errors

```scala
// Dotty / Scala 3
def createUser(userData: UserData): IO[Unauthorized | UserAlreadyExists, UserId] = {
  for {
    _ <- checkPermissions // IO[Unauthorized, Unit]
    newUser <- insertUserIntoDb // IO[UserAlreadyExists, User]
  } yield newUser
}
```

## Shapeless Coproduct!

```scala mdoc:invisible
sealed trait MyError
final case class E1() extends MyError
final case class E2() extends MyError
final case class E3() extends MyError
```

```scala mdoc
import shapeless._
type E12 = E1 :+: E2 :+: CNil
// Similar to Either[E1, Either[E2, CNil]]

import shapeless.syntax.inject._

val e1InE12: E12 = E1().inject[E12]
val e2InE12: E12 = E2().inject[E12]

e1InE12 match {
  case Inl(E1()) => println("it's E1!")
  case Inr(Inl(E2())) => println("it's E2!")
  case Inr(Inr(cnil)) => cnil.impossible // To satisfy exhaustiveness check
}
```

## Coproduct is cool!

- Combine small coproducts into larger ones
- Extract particular cases in a coproduct, leaving behind unhandled cases

```scala mdoc
import shapeless.ops.coproduct._

Remove[E12, E1].apply(e1InE12)
Remove[E12, E1].apply(e2InE12)

Basis[E1 :+: E2 :+: E3 :+: CNil, E12].inverse(Right(e2InE12))
```

## Introducing Hotpotato

## References

FIXME
- Luke's bifunctor IO blog post

## FIXME

- Error combination
- Partial handling and transformation
- Exhaustive matching
