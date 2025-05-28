# nmesos Architecture Diagrams

This directory contains Mermaid diagrams that visualize the architecture and design of the nmesos system. These diagrams complement the main architecture documentation and provide visual representations of system structure, flows, and interactions.

## Available Diagrams

### 1. [Command Processing Flow](command-processing-flow.md)
**Type**: Flowchart  
**Purpose**: Shows the overall command processing flow from CLI argument parsing through command execution and error handling.

**Key Elements**:
- CLI argument validation and parsing
- Command type routing (version, verify, deployment commands)
- Command chain building and validation
- Error handling and failure recovery
- Deploy freeze safety mechanisms

### 2. [Deployment Sequence](deployment-sequence.md)
**Type**: Sequence Diagram  
**Purpose**: Detailed interaction flow during a release command execution, including all API interactions with Singularity.

**Key Elements**:
- Configuration validation process
- Singularity API interactions
- Request and deploy lifecycle management
- Real-time deployment monitoring
- Error handling and log collection

### 3. [Component Architecture](component-architecture.md)
**Type**: Component Diagram  
**Purpose**: High-level component structure and dependencies within nmesos, organized by architectural layers.

**Key Elements**:
- Layer separation (CLI, Command, Config, Integration, Utility)
- Component dependencies and relationships
- External system integrations
- Design pattern implementations

### 4. [Data Flow](data-flow.md)
**Type**: Data Flow Diagram  
**Purpose**: Shows how data flows through the system from configuration files to deployment execution.

**Key Elements**:
- Input sources and parsing stages
- Data transformations between models
- Validation points and error handling
- API model conversions
- Output generation and formatting

## Diagram Conventions

### Color Coding
- **Blue tones**: Input/CLI related components
- **Purple tones**: Command processing components  
- **Green tones**: Configuration and validation components
- **Orange tones**: Singularity integration components
- **Pink tones**: Docker integration components
- **Red tones**: External systems and error states
- **Gray tones**: Utility and helper components

### Symbol Meanings
- **Rectangles**: Components, processes, or data stores
- **Diamonds**: Decision points or conditional logic
- **Circles**: Start/end points or important states
- **Arrows**: Data flow or control flow direction
- **Dashed lines**: Optional or conditional relationships

## How to View These Diagrams

These diagrams use Mermaid syntax and can be viewed in several ways:

1. **GitHub**: GitHub natively renders Mermaid diagrams in markdown files
2. **VS Code**: Use the "Markdown Preview Mermaid Support" extension
3. **Mermaid Live Editor**: Copy diagram code to https://mermaid.live
4. **Documentation Sites**: Many documentation platforms support Mermaid rendering

## Diagram Maintenance

When updating the nmesos codebase, consider updating these diagrams if:

- New commands are added to the CLI
- The command processing flow changes
- New external integrations are added
- Significant architectural changes are made
- New data models or transformations are introduced

## Related Documentation

- [Main Architecture README](../README.md): Comprehensive architecture analysis
- [CLAUDE.md](../../CLAUDE.md): Development commands and architecture overview
- [README.md](../../README.md): Project overview and usage instructions

## Contributing to Diagrams

When modifying or adding diagrams:

1. Follow the established color coding and symbol conventions
2. Keep diagrams focused on specific aspects of the system
3. Include explanatory text alongside the diagram code
4. Validate diagram syntax using Mermaid Live Editor
5. Update this README when adding new diagrams