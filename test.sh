#!/bin/bash
podman build -t per-unit-price-builder .
podman run --rm -v $(pwd):/app:Z per-unit-price-builder bash -c "cd core_logic && cargo test"
