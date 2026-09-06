---
title: Difflicious
layout: cover
class: cover-slide
---

<div class="eyebrow">London Scala User Group · Aug 2026</div>

# Difflicious

## Diffs for humans and <span class="robot">🤖</span>

<div class="presenter">Jacob Wang</div>

---
class: intro-slide
---

# Hello 👋

- **Writing Scala since 2015**
- **Library maintainer** — Difflicious, Doobie, and more
- **@jatcwang** on GitHub, Mastodon, and Bluesky

---
class: fact
---

# We love tests!

---
class: hate-tests-slide
---

# We hate tests!

<v-clicks>

- Writing tests
- Deciphering test failures!

</v-clicks>

---
class: failure-slide
---

# We hate tests!

```text
[info]   Order("order-123", 83.97, "42 Scala Lane, London, SW1A 1AA", List(Package(List(Item("item-1", "Cushion", 2, 19.99), Item("item-2", "Custom T-shirt", 1, 24.99)), InTransit), Package(List(Item("item-3", "Printed tote bag", 2, 19.00)), Preparing))) did not equal Order("order-123", 83.97, "42 Scala Lane, London, SW1A 1AA", List(Package(List(Item("item-1", "Engraved mug", 2, 19.99), Item("item-2", "Custom T-shirt", 1, 24.99)), InTransit), Package(List(Item("item-3", "Printed tote bag", 1, 19.00)), Preparing))) (OrderScalatestSuite.scala:13)
```

---

# We hate tests!

<p></p>

MUnit / Weaver are better with textual diffs

<img src="/munit_diff.png" alt="MUnit diff showing changed item names and quantities" style="display: block; width: 60%; margin: 1.5rem auto;" />

---

# We hate tests!

<p></p>

However, textual diff are less useful for more complex cases

<img src="/munit_diff_big.png" alt="MUnit diff showing changed item names and quantities" style="display: block; width: 40%; margin: 1rem auto;" />

---

# Difflicious

<v-clicks>

- Structural diffs
- Configurable comparison
- Readable diff results
  - Interactive terminal UI for humans 👥
  - Plain output modes for AIs 🤖
- Integrates with popular test frameworks
  
</v-clicks>
  
---

# Difflicious

````md magic-move

```scala {all|6|9|all}{lines:true}
import difflicious.Differ
import difflicious.munit.MUnitDiffliciousSuite

class OrderTest extends FunSuite with MUnitDiffliciousSuite {

  val orderDiffer: Differ[Order] = Differ.derivedDeep[Order]

  test("order matches expectation") {
    orderDiffer.assertNoDiff(actualOrder, expectedOrder)
  }

}
```

```scala {all}{lines:true}
import difflicious.Differ
import difflicious.munit.MUnitDiffliciousSuite

class OrderTest extends FunSuite with MUnitDiffliciousSuite {

  val orderDiffer: Differ[Order] = Differ.derivedDeep[Order]
    .ignoreAt(_.packages.each.packageId)

  test("order matches expectation") {
    orderDiffer.assertNoDiff(actualOrder, expectedOrder)
  }

}
```

```scala {all}{lines:true}
import difflicious.Differ
import difflicious.munit.MUnitDiffliciousSuite

class OrderTest extends FunSuite with MUnitDiffliciousSuite {

  val orderDiffer: Differ[Order] = Differ.derivedDeep[Order]
    .ignoreAt(_.packages.each.packageId)
    .configure(_.packages)(_.pairBy(_.packageId))

  test("order matches expectation") {
    orderDiffer.assertNoDiff(actualOrder, expectedOrder)
  }

}
```

````

---

# Demo time!

---

# Other challenges 

<p></p>

When comparing complex data, we might need to:

<p style="margin-top: 0.5rem"></p>

- Skip comparing a subset
- Customize how list elements are matched for comparison

---

# Not always in control

<img src="/orders.excalidraw.svg" style="display: block; width: 40%; margin: 1.5rem auto;" />

- Database-generated IDs
- Non-deterministic order of returned values

--- 

# Thanks! Questions?
