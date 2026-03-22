#!/bin/bash

# Test script for build.sh memory check logic
# We'll create a standalone test for the memory check logic extracted from build.sh

run_test() {
    local name=$1
    local mock_mem=$2
    local expected_exit=$3
    
    # Extract the logic and mock TOTAL_MEM
    local script_to_run="
REQUIRED_MEM=\$((6 * 1024 * 1024 * 1024))
TOTAL_MEM=$mock_mem

if [ \"\$TOTAL_MEM\" -lt \"\$REQUIRED_MEM\" ]; then
    # echo \"INFRA error: podman machine doesn't have enough ram\"
    exit 1
fi
exit 0
"
    
    bash -c "$script_to_run"
    local actual_exit=$?
    
    if [ $actual_exit -eq $expected_exit ]; then
        echo "PASS: $name"
    else
        echo "FAIL: $name (Expected exit $expected_exit, got $actual_exit)"
        return 1
    fi
}

# Test 1: Fail if less than 6GB (e.g., 4GB)
run_test "Fail on 4GB" $((4 * 1024 * 1024 * 1024)) 1

# Test 2: Pass if exactly 6GB
run_test "Pass on 6GB" $((6 * 1024 * 1024 * 1024)) 0

# Test 3: Pass if more than 6GB (e.g., 16GB)
run_test "Pass on 16GB" $((16 * 1024 * 1024 * 1024)) 0
