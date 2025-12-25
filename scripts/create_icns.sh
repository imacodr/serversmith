#!/bin/bash

# Convert PNG to ICNS for macOS
# Usage: ./create_icns.sh input.png output.icns

INPUT_PNG=$1
OUTPUT_ICNS=$2

if [ -z "$INPUT_PNG" ] || [ -z "$OUTPUT_ICNS" ]; then
    echo "Usage: $0 input.png output.icns"
    exit 1
fi

ICONSET_DIR="ServerSmith.iconset"
mkdir -p "$ICONSET_DIR"

# Resize to various standard sizes
sips -z 16 16     "$INPUT_PNG" --out "$ICONSET_DIR/icon_16x16.png"
sips -z 32 32     "$INPUT_PNG" --out "$ICONSET_DIR/icon_16x16@2x.png"
sips -z 32 32     "$INPUT_PNG" --out "$ICONSET_DIR/icon_32x32.png"
sips -z 64 64     "$INPUT_PNG" --out "$ICONSET_DIR/icon_32x32@2x.png"
sips -z 128 128   "$INPUT_PNG" --out "$ICONSET_DIR/icon_128x128.png"
sips -z 256 256   "$INPUT_PNG" --out "$ICONSET_DIR/icon_128x128@2x.png"
sips -z 256 256   "$INPUT_PNG" --out "$ICONSET_DIR/icon_256x256.png"
sips -z 512 512   "$INPUT_PNG" --out "$ICONSET_DIR/icon_256x256@2x.png"
sips -z 512 512   "$INPUT_PNG" --out "$ICONSET_DIR/icon_512x512.png"
cp "$INPUT_PNG" "$ICONSET_DIR/icon_512x512@2x.png" # Assuming input is large enough

# Convert to icns
iconutil -c icns "$ICONSET_DIR" -o "$OUTPUT_ICNS"

# Cleanup
rm -rf "$ICONSET_DIR"

echo "Created $OUTPUT_ICNS"
