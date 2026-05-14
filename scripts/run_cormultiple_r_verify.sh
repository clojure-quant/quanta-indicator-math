#!/usr/bin/env bash
# Run scripts/cormultiple_r_verify.R (reads scripts/data/*.csv; see script header).
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
exec Rscript "$ROOT/scripts/cormultiple_r_verify.R"
