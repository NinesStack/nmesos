# Contributing

## Contributor

* Create an issue (to get agreement on what you want to add/change)
* Fork the repo
* Make the changes (inlcuding writing the test(s) to verify/validate the changes)
* Check that you have not decreased test-coverage
  * run `sbt clean coverage nmesos-shared/test nmesos-shared/coverageReport`
  * open `shared/target/scala-2.12/scoverage-report/index.html`
  * and/or run this for `nmesos-cli`
* Create a PR (linking back to the issue; explain what/why/how you changed the code)
* Note: Please keep the `.gitignore` file clean of your personal build/IDE artifacts by using/setting up a global gitignore file on your side (probably a good idea/practice anyway; `git config --global core.excludesfile ~/.gitignore_global`)

## Maintainers

* Note: To publish a new release you need to get access to the S3 bucket ...
  * Contact one of the existing commiters/maintainers to get the aws keys
  * `export AWS_DEFAULT_REGION=eu-west-1`
  * `export AWS_REGION=eu-west-1`
  * `export AWS_ACCESS_KEY_ID=<key id>`
  * `export AWS_SECRET_ACCESS_KEY=<key>`
* Review the PR (discussing the change/implementation; making changes as necessary)
* Approve the PR
* Merge the PR
* Release a new version on the `master` branch (optional) ...
  * Bump the version in `build.sbt`
  * Build a new distributable `.tgz` with ...
    * `sbt nmesos-shared/clean` 
    * `sbt nmesos-cli/clean` 
    * `sbt nmesos-shared/test` 
    * `sbt nmesos-cli/test` 
    * `sbt nmesos-cli/publishLocal`
    * `sbt nmesos-cli/assembly`
    * `sbt nmesos-cli/universal:packageZipTarball`
  * Publish the new version in the public repo ...
    * `sbt nmesos-cli/universal:publish`
  * Update the `brew formula` by running `sbt updateBrew`
    * Note: This will run `./Formula/update.sh <version>` and will generate a new `./Formula/nmesos-cli.rb` file 
  * Add/Commit/Push to `master`
  * Tag the release (and push the tag)
* Note: If something goes wrong and you need to re-release you first need to cleanup (with `aws --profile nmesos s3 rm --recursive s3://nmesos-releases/nitro-public/repo/nitro/nmesos-cli/0.2.19/`).
