# lintDebug Fix Report

This report explains how to fix every finding category reported by `lint-results-debug.txt` from `./gradlew :app:lintDebug`.

## Current lint status

| Severity | Count |
|---|---:|
| Errors | 75 |
| Warnings | 188 |
| Hints | 5 |

The build is blocked by 75 errors. Fix those first; the warnings can then be handled in focused cleanup passes.

## Recommended fix order

1. Fix broken localized resources, especially `values-fr/strings.xml` and the `values-id` folder.
2. Fix Compose resource reads from `LocalContext.current`.
3. Remove the restricted `dispatchKeyEvent` override in `MainActivity`.
4. Re-run `./gradlew :app:lintDebug` and then handle warning groups by priority.
5. Run `./gradlew :app:assembleDebug` and the relevant unit tests after code changes.

## Blocking errors

### 1. `StringFormatInvalid`, `StringFormatMatches`, and `StringFormatCount`

| Count | Main files |
|---:|---|
| 9 `StringFormatInvalid` | `app/src/main/res/values-fr/strings.xml`, call sites in `ChatScreen.kt` and `OpenCodeConnectionService.kt` |
| 10 `StringFormatMatches` | `app/src/main/res/values-fr/strings.xml`, `OpenCodeConnectionService.kt` |
| 46 `StringFormatCount` warnings that explain the same root problem | `app/src/main/res/values-fr/strings.xml` |

Root cause: the French resource file contains translations whose placeholders do not match `app/src/main/res/values/strings.xml`. Some strings have missing arguments, extra arguments, or the wrong placeholder type (`%s` vs `%d`). This is not just cosmetic: it can crash at runtime when Android formats the string.

Fix strategy:

1. Treat `values/strings.xml` as the source of truth.
2. For every key reported in `values-fr/strings.xml`, make the French value preserve the same placeholder count, index, and type as the English value.
3. Do not change Kotlin call sites until the resource contract is correct. Most call-site errors disappear once the translation placeholders match.
4. For strings that intentionally contain a literal percent sign, escape it as `%%` or mark the string with `formatted="false"` only if it is never formatted.
5. After fixing `values-fr/strings.xml`, check all translated `values-*` folders for the same placeholder contract.

Examples from the lint output:

| Key | Source contract | French problem | Fix |
|---|---|---|---|
| `notification_connected` | `Connected to %1$s` | French has no argument | Keep one `%1$s` argument in French. |
| `notification_connected_count` | `%1$d` and `%2$d` | French uses one `%1$s` | Use two integer placeholders. |
| `notification_has_question` | one `%1$s` in source | French has two args | Match source or update source + all call sites consistently. |
| `chat_images_optimized_summary` | five args | French has no args | Add all five placeholders in the translated text. |

Validation:

```bash
./gradlew :app:lintDebug
```

### 2. `MissingTranslation`

| Count | File |
|---:|---|
| 2 | `app/src/main/res/values/strings.xml` |

Missing keys:

- `project_name_title`
- `project_directory_title`

Fix strategy:

1. Add these keys to every translated resource folder: `values-de`, `values-ru`, `values-ko`, `values-pt`, `values-it`, `values-fr`, `values-zh`, `values-ar`, `values-uk`, `values-ja`, `values-in`/`values-id`, `values-pl`, and `values-tr`.
2. If the strings should not be translated, mark them as `translatable="false"`; however, these are visible UI labels, so translating them is the better fix.
3. Because this repo uses `lokit.yaml`, keep translated XML files and `lokit.lock` in sync when changing localization files.

### 3. `LocalContextGetResourceValueCall`

| Count | Files |
|---:|---|
| 51 | `ChatScreen.kt`, `ServerProvidersScreen.kt`, `SessionListScreen.kt` |

Root cause: Compose code calls `context.getString(...)` using `LocalContext.current`. Configuration changes do not invalidate those reads, so strings can become stale after language, locale, font, or configuration changes.

Fix strategy:

1. Replace direct `context.getString(...)` reads in composable code with Compose-aware resource access.
2. Use `stringResource(...)` when the string can be resolved directly in composition.
3. Use `LocalResources.current` when a string must be resolved inside a callback, suspend block, or lambda where `stringResource` cannot be called.
4. Resolve strings before passing them into snackbar/event lambdas when possible.

Preferred patterns:

```kotlin
val terminalConnectFailed = stringResource(R.string.chat_terminal_connect_failed)

LaunchedEffect(errorEvent) {
    snackbarHostState.showSnackbar(terminalConnectFailed)
}
```

For dynamic callback values:

```kotlin
val resources = LocalResources.current

val message = resources.getString(R.string.chat_command_executed, commandName)
```

Be careful: `stringResource(...)` is composable and cannot be called from inside a suspend lambda unless that lambda itself is in composition.

### 4. `RestrictedApi`

| Count | File |
|---:|---|
| 3 | `app/src/main/kotlin/dev/mackenzie/coderemote/MainActivity.kt` |

Root cause: `MainActivity` overrides and calls `ComponentActivity.dispatchKeyEvent`, which lint now treats as a restricted AndroidX API.

Current code:

```kotlin
override fun dispatchKeyEvent(event: KeyEvent): Boolean {
    if (terminalKeyInterceptor?.invoke(event) == true) {
        return true
    }
    return super.dispatchKeyEvent(event)
}
```

Fix strategy:

1. Prefer moving terminal key handling into the terminal Compose surface with `Modifier.onPreviewKeyEvent` or `Modifier.onKeyEvent`.
2. If Activity-level handling is still required, replace the restricted override with non-restricted callbacks such as `onKeyDown(...)` and `onKeyUp(...)` and pass those events to `terminalKeyInterceptor`.
3. Remove the `dispatchKeyEvent` override entirely after the terminal path is migrated.

Do not suppress this unless there is no viable alternative. It is a real API-boundary lint error after the dependency/tooling upgrade.

## High-priority warnings

### 5. `LocaleFolder`

| Count | File/folder |
|---:|---|
| 1 | `app/src/main/res/values-id` |

Fix: rename `values-id` to `values-in`, because Android/Java use the legacy Indonesian locale code `in` for resource folders.

Also update `lokit.yaml` and `lokit.lock` if they reference `id` explicitly.

### 6. `DefaultLocale`

| Count | Files |
|---:|---|
| 7 | `ChatAttachmentUtils.kt`, `ChatPromptUtils.kt`, `ChatTopAppBar.kt`, `ChatViewModel.kt`, `MessageAuxiliaryCards.kt` |

Fix: replace implicit `String.format(...)` with an explicit locale.

Use `Locale.getDefault()` for user-facing numeric formatting, or `Locale.ROOT` for stable machine-like formatting.

Example:

```kotlin
String.format(Locale.getDefault(), "%.2f MB", value / (1024.0 * 1024.0))
```

### 7. `AppBundleLocaleChanges`

| Count | File |
|---:|---|
| 1 | `MainActivity.kt` |

Root cause: the app changes locales at runtime, but an Android App Bundle can split language resources unless configured otherwise.

Fix options:

1. If the app has an in-app language switcher, disable language splits in `app/build.gradle.kts`:

```kotlin
android {
    bundle {
        language {
            enableSplit = false
        }
    }
}
```

2. If Play delivery with language splits is required, integrate Play Core language downloads instead.

For this project, disabling language splits is likely the simplest and safest path.

### 8. `BatteryLife`

| Count | File |
|---:|---|
| 1 | `HomeScreen.kt` |

Root cause: `Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` is sensitive under Play Store policy.

Fix strategy:

1. Only show this action as an explicit user-driven troubleshooting option.
2. Explain why the app needs it before opening settings.
3. Prefer foreground-service reliability for long-running connections.
4. If the app is Play-distributed, verify the use case against the current Play policy before release.
5. If the app is not Play-distributed and the behavior is intentional, add a narrow suppression with a comment explaining the local-server/Termux use case.

### 9. `WakelockTimeout`

| Count | File |
|---:|---|
| 1 | `OpenCodeConnectionService.kt` |

Fix: use a timeout when acquiring the wakelock, and always release it in `finally` or lifecycle cleanup.

Example:

```kotlin
wakeLock.acquire(10 * 60 * 1000L)
```

If the operation can run longer than the timeout, prefer renewing intentionally from foreground-service lifecycle instead of acquiring an unbounded wakelock.

### 10. `SdCardPath`

| Count | File |
|---:|---|
| 1 | `LocalServerManager.kt` |

Root cause: `TERMUX_HOME` hardcodes `/data/data/com.termux/files/home`.

Fix options:

1. Best: resolve the Termux package context and derive the path when possible.
2. If Android package isolation prevents a reliable dynamic path, keep the constant but add a narrow lint suppression with a comment explaining that this path targets Termux's documented app-private home directory, not this app's own files directory.
3. Do not replace it with this app's `Context.getFilesDir()`; that would point to Code Remote's private storage, not Termux home.

## Compose and Kotlin cleanup warnings

### 11. `ConfigurationScreenWidthHeight`

| Count | Files |
|---:|---|
| 3 | `ChatInputBar.kt`, `ServerDialog.kt` |

Fix: replace `LocalConfiguration.current.screenHeightDp` sizing with `LocalWindowInfo.current.containerSize`, converting pixels to dp through `LocalDensity`.

### 12. `ModifierFactoryExtensionFunction`

| Count | File |
|---:|---|
| 1 | `SettingsScreen.kt` |

Fix: make the modifier factory an extension on `Modifier`.

```kotlin
private fun Modifier.amoledDialogModifier(): Modifier = this.then(...)
```

### 13. `ModifierParameter`

| Count | File |
|---:|---|
| 1 | `TerminalComponents.kt` |

Fix: reorder the composable parameters so `modifier: Modifier = Modifier` is the first optional parameter.

### 14. `FrequentlyChangingValue`

| Count | File |
|---:|---|
| 2 | `TerminalComponents.kt` |

Fix: avoid reading `terminalScrollState.value` directly in composition when it changes frequently.

Use one of these patterns:

- `derivedStateOf` when UI only needs recomposition after a threshold changes.
- `snapshotFlow` inside `LaunchedEffect` when the value drives side effects.
- Layout/draw phase reads when the value only affects placement or drawing.

### 15. `AutoboxingStateCreation`

| Count | Files |
|---:|---|
| 5 | `ChatInputBar.kt`, `ChatScreen.kt`, `ImagePreview.kt`, `TerminalComponents.kt` |

Fix: replace primitive `mutableStateOf(...)` with primitive state helpers.

Examples:

```kotlin
var previewIndex by remember { mutableIntStateOf(-1) }
var lastSentTime by remember { mutableLongStateOf(0L) }
```

### 16. `UseKtx`

| Count | Files |
|---:|---|
| 7 | `AboutScreen.kt`, `ChatAttachmentUtils.kt`, `ChatScreen.kt`, `HomeScreen.kt` |

Fix: use available Android KTX extensions.

Examples:

```kotlin
import androidx.core.net.toUri
import androidx.core.graphics.scale

githubUrl.toUri()
bitmap.scale(outWidth, outHeight)
```

### 17. `ClickableViewAccessibility`

| Count | File |
|---:|---|
| 1 | `ErrorPayloadContent.kt` |

Fix: when `setOnTouchListener` detects a click, call `v.performClick()`. If the touch behavior is complex, use a custom `View` that overrides `performClick()`.

## Resource and localization cleanup warnings

### 18. `PluralsCandidate`

| Count | File |
|---:|---|
| 15 | `app/src/main/res/values/strings.xml` |

Fix: convert count-dependent strings into `<plurals>` resources and call them with `pluralStringResource(...)` in Compose or `resources.getQuantityString(...)` outside composition.

Good candidates include selected counts, session delete counts, edit/command/step counts, file counts, token counts, image counts, and notification connected counts.

### 19. `UnusedResources`

| Count | File |
|---:|---|
| 55 | `app/src/main/res/values/strings.xml` |

Fix strategy:

1. Search for every reported key before deleting; some strings may be referenced indirectly, by previews, tests, future features, or notification code.
2. Remove genuinely unused source strings from `values/strings.xml`.
3. Remove matching keys from all translated `values-*` XML files.
4. If a resource is intentionally retained for dynamic access, document it with `tools:keep` or a narrow lint suppression.

Do not blindly delete translated keys only. Locale-only stale keys have already caused release lint problems in this project.

### 20. `TypographyEllipsis`

| Count | File |
|---:|---|
| 1 | `app/src/main/res/values-ar/strings.xml` |

Fix: replace `...` with the ellipsis character `…`.

### 21. `IconDipSize`

| Count | Files |
|---:|---|
| 2 | `mipmap-hdpi/ic_launcher.png`, `mipmap-hdpi/ic_launcher_round.png` |

Root cause: the HDPI launcher icons are `49x49 px`, which is about `33 dp`; the other densities are `48 dp`.

Fix: regenerate the HDPI launcher icons at the correct `48 dp` size for HDPI, normally `72x72 px`, and keep the round icon consistent.

### 22. `VectorPath`

| Count | Files |
|---:|---|
| 11 | Provider drawable XML icons |

Fix options:

1. Reduce vector precision and remove minor details from the path data.
2. Re-export simpler vector assets.
3. Rasterize particularly complex provider icons into PNG/WebP assets if they are static.

This is performance-related, not currently build-blocking.

## Build and dependency warnings

### 23. `OldTargetApi`

| Count | File |
|---:|---|
| 1 | `app/build.gradle.kts` |

Current state: `compileSdk = 37`, `targetSdk = 34`.

Fix options:

1. If runtime behavior should remain conservative, keep `targetSdk = 34` and add a documented lint suppression explaining the intentional compatibility choice.
2. If release readiness is the goal, plan a separate target SDK migration and test Android 35, 36, and 37 behavior changes before updating `targetSdk`.

Do not blindly bump `targetSdk` just to silence lint. That changes platform behavior.

### 24. `AndroidGradlePluginVersion`, `GradleDependency`, and `NewerVersionAvailable`

| Count | Main file |
|---:|---|
| 1 `AndroidGradlePluginVersion` | `gradle/wrapper/gradle-wrapper.properties` |
| 9 `GradleDependency` | `app/build.gradle.kts` |
| 16 `NewerVersionAvailable` | `app/build.gradle.kts` |

Fix strategy:

1. Upgrade dependencies in small, testable groups.
2. Start with patch/minor AndroidX updates that are low risk.
3. Treat major migrations separately, especially Ktor `2.3.11 -> 3.5.1`, Kotlinx Serialization/Coroutines `1.7/1.8 -> 1.11`, and the markdown renderer `0.28.0 -> 0.43.0`.
4. Keep the Compose BOM aligned with the Kotlin compiler and Android Gradle Plugin.
5. Run `./gradlew :app:assembleDebug`, `./gradlew :app:testDebugUnitTest`, and `./gradlew :app:lintDebug` after each dependency batch.

If dependency updates are intentionally deferred, add a documented lint baseline or narrow suppressions instead of mixing large migrations into the lint cleanup PR.

### 25. `ObsoleteSdkInt`

| Count | Files |
|---:|---|
| 4 | `ChatViewModel.kt`, `HomeViewModel.kt`, `LocalServerManager.kt`, `OpenCodeConnectionService.kt` |

Root cause: `minSdk = 26`, so checks for `Build.VERSION_CODES.O` are unnecessary.

Fix: remove the obsolete `SDK_INT >= O` branches and keep only the API 26+ path.

## Lower-priority intentional-review items

### 26. `BatteryLife`, `SdCardPath`, and target SDK warnings may need policy decisions

Some findings are not pure mechanical fixes. They represent product or distribution choices:

- Whether the app is Play Store distributed.
- Whether Termux integration intentionally depends on Termux private paths.
- Whether `targetSdk = 34` is a deliberate compatibility boundary after moving `compileSdk` to 37.

For these, write the decision down before suppressing. Lint suppressions without rationale are technical debt with a bow on top.

## Final verification checklist

- [ ] Fix `values-fr/strings.xml` placeholder mismatches.
- [ ] Rename `values-id` to `values-in` and update localization tooling files if needed.
- [ ] Add missing translations for `project_name_title` and `project_directory_title`.
- [ ] Replace `context.getString(...)` resource reads in Compose code.
- [ ] Remove the restricted `dispatchKeyEvent` override from `MainActivity`.
- [ ] Re-run `./gradlew :app:lintDebug` and confirm zero errors.
- [ ] Address or explicitly document high-priority warnings.
- [ ] Run `./gradlew :app:assembleDebug`.
- [ ] Run `./gradlew :app:testDebugUnitTest` for regression coverage.
