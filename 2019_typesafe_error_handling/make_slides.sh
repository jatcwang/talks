#! /bin/bash

FILE="target/mdoc/presentation.md"
pandoc -t revealjs --standalone -o public/output.html "$FILE" -i -V revealjs-url=https://unpkg.com/reveal.js@4.0.2/ --mathjax --slide-level=2 --css assets/style.css -V transition=none -V theme=white --highlight-style=pygments && echo 'DONE'
