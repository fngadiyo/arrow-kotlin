# Arrow Entangled

Arrow Entangled is a mind-bending puzzle game where players must strategically clear every arrow on the board to proceed. Featuring a native Android wrapper that hosts a highly customized, responsive web implementation, the app boasts a dynamic UI with adaptive themes, haptic feedback, and modern aesthetics. It provides a robust Kotlin environment for hosting this engine, featuring integrated monetization, Google Play Games Services, and adaptive launcher icons.

## Version

- Current app version: `0.0.1`
- Package name: `com.fngadiyo.arrow`

## Features

- **Puzzle Gameplay**: Direct integration of the Arrow Entangled logic, featuring casual & competitive modes.
- **Native Android Shell**: High-performance WebView implementation with haptic feedback bridge.
- **Premium Design**: Adaptive launcher icon, system-aware dynamic themes (Dark/Light).
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
