# nmesos Architecture

This directory contains architecture documentation and Mermaid diagrams that illustrate the structure and behavior of the nmesos CLI tool.

## Diagrams Overview

### 1. Main Architecture
**Purpose**: Shows the overall system architecture and component relationships

**Key Components**:
- **CLI Layer**: Entry point, argument parsing, and orchestration
- **Commands Layer**: Individual command implementations with base command pattern
- **Configuration Layer**: YAML parsing, validation, and model management
- **Singularity Integration**: API client with dry-run and live operation modes
- **Utilities**: Supporting services for formatting, HTTP operations, and version handling
- **External Systems**: Singularity API, Apache Mesos, and Docker runtime

**Relationships**: Demonstrates how data flows between layers and how external systems interact

View: [Main Architecture Diagram][main-architecture]

### 2. Command Flow
**Purpose**: Illustrates the decision flow and execution paths for different commands

**Key Flows**:
- **Argument Processing**: CLI parsing and validation
- **Configuration Loading**: YAML processing and validation
- **Command Chain Building**: Dependency resolution and cycle detection
- **Command Execution**: Different paths for release, check, docker-env, docker-run
- **Error Handling**: Failure scenarios and recovery mechanisms

**Decision Points**: Shows critical decision points and branching logic

View: [Command Flow Diagram][command-flow]

### 3. Data Flow
**Purpose**: Shows how data transforms as it moves through the system

**Key Transformations**:
- **CLI Arguments** → **Cmd Model**
- **YAML Configuration** → **Config Model**
- **Combined Configuration** → **CmdConfig Model**
- **nmesos Models** → **Singularity Models**
- **Processing Results** → **Formatted Output**

**Special Flows**:
- Command chain processing with dependency resolution
- Environment variable merging and validation
- Output formatting for different targets (terminal, files, logs)

View: [Data Flow Diagram][data-flow]

### 4. Deployment Sequence
**Purpose**: Step-by-step sequence of a typical deployment operation

**Key Interactions**:
- User initiating deployment
- Configuration loading and validation
- Singularity API interactions (ping, request management, deployment)
- Mesos orchestration of container deployment
- Progress monitoring and result reporting
- Error handling and failure recovery

**Alternative Flows**:
- Dry-run mode simulation
- After-deploy job chains
- Failure job execution

View: [Deployment Sequence Diagram][deployment-sequence]

## How to View the Diagrams

### Online Viewers
1. **Mermaid Live Editor**: https://mermaid.live
   - Copy and paste the diagram content
   - Provides real-time rendering and export options

2. **GitHub**: GitHub automatically renders `.mmd` files in the web interface

### Local Viewing
1. **VS Code**: Install the "Mermaid Preview" extension
2. **CLI Tools**: Install `@mermaid-js/mermaid-cli` for command-line rendering

### Integration in Documentation
These diagrams can be embedded in:
- README files (GitHub renders them automatically)
- Documentation sites (GitBook, GitLab Pages, etc.)
- Confluence or other wiki systems with Mermaid support

## Diagram Maintenance

When updating the codebase:
1. Review these diagrams for accuracy
2. Update component relationships if new modules are added
3. Modify flow diagrams if command logic changes
4. Ensure data flow diagrams reflect current model transformations

## Architecture Insights

### Key Design Patterns
- **Command Pattern**: All operations implement the Command trait
- **Strategy Pattern**: Dry-run vs. live execution modes
- **Chain of Responsibility**: Command chains with failure handling
- **Model-View-Controller**: Clear separation of data, logic, and presentation

### Scalability Considerations
- **Modular Design**: Components are loosely coupled
- **Configuration-Driven**: Behavior controlled through YAML files
- **Error Recovery**: Comprehensive failure handling and logging
- **Extensibility**: Easy to add new commands and integrations

### Security Features
- **Dry-run Default**: Safe testing before live operations
- **Version Validation**: Ensures compatibility
- **Input Validation**: Comprehensive configuration checking
- **Audit Trail**: Detailed logging of all operations

[main-architecture]: ./diagrams/main-architecture.mmd
[command-flow]: ./diagrams/command-flow.mmd
[data-flow]: ./diagrams/data-flow.mmd
[deployment-sequence]: ./diagrams/deployment-sequence.mmd