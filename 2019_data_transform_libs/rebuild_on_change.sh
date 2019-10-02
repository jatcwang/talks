#! /bin/bash

find target -name '*.md' | entr -c -d ./make_slides.sh
