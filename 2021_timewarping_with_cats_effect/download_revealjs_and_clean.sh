#! /bin/bash

set -euo pipefail

RJS_VERSION='4.1.0'

mkdir -p public
curl -L "https://github.com/hakimel/reveal.js/archive/$RJS_VERSION.tar.gz" | tar xvz -C ./public

rm -rf "reveal.js-$RJS_VERSION"
mv "./public/reveal.js-$RJS_VERSION" "./public/revealjs"

# Remove all unnecessary file
cd public/revealjs
fd --hidden --no-ignore --maxdepth=1 | rg --invert-match "(plugin|css|dist)" | xargs safe-rm -rf
