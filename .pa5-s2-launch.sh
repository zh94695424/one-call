#!/usr/bin/env bash
set -euo pipefail
cd /mnt/c/Users/Administrator/.codex/worktrees/pa4-v004-catalog-order-payment/codex-call
workspace="$(pwd -P)"
test -x /root/.local/bin/claude-agent-run
test -f "$workspace/.pa5-run-bounded.sh"
agent_timeout="${PA5_AGENT_TIMEOUT:-1h}"
run_dir="$(mktemp -d /tmp/sub-agents.pa5-s2impl.XXXXXX)"
task="pa5-v005-s2-impl-mimo26flash-$(date +%Y%m%d-%H%M%S)"
printf 'RUN_DIR=%s TASK=%s\n' "$run_dir" "$task" | tee "$run_dir/meta"
prompt="$(tr -d '\r' < .pa5-s2-prompt.md)
Task label: $task
Run directory for evidence: $run_dir"
set +e
bash "$workspace/.pa5-run-bounded.sh" implementation "$agent_timeout" /root/.local/bin/claude-agent-run --provider mimo26flash --cwd "$workspace" --instance "$task" --no-persist --output-format json --output "$run_dir/result.json" --stderr "$run_dir/stderr" --permission-mode acceptEdits --prompt "$prompt" -- --allowedTools 'Read,Glob,Grep,Bash,Edit,Write' --add-dir /mnt/d/codex-call/.git/worktrees/codex-call4 /tmp
result=$?
printf '%s\n' "$result" > "$run_dir/exit"
printf 'S2_RUNNER_EXIT=%s RUN_DIR=%s\n' "$result" "$run_dir"
exit "$result"
