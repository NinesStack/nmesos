![Logo][]

# Nmesos

[![Join the chat at https://gitter.im/NinesStack/nmesos][gitter-badge]][gitter-chat]
![Build Status][build-badge]

Nmesos is a command line tool that leverages the [Singularity][] API to deploy services and schedule jobs in an Apache [Mesos][] cluster.

![Example][]

## Install

Using asdf ...

``` bash
asdf plugin-add nmesos https://github.com/NinesStack/nmesos.git
asdf list-all nmesos
asdf install nmesos <version>
asdf global nmesos <version>
asdf uninstall nmesos <version>
```

**Note: For this to work you need to have `asdf 0.8` installed. `0.7` will not work.**

Using brew ...

``` bash
brew tap ninesstack/nmesos https://github.com/NinesStack/nmesos.git
brew install nmesos
brew upgrade nmesos
brew uninstall nmesos
```

Using curl ...

``` bash
curl https://nmesos-releases.s3-eu-west-1.amazonaws.com/public/ninesstack/nmesos/<version>/nmesos-<version>.tgz | tar -xz
chmod 755 nmesos-<version>/nmesos
mv nmesos-<version>/nmesos <to-dir-on-your-path>
```

## Usage

There are example configurations in the `example` directory.

Please run `nmesos help` to see/understand the commands and options.

To `release` the `example-service` from the `examples` directory you would run ...

``` bash
nmesos release example-service --environment dev --tag latest --dry-run false
```

## Support to deprecate env_vars

When you do not need an `env_var` anymore, it is hard to remove it from the deployment config right away, because you might need to rollback to a previous version of the service that still needs that `env_var` (and yes, strictly speaking you could say that the deployment config should/could also be rolled back, but then you might loose other changes that you had to make to the config and ... my experience is you do not want to fiddle with that while you are in the middle of a production outage).

To make this easier/work `nmesos` supports an annotation to deprecate `env_vars` ...

``` yaml
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

The date is the date, when you deprecated the `env_var`. `nmesos` checks the date to find `env_vars`, where the grace period is expired. The default for the `--deprecated-soft-grace-period` is 14 days. The default for the `--deprected-hard-grace-period` is 28 days.

When the soft-limit is reached a warning is printed. When the hard-limit it reached an error is printed and the deploy is aborted.

The default can be overriden with command line flags (see `nmesos help`.

## Support to run containers locally

To run containers locally you can generate a `<service-name>.env` file and a `docker-compose.<service-name).yml` with the `docker-env` command.

You can then start the service with `docker-compose --file docker-compose.<service-name>.yml` (to start it in the background just use the `--detach` flag).

You can also have nmesos run `docker-compose` for you with the `docker-run` command (it will start the service in the background).

## Troubleshooting

You can set/export the `NMESOS_LOG_LEVEL` env var. Valid log-levels are `error`, `warn`, `info` and `debug`. This will log to `stdout`. You can also set `NMESOS_LOG_APPENDER` to `file`. This will create an `nmesos.log` file in the current directory.

[Example]: ./assets/example.gif
[Logo]: ./assets/logo.png
[Mesos]: http://mesos.apache.org/
[Singularity]: https://github.com/HubSpot/Singularity
[build-badge]: https://github.com/ninesstack/nmesos/actions/workflows/ci.yml/badge.svg
[gitter-badge]: https://badges.gitter.im/NinesStack/nmesos.svg
[gitter-chat]: https://gitter.im/NinesStack/nmesos
