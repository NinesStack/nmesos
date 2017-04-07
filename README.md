![NMesos Logo](docs/nmesos_logo.png)

# Nmesos 

Nmesos is a command line tool that leverages [Singularity](https://github.com/HubSpot/Singularity) API to deploy 
services and schedule jobs in a [Apache Mesos](http://mesos.apache.org/) cluster.

![Terminal output](docs/nmesos-cli-example.gif)


## Features

 - Service configuration in Yaml format.
 - Dryrun mode
 - Auto detect changes between yaml and Singularity request (scale up instances and resources if needed)
 - [CLI tool](cli/)
 - [Integration with SBT](sbt-plugin/)(optional)
 - Scheduled jobs
 
# Usage

### Release a service
The following command will read [example-service.yml](docs/examples/example-service.yml)
and try to release the latest tag in the dev environment.

```
cd docs/examples
nmesos release example-service --environment dev --tag latest
```

To know more about the yml format check the [yml examples](docs/examples)

# Getting Started with the CLI tool

Install Nmesos CLI manually:

```
curl https://s3-us-west-2.amazonaws.com/nitro-public/repo/nitro/nmesos-cli/0.1.2/nmesos-cli-0.1.2.tgz | tar -xz
cd nmesos-cli-0.1.0 && chmod u+x nmesos
````

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
