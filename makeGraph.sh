#!/bin/bash

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

groovy "${PROJECT_ROOT}/src/main/groovy/makeGraph.groovy"
