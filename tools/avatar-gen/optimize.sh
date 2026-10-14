#!/usr/bin/env bash
# Optional: shrink generated avatar GLBs with glTF-Transform (texture resize,
# quantize, weld). Not required — the committed bases are already ~1 MB — but
# useful once parts/textures are added. Uses npx so no repo dependency is added.
#
# Usage: tools/avatar-gen/optimize.sh [file-or-dir ...]
set -euo pipefail

TARGETS=("$@")
if [[ ${#TARGETS[@]} -eq 0 ]]; then
  TARGETS=("$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)/tabletopweb/public/models/avatars")
fi

for target in "${TARGETS[@]}"; do
  if [[ -d "${target}" ]]; then
    mapfile -t files < <(find "${target}" -type f -name '*.glb')
  else
    files=("${target}")
  fi
  for file in "${files[@]}"; do
    echo "optimizing ${file}"
    npx --yes @gltf-transform/cli@^3 optimize "${file}" "${file}" \
      --texture-compress webp --texture-size 1024
  done
done
