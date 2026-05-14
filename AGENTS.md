# Android Agent Instructions

## Project

This is the Android app for Calai / BiteCal.

## Tech Stack

- Kotlin
- Jetpack Compose
- MVVM + Clean Architecture
- Hilt
- Room
- Retrofit / OkHttp
- DataStore
- WorkManager
- Google Play Billing
- Navigation Compose

## Rules

- Do not modify unrelated files.
- Do not break onboarding, billing, membership, referral, camera, or localization flows.
- DTO, Domain Model, Entity, and UI Model must be separated.
- Repository interface belongs to domain layer when applicable.
- Repository implementation belongs to data layer.
- ViewModel must expose immutable UI state.
- UI should follow unidirectional data flow.
- Compose screens should handle Loading, Empty, Error, Success, Offline, and Retry when applicable.
- Permission flows must handle first denial, permanent denial, and settings fallback.
- Do not hardcode API keys or secrets.
- Do not commit local.properties, keystore files, service account files, or .env files.
- Prefer small, reviewable changes.

## Test Commands

Preferred unit test:

```powershell
cd C:\Users\User\Projects\calai\android
.\gradlew.bat :app:testDevDebugUnitTest