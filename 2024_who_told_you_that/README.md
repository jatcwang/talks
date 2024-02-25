


# Instructions to build the slides
Pre-reqs to run scripts:

-  curl, tar, 
-  pandoc (for building the md into reveal.js html slides)
-  entr for watching file changes

Steps:

-  `./download_revealjs_and_clean.sh` to download revealjs artifacts and store it locally
- `sbt mdoc --watch` to generate the compiled/Scala-rendered md file
- `rebuild_on_change.sh` will run `make_slides.sh` (pandoc) whenever the md file changes
- Open public/output.html in browser. Refresh when change is made
