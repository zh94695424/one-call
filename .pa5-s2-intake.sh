#!/usr/bin/env bash
set -euo pipefail
export JAVA_HOME=/opt/jdk21
export PATH=/opt/jdk21/bin:/opt/maven-3.9.9/bin:$PATH
workspace='/mnt/c/Users/Administrator/.codex/worktrees/pa4-v004-catalog-order-payment/codex-call'
cd "$workspace"
workspace="$(pwd -P)"
bounded="$workspace/.pa5-run-bounded.sh"
test -f "$bounded"

validate_timeout="${PA5_VALIDATE_TIMEOUT:-5m}"
default_timeout="${PA5_DEFAULT_TEST_TIMEOUT:-10m}"
container_timeout="${PA5_CONTAINER_TEST_TIMEOUT:-30m}"
focused_timeout="${PA5_FOCUSED_TEST_TIMEOUT:-15m}"
concurrency_timeout="${PA5_CONCURRENCY_TEST_TIMEOUT:-10m}"
docs_timeout="${PA5_DOCS_CHECK_TIMEOUT:-60s}"

run_bounded() {
  local label="$1" duration="$2" log_file="$3"
  shift 3
  printf '[phase:start] %s timeout=%s log=%s\n' "$label" "$duration" "$log_file"
  bash "$bounded" "$label" "$duration" "$@" > "$log_file" 2>&1
  printf '[phase:pass] %s\n' "$label"
}

for required in mvn docker python3 awk bash sha256sum; do
  command -v "$required" >/dev/null 2>&1 || {
    printf '[preflight:fail] missing command: %s\n' "$required" >&2
    exit 69
  }
done

validation_dir="$(mktemp -d /tmp/pa5-s2-controller.XXXXXX)"
printf 'VALIDATION_DIR=%s\n' "$validation_dir"
cleanup_on_success="${PA5_CLEANUP_ON_SUCCESS:-0}"
cleanup_validation_dir() {
  local status=$?
  if [[ "$status" -eq 0 && "$cleanup_on_success" == 1 ]]; then
    rm -rf -- "$validation_dir"
    printf '[cleanup] removed successful validation directory\n'
  else
    printf '[cleanup] preserved validation directory: %s\n' "$validation_dir" >&2
  fi
  exit "$status"
}
trap cleanup_validation_dir EXIT
mkdir -p "$validation_dir/backend"
tar -C backend --exclude=target -cf - . | tar -C "$validation_dir/backend" -xf -
cp .github/workflows/ci.yml "$validation_dir/ci.yml"
run_bounded "docker-preflight" 30s "$validation_dir/docker.log" docker info
cd "$validation_dir/backend"
run_bounded "maven-validate" "$validate_timeout" "$validation_dir/validate.log" mvn -B -ntp validate
run_bounded "default-verify" "$default_timeout" "$validation_dir/default.log" mvn -B -ntp clean verify
run_bounded "runner-syntax" "$docs_timeout" "$validation_dir/runner-syntax.log" bash -n \
  "$workspace/.pa5-run-bounded.sh" "$workspace/.pa5-s2-intake.sh" \
  "$workspace/.pa5-s2-launch.sh" "$workspace/.pa5-s2-fix-launch.sh" \
  "$workspace/.pa5-s2-review-launch.sh"
run_bounded "runner-timeout-probe" "$docs_timeout" "$validation_dir/runner-timeout.log" \
  bash -o pipefail -c 'set +e; bash "$1" timeout-probe 1s sleep 3; rc=$?; test "$rc" -eq 124' _ \
  "$workspace/.pa5-run-bounded.sh"
export PA5_VALIDATION_DIR="$validation_dir"
focus='PostgresContainerIT#v005TaskTtlLifecycleAndGuardBackstops+v005SessionCallCanonicalCallerAndReciprocalLinkBackstops+v005LegEventDecisionFrozenIdentityAndAppendOnlyBackstops+v005ForeignKeyAndDeferredReservationFkBackstops'
run_bounded "focused-behavior" "$focused_timeout" "$validation_dir/focused-behavior.log" mvn -B -ntp -Pcontainer-smoke "-Dit.test=$focus" verify
cp -a target/failsafe-reports "$validation_dir/focused-behavior-reports"
for iteration in 1 2 3; do
  run_bounded "concurrency-$iteration" "$concurrency_timeout" "$validation_dir/concurrency-$iteration.log" mvn -B -ntp -Pcontainer-smoke '-Dit.test=PostgresContainerIT#concurrentV005TaskClaimAndActiveCallHasOneWinner' verify
  cp -a target/failsafe-reports "$validation_dir/concurrency-$iteration-reports"
done
python3 - <<'PY'
import os,pathlib,xml.etree.ElementTree as ET
root=pathlib.Path(os.environ['PA5_VALIDATION_DIR'])
for folder,n in [('focused-behavior-reports',4)]+[(f'concurrency-{i}-reports',1) for i in (1,2,3)]:
    report=next((root/folder).glob('TEST-*PostgresContainerIT.xml'))
    suite=ET.parse(report).getroot()
    counts=[int(suite.attrib[k]) for k in ('tests','failures','errors','skipped')]
    assert counts==[n,0,0,0],(folder,counts)
    print(folder,counts)
PY
run_bounded "container-verify" "$container_timeout" "$validation_dir/container.log" mvn -B -ntp -Pcontainer-smoke verify
cp -a target/failsafe-reports "$validation_dir/full-failsafe-reports"
python3 - <<'PY'
import os,re,pathlib,xml.etree.ElementTree as ET
root=pathlib.Path(os.environ['PA5_VALIDATION_DIR'])
source=(root/'backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java').read_text()
src=set(re.findall(r'@Test\s+void\s+(\w+)\(',source))
ci=(root/'ci.yml').read_text().split('expected = {',1)[1].split('}',1)[0]
expected=set(re.findall(r'"(\w+)"',ci))
report=next((root/'full-failsafe-reports').glob('TEST-*PostgresContainerIT.xml'))
suite=ET.parse(report).getroot()
actual={n.attrib['name'] for n in suite.findall('testcase')}
assert len(src)==len(expected)==len(actual)==38,(len(src),len(expected),len(actual))
assert src==expected==actual,(src^expected,expected^actual)
assert [int(suite.attrib[k]) for k in ('tests','failures','errors','skipped')]==[38,0,0,0]
print('SOURCE_CI_XML_38_EXACT')
PY
run_bounded "ci-script-parse" "$docs_timeout" "$validation_dir/ci-parse.log" bash -o pipefail -c 'awk '\''/^[ ]+python3 - <<.PY./ {p=1;next} p && /^[ ]+PY\r?$/ {exit} p {sub(/^          /, ""); sub(/\r$/, ""); print}'\'' "$1" | python3' _ "$validation_dir/ci.yml"
run_bounded "boot-smoke" 60s "$validation_dir/boot.log" java -jar target/onecall-runtime-0.0.1-SNAPSHOT.jar --server.port=0
grep -F 'Started OneCallApplication' "$validation_dir/boot.log"
jar tf target/onecall-runtime-0.0.1-SNAPSHOT.jar > "$validation_dir/jar-index.txt"
if grep -E 'BOOT-INF/lib/(spring-security|flyway|jooq|HikariCP|postgresql|testcontainers)' "$validation_dir/jar-index.txt"; then exit 1; fi
cd "$workspace"
gitcmd=(git --git-dir=/mnt/d/codex-call/.git/worktrees/codex-call4 --work-tree="$workspace")
for migration in backend/src/main/resources/db/migration/V00[1-4]*.sql; do
  "${gitcmd[@]}" diff --exit-code 02cb35494c6fcbaa8027003d83949ec2c30f211b -- "$migration"
done
printf '2f4cdef48fd5b1cec9b5f4479e99df3359073c94bf2c45cbc5b2cf8e87ba9310  backend/src/main/resources/db/migration/V005__ivr_task_session_call.sql\n' | sha256sum -c -
"${gitcmd[@]}" diff --check
normalized_doc="$validation_dir/doc14.md"
tr -d '\r' < docs/14-production-data-model-and-flyway-schema-spec.md > "$normalized_doc"
export FILE="$normalized_doc"
run_bounded "dictionary-check" "$docs_timeout" "$validation_dir/dictionary.log" bash -o pipefail -c 'awk '\''/^### 13.2.1 /{s=1;next} /^### 13.2.2 /{s=0} s && /^```bash$/{f=1;next} s && /^```$/{f=0;next} s && f && !/^FILE=/{print}'\'' "$1" | bash -e' _ "$normalized_doc"
grep -E 'Tests run:|BUILD SUCCESS' "$validation_dir/default.log" "$validation_dir/container.log" "$validation_dir/focused-behavior.log" "$validation_dir"/concurrency-*.log
tail -3 "$validation_dir/dictionary.log"
printf 'CONTROLLER_S2_INTAKE_PASS VALIDATION_DIR=%s\n' "$validation_dir"
