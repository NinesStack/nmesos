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

# Getting Started

The current (and temporal) way to install and use it:
`curl -O https://dl.dropboxusercontent.com/u/2853977/nmesos && chmod u+x nmesos`
./nmesos release example-service --environment dev --tag latest

## Release cli


```
sbt shared/it:test
sbt assembly
```

