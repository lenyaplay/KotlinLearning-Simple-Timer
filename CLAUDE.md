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
