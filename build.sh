#!/bin/bash

CONTAINER_NAME="per-unit-price-dev"
LOG_FILE="build.log"
CONTAINER_ENGINE="docker"

# --- Functions ---

run_with_spinner() {
    local msg="$1"
    shift
    # Run command in background, redirecting output to the log
    ( "$@" ) >> "$LOG_FILE" 2>&1 &
    local pid=$!
    local spin=('.  ' '.. ' '...')
    local i=0

    # Ensure cursor is shown when script exits or is killed
    trap "tput cnorm; exit 1" INT TERM
    tput civis # Hide cursor

    while kill -0 $pid 2>/dev/null; do
        i=$(( (i+1) % 3 ))
        printf "\r%s %s" "$msg" "${spin[$i]}"
        sleep 0.2
    done

    wait $pid
    local exit_code=$?
    
    tput cnorm # Show cursor
    printf "\r%s" "$msg"
    printf "\e[K" # Clear to end of line

    if [ $exit_code -eq 0 ]; then
        echo " OK"
    else
        echo " FAILED (Check $LOG_FILE)"
        exit 1
    fi
}

parse_arguments() {
    ARGS=()
    for arg in "$@"; do
        if [[ "$arg" == "--podman" ]]; then
            CONTAINER_ENGINE="podman"
        else
            ARGS+=("$arg")
        fi
    done
    set -- "${ARGS[@]}"
    GRADLE_TASK=${1:-"assembleDebug"}
}

initialize_log() {
    > "$LOG_FILE"
}

infrastructure_check() {
    echo "Infrastructure check..."
    if [ "$CONTAINER_ENGINE" == "podman" ]; then
        # Check if podman has enough memory (6GB = 6 * 1024 * 1024 * 1024 bytes)
        REQUIRED_MEM=$((6 * 1024 * 1024 * 1024))
        TOTAL_MEM=$(podman info --format '{{.Host.MemTotal}}' 2>/dev/null || echo 0)

        if [ "$TOTAL_MEM" -lt "$REQUIRED_MEM" ]; then
            echo "INFRA error: podman machine doesn't have enough ram (Found: $((TOTAL_MEM / 1024 / 1024 / 1024))GB, Required: 6GB)"
            exit 1
        fi
        echo "Podman memory check: OK"
    else
        echo "Using Docker engine (Skipping Podman-specific checks)"
    fi
}

handle_stop() {
    if [ "$GRADLE_TASK" == "stop" ]; then
        echo "Stopping build container..."
        $CONTAINER_ENGINE stop "$CONTAINER_NAME" >> "$LOG_FILE" 2>&1
        exit 0
    fi
}

build_image() {
    run_with_spinner "Building container image using $CONTAINER_ENGINE" \
        $CONTAINER_ENGINE build -t per-unit-price-builder -f infra/Dockerfile .
}

ensure_container_running() {
    if ! $CONTAINER_ENGINE ps --format '{{.Names}}' | grep -q "^$CONTAINER_NAME$"; then
        # Preparation step (removing old container) isn't shown with a spinner but logged
        $CONTAINER_ENGINE rm -f "$CONTAINER_NAME" >> "$LOG_FILE" 2>&1
        run_with_spinner "Starting persistent build container" \
            $CONTAINER_ENGINE run -d --name "$CONTAINER_NAME" -v $(pwd):/app:Z per-unit-price-builder tail -f /dev/null
    fi
}

format_code() {
    run_with_spinner "Formatting Rust code" \
        $CONTAINER_ENGINE exec "$CONTAINER_NAME" bash -c "cd src/core_logic && cargo fmt"
    
    run_with_spinner "Formatting Kotlin code" \
        $CONTAINER_ENGINE exec "$CONTAINER_NAME" bash -c "ktlint -F 'src/app/src/main/java/**/*.kt'"
}

execute_gradle() {
    run_with_spinner "Executing gradle $GRADLE_TASK" \
        $CONTAINER_ENGINE exec "$CONTAINER_NAME" gradle "$GRADLE_TASK"
}

copy_artifacts() {
    echo -n "Copying artifacts... "
    mkdir -p out
    local copied=false

    # Copy Debug APKs
    if [ -d "src/app/build/outputs/apk/debug" ]; then
        if ls src/app/build/outputs/apk/debug/*.apk >> "$LOG_FILE" 2>&1; then
            cp src/app/build/outputs/apk/debug/*.apk out/ >> "$LOG_FILE" 2>&1
            copied=true
        fi
    fi

    # Copy Release APKs
    if [ -d "src/app/build/outputs/apk/release" ]; then
        if ls src/app/build/outputs/apk/release/*.apk >> "$LOG_FILE" 2>&1; then
            cp src/app/build/outputs/apk/release/*.apk out/ >> "$LOG_FILE" 2>&1
            copied=true
        fi
    fi

    if [ "$copied" = true ]; then
        echo "OK (Artifacts in 'out/')"
    else
        echo "Done (No new artifacts)"
    fi
}

# --- Execution ---

parse_arguments "$@"
initialize_log
infrastructure_check
handle_stop
build_image
ensure_container_running
format_code
execute_gradle
copy_artifacts

