# Data Flow Diagram

This diagram shows how data flows through the nmesos system from configuration files to deployment execution.

```mermaid
flowchart LR
    subgraph "Input Sources"
        A[CLI Arguments]
        B[YAML Config Files]
        C[Environment Variables]
    end
    
    subgraph "Parsing & Validation"
        D[CliParser]
        E[ConfigReader]
        F[YamlParser]
        G[Validations]
    end
    
    subgraph "Internal Models"
        H[Cmd Model]
        I[Config Model]
        J[Environment Model]
        K[ValidConfig]
    end
    
    subgraph "Command Processing"
        L[CliManager]
        M[Command Chain Builder]
        N[Command Executor]
    end
    
    subgraph "API Models"
        O[SingularityRequest]
        P[SingularityDeploy]
        Q[ModelConversions]
    end
    
    subgraph "External APIs"
        R[Singularity API]
        S[Docker API]
    end
    
    subgraph "Output"
        T[Console Output]
        U[Deploy Status]
        V[Task Logs]
        W[Error Messages]
    end
    
    %% Data Flow Connections
    A --> D
    B --> E
    C --> E
    
    D --> H
    E --> F
    F --> I
    I --> G
    G --> J
    G --> K
    
    H --> L
    K --> L
    L --> M
    M --> N
    
    J --> Q
    K --> Q
    Q --> O
    Q --> P
    
    O --> R
    P --> R
    R --> U
    R --> V
    
    N --> S
    S --> T
    
    N --> T
    U --> T
    V --> T
    G --> W
    W --> T
    
    %% Data Transformations
    subgraph "Key Data Transformations"
        X["CLI Args → Cmd"]
        Y["YAML → Config"]
        Z["Config → Environment"]
        AA["ValidConfig → SingularityRequest"]
        BB["SingularityRequest → API JSON"]
        CC["API Response → Status"]
    end
    
    D -.-> X
    F -.-> Y
    E -.-> Z
    Q -.-> AA
    R -.-> BB
    R -.-> CC
    
    %% Styling
    classDef inputStyle fill:#e3f2fd
    classDef parsingStyle fill:#f3e5f5
    classDef modelStyle fill:#e8f5e8
    classDef processingStyle fill:#fff3e0
    classDef apiStyle fill:#fce4ec
    classDef externalStyle fill:#ffebee
    classDef outputStyle fill:#f1f8e9
    classDef transformStyle fill:#fafafa
    
    class A,B,C inputStyle
    class D,E,F,G parsingStyle
    class H,I,J,K modelStyle
    class L,M,N processingStyle
    class O,P,Q apiStyle
    class R,S externalStyle
    class T,U,V,W outputStyle
    class X,Y,Z,AA,BB,CC transformStyle
```

## Data Flow Stages

### 1. Input Collection
```
CLI Arguments → Cmd Model
YAML Files → Raw YAML Content
Environment Variables → Configuration Context
```

### 2. Parsing & Validation
```
Raw YAML → Parsed Config Model
Config Model → Environment-Specific Configuration
Environment Config → Validated Configuration
Cmd + ValidConfig → Command Context
```

### 3. Command Chain Processing
```
Initial Command → Command Chain Analysis
Command Chain → Dependency Resolution
Resolved Chain → Sequential Execution Plan
```

### 4. API Model Transformation
```
ValidConfig → SingularityRequest Model
ValidConfig → SingularityDeploy Model
API Models → JSON Payload
JSON Response → Status Objects
```

### 5. External System Integration
```
SingularityRequest → HTTP POST/PUT
SingularityDeploy → HTTP POST
API Responses → Status Updates
Docker Commands → Container Operations
```

### 6. Output Generation
```
Command Results → Formatted Console Output
Deploy Status → Progress Information
Task Information → Deployment Details
Errors → User-Friendly Messages
```

## Key Data Types and Their Flow

### Configuration Data Flow
```mermaid
graph LR
    A[service.yml] --> B[Config]
    B --> C[Environment]
    C --> D[ValidConfig]
    D --> E[CmdConfig]
    E --> F[SingularityRequest]
    F --> G[API Call]
```

### Command Data Flow
```mermaid
graph LR
    A[CLI Args] --> B[Cmd]
    B --> C[Command Chain]
    C --> D[CommandAndConfig]
    D --> E[CommandResult]
    E --> F[Console Output]
```

### Error Data Flow
```mermaid
graph LR
    A[Validation Error] --> B[ConfigError]
    B --> C[Error Message]
    C --> D[Formatted Output]
    A --> E[Command Error]
    E --> F[Failure Recovery]
    F --> G[Exit Code]
```

## Data Validation Points

1. **CLI Argument Validation**: Type checking and required field validation
2. **YAML Structure Validation**: Schema validation and field presence
3. **Environment Validation**: Environment-specific configuration completeness
4. **Business Rule Validation**: Deploy freeze, deprecation checks, resource limits
5. **API Contract Validation**: Request/response model compliance
6. **Runtime Validation**: Connection health, deployment status verification

## Error Handling in Data Flow

- **Early Validation**: Invalid data caught at parsing stage
- **Contextual Errors**: Errors include file paths and line numbers
- **Graceful Degradation**: Partial success scenarios handled appropriately
- **Error Propagation**: Errors bubble up with preserved context
- **User-Friendly Messages**: Technical errors translated to actionable messages