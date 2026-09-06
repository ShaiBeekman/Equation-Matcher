# Exact-artifact Windows release verification

`release-clean-windows.yml` runs deliberately with `workflow_dispatch` on a fresh
GitHub-hosted `windows-2025` VM. It never builds Equation Matcher.

Default release: `v1.1.0-rc1`
Asset: `Equation-Matcher-v1.1.0-windows.zip`
Required SHA-256: `1E19FAE300E37DA503BFDE2363138562E8FF492D7BFD48D5E00081917AA88324`

Upload that exact existing ZIP to the GitHub release before dispatching. Changing
source or rebuilding does not authorize replacing this candidate under the same hash.
The workflow refuses mismatches before extraction/execution. Use an explicitly
reviewed new hash for any later candidate.

The sparse checkout contains only `.github` definitions and test scripts. It does
not expose production source, Maven dependencies or build outputs to the job.
`PackagedRuntimeVerificationMain` is a test class and is intentionally absent from
the release ZIP. Consequently the workflow compiles one independent helper with
the **bundled javac** against the **extracted app JAR**, then runs it using the
bundled Java. No production code is compiled, and Maven is never invoked.

Checks include runtime/compiler/dependency provenance, Summandial, Factorial,
entered sequences, triangular and square Algorithms, independent verification,
actual worker executable paths, infinite-loop timeout, recovery, and cleanup.
The native EXE is separately launched; its window title and loaded native module
paths establish successful initialization. It must close normally without leaving
packaged processes. The job does not claim interactive visual GUI review.

Java/Maven environment variables and PATH entries are removed for the test process.
A fresh Java user home prevents reuse of the runner's JavaFX caches. Only processes
whose executable is under the uniquely extracted image are inspected/cleaned up.
The Actions summary and uploaded `clean-windows-release-evidence-*` artifact record
the hash, OS/image versions, all test output and native/process paths.

Run manually using GitHub's Actions page, or:

```powershell
gh workflow run release-clean-windows.yml --ref main -f release_tag=v1.1.0-rc1 -f expected_sha256=1E19FAE300E37DA503BFDE2363138562E8FF492D7BFD48D5E00081917AA88324
```

Local harness check (does not qualify as a fresh hosted-VM result):

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .github/scripts/Test-ReleaseWindows.ps1 -ZipPath target/Equation-Matcher-v1.1.0-windows.zip -EvidenceDirectory target/hosted-gate-local-check
```

Consumer SmartScreen/reputation and interactive downloaded-file warnings are not
covered by hosted CI. No signing or protection settings are changed by this test.
