![NMesos Logo](docs/nmesos_logo.png)

# Nmesos

Nmesos is a command line tool that leverages [Singularity](https://github.com/HubSpot/Singularity) API to deploy services and schedule jobs in a [Apache Mesos](http://mesos.apache.org/) cluster.

# Troubleshooting

First you can run `nmesos --verbose` to get more insight into what nmesos is doing.

Second you can set/export the `NMESOS_LOG_LEVEL` env var. Valid log-levels are `error`, `warn`, `info` and `debug`. This will log to `stdout`. You can also set `NMESOS_LOG_APPENDER` to `file`. This will create an `nmesos.log` file in the current directory.
