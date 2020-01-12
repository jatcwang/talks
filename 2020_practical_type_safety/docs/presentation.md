# Practical Type Safety

---

<style>
  .no-box img {
    box-shadow: none!important;
  }
</style>

## Hello

- Scala Developer at <img style="box-shadow: none; margin: 0 0 3px 5px; vertical-align: sub;" src="./assets/images/medidata.png"/>
- @jatcwang

## This talk

- The purpose of static types
- Empower you to think for yourselves 
- Practical tips when working in Scala

## The Goal

What do we want as software developers?

- Write correct, maintainable software faster
- Static type system is one of the best way towards the goal

<div class="fragment">
    <div class="figure no-box" style="height: 335px; ">
    </div>
</div>

## Static types as our Strategy

- Catch mistakes early, or prevent it!
- Tooling
  - e.g. Autocomplete, code browsing, refactoring
- Helps 
  - Design & write code
  - Understand the codebase

## I. Guard against change

Many types of change:

* Change in requirement
* Change in code architecture / code modelling

--- 

### Avoid default case class fields and parameters

<div class="fragment">

```scala 
final case class User(name: String, age: Int)

// V2
final case class User(name: String, age: Int, isAdmin: Boolean = true)
```

</div>

<div class="fragment">

```scala
def createFromLegacyUser(legacyUser: LegacyUser): User = {
  User(legacyUser.name, legacyUser.age)
}
```

</div>

--- 

### Avoid default case class fields and parameters

Use explicitly named values/functions

```scala 
final case class User(name: String, age: Int, isAdmin: Boolean)

object User {
  def withDefault(name: String, age: Int, isAdmin: Boolean = true): User
}
```

---

### Avoid catch-alls in pattern matching

```scala mdoc:silent
sealed trait User {
  def name: String
}
final case class Guest(name: String) extends User
final case class Member(name: String) extends User
final case class Admin(name: String, isSuperAdmin: Boolean) extends User

def canEdit(role: User): Boolean = {
  role match {
    case _: Guest => false
    case _     => true
  }
}
```

<div class="fragment">

Bug if we add another role which should not be able to edit

</div>

---

```scala mdoc:silent
def canEditV2(role: User): Boolean = {
  role match {
    case _: Guest => false
    case u @ (_: Member | _: Admin) => true // Can access u.name!
  }
}
```

## II. Make it easy to do the right thing

...and hard to make mistakes

<div class="fragment">
Use types & language features help you write the correct code
</div>

--- 

### "Newtypes" / Opaque types

Wrap existing types in a new class 

- Readability
- Avoid mistakes when using the type
- Enforce additional constraints

```scala
final case class Directory(id: UUID, parentId: Option[UUID], name: String)

// Safer and more readable
final case class Directory(id: DirectoryId, parentId: Option[DirectoryId], name: DirectoryName)
```

---

---

#### Simple newtype

```scala
final case class DirectoryId(uuid: UUID) extends AnyVal 

// Use it
val dirId = DirectoryId(someUuid)
```

- `AnyVal` can help avoiding an actual `DirectoryId` class most of the time.
  - Reduce allocations and thus less GC

#### Newtype with constraints

- Requirements:
  - When you see a `DirectoryName`, you know it is not-empty
  - No way to cheat around validation (No direct `new`, `copy`)
  - Still get extractors and `equals`
    
<div class=""fragment">

```scala
final class private DirectoryName // Need to implement equals manually :(
final case class DirectoryName private (str: String) // .copy still accessible :(
```

</div>

---

#### Newtype with constraints

```scala 
// No new/apply, no subclassing (outside this file), no copy
// Extractors, .equals are available
sealed abstract case class DirectoryName(str: String)

object DirectoryName {
  def fromString(str: String): Either[DirectoryNameError, DirectoryName] = {
    if (str.empty)
      Left(DirectoryNameError.StringIsEmpty)
    else
      // Use anonymous subclass (allowed only in this file) 
      // to create an instance
      Right(new DirectoryName(str) {})
  }

  // For tests or parsing from trusted/validated sources (e.g. Database)
  def fromStringUnsafe(str: String): DirectoryName = {
    fromString(str).getOrElse(e => throw e)
  } 
}
```

--- 

### Use named parameters

```scala
def calculateTotal(
  items: List[Item],
  addTax: Boolean,
  addServiceCharge: Boolean
)
```

```scala
def printReceipt(items: List[Item], options: PrintOption)
calculateTotal(
  items,
  options.addServiceCharge, // oops
  options.addTax,
)
```

---

```scala
def printReceipt(items: List[Item], options: PrintOption)
calculateTotal(
  items,
  addServiceCharge = options.addServiceCharge,
  addTax = options.addTax,
)
```

## III. Don't lie

* Types enables abstractions
Type signature communicates what a piece of code can and cannot do.

```scala
def f[T](list: List[T]): List[T]

f(List(1,2,3))
f(List("1","2","3"))
f(List(1,2,3))
```

```scala mdoc
def transform[T](list: List[T]): List[T] = {
  list match {
    case (s: String) :: Nil => list.headOption.toList
    case s :: Nil => list
    case _ => list
  }
}
```

FIXME parametricity

# Conclusion

Fixme 
* it's about trust in the abstractions

## FIXME

Principles:
- A semantic code change should be compile errors

- Avoid default parameters
- Use named parameters
- Make illegal state unrepresentable
- New type wrappers (including unsafeFromString)


