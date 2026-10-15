#!/usr/bin/env bash
# Generate the shared modular-avatar base GLBs into tabletopweb/public/models/avatars.
#
# Wraps Blender + the MPFB2 extension in an isolated, gitignored Blender config
# so the run is hermetic and reproducible. Run fetch-tools.sh first.
#
# Usage:
#   tools/avatar-gen/generate.sh [output-dir]
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

# Install MPFB2 into the isolated config on first run (idempotent).
if [[ ! -d "${CONFIG}/extensions/user_default/mpfb" ]]; then
  "${BLENDER_BIN}" --command extension install-file -r user_default -e "${CACHE}/mpfb2-latest.zip"
fi

"${BLENDER_BIN}" --background --python "${HERE}/generate_base.py" -- --out-dir "${OUT_DIR}"
echo "base GLBs in ${OUT_DIR}:"
ls -lh "${OUT_DIR}"/base-*.glb
