# LogPilot Core

The core logic module containing shared domain models, storage abstractions, and messaging protocols for the LogPilot event broker.

## Features
- **Storage Abstraction**: Defines pluggable storage interfaces (SQLite, File) for persistent message streams.
- **Domain Models**: Defines core entities like `LogEntry` used across Producers and Consumers.
- **Offset Management**: Logic for tracking and persisting consumer progress.

## Installation

This module is intended to be used as a dependency for other LogPilot modules.

```groovy
implementation project(':logpilot-core')
```

