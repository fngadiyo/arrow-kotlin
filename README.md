# Arrow Kotlin

Arrow Kotlin is a native Android wrapper for the Arrows puzzle game. It provides a robust Kotlin environment for hosting the responsive web implementation, featuring integrated monetization and Play Store services.

## Version

- Current app version: `0.1.0`
- Package name: `com.fngadiyo.arrow`

## Features

- **Puzzle Gameplay**: Direct integration of the Arrows logic.
- **Native Android Shell**: High-performance WebView implementation.
- **Monetization**: Integrated Google Mobile Ads (Banner/Interstitial) and placeholder for In-App Purchases.
- **Play Games Services**: Ready for achievements and leaderboards integration.

## Configuration

Monetization values are set in `gradle.properties`:

- `ADMOB_APP_ID`
- `ADMOB_BANNER_AD_UNIT_ID`
- `ADMOB_INTERSTITIAL_AD_UNIT_ID`
- `REMOVE_ADS_PRODUCT_ID`

Signing values are set in `local.properties` (never commit):

- `RELEASE_STORE_FILE`
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

## Build

```powershell
.\gradlew assembleDebug
.\gradlew bundleRelease
```

## Release Process

- See `CHANGELOG.md` for release history
- See `RELEASING.md` for required release/tagging steps
- See `CONVENTIONS.md` for repository standards
