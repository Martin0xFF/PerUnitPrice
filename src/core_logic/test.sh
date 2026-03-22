#!/bin/bash

CONTAINER_NAME="per-unit-price-dev"

# Ensure container is running by calling build.sh with no-op if needed, 
# or just check and start it directly here. 
# Using build.sh to ensure consistency is better.
if ! podman ps --format '{{.Names}}' | grep -q "^$CONTAINER_NAME$"; then
    echo "Starting build container via build.sh..."
    # We run a no-op task (or just help) to trigger container startup without a full build
    ../../build.sh help >/dev/null 2>&1
fi

echo "Running Rust core logic tests inside $CONTAINER_NAME..."
podman exec "$CONTAINER_NAME" bash -c "cd src/core_logic && cargo test"
