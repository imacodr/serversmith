#!/bin/bash

# Cross-platform icon generation
# Usage: ./scripts/create_icons.sh input.png output_dir

INPUT_PNG=$1
OUTPUT_DIR=$2

if [ -z "$INPUT_PNG" ] || [ -z "$OUTPUT_DIR" ]; then
    echo "Usage: $0 input.png output_dir"
    exit 1
fi

# macOS ICNS (only runs on macOS)
if [[ "$OSTYPE" == "darwin"* ]]; then
    echo "Detected macOS, generating .icns..."
    ICONSET_DIR="ServerSmith.iconset"
    mkdir -p "$ICONSET_DIR"
    sips -z 16 16     "$INPUT_PNG" --out "$ICONSET_DIR/icon_16x16.png" > /dev/null
    sips -z 32 32     "$INPUT_PNG" --out "$ICONSET_DIR/icon_16x16@2x.png" > /dev/null
    sips -z 32 32     "$INPUT_PNG" --out "$ICONSET_DIR/icon_32x32.png" > /dev/null
    sips -z 64 64     "$INPUT_PNG" --out "$ICONSET_DIR/icon_32x32@2x.png" > /dev/null
    sips -z 128 128   "$INPUT_PNG" --out "$ICONSET_DIR/icon_128x128.png" > /dev/null
    sips -z 256 256   "$INPUT_PNG" --out "$ICONSET_DIR/icon_128x128@2x.png" > /dev/null
    sips -z 256 256   "$INPUT_PNG" --out "$ICONSET_DIR/icon_256x256.png" > /dev/null
    sips -z 512 512   "$INPUT_PNG" --out "$ICONSET_DIR/icon_256x256@2x.png" > /dev/null
    sips -z 512 512   "$INPUT_PNG" --out "$ICONSET_DIR/icon_512x512.png" > /dev/null
    cp "$INPUT_PNG" "$ICONSET_DIR/icon_512x512@2x.png"
    iconutil -c icns "$ICONSET_DIR" -o "$OUTPUT_DIR/app-icon.icns"
    rm -rf "$ICONSET_DIR"
    echo "Created $OUTPUT_DIR/app-icon.icns"
fi

# Note: .ico and .png for Linux are handled via external tools in CI
# or jpackage takes the PNG directly and scales it for Linux.
# For Windows in CI, we will use a specific action or tool to convert.
