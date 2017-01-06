![NMesos Logo][docs/nmesos_logo.png]

# Nmesos 

Nmesos is a tool (CLI tool and sbt plugin) that leverages Singularity 
to deploy new services into Mesos.

## Features

 - Service config in Yaml format.
 - Auto update (scale up instances and resources if needed)
 - [CLI tool](cli/)
 - [Integration with SBT](sbt-plugin/)(optional)
 
# Usage

### Release a service

The following command will read [example-project.yml](sbt-plugin/example-project/example-project.yml)
and try to release the latest tag in the dev environment.

```
nmesos release example-service --environment dev --tag latest
```

# Getting Started with the CLI tool

Install Nmesos cli:

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

Alternatively, you can also download and run it with:

```

curl https://s3-us-west-2.amazonaws.com/nitro-public/repo/nitro/nmesos-cli/0.0.5/nmesos-cli-0.0.5.tgz | tar -xz
cd nmesos-cli-0.0.5 && chmod u+x nmesos
````

## Usage

./nmesos release example-service --environment dev --tag latest



## Release a new version

```
// publish shared lib
sbt "++2.10.6 nmesos-shared/publishLocal"  "++2.12.1 nmesos-shared/publishLocal"
sbt "++2.10.6 nmesos-shared/publish"  "++2.12.1 nmesos-shared/publish" "++2.10.6 sbt-plugin/publish"
```