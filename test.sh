#!/bin/bash
docker build -t per-unit-price-builder .
docker run --rm -v $(pwd):/app per-unit-price-builder bash -c "cd core_logic && cargo test"
