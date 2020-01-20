# Practical Type Safety

<img style="box-shadow: none;" src="./assets/images/lego.jpg" aria-details="https://www.tibco.com/blog/2015/05/05/lego-learning-the-building-blocks-of-data-visualization/" />

Jacob Wang

<style>
  .no-box img {
    box-shadow: none!important;
  }
  
  .slides {
    margin-top: 60px!important;
  }
  
  .reveal .slides h3 {
    margin-top: -20px;
    color: brown;
    text-transform: inherit;
  }
  
</style>

## Hello

::: nonincremental

- Scala Developer at <img style="box-shadow: none; margin: 0 0 3px 5px; vertical-align: sub;" src="./assets/images/medidata.png"/>
- @jatcwang

:::

## This talk

::: nonincremental

- **Type Safety** - What and why?
- Principles
- Practical techniques for Scala

:::

## Static Types & Type Safety

::: nonincremental

<ul>
  <div class="fragment">
    <li>Static types</li>
    <ul>
      <li>Prevent incorrect behaviour, without running the program</li>
    </ul>
  </div>
  <div class="fragment">
    <li>Type Safety is "how much" we take this approach</li>
  </div>
</ul>



:::

::: notes
Typesafety is the "how much" we try to maximise what
the compiler can check for us
:::

## The Goal

What do we want as software developers?

- Write **correct** & **maintainable** software **faster**!
- I believe that a language with good **static type systems** really helps!
  - Tooling: Autocomplete, code browsing, refactoring
  - Understanding the code
  - Design & Prototyping

## But..

- Consider the trade-offs
  - Ergonomics (in the language you're using)
  - Implementation Complexity / Time
  - Performance
  - Is it understandable?
  - Compile time

# Principles & Techniques

## I. Guard against change

* Code is written once, and changed many times
  * Change in requirement 
  * Change in code architecture (refactoring)
* **Want**: Compile errors where our previous assumptions no longer holds
  * Prompt us to reconsider the logic 
* "If I change a basic assumption I'm making here, will the compiler tell me?"  

## I. Guard against change
### Avoid catch-alls in pattern matching

```scala mdoc:silent
sealed trait User {
  def name: String
}
final case class Guest(name: String) extends User
final case class Member(name: String) extends User
final case class Admin(name: String, level: Int) extends User

def canEdit(role: User): Boolean = {
  role match {
    case _: Guest => false
    case _     => true
  }
}
```

<div class="fragment">

Bug if we add another role that isn't allowed to edit!

</div>

## I. Guard against change
### Avoid catch-alls in pattern matching

```scala mdoc:silent
def canEditV2(role: User): Boolean = {
  role match {
    case _: Guest => false
    case u @ (_: Member | _: Admin) => {
      // u is a User
      // useful if you want to access fields on User class
      true
    }
  }
}
```


## I. Guard against change
### Avoid default parameters

<div class="fragment">

```scala 
// Version 1
final case class User(name: String, age: Int)

// Version 2
final case class User(name: String, age: Int, isAdmin: Boolean = false)
```

</div>

<div class="fragment">

```scala
def createFromLegacyUser(legacyUser: LegacyUser): User = {
  User(legacyUser.name, legacyUser.age)
}
```

</div>

## I. Guard against change
### Avoid default parameters

Use explicitly named values/functions

```scala 
final case class User(name: String, age: Int, isAdmin: Boolean)

object User {
  val defaultUser: User = ...
  // or
  def notAdmin(name: String, age: Int, isAdmin: Boolean = true): User
}
```

## I. Guard against change
### Avoid toString

* Pervasive use of **toString** leads to bugs and bad logs 
  because **toString** is callable on everything!

<div class="fragment">

```scala
final case class Vacancy(job: String, since: Instant)

// Usage
Url(s"company.com/create_vacancy/${vacancy.job}").post.send()
// POST https://company.com/create_vacancy/engineer
```

</div>

<div class="fragment">

```scala
final case class Job(name: String, category: JobCategory)
final case class Vacancy(job: Job, since: Instant)

// Oops
// POST https://company.com/create_vacancy/Job(engineer, 2019-01-01T00:00:00Z)
```

</div>

## I. Guard against change
### Avoid toString

Solution 1:

- Use a custom interpolator that only allows `String` parameters, 
- Explicit conversion to String 
- Don't use toString unless you're forced to (e.g. Int)
  
<div class="fragment">

```scala
// Compile error! vacancy.job is not a String!
Url(str"company.com/create_vacancy/${vacancy.job}").post.send()

// Compiles, all interpolated values are type String!
Url(str"company.com/create_vacancy/${vacancy.job.name}").post.send()
```

</div>

## I. Guard against change
### Avoid toString

Solution 2: Custom typeclass

- Write custom typeclass e.g. `UrlShow`
- Provide typeclasses instances for types that are safe to be printed
- Scala allow custom string interpolators (For example see `cats.Show`)

<div class="fragment">

```scala
Url(url"company.com/jobs/${vacancy.job.value}?page=${someNumber}").get.send()
```

</div>

## II. Make it easy to do the right thing

...and hard to make mistakes

* Use types & language features as guard rails

## II. Make it easy to do the right thing
### Newtypes

Wrap existing types in a new class

<div class="fragment">
::: nonincremental

- Readability - Domain concepts!
- Avoid mistakes
- Improves refactoring
- Enforce additional constraints

:::
</div>

## II. Make it easy to do the right thing
### Newtypes

Which is easier to use and understand?

<style>
    .flex-container {
      display: flex;
      justify-content: center;
    }
    .half {
      width: 40%;
    }
    
    .half .sourceCode {
      padding: 5px;
    }
</style>

<div class="flex-container">

<div class="half">

```scala
final case class Directory(
  id: UUID,
  ownerId: UUID,
  parentId: Option[UUID],
  name: String
)
```

</div>

<div class="half">

```scala
final case class Directory(
  id: DirectoryId,
  ownerId: UserId,
  parentId: Option[DirectoryId],
  name: DirectoryName
)
```

</div>


## II. Make it easy to do the right thing
### Newtypes

How?

<div class="fragment">
::: nonincremental
* Scala 2: **Case class + AnyVal**, or [newtype](https://github.com/estatico/scala-newtype) library
* Scala 3: **Opaque Type**
:::
</div>

<div class="fragment" style="margin: 20px 0 20px 0;">

```scala
final case class DirectoryId(uuid: UUID) extends AnyVal 

// Use it like any other case class
DirectoryId(someUuid)
```

</div>

<div class="fragment">

::: nonincremental

- **AnyVal** will avoid allocating the wrapper class*
  - Reduce allocations (GC) and indirection
  - *Allocations are still incurred in some cases
- **newtype** library does not suffer from this issue

:::

</div>
  
## II. Make it easy to do the right thing
### Newtypes with constraints

Enforce constraints using wrapper classes

<div class="fragment">

::: nonincremental

- We want:
  - No direct construction (`new`, `apply`) nor `copy`
  - `unapply`, `hashCode` and `equals`
  
:::

</div>
    
<div class="fragment">

```scala
final class private DirectoryName // Need to implement equals manually :(
final case class DirectoryName private (str: String) // .copy still accessible :(
```

</div>

## II. Make it easy to do the right thing
### Newtypes with constraints

Introducing `sealed abstract case class`!!

* No `copy`, `new` nor `apply`
* `unapply`, `equals`, `hashCode` still works


<div class="fragment">
::: nonincremental

Recommendation:

* A validated construction function
  * Returns **Either / Validated (cats)**
* An unsafe construction (explicitly marked unsafe)
  * Throws exception if an invalid input is provided

:::
</div>

## II. Make it easy to do the right thing
### Newtypes with constraints

<div class="fragment">

```scala mdoc:invisible
sealed trait DirectoryNameError extends Throwable
object DirectoryNameError {
  case object StringIsEmpty extends DirectoryNameError
}
```

```scala mdoc
sealed abstract case class DirectoryName(strValue: String)

object DirectoryName {
  def fromString(str: String): Either[DirectoryNameError, DirectoryName] = {
    if (str.isEmpty)
      Left(DirectoryNameError.StringIsEmpty)
    else
      // Use anonymous subclass (allowed only in this file) to create an instance
      Right(new DirectoryName(str) {}) // ###
  }

  import cats.syntax.either._ // Provides .valueOr extension method
  // For tests or parsing from trusted/validated sources (e.g. Database)
  def fromStringUnsafe(str: String): DirectoryName = {
    fromString(str).valueOr(e  => throw e)
  } 
}
```

</div>

## II. Make it easy to do the right thing
### Named Parameters

Named parameters improves readability and help spot mistakes

```scala
def calculateTotal(
  items: List[Item],
  addTax: Boolean,
  addServiceCharge: Boolean
)
```

## II. Make it easy to do the right thing
### Named Parameters

```scala
def printReceipt(items: List[Item], options: PrintOption)
calculateTotal(
  items,
  options.addServiceCharge, // bug
  options.addTax,
)
```

```scala
def printReceipt(items: List[Item], options: PrintOption)
calculateTotal(
  items,
  addServiceCharge = options.addTax, // Aha!
  addTax = options.addServiceCharge,
)
```

## III. Build abstractions with types

* Use types to help build abstractions and communicate intent...and don't lie!
* Good abstractions 
  * Reduces cognitive overhead when reading code
  * Allows aggresive refactoring 

## III. Build abstractions with types
### Parametricity

* Obeying what we know about a type
* No reflections

<div class="fragment">

```scala
def f[T](list: List[T]): List[T]

f(List(1,2,3)) 
// List(1)

f(List(1.0, 2.0, 3.0)) 
// List(1.0)

f(List("1", "2", "3")) 
// Nil ??!!!
```

</div>

<div class="fragment">

```scala
// Don't do this!
list match {
  case (s: String) :: rest => Nil
  case s :: rest => List(s)
  // ...
}
```

</div>

## III. Build abstractions with types
### Parametricity

Solution: Use typeclasses! (or subtype constraints)

```scala 
def f[T](list: List[T])(implicit monoid: Monoid[T]): T = {
  // Might use "empty" and "combine" from the Monoid typeclass instance of T
}
```

## III. Build abstractions with types
### Use an IO type!

Use a referentially transparent IO type as found in cats-effect, Monix or ZIO

* Refactor freely because side-effect definition is separate from construction!

<div class="fragment">

```scala
// Refactoring this will change the behaviour :(
for {
  _ <- Future { println("hi") }
  _ <- Future { println("hi") }
} yield ()
```

</div>

* Cancellation, Retries, Parallelism

<div class="fragment">

```scala
// Usine ZIO
def task1: IO[Int] = IO { /* Side effect here */ }
def tasks: List[IO[Int]] = List(task1, task2, task3)

ZIO.sequence(tasks)    // Sequential execution
ZIO.sequencePar(tasks) // Parallel execution
task1.race(task2)      // First success is returned, other is cancelled
```

</div>

## Recap

- Guard against change
- Make it easy to do the right thing
- Build abstractions with types

<div class="fragment">
In practice:
</div>

- Avoid default parameters
- Avoid catch-alls in pattern matching
- Avoid toString
- Newtypes
- Used named parameters
- Parametricity
- Use IO

## Final Thoughts

* Learn, but verify - "Does it really solve a problem for me?"
* The principles applies to other languages too!
* Always keep the trade-offs in mind

# References

::: nonincremental

* [Constraints Liberate, Liberties Constrain](https://www.youtube.com/watch?v=GqmsQeSzMdw) by Rúnar Bjarnason
* [Cats Effect: The IO Monad for Scala](https://www.youtube.com/watch?v=GqmsQeSzMdw) by Gabriel Volpe
* [Scaluzzi](https://github.com/vovapolu/scaluzzi) - Linting rules with Scalafix

:::

# Thank you!
