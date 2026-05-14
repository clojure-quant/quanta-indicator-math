#!/usr/bin/env bash
# Regenerate lib/math/test/quanta/math/rdata/*.csv for quanta.math.neanderthal-test
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
exec Rscript "$ROOT/scripts/gen_quanta_math_test_rdata.R" "$ROOT"
