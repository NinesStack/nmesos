# Contributing

## Contributor

* Create an issue (to get agreement on what you want to add/change)
* Fork the repo
* Make the changes (inlcuding writing the test(s) to verify/validate the changes)
* Create a PR (linking back to the issue; explain what/why/how you changed the code)
* Note: Please keep the `.gitignore` file clean of your personal build/IDE artifacts by using/setting up a global gitignore file on your side (probably a good idea/practice anyway; `git config --global core.excludesfile ~/.gitignore_global`) 

## Maintainers

* Review the PR (discussing the change/implementation; making changes as necessary)
* Approve the PR
* Merge the PR
* Relase a new version (optional) ...
  * On the `master` branch ...
  * Manually bump the version in `build.sbt`
  * [nmesos cli](cli/README.md) release process ...
    * Build a new distributable `.tgz` with ...
      * `sbt clean`
      * `sbt nmesos-shared/publishLocal`
      * `sbt nmesos-cli/assembly`
      * `sbt nmesos-cli/universal:packageZipTarball`
    * Publish the new version in a public repo ...
      * `sbt nmesos-cli/universal:publish`
  * (optional) Update the `brew` [Formula](Formula/nmesos-cli.rb)
    * Modify url to the new distributable and `sha256`
    * Push to `master`
