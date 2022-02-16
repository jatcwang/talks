<p style="font-size: 4rem; margin: 0">
<span style="color: #047804">Diff</span><span style="color: #b60000;">licious</span>
</p>
<p style="margin: 0">
<span style="font-size: 2rem; color: #504f4f">Readable and Flexible Diffs</span>
</p>

<div style="font-size: 2rem;">
Jacob Wang
</div>

<span style="font-size: 1.5rem">Scala Love 2022</span>

<style>
  .no-box img {
    box-shadow: none!important;
  }
  
  .slides {
    margin-top: 30px!important;
  }
  
  .reveal .slides h3 {
    margin-top: -20px;
    color: brown;
    text-transform: inherit;
  }

  .reveal h2 {
    text-transform: none;
  }

  .reveal pre code {
    max-height: 800px;
    font-size: 16px;
  }

  .reveal code {
  }

  .diff-render {
      background: aliceblue;
      border-radius: 5px;
      padding: 15px!important;
      white-space: pre;
  }

  .diff-render .red {
     color: red;
  }

  .diff-render .green {
     color: green;
  }

  .diff-render .gray {
     color: gray;
  }
</style>

## Hello

::: nonincremental

- Software Developer
  at <img style="box-shadow: none; margin: 0 0 3px 5px; vertical-align: sub;" src="./assets/images/medidata.png"/>
- @jatcwang
- Talk to me about types, FP, libraries and tools :)

:::

## It's Friday afternoon...

<p style="margin-bottom: 0;">
<img class="fragment no-box" width="600px" src="assets/images/lego.jpeg" />
</p>

<p style="margin-top: 0;">
<span style="font-size: 50px; " class="fragment"> "If it compiles, it works" </span>
</p>

## But the tests are failing...

<img class="fragment no-box" width="800px" src="assets/images/test_failure.jpg" />
<img class="fragment no-box" width="400px" src="assets/images/angry_computer_guy.jpg" />

## That's enough for the day

<div class="fragment">
<img class="no-box" width="600px" src="assets/images/goats.jpg" />

A career in goat farming looks great all of a sudden...
</div>

## Tests should be nicer

- Tell what's wrong at a glance
- Need more flexibility in our comparisons
- <span style="color: #047804">Diff</span><span style="color: #b60000;">licious</span> to the rescue!

# Difflicious Step-by-step

<ul style="line-height: 1.4">
  <li class="fragment">Add **difflicious** to your test dependencies
    <ul>
      <li>difflicious-munit</li>
      <li>difflicious-scalatest</li>
      <li>Scala 2.13 & 3</li>
    </ul>
  </li>
  <li class="fragment"> Create **Differ**s (derive + configure)
  <ul>
    <li class="fragment">Differ.apply (e.g. `Differ[List[Int]]`) to "summon" an instance</li>
  </ul>
  </li>
  <li class="fragment">Use **Differ**s to diff values</li>
</ul>

# A Simple Example

```scala mdoc:invisible
import example.Helpers._
import difflicious.Differ
import difflicious.implicits._
```

```scala
import difflicious.Differ
import difflicious.implicits.*
import difflicious.munit.MUnitDiff.*

final case class Foo(i: Int, s: String)

// Create the differ
val differ: Differ[Foo] = Differ.derived

val expected = Foo(1, "a")
val actual = Foo(1, "b")

// Assert no difference between two values
differ.assertNoDiff(actual, expected)
```

<pre class="diff-render fragment">
Foo(
  i: 1,
  s: <span class="red">"b"</span> -> <span class="green">"a"</span>
)
</pre>

# Data structures diffs

* **Seq**, **Map** and **Set** are supported by default
* **Seq** pair by index when diffing
* **Map** pair by key when diffing
* **Set** uses `==`

<div class="fragment">
```scala
val expected = Vector(
  Map(
    "Bono" -> Set("Plant"),
    "Sven" -> Set("Plates")
  )
)
val actual = Vector(
  Map(
    "Bono" -> Set("Plant", "Plates")
    // Sven?
  )
)
  
// "Summon" an instance using Differ.apply
val differ = Differ[Vector[Map[String, Set[String]]]]

differ.assertNoDiff(actual, expected)
```

</div>

[//]: # (```scala mdoc:invisible)

[//]: # (printHtml&#40;Differ[Vector[Map[String, Set[String]]]].diff&#40;actual, expected&#41;&#41;)

[//]: # (```)

---

<pre class="diff-render">
Vector(
  Map(
    "Bono" -> Set(
        "Plant",
        <span class="red">"Plates"</span>
      ),
    <span class="green">"Sven"</span> -> <span class="green">Set(</span>
 <span class="green">       "Plates",</span>
 <span class="green">     )</span>
  ),
)
</pre>

:::nonincremental

From the diff we see that:

* Bono unexpectedly showed up with Plates
* Sven is missing

:::

# Pairing things up

:::nonincremental

* Sometimes, The default diffing behaviour of **Seq** and **Set** isn't what we want

:::

```scala mdoc:invisible
final case class Cat(name: String, age: Int)

object Cat: 
  given Differ[Cat] = Differ.derived
```

<div class="fragment">
```scala
val expectedCats = List(
  Cat("Bono", 8),
  // Sven?
  Cat("Lucy", 14)
)

val actualCats = List(
  Cat("Bono", 7),
  Cat("Sven", 3)
  // Lucy?
)

Differ[List[Cat]].assertNoDiff(actualCats, expectedCats)
```
</div>

[//]: # (```scala mdoc:invisible)

[//]: # (printHtml&#40;Differ[List[Cat]].diff&#40;actualCats, expectedCats&#41;&#41;)

[//]: # (```)

<pre class="diff-render fragment">
List(
  Cat(
    name: "Bono",
    age: <span class="red">7</span> -> <span class="green">8</span>
  ),
  Cat(
    name: <span class="red">"Sven"</span> -> <span class="green">"Lucy"</span>,
    age: <span class="red">3</span> -> <span class="green">14</span>
  ),
)
</pre>

# Pairing things up (2)

::: nonincremental

- We can use `pairBy` to get more meaningful diffs!

:::

::: incremental

<div class="fragment">
```scala
val catsDiffer = Differ[List[Cat]].pairBy(_.name)

catsDiffer.assertNoDiff(actualCats, expectedCats)

```

[//]: # (```scala mdoc:invisible)

[//]: # (printHtml&#40;catsDiffer.diff&#40;actualCats, expectedCats&#41;&#41;)

[//]: # (```)

<pre class="diff-render fragment">
List(
  Cat(
    name: "Bono",
    age: <span class="red">7</span> -> <span class="green">8</span>
  ),
  <span class="red">Cat(</span>
 <span class="red">   name: "Sven",</span>
 <span class="red">   age: 3</span>
 <span class="red"> )</span>,
  <span class="green">Cat(</span>
 <span class="green">   name: "Lucy",</span>
 <span class="green">   age: 14</span>
 <span class="green"> )</span>
)
</pre>
</div>

:::

## Ignoring fields

* Sometimes, we **can't** or **don't** want to compare certain fields
* e.g. Externally generated IDs, out-of-context data for current test

<div class="fragment">

```scala
// Register dogs, returning dogs with their ID 
def registerDogs(dogData: List[DogData]): List[Dog] =
  
  val dogIds = writeToDatabaseReturningIds(dogData)

  fetchDogsById(dogIds) // We cannot predict the IDs generated!
```

```scala mdoc:silent
final case class DogData(name: String)
final case class Dog(id: Int, name: String)
```

```scala mdoc:invisible
def registerDogs(dogData: List[DogData]): List[Dog] =
  List(Dog(3, "Wolfy"), Dog(5, "Bambi"))
```

</div>

## Ignoring fields (2)

Let's ignore dog IDs from comparison

```scala
given Differ[Dog] = Differ.derived

val expectedDogs = List(Dog(0, "Wolfy"), Dog(0, "Bamboo"))

val dogData = List(DogData("Wolfy"), DogData("Bamboa"))
val actualDogs = registerDogs(dogData)
```

<div class="fragment">
```scala
// Setup differ with field ignores
val dogDiffer: Differ[List[Dog]] = Differ[List[Dog]].ignoreAt(_.each.id)

dogDiffer.assertNoDiff(actualDogs, expectedDogs)
```
</div>

[//]: # (```scala mdoc)

[//]: # (printHtml&#40;dogDiffer.diff&#40;actualDogs, expectedDogs&#41;&#41;)

[//]: # (```)

<pre class="diff-render fragment">
List(
  Dog(
    id: <span class="gray">[IGNORED]</span>,
    name: "Wolfy",
  ),
  Dog(
    id: <span class="gray">[IGNORED]</span>,
    name: <span class="red">"Bamboa"</span> -> <span class="green">"Bamboo"</span>,
  ),
)
</pre>

## Deep configuration!

* **.ignoreAt** (and friends) takes a "path expression"
    * **\_.each.id** from the previous example
* Deep configuration allow us to quickly tweak our Differ for our current test

<div class="fragment" style="font-size: 1.3rem; margin: 10px 5%">
| Differ Type  | Allowed Paths          | Explanation                                                       |
| --           | --                     | --                                                                |
| Seq          | .each                  | Traverse down to the Differ used to compare the elements          |
| Set          | .each                  | Traverse down to the Differ used to compare the elements          |
| Map          | .each                  | Traverse down to the Differ used to compare the values of the Map |
| Case Class   | (any case class field) | Traverse down to the Differ for the specified sub type            |
| Sealed Trait / Enum | .subType[SomeSubType]  | Traverse down to the Differ for the specified sub type            |
</div>

## Putting it all together

```scala mdoc:silent
sealed trait Person:
  def name: String
  
object Person:
  case class Employee(name: String, attire: String) extends Person
  case class Contractor(name: String) extends Person

case class OfficeCapacity(
  home: List[Person],
  office: List[Person]
)
```

```scala
// Derive default differs
given Differ[Person] = Differ.derived
given Differ[OfficeCapacity] = Differ.derived

val officeCapacityDiffer = Differ[OfficeCapacity]
  .ignoreAt(_.home.each.subType[Employee].attire) // Pants optional!
  .configure(_.home)(d => d.pairBy(_.name))
```

---

<pre class="diff-render">
OfficeCapacity(
  home: Set(
    Employee(
      name: "Sarah",
      attire: <span class="gray">[IGNORED]</span>,
    ),
    <span class="red">Contractor</span> != <span class="green">Employee</span>
    <span class="red">=== Obtained ===
    Contractor(
      name: "Paolo",
    )</span>
    <span class="green">=== Expected ===
    Employee(
      name: "Paolo",
      attire: [IGNORED],
    )</span>,
  ),
  office: Map(
    1 -> Employee(
        name: "Percy",
        attire: <span class="red">"casual"</span> -> <span class="green">"suit"</span>,
      ),
  ),
)</pre>

## Final notes

<div class="fragment">

:::nonincremental

- For **IntelliJ**, you'd need to adjust some settings to not make all test failure color red
    - Editor | Color Scheme | Console Colors | Console | Error Output, uncheck the red foreground color

:::

</div>

## Thank you!

:::nonincremental

- SoftwareMill
    - Many inspirations from **diffx**
- EPFL & anyone who contributed to Scala 3
    - Macros are tremendous fun

:::


