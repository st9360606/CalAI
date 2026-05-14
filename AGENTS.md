# AGENTS.md — BiteCal Android Client

This file gives coding agents project-specific instructions for the BiteCal Android app.
It applies to the whole Android client repository unless a deeper `AGENTS.md` overrides it.

> Generated after reviewing `app_v1.88.zip`. The uploaded archive starts at the Android `main/` source tree. In a full Gradle project, treat these paths as `app/src/main/...`.

---

## 1. Project Identity

- **Product:** BiteCal / Calai — calorie tracking Android app.
- **Package:** `com.calai.bitecal`.
- **Primary UI:** Jetpack Compose.
- **App architecture:** MVVM + Clean Architecture style + Unidirectional Data Flow.
- **Dependency injection:** Hilt.
- **Networking:** Retrofit + OkHttp + Kotlinx Serialization.
- **Local state:** DataStore is currently used for auth tokens, profile/user settings, water/workout snapshots, locale, and session-related flags. Do not introduce Room unless the requested feature truly needs relational local persistence.
- **Background work:** WorkManager with Hilt worker factory.
- **Platform integrations:** Camera, Health Connect, Google Play Billing, Google Sign-In / Credentials, notifications, localization.
- **Main app entry:** `BiteCal.kt`, `BiteCalApp.kt`, `MainActivity.kt`.
- **Navigation:** `ui/nav/BiteCalNavHost.kt` and route constants in `Routes`.

---

## 2. Repository Layout

Use the full project path when available. In the uploaded zip, the source tree is rooted at `main/`; in a normal Android project, it is usually `app/src/main/`.

```text
main/
  AndroidManifest.xml
  java/com/calai/bitecal/
    core/                # pure/shared app logic, device/session/health calculations
    data/                # APIs, repositories, stores, syncers, billing, auth, network
    di/                  # Hilt modules and entry points
    i18n/                # language and locale management
    ui/                  # Compose screens, components, ViewModels, navigation, theme
  res/
    values/strings.xml
    values-zh-rTW/strings.xml
    drawable/, mipmap/, raw/, xml/
```

Important modules found in this codebase:

- `data/auth` — token store, auth API, Google/email auth, interceptors/authenticator.
- `data/billing` — `BillingGateway`, `PlayBillingGateway`, `FakeBillingGateway`, `BiteCalBillingProducts`.
- `data/entitlement` and `data/membership` — entitlement sync and membership summary APIs.
- `data/foodlog` — food log upload, overrides, portion multiplier, list/detail APIs.
- `data/activity` and `data/health` — Health Connect and daily activity sync.
- `data/referral` — referral APIs and repository.
- `ui/onboarding` — onboarding funnel screens and ViewModels.
- `ui/subscription` — onboarding/subscription paywall UI and `SubscriptionViewModel`.
- `ui/home` — home dashboard, scan FAB, food log detail, progress, saved food, settings, referral, premium reward, water, weight, workout.

---

## 3. Non-Negotiable Agent Rules

### Git and destructive operations

- Do **not** run `git commit`, `git push`, `git tag`, `git rebase`, `git reset --hard`, force-push, or rewrite history unless the user explicitly asks for that exact Git action.
- Do **not** tell the user to commit or push generated code as part of the default workflow. The default deliverable is code/files only; Git commit/push steps are allowed only after the user explicitly requests them.
- Do **not** stage files with `git add` unless the user explicitly asks for Git staging/commit help.
- Do **not** delete user files, generated release artifacts, keystores, or local config unless explicitly requested.
- Do **not** modify `.git/`, `.idea/`, `*.iml`, `build/`, `.gradle/`, release output folders, keystore files, or local machine-specific files unless the task is explicitly about those files.

### Android Studio green-code standard

All code produced by an agent must be suitable for direct paste into Android Studio / IntelliJ and should leave the edited files **green**. Treat this as a hard quality bar, not a best-effort suggestion.

Required before returning code:

- No unresolved references.
- No syntax errors.
- No missing imports.
- No unused imports.
- No unused variables, unused parameters, or dead local code introduced by the change.
- No deprecated APIs unless there is a documented, localized reason and no safe project-compatible alternative.
- No broad `@Suppress`, `@SuppressLint`, or opt-in annotations used to hide avoidable warnings.
- No type mismatch, nullability mismatch, coroutine-context error, or Compose modifier-scope error.
- No warning intentionally left for the user to clean up later.
- New string resources must be referenced correctly and added to both English and Traditional Chinese resource files.
- Function signatures and call sites must be updated together so Android Studio does not show red or yellow diagnostics after paste.

When the full project is available, run or recommend the closest available Gradle checks. If commands cannot be run, say exactly why and identify the files/methods the user should compile-check first.

### Build quality

- Prefer minimal, focused changes. Do not refactor unrelated screens while fixing a narrow issue.
- Before editing, inspect the current file and surrounding call sites. Do not assume route names, DTO fields, product IDs, or string resource names.
- If a requested change requires backend contract changes, call it out clearly and add Android changes only behind safe fallback behavior.
- When giving replacement code, include all required imports or clearly state that no import changes are needed.

### Security and privacy

- Never hardcode API keys, secrets, JWTs, purchase tokens, refresh tokens, OAuth client secrets, or backend internal tokens.
- Never log sensitive data: `Authorization`, cookies, purchase tokens, raw health data, profile PII, account identifiers, images, referral abuse signals, or full server error payloads that may contain PII.
- The existing OkHttp logging redacts `Authorization` and `Cookie`; preserve this behavior.
- Treat food logs, photos, Health Connect data, body metrics, calorie/macros, subscription status, and referral records as sensitive user data.
- Do not add analytics/tracking SDKs or new data collection without an explicit privacy review.

---

## 4. Build, Test, and Verification Commands

The uploaded archive does not include Gradle wrapper/build files, so verify the exact variants in the full repository before running commands.
When the full project is available, prefer these checks:

```bash
# Windows PowerShell
.\gradlew.bat :app:assembleDevDebug
.\gradlew.bat :app:testDevDebugUnitTest
.\gradlew.bat :app:lintDevDebug

# macOS/Linux
./gradlew :app:assembleDevDebug
./gradlew :app:testDevDebugUnitTest
./gradlew :app:lintDevDebug
```

If variants differ, inspect `app/build.gradle.kts` and run the closest equivalent, such as:

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

When changing billing, camera, Health Connect, WorkManager, notification permission, or navigation behavior, also do a manual smoke test on device/emulator.

Always report:

- commands run,
- whether they passed,
- commands not run and why.

---

## 5. Architecture Rules

### Layering

Keep layers separated:

- **Composable UI:** render state and emit events only.
- **ViewModel:** owns UI state, validation, event handling, navigation events, and coroutine orchestration.
- **Repository:** talks to APIs, DataStore, platform services, or local stores. It should not know Compose.
- **API DTO:** mirrors backend JSON contract exactly.
- **UI model:** screen-specific, localized/presentation-ready where appropriate.
- **Core/domain logic:** pure Kotlin when possible; easy to unit test.

Do not call Retrofit APIs directly from Composables.
Do not place business logic inside large Composable branches.
Do not mix DTOs directly into UI when a UI model is needed for formatting, fallback text, status display, or localization.

### State management

- Prefer `StateFlow` / immutable UI state from ViewModels.
- Use `collectAsStateWithLifecycle()` where lifecycle-aware collection is available.
- Use one-off events for navigation/toasts/dialog triggers; avoid encoding transient events as permanent state unless deliberately consumed.
- Preserve Unidirectional Data Flow: UI event → ViewModel intent → repository/use-case → state update → UI render.

### Coroutines

- Use `viewModelScope` in ViewModels.
- Keep blocking IO off the main thread.
- Handle cancellation correctly; do not swallow `CancellationException` as a generic error.
- Prefer explicit error mapping over broad `catch (Throwable)`.

### Dependency injection

- Register app-wide dependencies in `di/` Hilt modules.
- Use constructor injection for ViewModels/repositories/services when possible.
- Do not create long-lived Retrofit, OkHttp, BillingClient, DataStore, or repository instances manually inside Composables.

---

## 6. Compose UI Rules

Every meaningful screen should account for:

- Loading
- Empty
- Error
- Success
- Offline / network failure when relevant
- Retry action when the error is recoverable

Compose-specific guardrails:

- Keep Composables small enough to review; split reusable UI into components under the same feature package.
- Avoid unnecessary recomposition: use `remember`, `derivedStateOf`, stable parameters, and immutable UI state where appropriate.
- For frequently changing UI-derived values, such as scroll progress, selected index, chart drag/press position, or visibility thresholds, use `derivedStateOf` with stable inputs to avoid unnecessary recomposition.
- Avoid expensive calculations directly inside Composable functions. Pre-compute in the ViewModel/use-case when the result is business or presentation state; use `remember(...)` / `derivedStateOf { ... }` only for cheap UI-derived values tied to Compose state.
- Do not filter/sort/map large lists directly during every recomposition. Move the transformation to ViewModel state, a repository/use-case, or a memoized derived state with stable keys.
- Avoid launching side effects directly during composition. Use `LaunchedEffect`, `DisposableEffect`, or ViewModel events intentionally.
- Do not use hardcoded user-visible strings in UI. Add strings to both:
    - `main/res/values/strings.xml`
    - `main/res/values-zh-rTW/strings.xml`
- Keep string keys stable and descriptive. Do not delete existing string keys unless all usages are removed.
- Use locale-aware formatting for date, time, numbers, calories, grams, ml/oz, kg/lb, and currency.
- Respect `android:supportsRtl="true"`; do not hardcode layouts that break RTL without reason.
- Do not use `contentDescription = null` for meaningful icons/buttons.
- Critical buttons, icon buttons, chips, and tappable rows must keep a minimum touch target of **48dp x 48dp** unless there is a deliberate, documented exception.
- Validate important screens in both Light Mode and Dark Mode. Do not hardcode colors that become unreadable or low-contrast in Dark Mode; prefer theme tokens where possible.
- Interactive elements should have reasonable touch targets and visual feedback.
- For critical action buttons such as Save, Done, Purchase, Restore Purchase, Delete, Submit, and Continue, implement click-debouncing in the ViewModel or event reducer, and disable the UI action while the operation is in-flight when appropriate, so rapid repeated taps cannot trigger duplicate API calls, duplicate navigation events, duplicate food-log saves, duplicate purchase launches, or repeated destructive operations.

### Lifecycle, effects, and memory safety

- Any `DisposableEffect` that registers an external listener, callback, receiver, observer, lifecycle observer, billing listener, sensor/Health Connect callback, or broadcast receiver must remove/unregister it in `onDispose`.
- Any manual coroutine/Flow collection tied to a lifecycle owner must be cancelled or scoped with lifecycle-aware APIs. Prefer `collectAsStateWithLifecycle()` in Compose and `repeatOnLifecycle` where appropriate.
- Do not capture `Activity`, `Context`, `NavController`, `View`, or large objects in long-lived singletons/callbacks. Use `applicationContext` for app-scoped dependencies and keep UI references short-lived.

### Activity result / permission launchers

This project has known Compose route/preview/container cases where `LocalActivityResultRegistryOwner.current` may be `null`.
When adding or modifying `rememberLauncherForActivityResult`:

- Check `LocalActivityResultRegistryOwner.current` before creating the launcher.
- If no registry owner exists, do not crash; provide a fallback such as opening the app/system settings page or showing a safe message.
- Preserve existing proxy/fallback patterns used by camera, notification, Health Connect, and weight image flows.

---

## 7. Navigation Rules

The central route list is in `ui/nav/BiteCalNavHost.kt` under `object Routes`.
Do not introduce route strings in random files.

State restoration rules:

- For long forms, onboarding input screens, food-log editing, referral-code entry, and other important edit pages, preserve draft UI state across configuration changes and process/activity recreation.
- Prefer `rememberSaveable` for small UI-local fields that are safe to restore, such as text input, selected tab, temporary toggle state, or scroll-related UI state.
- Use ViewModel state, repository-backed draft state, or DataStore only when the draft must survive process death, app relaunch, or navigation away from the screen.
- Do not persist sensitive data, raw images, purchase tokens, or unnecessary health/profile details just to restore UI state.
- When using Navigation Compose, preserve state intentionally with `saveState` / `restoreState` only when the route semantics require it. Do not accidentally restore stale paywall, purchase, delete, or one-off success events.

Important route semantics:

- `ONBOARD_SUBSCRIPTION` is for the onboarding funnel after onboarding/sign-in.
- `HOME_SCAN_SUBSCRIPTION` is for Home ScanFab paywall gating.
- `SETTINGS_SCAN_SUBSCRIPTION` is for Settings ScanFab paywall gating.
- These paywall routes may share UI, but their close/back/success behavior is intentionally different.
- `SUBSCRIPTION`-style post-onboarding gating must not be blindly merged with onboarding subscription routes.
- Camera access should be gated by membership status; `TRIAL` and `PREMIUM` may proceed, `FREE` / expired / revoked users should be redirected to the correct paywall route.

When changing navigation:

- Verify back stack behavior explicitly.
- Avoid `popUpTo(0)` unless the task truly requires clearing the whole stack.
- Use `launchSingleTop` where repeated taps could duplicate destinations.
- Preserve saved state handoff patterns for recent uploads and saved food detail.

---

## 8. Subscription, Billing, and Entitlement Rules

Billing is commercial-critical. Be conservative.

Current product IDs:

```kotlin
BiteCalBillingProducts.MONTHLY = "bitecal_monthly"
BiteCalBillingProducts.YEARLY = "bitecal_yearly"
```

Current offer tags:

```kotlin
DEFAULT_YEARLY = "default-yearly"
ONBOARD_DISCOUNT_YEARLY = "onboard-discount-yearly"
ONBOARD_TRIAL_DISCOUNT_YEARLY = "onboard-trial-discount-yearly"
```

Rules:

- Google Play Billing is the source for purchase UI and purchase tokens.
- Backend entitlement sync is the source for final app entitlement state.
- Do not mark users premium only because the local client says a purchase happened; sync to backend first, then refresh membership/entitlement state.
- Acknowledge purchases only after successful backend validation/activation, unless a specific flow has already defined a safe retry strategy.
- Do not log purchase tokens.
- Do not invent product IDs, offer tags, trial length, pricing, or localized price text. Use Google Play `ProductDetails` / pricing phases when available.
- Do not add dark patterns. Trial, renewal, cancellation, price, and restore behavior must be clear.
- Payment issue states may still be entitled during grace period. UI may show `PAYMENT ISSUE`, but Camera access should follow the backend entitlement result.
- If backend returns `trialEligible = false`, avoid CTA text that promises a free trial.

Relevant APIs/DTOs:

- `data/entitlement/api/EntitlementApi.kt`
- `data/membership/api/MembershipApi.kt`
- `data/billing/BillingGateway.kt`
- `data/billing/PlayBillingGateway.kt`
- `data/billing/FakeBillingGateway.kt`
- `ui/home/ui/membership/MembershipUiMapper.kt`
- `ui/home/ui/membership/MembershipViewModel.kt`
- `ui/subscription/SubscriptionViewModel.kt`

---

## 9. Food Log, AI, Camera, and Nutrition Rules

Food photo recognition and nutrition estimates are user-facing but probabilistic.

Rules:

- Do not present AI/nutrition output as medical advice or guaranteed accuracy.
- Low-confidence recognition must degrade to manual correction/search/input/barcode flow rather than silently saving bad data.
- Users must be able to review and edit food, quantity, calories, and macros.
- Preserve portion multiplier behavior carefully. Draft UI changes should not be committed to the server until the intended action, such as `Done`, when the current flow requires explicit confirmation.
- Keep status semantics clear: `PENDING`, `DRAFT`, `SAVED`, `DELETED` are backend/business concepts; do not collapse them into booleans.
- Do not upload uncompressed full-size images when existing compression/cropping utilities are available.
- Do not store or log raw image URIs in analytics/logs.

Relevant files:

- `ui/home/ui/camera/CameraScreen.kt`
- `ui/home/ui/camera/barcode/*`
- `ui/home/ui/foodlog/RecentUploadDetailScreen.kt`
- `ui/home/ui/foodlog/RecentUploadCard.kt`
- `ui/home/ui/foodlog/FoodLogTimeResolver.kt`
- `data/foodlog/api/FoodLogsApi.kt`
- `data/foodlog/repo/FoodLogsRepository.kt`
- `data/foodlog/repo/ImageCompressUtil.kt`

---

## 10. Health Connect, Activity, Water, Workout, and Progress Rules

Health and fitness data must be treated as sensitive.

Rules:

- Respect Health Connect permission flows and fallback screens.
- Before calling Health Connect APIs, verify Health Connect SDK availability/status. Handle cases where Health Connect is unavailable, not installed, disabled, or blocked by outdated Google Play Services / Play Store components.
- Do not assume permissions are granted. Always handle denied, permanently denied, unavailable, and retry states.
- Use locale-aware units and formatting.
- Avoid fake precision in charts; show empty/loading states instead of misleading zeros when data is unavailable.
- Keep weekly/daily aggregation definitions explicit when changing progress charts.
- Do not silently mix server state and local cache without a clear source-of-truth rule.

Relevant files:

- `data/activity/*`
- `data/health/HealthConnectRepository.kt`
- `ui/home/ui/progress/ProgressScreen.kt`
- `ui/home/ui/progress/NutritionChartCard.kt`
- `ui/home/ui/progress/WaterChartCard.kt`
- `ui/home/ui/progress/WorkoutChartCard.kt`
- `data/water/*`
- `data/workout/*`
- `data/weight/*`

---

## 11. Auth, Session, and Network Rules

Network setup is centralized in `di/NetworkModule.kt`.

Current behavior to preserve:

- `Json { ignoreUnknownKeys = true; explicitNulls = true; encodeDefaults = false }`.
- Auth and API clients are separate named OkHttp/Retrofit instances.
- `BaseHeadersInterceptor` is added to clients for device/language/timezone headers.
- `AuthInterceptor` and `TokenAuthenticator` are only on authenticated API client.
- Logging is headers-only in debug and disabled in release, with sensitive headers redacted.

Rules:

- Do not create a second Retrofit instance in feature code.
- Do not bypass token refresh/authenticator behavior for authenticated APIs.
- Do not add broad retry loops that can double-submit write requests.
- For write APIs, preserve idempotency behavior when the backend supports it.
- Map server errors into user-safe UI messages; do not dump raw backend errors to the UI.

---

## 12. Localization Rules

This app currently has matching string counts in:

- `main/res/values/strings.xml`
- `main/res/values-zh-rTW/strings.xml`

Rules:

- Any new user-facing string must be added to both files.
- Prefer `stringResource(...)` in Compose.
- Do not concatenate localized strings manually when placeholders can be used.
- Use placeholder strings for dynamic values:

```xml
<string name="example_remaining_kcal">%1$d kcal left</string>
```

- Keep app language switching compatible with `LanguageManager`, `LanguageStore`, `ComposeLocale`, and `BiteCalApp` startup behavior.
- Do not hardcode English text in screens unless it is temporary debug-only UI and guarded appropriately.

---

## 13. Referral and Premium Rewards Rules

Referral and premium rewards affect paid entitlement UX.

Rules:

- App UI should display backend-provided reward state; it should not calculate final eligibility independently.
- Do not claim that renewal was deferred by Google Play unless backend explicitly reports the Google defer/reward channel result.
- Keep reward status display resilient to null fields from older backend responses.
- Avoid leaking risk/fraud/internal review details to end users.

Relevant files:

- `data/referral/*`
- `data/membership/api/MembershipApi.kt`
- `ui/home/ui/settings/referral/*`
- `ui/home/ui/settings/premium/*`
- `ui/onboarding/referralcode/*`

---

## 14. Manifest and Platform Rules

`AndroidManifest.xml` declares permissions and platform integrations. Be careful when changing it.

Current notable declarations:

- Internet
- Notifications
- Boot completed and exact alarm for fasting/reminders
- Health Connect read permissions
- Camera permission
- Camera hardware marked `required="false"`
- `supportsRtl="true"`
- `localeConfig`
- `networkSecurityConfig`
- Health Connect rationale/proxy activities
- Fasting receivers and boot reschedule receiver

Rules:

- Do not add dangerous permissions without a user-facing rationale and fallback behavior.
- Keep camera hardware optional unless product requirements change.
- Do not make receivers/activities exported unless strictly necessary.
- Be cautious with `usesCleartextTraffic`; production should not rely on cleartext backend traffic.

---

## 15. Testing Expectations

The uploaded archive does not include `test/` or `androidTest/` source sets. When full project files are available, add tests under the standard Android locations.

Recommended test targets:

- ViewModel state transitions.
- Membership and entitlement UI mapping.
- Subscription purchase result handling.
- Onboarding route decisions and back stack expectations.
- Food log portion multiplier commit/cancel behavior.
- Permission fallback behavior.
- Date/time/locale formatting.
- Repository error mapping.
- Chart aggregation calculations.

Test rules:

- Do not depend on real Google Play Billing, real Health Connect, real network, or real current time in unit tests.
- Inject fake repositories/gateways/clocks where possible.
- For date/time-sensitive code, test timezone boundaries explicitly.
- For localization-sensitive code, avoid brittle hardcoded English-only expectations.

---

## 16. Code Style

- Kotlin should be explicit, readable, and warning-free.
- Prefer immutable data classes for UI state.
- Prefer named parameters for long argument lists.
- Avoid magic numbers in business logic; use constants with names.
- Avoid huge files growing further. For large screens, extract private Composables or feature components.
- Keep imports clean. Remove unused imports before returning code.
- Prefer `private` for implementation details.
- Do not add `@Suppress`, `@SuppressLint`, or broad opt-ins unless the reason is clear and localized.
- Do not introduce deprecated APIs when a supported alternative exists.
- Do not leave Android Studio inspections for the user to fix later. If a warning is unavoidable, document the exact warning and why it is acceptable.
- Keep Compose modifier order and scope valid. For example, scoped modifiers such as `align(...)` must only be used in the matching scope.
- Keep coroutine calls valid. `suspend` functions must be called only from a coroutine or another suspend function.
- Do not introduce API calls or constructor parameters that do not exist in the current codebase. Check existing function signatures before returning code.

---

## 17. How to Respond to the User

When asked to implement or review code:

1. Inspect the relevant current files first.
2. State assumptions if the full Gradle/backend context is missing.
3. Give exact file paths.
4. For edits, provide complete replacement blocks or clearly bounded method-level replacements.
5. Explain build/test commands to run.
6. Report risks and fallback behavior.
7. Do not instruct automatic commit/push unless the user explicitly asks for Git steps.
8. Before presenting code, mentally verify Android Studio diagnostics: imports, deprecated APIs, modifier scopes, coroutine scopes, nullability, string resources, and all changed call sites.

The user prefers Traditional Chinese explanations, detailed file-level guidance, and copy-paste-ready code.

---

## 18. Common Safe Defaults

Use these defaults unless current code or the user’s explicit instruction says otherwise:

- Screen state: Loading / Empty / Error / Success / Retry.
- Permission fallback: denied → rationale; permanently denied/no launcher → system settings page.
- Subscription gating: `TRIAL` and `PREMIUM` can access premium scan/camera; `FREE` and expired states cannot.
- Payment issue: show clear recovery UI, but rely on backend entitlement for access.
- Network errors: user-safe message + retry; no raw stack traces in UI.
- AI food recognition: editable estimate, not a diagnosis, with manual fallback.
- Localization: update English and Traditional Chinese resources together.
- Security: no secrets, no token logs, no PII logs.

---

## 19. When Unsure

Prefer a small, safe, reversible change over a broad refactor.
If a behavior depends on backend state, Google Play, Health Connect, or Play Console configuration, do not guess. Surface the dependency clearly and code the Android side with defensive fallback behavior.
