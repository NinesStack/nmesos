# Command Processing Flow

This flowchart shows the overall command processing flow in nmesos, from CLI argument parsing through command execution and error handling.

```mermaid
flowchart TD
    A[CLI Arguments] --> B[CliParser.parse]
    B --> C{Valid Arguments?}
    C -->|No| D[Exit - Invalid Args]
    C -->|Yes| E[processCmd]
    
    E --> F{Command Type?}
    F -->|VersionAction| G[VersionCommand.run]
    F -->|VerifyAction| H[VerifyEnvCommand.run]
    F -->|Other Actions| I[processYmlCommand]
    
    G --> J[Exit with Result]
    H --> J
    
    I --> K[getCommandChain]
    K --> L{Command Chain Valid?}
    L -->|No| M[Show Error & Exit]
    L -->|Yes| N[Extract Success & Failure Chains]
    
    N --> O[For Each Command in Success Chain]
    O --> P{Deploy Freeze Check}
    P -->|Frozen| Q[Show Error & Exit]
    P -->|Not Frozen| R[executeCommand]
    
    R --> S{Command Result?}
    S -->|Success| T[Show Success Message]
    S -->|Error| U[Show Error Message]
    
    T --> V{More Commands?}
    V -->|Yes| O
    V -->|No| W[Complete Success]
    
    U --> X[Execute Failure Command]
    X --> Y[Exit with Error]
    
    subgraph "Command Chain Building"
        K --> K1[Parse Initial Config]
        K1 --> K2[Build Success Chain]
        K2 --> K3[Build Failure Chain]
        K3 --> K4{Cyclic Dependencies?}
        K4 -->|Yes| K5[Return Error]
        K4 -->|No| K6[Return Valid Chain]
    end
    
    subgraph "Command Execution Details"
        R --> R1{Command Type?}
        R1 -->|ReleaseAction| R2[ReleaseCommand.run]
        R1 -->|CheckAction| R3[CheckCommand.run]
        R1 -->|DockerEnvAction| R4[DockerEnvCommand.run]
        R1 -->|DockerRunAction| R5[DockerRunCommand.run]
        
        R2 --> R6[Verify Config]
        R6 --> R7[Update Singularity Request]
        R7 --> R8[Deploy Version]
        R8 --> R9[Show Deploy Status]
        R9 --> R10[Return Result]
        
        R3 --> R10
        R4 --> R10
        R5 --> R10
    end
    
    style A fill:#e1f5fe
    style D fill:#ffebee
    style M fill:#ffebee
    style Q fill:#ffebee
    style Y fill:#ffebee
    style W fill:#e8f5e8
    style J fill:#e8f5e8
```

## Key Decision Points

1. **Argument Validation**: Early exit if CLI arguments are invalid
2. **Command Type Routing**: Different paths for version/verify vs deployment commands
3. **Command Chain Validation**: Complex validation for dependent deployments
4. **Deploy Freeze Check**: Safety mechanism to prevent deployments
5. **Error Handling**: Comprehensive error handling with failure command execution
6. **Success Chain Processing**: Sequential execution of dependent services

## Error Handling Strategy

- **Early Validation**: Arguments and configuration validated before execution
- **Graceful Degradation**: Failed commands trigger failure recovery mechanisms
- **Context Preservation**: Errors include detailed context and file information
- **Clean Exit**: All error paths lead to proper application termination