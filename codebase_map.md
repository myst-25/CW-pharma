# CW Pharma LSPosed Module & Target App Codebase Map

> **IMPORTANT INSTRUCTIONS TO AI AGENTS**
> 
> - **READ THIS FILE FIRST** before making any edits to understand the structure of the module and the target app.
> - **UPDATE THIS FILE** after every session if you add new features, files, or change the architecture.
> - Keep the **Tree Structure** below up to date with new features and files.
> - Append a **Change Log** entry at the very end of this file documenting what you changed and why.

---

## Architecture & Features Tree

Below is the structure of the LSPosed module and the key files in the decompiled target app.

```text
LSPosed-PDFSaver/app/src/main/java/com/cwpdf/saver/
├── MainHook.java (385 lines)
├── MainActivity.java (29 lines)

Target App (com.appx.core) Key Files:
com/appx/core/
├── activity/
│   ├── PdfViewerActivity.java - Old PDF viewer, takes "url" or "uri" intent extra
│   ├── NewPDFViewerActivity.java - New PDF viewer
│   ├── CourseActivity.java - Main course display
│   ├── FolderCourseTabContentsActivity.java - Displays contents of a folder, passes "isPurchased"
│   └── NewTestTitleActivity.java - Test series
├── model/ - Data models holding "isPaid" flag
│   ├── CourseModel.java - getIsPaid() returns String ("1" for paid)
│   ├── FolderCourseModel.java - isPaid() returns int (1 for paid)
│   ├── TestSeriesModel.java - getIsPaid() returns String ("1" for paid)
│   └── MyCourseStudyModel.java - getIsPaid() returns String
```

---

# CHANGE LOG

> **AI agents: append changes here after every session.**
> Format: `## [YYYY-MM-DD] - Short description`
> Then bullet list exact files changed and a one-line reason.

## [2026-06-19] - IPC & UI Redesign for Module App
- **Added `PdfDatabaseHelper` and `PdfContentProvider`**: Set up IPC so the target app's hook can securely send intercepted PDF data to the module app.
- **Added `PdfAdapter` and redesigned `activity_main.xml`**: Transformed the module's `MainActivity` into a dashboard that lists all intercepted PDFs, allowing direct downloads.
- **Extracted `DownloadEngine`**: Moved all OkHttp download and AES/XOR decryption logic out of `MainHook` and into a shared utility for the UI to use.
- **Updated `MainHook`**: Replaced the UI injection logic with `syncPdfToModule()`, which silently pushes intercepted PDFs to the module's `ContentProvider`.
