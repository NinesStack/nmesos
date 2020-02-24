# Contributing

## Contributor

* Create an issue (to get agreement on what you want to add/change)
* Fork the repo
* Make the changes (inlcuding writing the test(s) to verify/validate the changes)
* Create a PR (linking back to the issue; explain what/why/how you changed the code)

## Maintainers

* Review PR (discussing the change/implementation; making changes as necessary)
* Approve PR
* Merge PR
* Relase a new version (optional) ...
  * Manually bump the version at `build.sbt`
  * [nmesos cli](cli/README.md) release process:
    * Build a new distributable `.tgz` with:
    `sbt clean "nmesos-shared/publishLocal"  "nmesos-cli/assembly" nmesos-cli/universal:packageZipTarball
    * Publish the new version in a public repo:
    `sbt nmesos-cli/universal:publish`
  * (optional) Update the `brew` [Formula](Formula/nmesos-cli.rb)
    * Modify url to the new distributable and `sha256`
    * Publish to Master
