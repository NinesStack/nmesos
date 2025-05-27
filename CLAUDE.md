# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Common Development Commands

### Build and Test
- `sbt compile` - Compile the project
- `sbt test` - Run all tests
- `sbt assembly` - Build the executable jar (creates `target/scala-3.3.1/nmesos`)
- `sbt Universal/packageZipTarball` - Create distribution package

### Running the CLI
After assembly: `./target/scala-3.3.1/nmesos <command>`

### Development Commands
- `sbt updateAsdf` - Update asdf versions
- `sbt updateBrew` - Update brew formula
- `sbt coverage test coverageReport` - Generate test coverage report

## Architecture Overview

### Core Components
This is a Scala 3 CLI tool that deploys services to Apache Mesos clusters via the Singularity API.

**CLI Layer (`cli/`)**
- `Main.scala` - Entry point, delegates to CliManager
- `CliParser.scala` - Command line argument parsing using scopt
- `CliManager.scala` - Main orchestration logic, handles command chains and failure scenarios

**Commands (`commands/`)**
- `BaseCommand.scala` - Base trait for all commands with common Singularity connectivity
- Command implementations: `ReleaseCommand`, `CheckCommand`, `DockerEnvCommand`, etc.
- All commands follow the Command pattern with `run(): CommandResult`

**Configuration (`config/`)**
- `ConfigReader.scala` - Parses YAML configuration files
- `YamlParser.scala` - YAML processing with validation
- `model.scala` - Case classes for configuration structure
- `Validations.scala` - Configuration validation logic

**Singularity Integration (`singularity/`)**
- `SingularityManager.scala` - Main API client for Singularity
- `ModelConversions.scala` - Converts between nmesos and Singularity models
- Handles request creation, updates, and deployments

### Key Architecture Patterns

**Command Chain Processing**: Commands can specify `after_deploy` configurations that create chains of dependent deployments. The system builds these chains upfront and processes them sequentially, with support for failure handling via `on_failure` jobs.

**Configuration Model**: YAML files define service configurations with environment-specific overrides. The system validates configurations including resource requirements, deprecated environment variables, and required fields.

**Dry-run Support**: All commands support dry-run mode (default: true) for safe testing without actual deployments.

### Configuration Structure
Service configurations are YAML files with:
- `nmesos_version` - Required minimum version
- `common` - Shared configuration (resources, container, singularity settings)
- `environments` - Environment-specific overrides (dev, prod, etc.)

Services are deployed using: `nmesos release <service-name> --environment <env> --tag <tag>`

### Testing
- Uses ScalaTest 3.2.12 with custom matchers
- Test files mirror source structure in `src/test/scala/`
- Configuration examples in `src/test/resources/config/`