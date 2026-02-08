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

Scala 3 + more helper methods:
<<< @/snippets3/iohandlee.scala#go scala {all|3|8|12|all}

---

# How it works

- `ioHandling`: Creates a unique marker, stored in the `IORaise` capability object
- `ioAbort`: throws `IOHandleErrorWrapper` with the marker
- Handler methods (`.toEither`) handles `IOHandleErrorWrapper` with the expected marker

<p></p>
```scala
class IOHandleErrorWrapper[E](error: E, marker: AnyRef) extends RuntimeException
```


---

# Caveats

- `IOHandleErrorWrapper` can be unintentionally caught & swallowed by user code!
- Solution: use `handleUnexpectedWith` instead of `handleErrorWith`

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

# Summary

How do the libraries compare when it comes to typed-errors?

| <b>Library</b>    | <b>Succint?</b>                    | <b>Footguns / edgecases?</b>   |
|-------------------|------------------------------------|--------------------------------|
| EitherT           | <span class="hm">Not really</span> | <span class="hm">Some</span>   |
| cats-mtl/IOHandle | <span class="ok">Decent</span>     | <span class="hm">Some</span>   |
| ox                | <span class="good">Great</span>    | <span class="hm">Some</span>   |
| ZIO               | <span class="good">Great</span>    | <span class="good">None</span> |

---

# Honourable mentions

- **Kyo**: Mix-and-match effects, including typed error handling
- **raise4s/yaes**: For direct-style scala
- `cats.ApplicativeError`: Equivalent to `cats.mtl.Handle` but a bit less ergonomic

---

# Acknowledgments

- IOHandle contributors: Alex, Dmitryo, Francesco, Pavel
- Daniel Spiewak & Thanh Le for innovation in `cats-mtl` `Raise/Handle`
- Noel Welsh for reviewing this talk!

--- 

# Thank you

- https://typelevel.org/blog/2025/09/02/custom-error-types.html
- https://github.com/jatcwang/iohandle
  - `"com.github.jatcwang" %% "iohandle" % "0.1.0"`
