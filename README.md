![Logo](logo.png)

# Nmesos

Nmesos is a command line tool that leverages [Singularity](https://github.com/HubSpot/Singularity) API to deploy services and schedule jobs in a [Apache Mesos](http://mesos.apache.org/) cluster.

## Install

Using asdf ...

``` bash
asdf plugin-add nmesos https://github.com/Nitro/nmesos.git
asdf list-all nmesos
asdf install nmesos <version>
asdf global nmesos <version>
asdf uninstall nmesos <version>
```

## Troubleshooting

You can set/export the `NMESOS_LOG_LEVEL` env var. Valid log-levels are `error`, `warn`, `info` and `debug`. This will log to `stdout`. You can also set `NMESOS_LOG_APPENDER` to `file`. This will create an `nmesos.log` file in the current directory.
