#! /bin/bash

echo target/scala-2.12/tut/presentation.md | entr pandoc -t revealjs -s -o public/output.html target/scala-2.12/tut/presentation.md -i -V revealjs-url=http://lab.hakim.se/reveal-js --mathjax --slide-level=2 --css style.css -V transition=none -V theme=white --highlight-style=pygments
