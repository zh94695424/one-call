#!/usr/bin/env bash
set -euo pipefail
workspace='/mnt/c/Users/Administrator/.codex/worktrees/pa4-v004-catalog-order-payment/codex-call'
cd "$workspace"
workspace="$(pwd -P)"
test -x /root/.local/bin/claude-agent-run
test -f "$workspace/.pa5-run-bounded.sh"
test -f docs/gateflow/pa5-v005-ivr-task-session-call-foundation-s2-implementation.md
agent_timeout="${PA5_AGENT_TIMEOUT:-1h}"
run_dir="$(mktemp -d /tmp/sub-agents.pa5-s2review.XXXXXX)"
printf 'RUN_DIR=%s\n' "$run_dir"
sha256sum backend/src/main/resources/db/migration/V005__ivr_task_session_call.sql backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java .github/workflows/ci.yml backend/README.md docs/14-production-data-model-and-flyway-schema-spec.md docs/gateflow/pa5-v005-ivr-task-session-call-foundation-plan.md docs/gateflow/pa5-v005-ivr-task-session-call-foundation-s2-implementation.md > "$run_dir/frozen.sha256"
launch() {
  local role="$1" provider="$2" ts artifact private task pid
  ts="$(date +%Y%m%d-%H%M%S)"
  artifact="docs/reviews/code-review-${ts}.md"
  test ! -e "$artifact"
  private="$(mktemp -d "/tmp/pa5-s2r-${role}-build.XXXXXX")"
  mkdir -p "$private/backend"
  tar -C backend --exclude=target -cf - . | tar -C "$private/backend" -xf -
  task="pa5-v005-s2-review-${role}-${ts}"
  (
    unset GIT_DIR GIT_WORK_TREE
    prompt="$(tr -d '\r' < "$workspace/.pa5-s2-review-prompt.md")
Reviewer=$role Provider=$provider Task=$task
Own artifact=$workspace/$artifact
Private backend=$private/backend
Frozen manifest=$run_dir/frozen.sha256"
    set +e
    bash "$workspace/.pa5-run-bounded.sh" "review-$role" "$agent_timeout" /root/.local/bin/claude-agent-run --provider "$provider" --cwd "$workspace" --instance "$task" --no-persist --output-format json --output "$run_dir/$role.json" --stderr "$run_dir/$role.stderr" --permission-mode acceptEdits --prompt "$prompt" -- --allowedTools 'Read,Glob,Grep,Bash,Edit,Write' --add-dir /mnt/d/codex-call/.git/worktrees/codex-call4 "$private" "$run_dir"
    result=$?
    printf '%s\n' "$result" > "$run_dir/$role.exit"
    exit "$result"
  ) &
  pid=$!
  review_pids+=("$pid")
  printf '%s PID=%s TASK=%s ARTIFACT=%s BUILD=%s\n' "$role" "$pid" "$task" "$artifact" "$private" | tee "$run_dir/$role.meta"
}
review_pids=()
launch A ds
sleep 1
launch B mimo26
review_status=0
for pid in "${review_pids[@]}"; do
  if ! wait "$pid"; then
    review_status=1
  fi
done
printf 'REVIEW_RUNNERS_EXITED\n'
if (( review_status != 0 )); then
  printf 'REVIEW_RUNNERS_FAILED\n' >&2
  exit "$review_status"
fi
