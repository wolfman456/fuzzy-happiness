#!/usr/bin/env bash
# Generate the race-feature avatar part GLBs into tabletopweb/public/models/avatars/parts.
#
# Wraps Blender in an isolated, gitignored config so the run is hermetic and
# reproducible (no MPFB2 needed — parts are plain meshes). Run fetch-tools.sh first.
#
# Usage:
#   tools/avatar-gen/generate_parts.sh [output-dir]
# Default output-dir: tabletopweb/public/models/avatars

set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${HERE}/../.." && pwd)"
CACHE="${HERE}/.cache"
CONFIG="${CACHE}/blender-config"
OUT_DIR="${1:-${REPO_ROOT}/tabletopweb/public/models/avatars}"

BLENDER_BIN="$(find "${CACHE}" -maxdepth 2 -type f -name blender -path '*-linux-x64/*' | head -1 || true)"
if [[ -z "${BLENDER_BIN}" ]]; then
  echo "Blender not found in ${CACHE}; run fetch-tools.sh first." >&2
  exit 1
fi

export BLENDER_USER_CONFIG="${CONFIG}"
export BLENDER_USER_EXTENSIONS="${CONFIG}/extensions"
export BLENDER_USER_SCRIPTS="${CONFIG}/scripts"

"${BLENDER_BIN}" --background --python "${HERE}/generate_parts.py" -- --out-dir "${OUT_DIR}"
echo "part GLBs in ${OUT_DIR}/parts:"
ls -lhR "${OUT_DIR}/parts"
