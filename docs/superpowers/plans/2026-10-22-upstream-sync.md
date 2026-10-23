# Upstream 1.1 integration plan

> **For agentic workers:** Use superpowers:executing-plans to implement this plan task by task.

**Goal:** Integrate upstream through e11c608 into a separate PR and validate the signed update on the owner's TCL TV.

**Architecture:** Merge upstream history into a branch based on fork main (6e04c17). Combine upstream safety and performance changes with the fork's durable recovery journal, configurable IP display, silent live wallpaper and red palette.

**Tech Stack:** Java 8, Android SDK 34/JDK 17, ES5 JavaScript, Python packaging, ADB.

**Spec:** User request in this session: separate update PR, tested on their own TV at 192.168.127.16.

## Constraints

- Preserve Original, watchdog, write-ahead recovery and strict Wi-Fi ownership checks.
- Never uninstall/clear system packages or change network, picture, sound or accounts.
- Mode switches are performed by the owner on the TV, never by the computer.
- Keep config.properties and signing keys private; retain the existing signing key.
- Keep experiments out of the PR; preserve Android/web personalization.

## Review focus

- Setup must be disabled before stock home, and enabled only after home is restored.
- Unknown package/Wi-Fi inspection must retain safety components.
- Recovery after state loss must preserve user-disabled packages and accessibility.
- Translation migration must retain both customization toggles and the unavailable-IP text.
- Home resume/cache changes must retain video release/restart and IP refresh behavior.

## Tasks

- [x] Establish baseline: run the five existing suites (72 checks), connect to TV, retain rollback APK and read-only state evidence.
- [x] Merge upstream; resolve README, HomeMode, HomeView, SettingsActivity, deleted Java catalogs and web CSS. Migrate customization strings to JSON catalogs.
- [x] Add meaningful regressions for setup/home ordering and dynamic package guards; retain the existing recovery cases. Verify failures before fixes where behavior is newly covered.
- [x] Build Android, run all suites plus upstream catalog/ES5/package checks. Adapt CI to cover fork regressions.
- [x] Install the signed APK over the current app, open home and inspect runtime errors, video decoding, IP, preferences and lifecycle. Owner performs any mode-switch/visual confirmation.
- [ ] Obtain independent code review; resolve material findings, commit, push this branch and open a separate PR with exact evidence and remaining manual checks.

## Execution evidence

- Baseline: 72 checks passed. Final: 90 passed (43 recovery, 11 local IP, 11 native wallpaper, 14 web IP, 11 web wallpaper). All 25 catalogues match 188 source keys; Android, webOS and unsigned Tizen packages build; ES5 checks pass.
- Review: five material safety findings fixed. Six new failing regressions reproduced the findings before the fixes; the durable-journal retry also failed before its fix.
- Package diagnostics now require a successful command, the package's own header, user-state footer and one complete result per candidate.
- Setup HOME capability survives repeated switches and state loss; actual enabled state controls the setup/home guard. Restoring setup requires positive stock-home recovery evidence. The Kalley model guard also applies during boot/update verification.
- TV: signed in-place update succeeded on TCL Android 12. Home/Original-package state, screensaver and remote accessibility service match the baseline. Screenshot confirms signature/IP, Portuguese date/weather, red accents and shelf. Hardware AVC decoder plays 1920x1080 in a muted loop, releases in Settings and resumes on Home. No AndroidRuntime error in the updated app process.
- Manual limitations: physical smoothness confirmation and a fresh Original/Free round trip await owner interaction. No mode switch or reboot was triggered from the computer.
