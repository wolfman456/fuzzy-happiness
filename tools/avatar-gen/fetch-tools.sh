#!/usr/bin/env bash
# Fetch the Blender + MPFB2 + MakeHuman CC0 asset toolchain used to generate the
# modular avatar base meshes. Everything lands in .cache/ (gitignored) so the
# generated GLBs are reproducible without hand-authoring in a Blender GUI.
#
# Usage:
#   tools/avatar-gen/fetch-tools.sh                 # latest 4.5 LTS + MPFB2 + assets
#   BLENDER_VERSION=4.5.14 tools/avatar-gen/fetch-tools.sh
#
# Requires: curl, tar, unzip. No root.

set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CACHE="${HERE}/.cache"
mkdir -p "${CACHE}"

BLENDER_SERIES="${BLENDER_SERIES:-4.5}"
MPFB2_URL="${MPFB2_URL:-https://files.makehumancommunity.org/plugins/mpfb2-latest.zip}"
ASSETS_URL="${ASSETS_URL:-https://files.makehumancommunity.org/asset_packs/makehuman_system_assets/makehuman_system_assets_cc0.zip}"

resolve_blender_version() {
  if [[ -n "${BLENDER_VERSION:-}" ]]; then
    echo "${BLENDER_VERSION}"
    return
  fi
  # Pick the highest patch of the requested LTS series from the release index.
  curl -fsSL "https://download.blender.org/release/Blender${BLENDER_SERIES}/" \
    | grep -oE "blender-${BLENDER_SERIES}\.[0-9]+-linux-x64\.tar\.xz" \
    | sed -E "s/blender-(${BLENDER_SERIES}\.[0-9]+)-linux-x64\.tar\.xz/\1/" \
    | sort -t. -k3 -n \
    | tail -1
}

fetch() {
  local url="$1" out="$2"
  if [[ -s "${out}" ]]; then
    echo "cache hit: $(basename "${out}")"
    return
  fi
  echo "downloading $(basename "${out}")"
  curl -fL --retry 3 --retry-delay 2 -o "${out}.part" "${url}"
  mv "${out}.part" "${out}"
}

BVERSION="$(resolve_blender_version)"
[[ -n "${BVERSION}" ]] || { echo "could not resolve Blender ${BLENDER_SERIES}.x version" >&2; exit 1; }
echo "Blender ${BVERSION} (LTS series ${BLENDER_SERIES})"

BLENDER_TARBALL="${CACHE}/blender-${BVERSION}-linux-x64.tar.xz"
fetch "https://download.blender.org/release/Blender${BLENDER_SERIES}/blender-${BVERSION}-linux-x64.tar.xz" "${BLENDER_TARBALL}"

fetch "${MPFB2_URL}" "${CACHE}/mpfb2-latest.zip"
fetch "${ASSETS_URL}" "${CACHE}/makehuman_system_assets_cc0.zip"

if [[ ! -x "${CACHE}/blender-${BVERSION}-linux-x64/blender" ]]; then
  echo "extracting Blender"
  tar -xJf "${BLENDER_TARBALL}" -C "${CACHE}"
fi

echo "toolchain ready in ${CACHE}"
echo "  blender: ${CACHE}/blender-${BVERSION}-linux-x64/blender"
echo "  mpfb2:   ${CACHE}/mpfb2-latest.zip"
echo "  assets:  ${CACHE}/makehuman_system_assets_cc0.zip"
