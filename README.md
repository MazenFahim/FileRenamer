# FileRenamer

FileRenamer is a Java 21 + JavaFX desktop application for safely renaming groups of files with a sequential naming strategy.

## Requirements

- JDK 21
- Apache Maven 3.9+

## Run

```bash
mvn clean javafx:run
```

## Build

```bash
mvn clean package
```

## Architecture

- `controller`: JavaFX UI orchestration only
- `model`: immutable rename plans and UI row state
- `strategy`: pluggable filename-generation strategies
- `service`: rename planning and safe execution
- `validation`: preflight validation and collision checks
- `util`: filename, size, and animation helpers

The executor uses a two-stage move:

1. Every source file is moved to a unique temporary path.
2. Every temporary file is moved to its final destination.

If an operation fails, the executor attempts to restore every file to its original path.

## Responsive layout

The main content is placed inside a vertical `ScrollPane`, while the footer action bar stays fixed at the bottom of the window. The **Rename Files** button therefore remains visible on short screens. The controller also switches the table to compact modes as the window becomes narrower, hiding lower-priority columns while preserving the original name, generated name, and remove action.

Minimum supported window size: `560 × 480`.
