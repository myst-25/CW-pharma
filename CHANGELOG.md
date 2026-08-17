# Changelog

All notable changes to the CW Pharmacy PDF Saver will be documented in this file.

## [v1.4.0] - 2026-08-18
### Added
- Injected a bottom download bar (starting from the left edge) into the PDF viewer that fetches, decrypts, and saves the PDF.
- The download bar auto-hides while the PDF is scrolled and reappears when scrolling stops.
- The download bar is positioned to end just before the app's existing FAB menu so the two never overlap.

### Changed
- Switched to the modern libxposed API 102 and bumped `minSdk` to 26.
- Fixed the release build's ProGuard/R8 rules so the `MainHook` entry class is never obfuscated (was silently breaking module loading).
- Rebranded the module/APK/download filename and all Telegram/GitHub links to `myzanori`.
- Rewired the scroll-to-hide listener to the content root instead of the PDFView so PDF scrolling/pinch is no longer broken.

### Fixed
- Fixed a launch crash caused by the app's `isScreenshotEnabled()` dereferencing a null `CrashViewModel`.
- Bypassed the app's boolean screenshot gates that blocked opening paid PDFs/videos.
- Disabled the app's "Screenshot Disabled. Restarting app" detection (Appx.a) that restart the app when a screenshot module is active.
- Removed the detectable installer-spoofing hooks and framework `Window.setFlags` hook, matching the older undetected module layout.

## [v1.2.0] - 2026-06-10
### Added
- Injected a global welcome popup dialog when launching CW Pharmacy to confirm module activation.
- Added quick action buttons on the popup to Join the Telegram Community and Star the project on GitHub.

## [v1.0.2] - 2026-06-10
### Changed
- Refined the floating action button UI by replacing "DL" text with a native download icon (⬇️).
- Lowered the minimum SDK requirement to Android 7.0 (API 24) to support older devices.
- Refined GitHub-to-Telegram Action workflows to dynamically inject custom APK names (`cw-pharma-[version]-myzanori.apk`).

## [v1.0.1] - 2026-06-10
### Changed
- Stripped away the experimental client-side paywall unlocker.
- Core focus cleanly adjusted strictly to the PDF Downloader & Decryptor features.
- Implemented automated CI/CD Github Actions pipelines for Telegram distribution.

## [v1.0.0] - 2026-06-10
### Added
- Initial Release of the CW Pharmacy module based on `libxposed` API 101.
- Injected custom Material Design Download button securely into the CW Pharmacy PDF Viewer.
- Added intelligent network decryption hooks that automatically fetch XOR/AES protected PDFs, strip their encryption layer, and safely extract the raw, readable `.pdf` to the device's `Downloads` directory.
