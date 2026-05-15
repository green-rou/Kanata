# Contributing to Kanata

Thanks for taking the time to contribute! Here's everything you need to know.

---

## Table of Contents

- [Reporting Bugs](#reporting-bugs)
- [Suggesting Features](#suggesting-features)
- [Development Setup](#development-setup)
- [Submitting a Pull Request](#submitting-a-pull-request)
- [Code Style](#code-style)
- [Branch & Commit Conventions](#branch--commit-conventions)

---

## Reporting Bugs

Before opening an issue, please:

1. Search [existing issues](../../issues) to avoid duplicates.
2. Reproduce the bug on the latest commit from `main`.

When filing a bug report, include:

- **Device / emulator** and Android version
- **Steps to reproduce** — be as specific as possible
- **Expected vs actual behaviour**
- **Logcat output** (filter by `ExternalSearch`, `EpisodeList`, `Player`, or the relevant tag)

---

## Suggesting Features

Open a [feature request issue](../../issues/new) and describe:

- The problem you're trying to solve
- Your proposed solution
- Any alternatives you considered

---

## Development Setup

1. **Fork** the repo and clone your fork.
2. Open in **Android Studio Hedgehog** or newer.
3. Let Gradle sync — no additional setup required (no API keys needed).
4. Run on a device or emulator with **API 28+**.

> The project uses a version catalog (`gradle/libs.versions.toml`).  
> Add new dependencies there, not directly in `build.gradle.kts`.

---

## Submitting a Pull Request

1. Create a branch from `main` (see [conventions](#branch--commit-conventions) below).
2. Make your changes — keep each PR focused on a single concern.
3. Ensure the project builds cleanly (`./gradlew assembleDebug`).
4. Open a PR against `main` with a clear description of what changed and why.

---

## Code Style

- **Kotlin** — follow the [official Kotlin coding conventions](https://kotlinlint.io/0.50.0/rules/standard/).
- **Compose** — one composable per file when it's a screen; small helper composables can be co-located.
- **Architecture** — follow the existing MVVM + Clean Architecture + UDF pattern:
  - User actions → `handleEvent()` in the ViewModel
  - One-shot navigation/UI effects → `Channel<Event>` (not `StateFlow`)
  - No business logic in Composables
- **Comments** — only when the *why* is non-obvious. No block comments, no docstrings on obvious functions.
- **No unused imports** — keep files clean.

---

## Branch & Commit Conventions

**Branches:**

```
feat/short-description      # new feature
fix/short-description       # bug fix
refactor/short-description  # refactoring without behaviour change
chore/short-description     # deps, config, CI
```

**Commits** — use the [Conventional Commits](https://www.conventionalcommits.org/) format:

```
feat: add episode download support
fix: prevent duplicate ViewModel load on back navigation
refactor: simplify VideoRepositoryImpl HLS chain
chore: bump Media3 to 1.6.0
```

Keep the subject line under **72 characters** and in the **imperative mood** ("add", not "added").

---

## Questions?

Open a [discussion](../../discussions) or file an issue — happy to help.
