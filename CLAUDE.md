# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this
repository.

## Overview

Single-module Android app (Kotlin + Jetpack Compose, Material3): one global countdown timer with
hours/minutes/seconds input, pause/resume/stop, and a notification when it finishes. Docs, comments
and user-facing strings are in Russian — when you do write them, write them in Russian.

Gradle module: `app`, namespace/applicationId `com.lenyaplay.simple.timer`, minSdk 24 / target 35,
JVM target 11. Dependency versions live in `gradle/libs.versions.toml` (version catalog) — add
libraries there, not inline in `app/build.gradle.kts`.

## Comments

Do not add comments to the code on your own initiative — neither when writing code nor when proposing
a plan. Write a comment only where the user has explicitly asked for one, and only there. Existing
comments stay as they are unless the code they describe changes.

## Commands

Use `./gradlew` (bash) or `.\gradlew.bat` (PowerShell).

- Build debug APK: `./gradlew assembleDebug`
- Install on a connected device/emulator: `./gradlew installDebug`
- Compile check only: `./gradlew :app:compileDebugKotlin`
- Unit tests (JVM): `./gradlew testDebugUnitTest`
- Single unit test:
  `./gradlew testDebugUnitTest --tests "com.lenyaplay.simple.timer.ExampleUnitTest.addition_isCorrect"`
- Instrumented tests (needs a device): `./gradlew connectedDebugAndroidTest`
- Single instrumented test:
  `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.lenyaplay.simple.timer.ExampleInstrumentedTest`

There is no lint/format step configured beyond the default `./gradlew lint`.

## Tests

UI behaviour — scrolling, gestures, animations, timing — is verified by instrumented tests in
`app/src/androidTest`, not by reasoning about the code. `TimerScreenTest.kt` is the reference.

- **Test at screen level, not the isolated component.** Mount `TimerViewContent` and interact with
  real controls. Several bugs existed only in the interaction: tapping a preset happens with the
  finger *outside* the wheel, which a component-only test never reproduces.
- **Assert what is on screen, not what the component reported.** Checking the value a component
  passed to its own callback is circular — the wheel displayed one number, reported another, and the
  test stayed green. `assertWheelShows` finds the number physically closest to the wheel centre.
- **Account for touch slop**: the system consumes part of a gesture before scrolling starts, so
  short drags fall short of their intended distance. See `touchSlopPx()`.
- **One test per bug, written before the fix.** The test must fail on the broken code. If the fix
  already landed, temporarily reintroduce the breakage and confirm the test goes red — a test that
  has never failed proves nothing.
- Name tests after the behaviour they pin down: `dragPastBarrierCommitsToNextValue`,
  `wheelStillSettlesAfterPresetInterruptedSettle`.

## Tracing

Gesture and timing bugs do not reproduce from a description. Instead of guessing, turn on tracing
and read what actually happened — this is how the value oscillation and the settle handler dying on
`CancellationException` were found, after three wrong hypotheses.

- Switch: `TRACE_ENABLED` in `Trace.kt`, then `./gradlew installDebug`. It is a `const val`, so with
  tracing off the compiler removes the calls entirely — including in release builds.
- Read: `adb logcat -s SimpleTimer:I`. Record a session:
  `adb logcat -c && adb logcat -s SimpleTimer:I > trace.log`.
- Messages go through a lambda (`trace("Барабан") { "..." }`) so nothing is built when tracing is off.
- **Log the silent branches too** — early returns and skips. "I tapped the preset and nothing
  happened" only became visible once the skip reasons were logged.
- Mark anomalies in the log itself (`WARN pull-back`, `WARN misaligned`) so they can be grepped
  instead of spotted by eye.
- Log the numbers a decision was made from (`moved`, `fraction`, `direction`, `step`, `distance`),
  not just the outcome.

Trace calls are permanent diagnostics — leave them in place rather than deleting them once a bug is
fixed.
