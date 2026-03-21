#!/bin/bash
podman build -t per-unit-price-builder .
podman run --rm -v $(pwd):/app:Z per-unit-price-builder bash -c "gradle wrapper && ./gradlew assembleDebug --no-daemon"
