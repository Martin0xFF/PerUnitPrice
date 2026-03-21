#!/bin/bash

CONTAINER_NAME="per-unit-price-dev"

# Check if podman has enough memory (8GB = 8 * 1024 * 1024 * 1024 bytes)
REQUIRED_MEM=$((8 * 1024 * 1024 * 1024))
TOTAL_MEM=$(podman info --format '{{.Host.MemTotal}}' 2>/dev/null || echo 0)

if [ "$TOTAL_MEM" -lt "$REQUIRED_MEM" ]; then
    echo "INFRA error: podman machine doesn't have enough ram (Found: $((TOTAL_MEM / 1024 / 1024 / 1024))GB, Required: 8GB)"
    exit 1
fi

# Handle 'stop' command
if [ "$1" == "stop" ]; then
    echo "Stopping build container..."
    podman stop "$CONTAINER_NAME"
    exit 0
fi

# Ensure image is built
podman build -t per-unit-price-builder .

# Ensure container is running
if ! podman ps --format '{{.Names}}' | grep -q "^$CONTAINER_NAME$"; then
    echo "Starting persistent build container..."
    # If it exists but is stopped, remove it first to ensure clean state
    podman rm -f "$CONTAINER_NAME" >/dev/null 2>&1
    podman run -d --name "$CONTAINER_NAME" -v $(pwd):/app:Z per-unit-price-builder tail -f /dev/null
fi

# Determine gradle task (defaults to assembleDebug)
GRADLE_TASK=${1:-"assembleDebug"}

echo "Executing: gradle $GRADLE_TASK inside $CONTAINER_NAME..."
podman exec "$CONTAINER_NAME" gradle "$GRADLE_TASK"

# Create local output directory and move artifacts if they exist
mkdir -p out
if [ -d "app/build/outputs/apk/debug" ]; then
    # Check if there are any files to move
    if ls app/build/outputs/apk/debug/* >/dev/null 2>&1; then
        mv app/build/outputs/apk/debug/* out/
        echo "Build artifacts moved to local 'out' directory."
    fi
fi
