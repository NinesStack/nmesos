# TODOs

* add build badges
  * build status
  * version
  * code coverage
  * number of issues
* make verify work
* deploy digits/test with digits
* add more logging
* make github actions work
  * push/commit triggers build/test
    * PRs can only be merged after build/test was successful
  * push/commit with label release triggers ...
    * build/test
    * assembly/packageZipTarball
    * push/upload tgz to GitHub releases (With changelog/list of PRs that got merged)
    * tag the commit with what is in VERSION.txt (overwriting/removing old tag, if it exists)
* make brew work
* update/finish README
* add scoverage
* increase code coverage
* create full/complete set of open source docs (e.g. CHANGELOG (with list of PRs that got merged since last release))
* replace aws with github (releases)
