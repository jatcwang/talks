---
theme: seriph
# some information about your slides (markdown enabled)
title: Error handling
# apply UnoCSS classes to the current slide
class: text-center
# https://sli.dev/features/drawing
drawings:
  persist: false
---

# A tour of error handling in Scala

---

# Errors! Failures! Faults! 💥

Errors are a fact of life for (almost) any program

<v-clicks>

- Bad user input
- Expected random failures (e.g. network failures)
- Bugs in the code itself!

</v-clicks>

---

# Modeling errors with types

Why?

<ul style="list-style: none">
<li>➕ asdf</li>
<li>➕ asdf</li>
</ul>
--- 

# EitherT

````md magic-move
```scala
ohNo:
  hello.EitherT[A, B]
```
```scala
ohNo:
  hello.EitherT[A, B].ohno
```
````
