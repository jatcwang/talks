
# Data transformation

without hurting your eyes

<span style="font-size:22px;">Jacob Wang</span>

## About me

- Scala Developer at <img style="box-shadow: none; margin: 0 0 3px 5px; vertical-align: sub;" src="./assets/images/medidata.png"/>
- Typed FP and other useful tools
- @jatcwang

## Data transformation

- As developers, we shuffle and transform data all day
- Boring, boilerplatey, error prone?
- Let's look at 3 Scala libraries that sparks joy :)

## Scala

- **sealed traits** and **case classes** are great for modeling domain data
<div class="fragment">

```scala mdoc
final case class Employee(name: String, age: Int)

Employee("Nat", 30).copy(name = "Tat")
```
</div>

## Welcome to the real world

```scala mdoc:reset:invisible
import java.time.LocalDate
final case class EmployeeId(str: String)
```
```scala mdoc
final case class Employee(
  id: EmployeeId,
  firstName: String,
  lastName: String,
  addresses: List[Address],
  dateOfBirth: LocalDate,
  // ...probably 10 other fields
)
```

```scala mdoc:silent
final case class Address(
  streetNumber: String,
  line1: String,
  line2: String,
  country: String,
  postCode: String,
  verified: Boolean
)
```

It can't be that bad...right?


```scala mdoc:invisible
val employee = Employee(
  id = EmployeeId("id1"),
  firstName = "Hope",
  lastName = "Atkins",
  addresses = List(
    Address(streetNumber = "15", line1 = "Candy Lane", line2 = "", country = "United Kingdom", postCode = "3333", verified = false),
    Address(streetNumber = "50", line1 = "Donut St", line2 = "", country = "United Kingdom", postCode = "3333", verified = false),
  ),
  dateOfBirth = LocalDate.parse("1985-04-01")
)
```

--- 

```scala mdoc:silent
val updatedEmployee = employee.copy(
  addresses = employee.addresses.map(
    a => a.copy(verified = true)
  )
)
```

or worse...

```scala
employee.copy(
  company = employee.company.copy(
    address = employee.company.address.copy(
      street = employee.company.address.street.copy(
        name = employee.company.address.street.name.capitalize
      )
    )
  )
)
```

Can we do better?

# Quicklens

- Update deeply nested immutable data effortlessly

<div class="fragment">

```scala mdoc
import com.softwaremill.quicklens._
employee.modify(_.addresses.each.country).setTo("Kingdom")
```
</div>

### More quicklens

```scala mdoc:silent
sealed trait Animal
case class Dog(age: Int) extends Animal
case class Cat(ages: List[Int]) extends Animal

case class Zoo(animals: List[Animal])

val zoo = Zoo(List(Dog(4), Cat(List(1,2,3,4,5,6,7,8,9))))
```

```scala mdoc
val olderZoo = zoo.modifyAll(
  _.animals.each.when[Dog].age,
  _.animals.each.when[Cat].ages.at(0)
).using(_ + 1)
```

... and support for more types like **Either** and **Map**!

## Transforming to similar structures

- We create a new case class **EmployeeV2** with some extra fields
- Want code to operate mostly on EmployeeV2
  - Need a function **Employee** => **EmployeeV2**

```scala mdoc:invisible
final case class EmployeeV2(
  id: EmployeeId,
  firstName: String,
  lastName: String,
  addresses: List[Address],
  dateOfBirth: LocalDate,
  level: Int,
)

def calculateLevel(): Int = 3
```

<div class="fragment">

```scala mdoc:silent
def toEmployeeV2(em: Employee): EmployeeV2 = {
  EmployeeV2(
    id = employee.id,
    firstName = employee.firstName,
    lastName = employee.lastName,
    addresses = employee.addresses,
    dateOfBirth = employee.dateOfBirth,
    level = calculateLevel(),
  )
}
```

</div>

# Chimney

- Boilerplate-free data transformation
- Transform from one case class / sealed trait to another, mapping fields by name and type

<div class="fragment">

```scala mdoc
import io.scalaland.chimney.dsl._

val employeeV2 = employee.into[EmployeeV2]
  .withFieldComputed(_.level, _ => calculateLevel())
  .transform
```
</div>

<div class="fragment">

If we forgot to provide value for a missing field...
```
level: scala.Int - no accessor named level in source type repl.Session.App1.Employee
```
</div>

## More Chimney transformations!

```scala mdoc:silent
final case class User(id: String, firstName: String, lastName: String, years: Int)
final case class UserV2(id: String, name: String, age: Int, extraInfo: String)

val userV1 = User("user1", "John", "Doe", 10)
```

```scala mdoc
userV1.into[UserV2]
  .withFieldComputed(_.name, u => s"${u.firstName} ${u.lastName}")
  .withFieldRenamed(_.years, _.age)
  .withFieldConst(_.extraInfo, "")
  .transform
```

## Time to test your code!

- You found a bug
- But the test failure looks like this

<div class="fragment">

```
EmployeeV2(EmployeeId(id1),Hope,Atkins,Address(15,Doe St,,United Kingdom,3333),1985-04-01,0) was not equal to EmployeeV2(EmployeeId(id1),Hope,Atkins,Address(15,Doe St,,United Kingdom,3333),1985-04-01,3)
```

</div>

# Diffx

- Human-readable data diffs

<div class="fragment">

```scala
class EmployeeSpec extends WordSpec with Matchers with DiffMatcher {
  "some test" in {
    val actual = toEmployeeV2(employeeV1)
    actual should matchTo(employeeV2)
  }
}
```

</div>

<div class="fragment">

![](./assets/images/diffx_failure.jpg)
</div>


## Diffx features

- Diff case classes and standard library types
- Add support to your own types/libraries by implementing typeclass instances

## Summary

- [Quicklens](https://github.com/softwaremill/quicklens) for deep immutable updates (or [Monocle](https://julien-truffaut.github.io/Monocle) + [Goggles](https://github.com/kenbot/goggles))
- [Chimney](https://github.com/scalalandio/chimney) for transforming data of similar structure
- [Diffx](https://github.com/softwaremill/diffx) for readable test failures

## Thank you!

<img src="./assets/images/cat.jpg" style="margin: 0; height: 500px;"/>
