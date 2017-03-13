## NMesos CLI

*Deploy projects to Mesos with Singularity!*

Requirements
------------

* Java 8
* Project already Dockerized and published on DockerHub. 


## Release cli

```
sbt "++2.12.1 nmesos-shared/test" "++2.12.1 nmesos-shared/it:test"
sbt clean "++2.12.1 nmesos-shared/publishLocal"  "++2.12.1 nmesos-cli/assembly" nmesos-cli/universal:packageZipTarball nmesos-cli/universal:publish
```

## Release a brew update

Update the Formula `nmesos-cli.rb` with the new public url 
and push the changes to the Master branch.
