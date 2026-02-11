---
title: Tour of Typed Error Handling
layout: cover
background: /safari2.jpg
---

# Tour of Typed Error Handling

## Jacob Wang

Feb 2026, London Scala User Group

---

# Hello 👋

- Writing Scala since 2015
- Maintainer of libraries like Difflicious, Doobie, etc
- @jatcwang (GitHub, mas.to, Bluesky)

---

# Today's itinerary

- Why typed error handling?
- Which errors should we track with types?
- Tour of error handling libraries

---

# Errors! Failures! Faults! 💥

<v-clicks>

- Errors are a fact of life for (almost) any program
- The basics: `try-catch`, `IO/Future`'s `recoverWith`

</v-clicks>

<v-click>

- Untyped
  - Read the documentation, or implementation!
  
```scala
def uploadFile(userId: UserId, parentPath: Path, file: File): IO[Unit] =
...
```

</v-click>


---

# The case for typed errors

<v-click>
Putting possible errors in the types:

- Exhaustive error handling, checked by the compiler.
- Free documentation!
</v-click>

<v-click>
```scala
def uploadFile(userId: UserId, parentPath: Path, file: File): IO[Unit]
```

```scala
enum FileUploadError {
  case UnauthorizedUpload(...)
  case NotEnoughStorageQuota(...)
  case FileAlreadyExist(...)
}
```

</v-click>

---

# Typed errors

But which errors?

<v-clicks>

- Can the errors be <u>**meaningfully**</u> handled by the function caller?
  - Additional actions on error
  - Different code paths for different errors

</v-clicks>

<v-click>

**When NOT to use typed errors?**

- The caller can't do much with it other than rethrowing it

</v-click>

--- 

# Typed errors - but which ones?

What errors should we track with types?

- <b>Fatal errors</b> (OutOfMemoryError, StackOverflowError)
- <b>Bugs</b> (AssertionError)
- <b>Expected-unexpected failures</b> (Network / IO exceptions)
- <b>Domain errors</b> (Validation / Incorrect state) 

---

# Typed errors - but which ones?

What errors should we track with types?

- <s class="color-gray">Fatal errors (OutOfMemoryError, StackOverflowError)</s>
- <s class="color-gray">Bugs (AssertionError)</s>
- <b>Expected-unexpected failures</b> (Network / IO exceptions) <span style="color: #d68e05;font-weight: bold;">Depends..?</span>
- <b>Domain errors</b> (Validation / Incorrect state)  <span style="color: rgb(90, 203, 12);font-weight: bold;">Great!</span>

--- 

# What's a good error handling mechanism?

<v-clicks>

- **Succinct**
  - Good type inference
- **Precise**
  - Each function expresses its possible errors
- **Safe**
  - ..from refactorings and user mistakes

</v-clicks>

---

# Example

```
def upload(user: User, path: Path, file: File)
```

The function either succeeds or fail with `FileUploadError`:

<<< @/snippets3/models.scala#go scala

Or if modeled using union types:
```scala
type FileUploadError = 
  UnauthorizedUpload | 
  NotEnoughStorageQuota | 
  FileAlreadyExist
```

---
layout: section
---

# 🚙 _Let the tour begin!_ 🦖

--- 
layout: section
---

# IO[Either[E, A]]

---

# IO[Either[E, A]]

<<< @/snippets2/IOEither.scala#go scala {all|1-2|6-7|all}{lines:true}


---

# IO[Either[E, A]]

```scala {all|7,9}{lines:true}
def step1(): IO[Either[Err, A]] = ...
def step2(): IO[Either[Err, B]] = ...
def step3(): IO[Either[Err, C]] = ...

def run(): IO[Either[E, A]] =
  step1().flatMap {
    case Left(err) => IO.pure(Left(err))
    case Right(a) => step2().flatMap {
      case Left(err) => IO.pure(Left(err))
      case Right(b) => step3().map(Right(_))
    } 
  }
```

--- 
layout: section
---

# EitherT

---

# EitherT

`case class EitherT[F[_], E, A](value: F[Either[E, A]])`

- A thin wrapper around e.g. `IO[Either[E, A]]`
- `EitherT`'s `flatMap` handles the short-circuiting

---

# EitherT

<<< @/snippets2/EitherT.scala#go scala {all|5|all|13|all}{lines:true}

--- 
layout: section
---

# ZIO

---

# ZIO

`ZIO[-R, +E, +A]`

- Integrates typed error directly into the effect type (`E`)

---

# ZIO

<<< @/snippets2/zio.scala#go scala {all|3|4-6|11-12|all}{lines:true}

--- 
layout: section
---

# cats-mtl / IOHandle

---

# cats-mtl

- `Raise[F, E]`

`def doThings(param: Int)(given Raise[IO, PossibleErrors]): IO[Int]`

---

# cats-mtl / IOHandle

<p></p>

**cats-mtl**:
- `Raise[F, -E]` - Capability to raise errors of type `E` in effect `F`
- `Handle[F, E]` - Extends `Raise`, and can incepcept errors of type `E` in effect `F`
 
<v-click>

**IOHandle**

- A library specializes the above for `cats.effect.IO` (+ many more conveniences!)
- `IORaise` and `IOHandle`

`def doThings(param: Int)(given IORaise[PossibleErrors]): IO[Int]`

</v-click>

---

# cats-mtl / IOHandle

### IOHandle Usage:
  - Call `ioHandling[E]` to open a scope
  - Call `ioAbort(err)` to abort with an error
  - Handle the result with methods like:
    - `.toEither`: Converts to `IO[Either[E, A]]`
    - `.rescueWith`: Handle the error directly

---

# IOHandle

Scala 2:
<<< @/snippets2/iohandlee.scala#go scala {all|8|11-13|3,10|14|17|all}
--- 

# IOHandle

Scala 3 + more helper methods:
<<< @/snippets3/iohandlee.scala#go scala {all|3|8|11-12|4,11-12|all}

---

# How it works

<v-clicks>

- `ioHandling`: Creates a unique marker, carried in `IORaise` capability object
- `ioAbort`: throws `IOHandleErrorWrapper` with the marker
  - `class IOHandleErrorWrapper[E](error: E, marker: AnyRef) extends RuntimeException`
- Handler methods (`.toEither`) catch and handle `IOHandleErrorWrapper` with the expected marker

</v-clicks>

---

# Caveats

- `IOHandleErrorWrapper` can be unintentionally caught & swallowed by user code!
- Solution: use `handleUnexpectedWith` instead of `IO#handleErrorWith`

````md magic-move
```scala
ioHandling[MyError]:
  checkSomething()
    .flatMap(succeeded => ioAbortIf(!succeeded, BadResult(..)))
    .handleErrorWith: e =>
        IO.println("something bad happened!") // swallowed! :(
```

```scala {4}
ioHandling[MyError]:
  checkSomething()
    .flatMap(succeeded => ioAbortIf(!succeeded, BadResult(..)))
    .handleUnexpectedWith: e =>
        IO.println("something bad happened!")
```
````

--- 
layout: section
---

# Ox

---

# Ox

- A library for **direct-style** concurrency
- Error-handling utility built on top of **boundary-break**

<v-click>

- Usage:
  - `ox.either` to start a scope
  - Unwrap `Either`s with `.ok()`
  - `someError.fail()` to abort with an error
  
</v-click>

---

# Ox

<<< @/snippets3/ox.scala#go scala {all|4-6|9|4,10|11|all}{lines: true}

--- 
layout: section
---

# Summary

---

# Summary

How do the libraries compare when it comes to typed-errors?

| <b>Library</b>    | <b>Precise</b>                | <b>Succint?</b>                    | <b>Footguns / edgecases?</b>   |
|-------------------|-------------------------------|------------------------------------|--------------------------------|
| EitherT           | <span class="good">Yes</span> | <span class="hm">Not really</span> | <span class="hm">Some*</span>  |
| cats-mtl/IOHandle | <span class="good">Yes</span> | <span class="ok">Decent</span>     | <span class="hm">Some</span>   |
| ox                | <span class="good">Yes</span> | <span class="good">Great</span>    | <span class="hm">Some</span>   |
| ZIO               | <span class="good">Yes</span> | <span class="good">Great</span>    | <span class="good">None</span> |

---

# Honourable mentions

- **Kyo**: Mix-and-match effects, including typed error handling
- **raise4s/yaes**: For direct-style scala
- `cats.ApplicativeError`: Equivalent to `cats.mtl.Handle` but a bit less ergonomic

---

# Acknowledgments

- **Daniel Spiewak & Thanh Le** for innovation in `cats-mtl` `Raise/Handle`
- IOHandle contributors: **Alex, Dmitryo, Francesco, Pavel, David**
- **Noel Welsh** for reviewing this talk!

--- 

# Thank you

Here are some relevant links

- [Monad Transformer issues with concurrency](https://github.com/typelevel/cats-effect/discussions/3765)
- https://typelevel.org/blog/2025/09/02/custom-error-types.html
- https://github.com/jatcwang/iohandle
  - `"com.github.jatcwang" %% "iohandle" % "0.1.0"`

---

# Bonus: Effectful error accumulation?

```scala {all|4|6-7|all}
import iohandle.ioscreen.*

def validatePackage(id: String, width: Int, height: Int): IO[Either[BadPackageError, Package]] =
  ioScreen[String]:
    (
      checkWidthAllowedRemotely(width).reportIf(_ == false, "Too wide"),
      checkHeightAllowedRemotely(width).reportIf(_ == false, "Too tall"),
    ).parZip: (width, height) =>
      Right(Package(id, width, height))
  .handleErrors: (errors: NonEmptyVector[String]) =>
    Left(BadPackageError(id, errors))

```

