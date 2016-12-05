## NMesos sbt plugin

*Deploy Sbt projects to Mesos with Singularity!*

sbt-nmesos is a sbt plugin with the some functionality as the nmesos CLI tool

Requirements
------------

* sbt
* Project already Dockerized and published on DockerHub. 


Setup
-----

For sbt 0.13.6+ add sbt-nmesos as a dependency in `project\plugins.sbt`:

```scala
addSbtPlugin("com.nitro" % "sbt-nmesos" % "0.1-SNAPSHOT")
```

# Usage

Since sbt-nmesos is an auto plugin, it doesn't need any extra setup to be able to use `nmesosRelease` or any other task
in your project.

To deploy you will need a deployment configuration file at [`{projectName}.yml`](example-project/example-project.yml) in the configuration folder.
By default it'll assume the `baseDirectory` but you can change the configuration folder defining an environment var `NMESOS_CONFIG_REPOSITORY`.


## Publish the sbt plugin

Publish locally first with:

`sbt ++2.10.6 shared/publishLocal`
`sbt ++2.10.6 sbt-plugin/publishLocal`

Remote publish:

`sbt ++2.10.6 shared/publish`
`sbt ++2.10.6 sbt-plugin/publish`

Note: This is a cross scala version project (2.10 and 2.12). The sbt plugin needs to use Scala 2.10.