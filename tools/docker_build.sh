#!/bin/bash
set -euo pipefail
# Source is read-only; all Gradle/decoded/build writes go to a fresh Linux tree.
exec python3 /source/tools/build_production.py \
  --input /source/twitch.zip --workspace /tmp/bttv-build --output /output/build
