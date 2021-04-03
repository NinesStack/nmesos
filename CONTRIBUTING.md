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
  * And add the following section to your `~/.aws/config` file ...
    ```
    [nmesos]
    region = eu-west-1
    ```
* Review the PR (discussing the change/implementation; making changes as necessary)
* Approve the PR
* Merge the PR
* Release a new version on the `master` branch (optional) ...
  * Bump the version in `VERSION.txt`
  * Build and publish a new distributable `.tgz` with `sbt clean assembly universal:publish` 
  * Add/Commit/Push to `master`
  * Tag the release (and push the tag)
* Note: If something goes wrong and you need to re-release you first need to cleanup (with `aws --profile nmesos s3 rm --recursive s3://nmesos-releases/nitro-public/repo/nitro/nmesos/1.0.1`).
