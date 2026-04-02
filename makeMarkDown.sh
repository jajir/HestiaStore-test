#!/bin/bash

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${PROJECT_ROOT}/report-env.sh"

resolve_local_report_environment "${PROJECT_ROOT}"

groovy "${PROJECT_ROOT}/src/main/groovy/makeMarkDown.groovy" out-write-single-thread
groovy "${PROJECT_ROOT}/src/main/groovy/makeMarkDown.groovy" out-read-single-thread
groovy "${PROJECT_ROOT}/src/main/groovy/makeMarkDown.groovy" out-sequential-read
groovy "${PROJECT_ROOT}/src/main/groovy/makeMarkDown.groovy" out-read-multi-thread
groovy "${PROJECT_ROOT}/src/main/groovy/makeMarkDown.groovy" out-write-multi-thread
