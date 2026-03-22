#!/bin/bash

CONTAINER_NAME="per-unit-price-dev"
LOG_FILE="build.log"

# Clear/Initialize log file
> "$LOG_FILE"

echo "Infrastructure check..."
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
    podman stop "$CONTAINER_NAME" >> "$LOG_FILE" 2>&1
    exit 0
fi

# Ensure image is built
echo -n "Building container image... "
if podman build -t per-unit-price-builder -f infra/Dockerfile . >> "$LOG_FILE" 2>&1; then
    echo "OK"
else
    echo "FAILED (Check $LOG_FILE)"
    exit 1
fi

# Ensure container is running
if ! podman ps --format '{{.Names}}' | grep -q "^$CONTAINER_NAME$"; then
    echo -n "Starting persistent build container... "
    # If it exists but is stopped, remove it first to ensure clean state
    podman rm -f "$CONTAINER_NAME" >> "$LOG_FILE" 2>&1
    if podman run -d --name "$CONTAINER_NAME" -v $(pwd):/app:Z per-unit-price-builder tail -f /dev/null >> "$LOG_FILE" 2>&1; then
        echo "OK"
    else
        echo "FAILED (Check $LOG_FILE)"
        exit 1
    fi
fi

# Determine gradle task (defaults to assembleDebug)
GRADLE_TASK=${1:-"assembleDebug"}

# Run formatters
echo -n "Formatting Rust code... "
if podman exec "$CONTAINER_NAME" bash -c "cd src/core_logic && cargo fmt" >> "$LOG_FILE" 2>&1; then
    echo "OK"
else
    echo "FAILED (Check $LOG_FILE)"
    exit 1
fi

echo -n "Formatting Kotlin code... "
if podman exec "$CONTAINER_NAME" bash -c "ktlint -F 'src/app/src/main/java/**/*.kt'" >> "$LOG_FILE" 2>&1; then
    echo "OK"
else
    echo "FAILED (Check $LOG_FILE)"
    exit 1
fi

echo -n "Executing gradle $GRADLE_TASK... "
if podman exec "$CONTAINER_NAME" gradle "$GRADLE_TASK" >> "$LOG_FILE" 2>&1; then
    echo "OK"
else
    echo "FAILED (Check $LOG_FILE)"
    exit 1
fi

# Create local output directory and move artifacts if they exist
echo -n "Moving artifacts... "
mkdir -p out
if [ -d "src/app/build/outputs/apk/debug" ]; then
    # Check if there are any files to move
    if ls src/app/build/outputs/apk/debug/* >> "$LOG_FILE" 2>&1; then
        mv src/app/build/outputs/apk/debug/* out/ >> "$LOG_FILE" 2>&1
        echo "OK (Artifacts in 'out/')"
    else
        echo "Done (No new artifacts)"
    fi
else
    echo "Done"
fi
