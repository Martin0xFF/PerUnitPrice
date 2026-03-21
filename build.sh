#!/bin/bash

# Check if podman has enough memory (8GB = 8 * 1024 * 1024 * 1024 bytes)
REQUIRED_MEM=$((8 * 1024 * 1024 * 1024))
TOTAL_MEM=$(podman info --format '{{.Host.MemTotal}}' 2>/dev/null || echo 0)

if [ "$TOTAL_MEM" -lt "$REQUIRED_MEM" ]; then
    echo "INFRA error: podman machine doesn't have enough ram (Found: $((TOTAL_MEM / 1024 / 1024 / 1024))GB, Required: 8GB)"
    exit 1
fi

# Create local output directory
mkdir -p out

# Build
podman build -t per-unit-price-builder .
podman run --rm -v $(pwd):/app:Z per-unit-price-builder bash -c "gradle assembleDebug --no-daemon"

# Move resulting APK to the local output folder
if [ -d "app/build/outputs/apk/debug" ]; then
    mv app/build/outputs/apk/debug/* out/
    echo "Build artifacts moved to local 'out' directory."
fi
