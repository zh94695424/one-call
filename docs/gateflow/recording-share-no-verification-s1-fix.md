# S1 fix — recording share without verification

- Finding: S1-01 accepted.
- Fix: added `shareLifecycle` as the record lifecycle truth for management and recipient checks. Management now derives “有效 / 已过期 / 已撤销 / 次数用尽” and action availability from it; `shareBlockedState` only adds the current admitted-session exception needed to allow the final accepted visit to keep playing.
- QA: added management assertions for visit exhaustion and dynamic expiry. Also strengthened the no-verification path to open while signed out.
- Validation: full Web QA passed after fix; build check and `git diff --check` passed; refreshed management screenshot visually inspected.
- Finding status: S1-01 `已修复`.
- Residual risks: R1–R3 remain classified and assigned in control. No new risks.
- Completion: fix complete; next gate `re-review`.
