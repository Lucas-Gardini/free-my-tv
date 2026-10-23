# Upstream customization follow-up plan

> **For agentic workers:** Use superpowers:executing-plans for the integration and superpowers:requesting-code-review before publishing.

**Goal:** Bring upstream through 4d73951 into a new PR based on fork main 72ca1b8, after PR #5 was merged.

**Architecture:** Preserve upstream merge history, combine catalogues by key and migrate fork web customizations to the shared single-key translation API. Keep the existing recovery implementation unchanged.

**Tech stack:** Java 8, Android SDK 34/JDK 17, ES5 JavaScript and Python packaging.

**Spec:** Continue the authorized upstream update/testing workflow, with a separate PR because the previous PR has already merged.

## Constraints and review focus

- Retain local IP after the signature, both customization toggles, the wallpaper lifecycle and red accents.
- Retain write-ahead recovery, strict Wi-Fi checks, Original and watchdog; never switch modes from the computer.
- Keep private configuration, keys and experiments out of git.
- Check the new web catalogue packaging includes fork text and locales beyond the old five.
- Check new top-button navigation, menu geometry and shelf holds coexist with the fork layout.
- Verify quotation fallback fixes do not regress the currency settings.

## Tasks

- [x] Merge upstream and resolve catalogues/web translation conflicts without losing either side's keys.
- [x] Add regression coverage for packaged web customization translations and their UI calls, exercise the failure before fixing, and run all checks.
- [x] Obtain independent review, resolve material findings and validate the signed update on the owner's TV when reachable.
- [ ] Commit and push a new branch, open the follow-up PR and verify GitHub CI.

## Validation record

- `tools/check.sh`: all checks passed. Existing 90 regressions plus 104 new package/translation checks, total 194. All 25 catalogues match 197 source keys; Android, LG and unsigned Samsung builds and ES5 checks pass.
- The new package regression first failed on the Czech IP toggle showing Portuguese. After migrating the fork's web calls, all locales pass, including a runtime without Intl and both toggle callbacks.
- Independent review covered native top-button indices/navigation/accessibility, compact settings geometry, All Apps holds, web catalogue loading, personalization and currency fallback. No actionable findings.
- Signed in-place installation on the owner's TCL Android 12 TV succeeded. Home screenshot preserves IP, red accents, date/weather and shelf; Settings screenshot confirms the seven-row menu including Top Buttons fits.
- Hardware AVC decoder plays the muted 1920x1080 loop, releases in Settings and resumes on Home. No AndroidRuntime errors in the updated app process. Launcher, screensaver, remote accessibility service and disabled packages match the pre-update snapshot.
- HomeMode, HomeActivity and LiveWallpaper are unchanged from fork main. Original/Free switching and new preference edits were not triggered remotely; physical long-press, button configuration and Samsung/LG hardware behavior remain manual checks.
