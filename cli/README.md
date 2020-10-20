## NMesos CLI

*Deploy projects to Mesos with Singularity!*

Requirements
------------

* Java 8
* Project already Dockerized and published on DockerHub. 


## Release CLI

```
sbt "nmesos-shared/test" "nmesos-shared/it:test" "nmesos-cli/test"
sbt clean "nmesos-shared/publishLocal"  "nmesos-cli/assembly" "nmesos-cli/universal:packageZipTarball" "nmesos-cli/universal:publish"
```

## Release a brew update

Update the Formula `nmesos-cli.rb`
1. with the new public url
2. with the new sha256
    `shasum -a 256 nmesos-cli-X.Y.Z.tgz`
3. push the changes to the `master` branch
