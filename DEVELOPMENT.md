# Linkora Development Guide

This document outlines the technical workings and conventions of Linkora. If you need to know about
branch naming conventions, commit message styles, PR workflows, or rules on AI usage, read
`CONTRIBUTING.md`.

## Environment

* **JDK:** JDK 21. Both Android and Desktop targets are explicitly compiled against `JVM_21`.
    * *Note on newer JDKs:* If your system default JDK is newer (e.g., JDK 25), use the JetBrains
      Runtime (JBR) 21 bundled with Android Studio. If you must use the terminal, prefix commands
      with your JBR path: `JAVA_HOME="/path/to/your/android-studio/jbr" ./gradlew <command>`.
* **NDK (Required):** The Android NDK is **mandatory**. Linkora compiles Rust code for Android
  targets (`aarch64-linux-android`, `x86_64-linux-android`, `armv7-linux-androideabi`). You **must**
  install the NDK via Android Studio: `Tools` -> `SDK Manager` -> `SDK Tools` tab -> check
  `NDK (Side by side)` -> Apply. The build scripts are preset to automatically detect it via the
  `$ANDROID_HOME` environment variable. Do not hardcode paths.
* **IDE:** Android Studio.

Your first build will take 5 to 20 minutes depending on your machine and internet speed. Subsequent
builds will be significantly faster.

## Codebase Map

Linkora is a multi-module KMP project.

* `composeApp/`: The main application module, separated by target source sets.
    * `src/commonMain`: The core of the app.
    * `src/androidMain`: Android-specific implementations, services (like `AutoSaveLinkService`),
      and `actual` declarations.
    * `src/desktopMain`: JVM/Desktop specific implementations.
    * `src/wasmJsMain`: Web target implementations.
    * `worker`: JavaScript Web Worker implementation required for the Wasm target functionality.
* `web-capture/`: The core implementation of the web-capture feature. This module contains both Rust
  and Kotlin files that work together to handle the underlying capture logic.

## Build Commands

I use Run Configurations in the IDE to configure all these and run them via `Shift + F9`. If you
want to manually run them via the command line, these should help. Run them from the root of the
project:

* **Android:** `./gradlew assembleDebug` (or just hit run in Android Studio)
* **Desktop:** `./gradlew desktopRun`
* **Desktop (Hot Reload):** `./gradlew hotRunDesktop --mainClass "com.sakethh.linkora.MainKt"`
* **Web (Wasm):** `./gradlew wasmJsBrowserDevelopmentRun`
* **Tests:** `./gradlew verifyAll`

## Git Hooks (Pre-commit & Pre-push)

* **Pre-commit:** Automatically runs formatting (`cargo fmt` for Rust, `spotlessApply` for Kotlin)
  on tracked but uncommitted changed files.
* **Pre-push:** Runs Kotlin and Rust test cases before allowing the push to proceed.

You can bypass these locally using `git commit --no-verify` or `git push --no-verify`. The exact
same checks are enforced on GitHub Actions for every PR and commit. If you skip locally, the remote
CI will fail, and you will have to fix it and push again.

---

## Basics

### Use Kotlin Properly

Keep it simple. Kotlin offers a wide range of features. Do not overcomplicate things just because
the language allows it.

### Reusability

If a code block is exactly repeated or can be extracted into a separate reusable block, it must be
either an extension function, context-based function, or just a regular function. It must not be
copy-pasted. Kotlin supports OOP out of the box, and I expect you to use it, so it won't turn into a
mess.

Use context-based functions only when you want to extend based on multiple types. If it is a single
type extended, then use extension functions.

### Custom UI Components

Before building a new UI element, check
`composeApp/src/commonMain/kotlin/com/sakethh/linkora/ui/components/`. If you need a dialog, button,
or settings row, a pre-built component likely already exists. Do not reinvent the wheel. If a
component does not exist and is required, feel free to add it.

### Inheritance

Use inheritance cautiously. For example, a class like `SettingsScreenViewModel` can be inherited as
long as it doesn't have any side effects in its initialization and the inheritance is within the
context. Usually `UseCases` are used here, and I do plan to refactor certain things into `UseCases`,
but for now, read what's happening before inheriting.

### UseCases

If you are refactoring or using things that may require a "UseCase", feel free to add, since I want
to add a couple of them for a couple of things. Do not write UseCases just because the theory says
so.

### Delegation

When handling Compose states, use delegation. Existing code does not always use delegation when
dealing with compose states since I like explicit `.value` when dealing with states, which also
comes with `.value` laying around more than it should, especially in composables. Moving forward,
delegation on compose states is a must.

I have not yet seen a reason to use delegation on classes, so if that is something I see, then of
course, delegation will be used.

### Comments and Naming

Code should not be filled with comments unless it is necessary, or you are explaining why it is in
the way it is rather than the most obvious way. Write code that is self-explanatory.

Do not trim down variable, functions or any sort of properties names just to make it short. They
must describe exactly what they intend for. Do not use character variables like `i`, `j`, or `k`,
especially in loops. If you use them, I will assume you do not know what you are doing or are using
some random LLM, and I won't even see the rest of the diff. Have
some [faith](https://youtu.be/ZwOBYAkXMYY) and name things properly.

### Tests

Run `./gradlew verifyAll`. This automatically runs `desktopTest` (Kotlin) and `cargo test` (Rust).

Although `verifyAll` executes tests against the desktop target irrespective of source sets, almost
all your Kotlin test cases must be written directly in `desktopTest`. The core reason for this is
the `mockk` library. `mockk` supports Android and Desktop, but since Linkora also targets the Web,
writing mocks for the web target isn't possible from `commonMain`. Running tests via the desktop
target allows for easy mocking across the board without that friction.

Additionally, some components like the `Log` class require a mock on Android, which just adds
unnecessary work. If you are testing a specific platform implementation, write them in the source
sets meant for platform-specific tests and make sure they pass locally. But for almost every other
case, if it does not interact directly with native Android or Web APIs, the test goes in
`desktopTest`.

### Recompositions

Use `retain` when it makes sense, but for almost every case, `rememberSaveable` should do it. In
some cases, use a custom `Saver` if needed.

### Coroutines and Cancellation

When working with concurrent code, cancellation must be handled properly. Do not stick to
theoretical boilerplate found on the internet, and do not attempt "fire-and-forget" operations just
because you can. Linkora's core implementations, including the Rust integration, depend entirely on
cancellation being handled correctly.

---

## Internals

### Database (Room)

Room manages the local SQLite database. It acts as the single source of truth for both local and
remote data.

Linkora ships with a bundled SQLite driver. If your database changes pass the desktop tests, they
will work on Android, and they will work on the web (assuming OPFS is not locking the database, this
is a known limitation, and we can't directly fix this). I expect you to understand how to work with
Room and SQLite itself. There are some cases where raw queries get too long, feel free to use LLMs
here, but you must completely understand what that SQL query does before committing it.

Any changes made to the database schema require a proper Room migration. Do not just bump the
database version.

### Networking (Ktor)

Ktor is used for network requests. When the app is configured with the sync-server, there are two
distinct client instances:

* `standardClient`: Used for all general network calls.
* `syncServerClient`: Used **only** for requests to the sync-server. This client does not restrict
  certain certificate validations, so you absolutely do not want to use it for standard external
  requests.
* **Proxy usage:** When saving links, use the `retrieveFromProxy` function instead of the standard
  client to handle data fetching properly.

### Preferences

Preferences operate differently depending on the platform. Android and Desktop use DataStore, while
the Web target uses the native JS API via `kotlinx.browser`.

To handle this, `commonMain` defines a `PlatformPreference` interface with specific `actual`
implementations on each platform.

`AppPreferences` handles the keys. The number of variables defined in the `AppPreferences`
constructor **must** exactly match the number of keys in its companion object. This is verified
during testing, and the tests will fail if the counts are irregular. Read the platform-specific
implementations to see how it operates under the hood.

### Dependency Injection

Linkora does not use a DI framework like Hilt or Koin. It relies on a custom, manual dependency
container. If you create a new Repository, or anything that needs instances across the codebase, you
must wire it up manually. Read through `DependencyContainer.kt` and `LinkoraSDK.kt` in
`composeApp/src/commonMain/kotlin/com/sakethh/linkora/di` to understand how dependencies are
provided.

All instances must be passed externally via constructor and not hardcoded within a class. As of now,
all instances are initialized lazily as static singletons. There is not yet a case where a new
instance is required for certain cases. If that comes up, this dependency container will be used to
return new instances on calls instead of the static instances.

### Platform-Specific APIs (Expect/Actual)

For OS-level features (File Management, Permissions, Native Utilities), use KMP's `expect` /
`actual` pattern. Check `Expected.kt` in `commonMain` and the respective `actual` implementations in
`androidMain`, `desktopMain`, or `wasmJsMain` before implementing new platform-specific behavior.

### Background Processing (Android)

There are cases where Linkora on Android uses background processing via WorkManager, see
`SnapshotWorker.kt`, `RefreshAllLinksWorker.kt`. These operations may re-run based on Android OS
scheduling, and it doesn't guarantee the operation will be finished when it is triggered. This also
means for a worker like `RefreshAllLinksWorker`, we should not perform any duplicate refreshes just
because the OS nuked the first initialization and successfully completed at the 4th initialization.
All the refreshes need to persist locally as Linkora currently does.

Any implementation that involves WorkManager on Android must have a persistence record in the local
database if they can lead to duplicate operations when it is not required.

### Service (Android)

`AutoSaveLinkService.kt` uses Android services to auto-save links, please go through it for its
implementation.

Note that both Service and WorkManager show notifications, i.e., they do get "promoted" as
foreground services eventually during their execution. So if your implementation can do this and
inform via notifications on current status, feel free to use mechanisms like this.

### Intents (Android)

Linkora handles URL sharing via `ShareToSaveActivity.kt` in `androidMain`. This activity intercepts
system-level share intents to push links into the database (of course the call will reach
`LocalLinksRepoImpl` as it would generally even via the usual way of saving links by opening the
application).

### Localization (i18n)

Linkora uses a custom localization system. If you add new text to the UI, it must be routed through
this system and not hardcoded as plain strings in the Compose files. Do not touch
`locales/default_en.json`; that is for the server, generated manually, and should not be modified.

### Import / Export

Data portability is critical. The app supports HTML (standard Netscape bookmarks) and JSON (Linkora
specific schema) formats. If you are modifying database schemas or adding new data types, you must
update `ExportDataRepoImpl.kt` and `ImportDataRepoImpl.kt` and ensure backward compatibility.
Previous schema versions must not break and must get imported as expected. `schemaVersion` refers to
the version of schema a file is currently based on.

### Fastlane

The `fastlane/` directory tracks metadata and images for F-Droid. You can submit PRs for translation
fixes in store listings or new images. However, release notes are strictly managed by me. Do not
write or modify release notes; I handle those right before a release.

### Architecture

Linkora is strictly local-first software. **Every operation must happen and succeed locally before
it triggers the remote sync-server.**

* Use the `performLocalOperationWithRemoteSyncFlow` function to handle any feature that syncs. It
  ensures the remote operation is only called if the local operation succeeds.
* You should have basic understanding of how to work with Kotlin Coroutines and Flows.
* UI events (Snackbars, Menus, Dialogs, etc.) operate on an event bus.
    * Send them using `pushUIEvent` via `UIEvent` in `commonMain`, or use `androidUIEvent` for
      Android-specific events.
    * These are collected respectively in `AppVM` (common) or `MainVM` (Android-specific).
* Snapshots/auto-backups use Kotlin Flows to subscribe to all relevant tables with a 1-second
  debounce. If you are working around this, please go through `SnapshotRepoImpl.kt` properly.
* Lists that can grow infinitely use a custom `Paginator.kt`. It relies on cursor-based pagination.
    * If you know the concept, this is just a Kotlin implementation using a `Job` that cancels and
      subscribes based on what is visible on the screen.
    * Read the file to see exactly how it is implemented.
* If you go through any `Local****Repo`, you will find functions that have a param `viaSocket`.
    * The sole reason this exists is that these functions are called when an operation is
      initiated by the user and also when an event that is sent via websocket needs the same
      functionality as whatever a function is doing.
    * Since an operation is done via web-socket, it must not be sent back to the sync-server.
      That's also the reason `performLocalOperationWithRemoteSyncFlow` has
      `performRemoteOperation = !viaSocket` in all implementations.
    * This helps to not make a loop of calls from local to remote when receiving something via
      websocket.
* Do not stick to MVVM/MVI/Clean implementations just because you consider it as your favorite
  style to do things, or you have read it is the best way to do it.
    * This codebase isn't based on strict MVVM nor MVI. It is a hybrid based on both
      implementations, so use whatever makes sense practically and keep it simple.
    * If you think MVVM makes sense for whatever you are implementing, use it. If you see that a
      state and action-driven implementation makes sense, use MVI. If you think a hybrid model of
      these two makes sense, use it.
* For reusable composables, do not attach them to any specific viewmodel; hoist them via
  `performAction(SomeAction) -> <WhatEver>` and the actual implementation must be done on the
  caller side.

The above information should give you a clear idea on how things work, if you think anything is
missing here, feel free to mail me at sakethh@proton.me or DM on Discord (@sakethpathike).