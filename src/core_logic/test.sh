#!/bin/bash

CONTAINER_NAME="per-unit-price-dev"
CONTAINER_ENGINE="docker"
BUILD_FLAGS=""

# Parse arguments for --podman flag
for arg in "$@"; do
    if [[ "$arg" == "--podman" ]]; then
        CONTAINER_ENGINE="podman"
        BUILD_FLAGS="--podman"
    fi
done

# Ensure container is running by calling build.sh with no-op if needed, 
# or just check and start it directly here. 
# Using build.sh to ensure consistency is better.
if ! $CONTAINER_ENGINE ps --format '{{.Names}}' | grep -q "^$CONTAINER_NAME$"; then
    echo "Starting build container via build.sh ($CONTAINER_ENGINE)..."
    # We run a no-op task (or just help) to trigger container startup without a full build
    ../../build.sh $BUILD_FLAGS help >/dev/null 2>&1
fi

echo "Running Rust core logic tests inside $CONTAINER_NAME using $CONTAINER_ENGINE..."
$CONTAINER_ENGINE exec "$CONTAINER_NAME" bash -c "cd src/core_logic && cargo test"
