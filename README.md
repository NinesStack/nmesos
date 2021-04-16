![NMesos Logo](docs/nmesos_logo.png)

# !!! NOTE: `master` branch was renamed to `trunk`!!!
## Please re-clone the repository!

<!-- markdown-toc start - Don't edit this section. Run M-x markdown-toc-refresh-toc -->
**Table of Contents**

- [Nmesos](#nmesos)
    - [Features](#features)
    - [Usage](#usage)
        - [Release a service](#release-a-service)
        - [Config Yml file](#config-yml-file)
    - [Getting Started with the CLI tool](#getting-started-with-the-cli-tool)
    - [Bash Completion support](#bash-completion-support)
    - [Support to deprecate env_vars](#support-to-deprecate-env_vars)
    - [Other Comands](#other-comands)

<!-- markdown-toc end -->

# Nmesos

![Build Status](https://github.com/nitro/nmesos/actions/workflows/ci.yml/badge.svg)

Nmesos is a command line tool that leverages [Singularity](https://github.com/HubSpot/Singularity) API to deploy services and schedule jobs in a [Apache Mesos](http://mesos.apache.org/) cluster.

![Terminal output](docs/nmesos-cli-example.gif)

## Features

* Service configuration in Yaml format.
* Dry-run mode
* Auto-detect changes between yaml and Singularity request (scale up instances and resources if needed)
* [CLI tool](cli/)
* [Integration with SBT](sbt-plugin/)(optional)
* Scheduled jobs
* Support to deprecate env_vars

## Usage

Note: You can run `nmesos` from the directory that got the config files in it or from any other directory, but then you need to let it know where the config files are by setting the `NMESOS_CONFIG_REPOSITORY` environment variable (e.g. `NMESOS_CONFIG_REPOSITORY=<some-dir> nmessos <rest-of-the-command-line`).

### Release a service

The following command will read [example-service.yml](docs/examples/example-service.yml)
and try to release the latest tag in the dev environment.

```
cd docs/examples
nmesos release example-service --environment dev --tag latest
```

### Config Yml file

```
nmesos_version: '0.2.20'
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
      url: "http://localhost:7099/singularity"

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
curl https://nmesos-releases.s3-eu-west-1.amazonaws.com/nitro-public/repo/nitro/nmesos-cli/0.2.20/nmesos-cli-0.2.20.tgz | tar -xz
cd nmesos-cli-0.2.20 && chmod u+x nmesos
```

Note: Older version of Nmesos (up to 0.2.18) are available here:
```
curl https://s3-us-west-2.amazonaws.com/nitro-public/repo/nitro/nmesos-cli/0.1.2/nmesos-cli-0.1.2.tgz | tar -xz
```

Alternatively on MacOS, you can also install it with `brew`:

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

And or use `asdf`:

```
asdf plugin-add nmesos https://github.com/Nitro/nmesos.git
asdf list-all nmesos
asdf install nmesos <version>
asdf global nmesos <version>
```

## Bash Completion support

If you want to install the support for the [bash-completion](contrib/etc/bash_completion.d/nmesos), you can use the brew option `--with-bash-completion` when installing:

```
brew install --with-bash-completion nmesos-cli
```

## Support to deprecate env_vars

When you do not need an `env_var` anymore, it is hard to remove it from the deployment config right away, because you might need to rollback to a previous version of the service that still needs that `env_var` (and yes, strictly speaking you could say that the deployment config should/could also be rolled back, but then you might loose other changes that you had to make to the config and ... my experience is you do not want to fiddle with that while you are in the middle of a production outage).

To make this easier/work `nmesos` supports an annotation to deprecate `env_vars` ...

```
nmesos_version: '0.2.20'
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
      OLD_ENV_VAR: "old value" # @deprecated-on 01-Jan-2020
      NEW_ENV_VAR: "new value"

  singularity:
    healthcheckUri: "/hello"
```

The date is the date, when you deprecated the `env_var`. `nmesos` checks the date to find env_vars, where the grace period is expired. The default for the `--deprecated-soft-grace-period` is 14 days. The default for the `--deprected-hard-grace-period` is 28 days.

When the soft-limit is reached a warning is printed. When the hard-limit it reached an error is printed and the deploy is aborted.

The default can be overriden with command line flags (see below or run `nmesos help`).

## Support to run containers locally

To run containers locally you can generate a `<service-name>.env` file and a `docker-compose.<service-name).yml` with the `docker-env` command.

You can then start the service with `docker-compose --file docker-compose.<service-name>.yml` (to start it in the background just use the `--detach` flag).

You can also have nmesos run `docker-compose` for you with the `docker-run` command (it will start the service in the background).

## Other Comands

```
nmesos release [options] service-name
 Release the a new version of the service.
 Usage:  nmesos release example-service --environment dev --tag 0.0.1
  service-name                             Name of the service to release
  -e, --environment <value>                The environment to use
  -t, --tag <value>                        Tag/Version to release
  -f, --force                              Force action!
  -n, --dry-run <true|false>               Is this a dry run? Default: true.
  -S, --deprecated-soft-grace-period 10    Number of days, before warning
  -H, --deprecated-hard-grace-period 20    Number of days, before error/abort

nmesos scale [options] service-name
 Update the Environment.
 Usage: nmesos scale service_name --environment dev
  service-name                             Name of the service to scale
  -e, --environment <value>                The environment to use
  -n, --dry-run <true|false>               Is this a dry run? Default: true.

nmesos check [options] service-name
 Check the environment conf without running it.
 Usage: nmesos check service_name --environment dev
  service-name                             Name of the service to verify
  -e, --environment <value>                The environment to verify
  -S, --deprecated-soft-grace-period 10    Number of days, before warning
  -H, --deprecated-hard-grace-period 20    Number of days, before error/abort
```
