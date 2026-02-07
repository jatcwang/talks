---
title: Tour of Typed Error Handling
layout: cover
background: /safari2.jpg
---

# Tour of Typed Error Handling

---

# Hello 👋

- Scala since 2015
- Maintainer of libraries like Difflicious, Doobie, etc
- @jatcwang (GitHub, mas.to, Bluesky)

---

# Today's itinerary

- Why typed error handling?
- Which errors should we track with types?
- Tour of error handling libraries

---

# Errors! Failures! Faults! 💥

- Errors are a fact of life for (almost) any program

<v-clicks>

- The basics: `try-catch`, `IO/Future`'s `recoverWith`
- Untyped
  - Read the documentation, or implementation!

</v-clicks>

<v-click>
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

- The caller can only rethrow it - i.e. _exceptional_ circumstances

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

# EitherT

`case class EitherT[F[_], E, A](value: F[Either[E, A]])`

- A thin wrapper around e.g. `IO[Either[E, A]]`
- `EitherT`'s `flatMap` handles the short-circuiting

---

# cats.data.EitherT

<<< @/snippets2/EitherT.scala#go scala {all|5|13|all}{lines:true}

---

# ZIO

`ZIO[-R, +E, +A]`

- Integrates typed error directly into the effect type

---

# ZIO

<<< @/snippets2/zio.scala#go scala {all|1|all}{lines:true}

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

# cats-mtl / IOHandle


### **cats-mtl**:

`def doThings(param: Int)(given Raise[IO, PossibleErrors]): IO[Int]`

### **IOHandle** (specialising for IO):

`def doThings(param: Int)(given IORaise[PossibleErrors]): IO[Int]`

---

# cats-mtl / IOHandle

<p></p>

- **cats-mtl** provides two capabilities for error handling:
  - `Raise[F, -E]` - Capability to raise errors of type `E` in effect `F`
  - `Handle[F, E]` - Extends `Raise`, and can incepcept errors of type `E` in effect `F`
- **IOHandle** library specializes the above for `cats.effect.IO` (+ many more conveniences!)

---

# cats-mtl / IOHandle

### IOHandle Usage:
  - Call `ioHandling[E]` to open a scope
  - Call `ioAbort(err)` to abort with an error

---

# IOHandle

Scala 2:
<<< @/snippets2/iohandlee.scala#go scala {all|8|11-13|3,10|14|17|all}
--- 

# IOHandle

Scala 3:
<<< @/snippets3/iohandlee.scala#go scala {all|1|4|3,13-16|all}

---

# How it works

- `ioAbort` wraps your error and throws it

```scala
class IOHandleErrorWrapper[E](error: E, marker: AnyRef) extends RuntimeException
```

- Each new scope (`ioHandling`) creates a unique marker
- The `IORaise` will attach the marker along with the error it's raising

---

# How it works

What happens inside `ioHandling`

```scala {all|3|5-7|9-13|14-16}
def doStuff(input: Int)(using IORaise[FileUploadError]): IO[Int] = ...

val myMarker = new AnyRef // java.lang.Object

given ioHandle: IOHandle[FileUploadError] = new IOHandle[FileUploadError]:
  def raise(e: FileUploadError): IO[Nothing] = IO.raiseError(IOHandleErrorWrapper(e, myMarker))
  def handleWith[A](fa: IO[A])(f: E => IO[A]): IO[A] = ...

doStuff(42)(ioHandle)
  .handleErrorWith:
    case s: IOHandleErrorWrapper[?] if s.marker == myMarker =>
      val err = s.error.asInstanceOf[FileUploadError]
      // ... Now we can run error recovery with err
    case e =>
      // Re-throw any other types of exceptions, or IOHandleErrorWrapper with a different marker
      IO.raiseError(e)
```

---

# Caveats

- IOHandle throws `IOHandleErrorWrapper`... it can be unintentionally caught & swallowed!
- Boundary-break has similar failures

````md magic-move
```scala
ioHandling[MyError]:
  checkSomething()
    .flatMap(succeeded => ioAbortIf(!succeeded, BadResult(..)))
    .handleErrorWith:
      case e =>
        IO.println("something bad happened!") // swallowed!
```

```scala {4}
ioHandling[MyError]:
  checkSomething()
    .flatMap(succeeded => ioAbortIf(!succeeded, BadResult(..)))
    .handleUnexpectedWith:
      case e =>
        IO.println("something bad happened!")
```
````

---

# Summary of libraries

- **Cats-effect**: Check out cats-mtl / IOHandle!
- **ZIO**: You're not missing out :) 
- **Direct-style**: Young & maturing ecosystem

<v-click>

## Honourable mentions

- **Kyo**: Mix-and-match effects, including typed error handling
- **raise4s/yaes**: For direct-style scala
- `cats.ApplicativeError`:  Equivalent to `cats.mtl.Handle` but a bit less ergonomic
 
</v-click>

---

# Acknowledgments

- IOHandle contributors: Alex, Dmitryo, Francesco, Pavel
- Daniel Spiewak & Thanh Le for improving `cats-mtl` error handling
- Noel Welsh for refining this talk!

--- 

# Thank you

- https://typelevel.org/blog/2025/09/02/custom-error-types.html
- https://github.com/jatcwang/iohandle
  - `"com.github.jatcwang" %% "iohandle" % "0.1.0"`
