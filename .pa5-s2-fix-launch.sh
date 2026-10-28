#!/usr/bin/env bash
set -euo pipefail
workspace='/mnt/c/Users/Administrator/.codex/worktrees/pa4-v004-catalog-order-payment/codex-call'
cd "$workspace"
workspace="$(pwd -P)"
test -f "$workspace/.pa5-run-bounded.sh"
agent_timeout="${PA5_AGENT_TIMEOUT:-1h}"
run_dir="$(mktemp -d /tmp/sub-agents.pa5-s2fix.XXXXXX)"
printf 'RUN_DIR=%s\n' "$run_dir"
private="$(mktemp -d /tmp/pa5-s2fix-build.XXXXXX)"
mkdir -p "$private/backend"
tar -C backend --exclude=target -cf - . | tar -C "$private/backend" -xf -
prompt="$(perl -pe 's/\x0D\z//' "$workspace/.pa5-s2-fix-prompt.md")
Fix author=MiMo-V2.6-Flash
Own artifact=$workspace/docs/gateflow/pa5-v005-ivr-task-session-call-foundation-s2-fix-1.md
Private backend=$private/backend
Run directory=$run_dir"
set +e
bash "$workspace/.pa5-run-bounded.sh" fix "$agent_timeout" /root/.local/bin/claude-agent-run --provider mimo26flash --cwd "$workspace" --instance pa5-v005-s2-fix1-mimo26flash-20261028-214500 --no-persist --output-format json --output "$run_dir/result.json" --stderr "$run_dir/stderr" --permission-mode acceptEdits --prompt "$prompt" -- --allowedTools 'Read,Glob,Grep,Bash,Edit,Write' --add-dir /mnt/d/codex-call/.git/worktrees/codex-call4 "$private" "$run_dir"
status=$?
printf '%s\n' "$status" > "$run_dir/exit"
printf 'PRIVATE=%s\n' "$private" > "$run_dir/meta"
printf 'FIX_RUN_EXIT=%s RUN_DIR=%s\n' "$status" "$run_dir"
exit "$status"
