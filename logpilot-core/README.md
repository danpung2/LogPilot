# LogPilot Core

The core module containing shared domain models, validatiors, and configuration classes for LogPilot.

## Features
- **Configuration**: Manages `LogPilotProperties` for Type-safe configuration injection.
- **Domain Models**: Defines core entities like `LogEvent`, `LogMessage` used across Client and Server.
- **Utilities**: Common utility classes for validation and data processing.

## Installation

This module is intended to be used as a dependency for other LogPilot modules.

```groovy
implementation project(':logpilot-core')
```

