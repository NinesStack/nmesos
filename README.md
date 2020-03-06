![NMesos Logo](docs/nmesos_logo.png)

# Nmesos

Nmesos is a command line tool that leverages [Singularity](https://github.com/HubSpot/Singularity) API to deploy services and schedule jobs in a [Apache Mesos](http://mesos.apache.org/) cluster.

![Terminal output](docs/nmesos-cli-example.gif)

## Features

* Service configuration in Yaml format.
* Dryrun mode
* Auto detect changes between yaml and Singularity request (scale up instances and resources if needed)
* [CLI tool](cli/)
* [Integration with SBT](sbt-plugin/)(optional)
* Scheduled jobs

## Usage

### Release a service

The following command will read [example-service.yml](docs/examples/example-service.yml)
and try to release the latest tag in the dev environment.

```
cd docs/examples
nmesos release example-service --environment dev --tag latest
```

### Config Yml file

```
nmesos_version: '0.1.2'
common:
  resources:
    memoryMb: 128

  container:
    image: hubspot/singularity-test-service
    ports:
      - 8080
    labels:
      ServiceName: "exampleServer"
    env_vars:
      NEW_RELIC_LICENSE_KEY: "xxxxx"

  singularity:
    healthcheckUri: "/hello"

environments:
  dev:
    resources:
      instances: 1
      cpus: 0.1
    singularity:
      url: "http://192.168.99.100:7099/singularity"

  prod:
    resources:
      instances: 3
      cpus: 1
    container:
      env_vars:
        JAVA_OPTS: "-Xmx1024m"
    singularity:
      url: "http://prod-singularity/singularity"
```

To know more about the yml format check the [yml examples](docs/examples)

## Getting Started with the CLI tool

Install Nmesos CLI manually:
```
curl https://nmesos-releases.s3-eu-west-1.amazonaws.com/nitro-public/repo/nitro/nmesos-cli/0.2.19/nmesos-cli-0.2.19.tgz | tar -xz
cd nmesos-cli-0.1.2 && chmod u+x nmesos
```

Note: Older version of Nmesos (up to 0.2.18) are available here:
```
curl https://s3-us-west-2.amazonaws.com/nitro-public/repo/nitro/nmesos-cli/0.1.2/nmesos-cli-0.1.2.tgz | tar -xz
```

Alternatively on MacOS, you can also install it with brew:

```
brew tap nitro/nmesos  git@github.com:Nitro/nmesos.git
brew install nmesos-cli
```

Update Nmesos cli:

```
brew update
brew upgrade nmesos-cli
```

Uninstall Nmesos cli:

```
brew uninstall nmesos-cli
```

## Bash Completion support

If you want to install the support for the [bash-completion](contrib/etc/bash_completion.d/nmesos), you can use the brew option `--with-bash-completion` when installing:

```
brew install --with-bash-completion nmesos-cli
```

## Other Comands

```
nmesos release [options] service-name
 Release the a new version of the service.
 Usage:  nmesos release example-service --environment dev --tag 0.0.1
  service-name             Name of the service to release
  -e, --environment <value>
                           The environment to use
  -t, --tag <value>        Tag/Version to release
  -f, --force              Force action
  -n, --dryrun <value>     Is this a dry run?

nmesos scale [options] service-name
 Update the Environment.
 Usage: nmesos scale service_name --environment dev
  service-name             Name of the service to scale
  -e, --environment <value>
                           The environment to use
  -n, --dry-run <value>    Is this a dry run?

nmesos check [options] service-name
 Check the environment conf without running it.
 Usage: nmesos check service_name --environment dev
  service-name              Name of the service to verify
  -e, --environment <value> The environment to verify
```
