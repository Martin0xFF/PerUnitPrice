# PerUnitPrice Android App

An Android application for calculating per-unit prices, featuring a high-performance Rust core and a Kotlin XML-based UI. The development environment is containerized using **Docker** (default) or **Podman** to ensure a consistent, multi-arch compatible build process on a `debian:bookworm-slim` base.

## Architecture
- **Core Logic (`src/core_logic/`):** Rust crate for parsing units (kg, ml, items) and calculating prices.
- **UI (`src/app/`):** Kotlin-based Android application using XML Views.
- **Interop:** Standard JNI bindings between Kotlin and Rust.
- **Infrastructure (`infra/`):** Containerized build system optimized for multi-arch support (e.g., ARM64 Darwin hosts with x86_64 Android toolchains).

## Prerequisites
- **Docker** (recommended) or **Podman** installed and running.
- **Resources:** If using Podman, the machine **must have at least 8GB of RAM** allocated.
  ```bash
  podman machine set --memory 8192
  ```

## Development Flow

### 1. Building the App
The `build.sh` script manages a persistent development container and performs automatic code formatting before building.

- **Standard Build (Docker):**
  ```bash
  ./build.sh
  ```
- **Standard Build (Podman):**
  ```bash
  ./build.sh --podman
  ```
- **Clean Build:**
  ```bash
  ./build.sh clean
  ```
- **Stop the Dev Container:**
  ```bash
  ./build.sh stop
  ```
- **Formatting:** Code is automatically formatted using `rustfmt` (Rust) and `ktlint` (Kotlin) during the build.
- **Logging:** All build output is redirected to `build.log`.
- **Output:** Resulting APKs are moved to the local `out/` directory.

### 2. Running Core Logic Tests
Run the Rust unit tests inside the persistent development container:
```bash
./src/core_logic/test.sh [--podman]
```

### 3. Infrastructure Tests
Validate the build system's requirements (like the 8GB memory check for Podman):
```bash
./infra/tests/test_mem_check.sh
```

## Project Structure
- `src/app/`: Android project source code (Kotlin).
- `src/core_logic/`: Rust source code and logic tests.
- `infra/`: Container environment (`Dockerfile`) and infrastructure tests.
- `out/`: Local directory for build artifacts (ignored by git).
- `build.sh`: Main entry point for the containerized build process.
- `build.log`: Detailed output from the last build attempt (ignored by git).
