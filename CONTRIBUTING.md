# Contributing

## Contributor

* Create an issue (to get agreement on what you want to add/change)
* Fork the repo
* Make the changes (inlcuding writing the test(s) to verify/validate the changes)
* Create a PR (linking back to the issue; explain what/why/how you changed the code)
* Note: Please keep the `.gitignore` file clean of your personal build/IDE artifacts by using/setting up a global gitignore file on your side (probably a good idea/practice anyway; `git config --global core.excludesfile ~/.gitignore_global`)

## Maintainers

* Note: To publish a new release you need to get access to the S3 bucket ...
  * Contact one of the existing commiters/maintainers to get the aws keys
  * Add the following section to your `~/.aws/credentials` file ...
    ```
    [nmesos]
    aws_access_key_id = <access>
    aws_secret_access_key = <secret>    
    ```
* Review the PR (discussing the change/implementation; making changes as necessary)
* Approve the PR
* Merge the PR
* Release a new version (optional) ...
  * On the `master` branch ...
  * Manually bump the version in `build.sbt`
  * [nmesos cli](cli/README.md) release process ...
    * Build a new distributable `.tgz` with ...
      * `sbt clean`
      * `sbt nmesos-cli/publishLocal`
      * `sbt nmesos-cli/assembly`
      * `sbt nmesos-cli/universal:packageZipTarball`
    * Publish the new version in a public repo ...
      * `sbt nmesos-cli/universal:publish`
  * Update the `brew` [Formula](Formula/nmesos-cli.rb)
    * Modify url to the new release and also replace the sha with the new sha (by running `shasum -a 256 cli/target/universal/nmesos-cli-0.2.19.tgz`)
  * Commit to `master`
* Note: If something goes wrong and you need to re-release you first need to cleanup (with `aws --profile nmesos s3 rm --recursive s3://nmesos-releases.s3-eu-west-1.amazonaws.com/nitro-public/repo/nitro/nmesos-cli/0.2.19/`).
