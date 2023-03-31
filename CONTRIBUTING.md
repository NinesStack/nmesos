# Contributing

## Building it/Testing it

* You need to run `Java 8` (otherwise you will see errors like
  `java.lang.NoClassDefFoundError: javax/xml/bind/JAXBException`
  and/or `com.amazonaws.SdkClientException: Unable to execute HTTP
  request: sun.security.validator.ValidatorException: PKIX path
  building failed:
  sun.security.provider.certpath.SunCertPathBuilderException: unable
  to find valid certification path to requested target`
* run `sbt compile` and/or `sbt test`
* run `sbt assembly` and `./target/scala-<-scala-version>/nmesos`

## Contributor

* Create an issue to get agreement on what you want to
  add/change/fix. Feel free to also reach out on [Gitter][].
* Fork the repo
* Make the changes (inlcuding writing the test(s) to verify/validate
  the changes)
* Create a PR (linking back to the issue; explain what/why/how you
  changed the code)
* Note: Please keep the `.gitignore` file clean of your personal
  build/IDE artifacts by using/setting up a global gitignore file on
  your side (probably a good idea/practice anyway; `git config
  --global core.excludesfile ~/.gitignore_global`)

## Maintainers

* Note: To publish a new release you need to get access to the S3
  bucket ...
  * Contact one of the existing commiters/maintainers to get the aws keys
  * `export AWS_ACCESS_KEY_ID=<key id>`
  * `export AWS_SECRET_ACCESS_KEY=<key>`
* Review the PR (discussing the change/implementation; making changes
  as necessary)
* Approve the PR
* Merge the PR
* Release a new version on the `trunk` branch (optional) ...
  * Bump the version in `VERSION.txt`
  * Update `CHANGELOG.md`
  * Build and publish a new distributable `.tgz` with `sbt clean
    assembly universal:publish updateAsdf updateBrew`
  * Add/Commit/Push to `trunk`
  * Tag the release (and push the tag)
* Note: For the time being you manually need to grant a/the public
  READ permission to the object(s) by running `echo
  "tgz\ntgz.md5\ntgz.sha1" | xargs -I {extension} aws s3api
  put-object-acl --bucket nmesos-releases --key
  public/ninesstack/nmesos/<VERSION>/nmesos-<VERSION>.{extension}
  --acl public-read` (replacing `<VERSION>` with the version at hand
  (e.g. `3.0.6`)).
* Note: If something goes wrong and you need to re-release you first
  need to cleanup (with `aws --region eu-west-1 s3 rm --recursive
  s3://nmesos-releases/public/ninesstack/nmesos/<VERSION>`).

## Code coverage

We can also do code coverage reporting (and upload it to [coveralls][]).

Note: This only works on MacOS/Linux. And you need Java 1.8.

* set `export COVERALLS_REPO_TOKEN=<coveralls-token>`
  * you need to get that token from [Roland][]
* run `sbt clean compile coverage test coverageReport coveralls` to
  generate the report (in
  `target/scala-<version>/scoverage-data/scoverage-report`) and upload
  it to [coveralls][]

[Gitter]: https://gitter.im/NinesStack/nmesos
[Roland]: mailto:roland@tritsch.email
[coveralls]: https://coveralls.io/github/rolandtritsch/nmesos
