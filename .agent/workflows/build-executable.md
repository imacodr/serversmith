---
description: How to build a professional executable for ServerSmith
---

# Building the ServerSmith Executable

This workflow describes how to package the ServerSmith application into a professional standalone executable using `jpackage`.

## Prerequisites
- JDK 21+ with `jpackage` tool installed (standard in modern JDKs)
- macOS (for building `.dmg` or `.app`)

## Build Steps

// turbo
1. Prepare the application distribution:
```bash
./gradlew installDist
```

// turbo
2. Build the standalone executable:
```bash
jpackage \
  --name "ServerSmith" \
  --vendor "Sam Perillo" \
  --description "A professional server management and creation tool." \
  --app-version "1.0.0" \
  --main-jar "ServerSmith.jar" \
  --main-class "dev.perillo.serversmith.App" \
  --input "build/install/ServerSmith/lib" \
  --dest "build/dist" \
  --icon "src/main/resources/dev/perillo/serversmith/app-icon.png" \
  --type "dmg"
```

## Output
The final executable/installer will be located in `build/dist/ServerSmith.dmg`.

## Metadata
The executable includes the following professional information:
- **Author/Vendor**: Sam Perillo
- **App Name**: ServerSmith
- **Version**: 1.0.0
