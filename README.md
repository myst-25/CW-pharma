# CW Pharmacy PDF Saver & Bypass (v1.3.0)

An LSPosed module to enable downloading encrypted PDFs and bypassing screenshot restrictions (FLAG_SECURE) from the CW Pharmacy app.

## Features
- **PDF Extraction**: Instantly decrypt and download paid PDF notes.
- **Screenshot & Recording Bypass**: Clears `FLAG_SECURE` to let you take screenshots or record lectures.
- **Root-less Support via LSPatch**: Fully compatible with LSPatch for users without rooted devices.

## Requirements
- CW Pharmacy App v1.1.3
- LSPosed Framework with `libxposed` API 102+
- OR use **LSPatch** (for non-rooted devices)

## Non-Rooted Usage (NPatch / LSPatch)
If your device is not rooted, you can patch the CW Pharmacy APK directly using NPatch (a modern fork of LSPatch).

**Step-by-Step Guide:**
1. Download the latest release APK of this module (CW PDF Saver).
2. Download and install **NPatch Manager** (or LSPatch Manager).
3. Open NPatch and go to the **Manage** tab. Click the **+** button.
4. Select the **original CW Pharmacy APK** from your storage.
5. In the patching options, choose **Local Mode** (this embeds the module directly into the APK).
6. Under modules, select the **CW PDF Saver** module you downloaded.
7. **CRITICAL TOGGLES TO ENABLE:**
   - ✅ **Bypass Signature Verification**: You *must* enable this. Why? The CW Pharmacy app has anti-tamper protections. If it detects that the app's signature has changed (which happens when we patch it), it will crash or show an error saying *"Get this app from Play Store"*. Enabling this toggle tricks the app into seeing its original developer signature.
   - ✅ **Spoof Installer**: Sometimes required so the app thinks it was installed by Google Play Store (`com.android.vending`) instead of the package installer.
8. Start the patch process. Once done, uninstall the original CW Pharmacy app, and install the newly patched APK.
9. Open the CW Pharmacy app, log in, and you will see your PDFs synced to the module's dashboard!
## Contributions
- App Idea - Naazneen

## Legal
This tool is strictly for educational purposes and personal backup. Knowledge must be free, accessible, and shareable for all.
