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
  --main-class "dev.perillo.serversmith.Launcher" \
  --input "build/install/ServerSmith/lib" \
  --dest "build/dist" \
  --icon "src/main/resources/dev/perillo/serversmith/app-icon.png" \
  --type "dmg"
```

## Creating a GitHub Release

The project is configured to automatically create a GitHub Release when you push a version tag.

1. **Tag your commit**:
   ```bash
   git tag -a v1.0.0 -m "Release version 1.0.0"
   ```

2. **Push the tag**:
   ```bash
   git push origin v1.0.0
   ```

3. **Check GitHub Actions**:
   The "Build and Release" workflow will trigger, build the DMG, and automatically create a release with the DMG attached as an asset.

### Manual Release
You can also trigger a release manually for any existing tag:
1. Go to the **Actions** tab on GitHub.
2. Select the **Build and Release** workflow.
3. Click **Run workflow**.
4. Enter the tag name (e.g., `v1.0.0`) and click **Run workflow**.

## Output
The final executable/installer will be located in `build/dist/ServerSmith-1.0.0.dmg`.

## Metadata
The executable includes the following professional information:
- **Author/Vendor**: Sam Perillo
- **App Name**: ServerSmith
- **Version**: 1.0.0
