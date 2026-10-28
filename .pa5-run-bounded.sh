#!/usr/bin/env bash
set -Eeuo pipefail

# Run one external check with a hard wall-clock bound.
if [[ $# -lt 3 ]]; then
  printf 'usage: %s <label> <duration> <command> [args...]\n' "$0" >&2
  exit 64
fi

label="$1"
duration="$2"
shift 2
grace="${PA5_KILL_GRACE:-10s}"

command -v timeout >/dev/null 2>&1 || {
  printf '[bounded:%s] timeout command is required\n' "$label" >&2
  exit 69
}

started_at="$(date +%s)"
printf '[bounded:start] %s timeout=%s\n' "$label" "$duration" >&2
set +e
# Non-interactive runner: keep the child in a separate process group so a
# timeout also reaches descendants such as shell pipelines and test helpers.
timeout --kill-after="$grace" "$duration" "$@"
status=$?
set -e
elapsed=$(( $(date +%s) - started_at ))

if [[ "$status" -eq 124 || "$status" -eq 137 ]]; then
  printf '[bounded:timeout] %s elapsed=%ss status=%s\n' "$label" "$elapsed" "$status" >&2
elif [[ "$status" -eq 0 ]]; then
  printf '[bounded:pass] %s elapsed=%ss\n' "$label" "$elapsed" >&2
else
  printf '[bounded:fail] %s elapsed=%ss status=%s\n' "$label" "$elapsed" "$status" >&2
fi
exit "$status"
