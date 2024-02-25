#! /bin/bash

FILE="target/mdoc/presentation.md"
pandoc -t revealjs --wrap=preserve --standalone --include-after-body "public/body_pre_js.html" -o public/output.html "$FILE" -i -V revealjs-url=./revealjs --mathjax --slide-level=2 --css assets/style.css -V transition=none -V theme=white -V center=false --highlight-style=pygments && echo 'DONE'
