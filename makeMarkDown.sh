#!/bin/bash

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RESULTS_DIR="${PROJECT_ROOT}/results"

mkdir -p "${RESULTS_DIR}"

groovy "${PROJECT_ROOT}/src/main/groovy/makeMarkDown.groovy" out-write
groovy "${PROJECT_ROOT}/src/main/groovy/makeMarkDown.groovy" out-read
groovy "${PROJECT_ROOT}/src/main/groovy/makeMarkDown.groovy" out-sequential
groovy "${PROJECT_ROOT}/src/main/groovy/makeMarkDown.groovy" out-multithread-read
groovy "${PROJECT_ROOT}/src/main/groovy/makeMarkDown.groovy" out-multithread-write
