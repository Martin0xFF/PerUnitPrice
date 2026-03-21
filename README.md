# PerUnitPrice Android App

An Android application for calculating per-unit prices, featuring a high-performance Rust core and a Kotlin XML-based UI. The entire development environment is containerized using Podman to ensure a consistent, multi-arch compatible build process.

## Architecture
- **Core Logic (`core_logic/`):** Rust crate for parsing units (kg, ml, items) and calculating prices.
- **UI (`app/`):** Kotlin-based Android application using XML Views.
- **Interop:** Standard JNI bindings between Kotlin and Rust.
- **Infrastructure:** Podman-based build system optimized for ARM64 (Darwin) hosts with x86_64 Android toolchain support.

## Prerequisites
- **Podman:** Installed and running.
- **Resources:** The Podman machine **must have at least 8GB of RAM** allocated.
  ```bash
  podman machine set --memory 8192
  ```

## Development Flow

### 1. Building the App
The `build.sh` script manages a persistent development container and utilizes the Gradle daemon for fast iterative builds.

- **Standard Build (assembleDebug):**
  ```bash
  ./build.sh
  ```
- **Clean Build:**
  ```bash
  ./build.sh clean
  ```
- **Stop the Dev Container:**
  ```bash
  ./build.sh stop
  ```
- **Output:** Resulting APKs are moved to the local `out/` directory.

### 2. Running Core Logic Tests
Run the Rust unit tests inside the persistent development container:
```bash
./test.sh
```

### 3. Infrastructure Tests
Validate the build system's requirements (like the 8GB RAM check):
```bash
./infra-tests/test_mem_check.sh
```

## Project Structure
- `app/`: Android project source code.
- `core_logic/`: Rust source code.
- `out/`: Local directory for build artifacts (ignored by git).
- `infra-tests/`: Scripts to verify the development environment.
- `Dockerfile`: Multi-arch build environment definition.
