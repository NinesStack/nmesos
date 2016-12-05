# Nmesos 

Nmesos is a tool (CLI tool and sbt plugin) that leverages Singularity 
to deploy new services into Mesos.

## Features

 - Service config in Yaml format.
 - Auto update (scale up instances and resources if needed)
 - TODO ...

# Usage

### Release a service

The following command will read [example-service.yml](shared/src/it/resources/config/example-service.yml)
and try to release the latest tag in the dev environment.

```
nmesos release example-service --environment dev --tag latest
```

# Getting Started with the CLI tool

Install Nmesos cli:

```
brew  -v tap nitro/nmesos  git@github.com:Nitro/nmesos.git
brew install nmesos-cli
```

Update Nmesos cli:
```
brew update
brew upgrade  nmesos-cli
```

## Usage

./nmesos release example-service --environment dev --tag latest

## Release cli


```
sbt shared/it:test
sbt cli/assembly
```

