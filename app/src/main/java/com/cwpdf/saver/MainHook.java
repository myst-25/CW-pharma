package com.cwpdf.saver;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import com.cwpdf.saver.util.DownloadEngine;

public class MainHook extends XposedModule {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final String TAG = "CWPDFSaver";
    private static boolean hasShownPopup = false;

    public MainHook() {
        super();
        Log.d(TAG, "MainHook instantiated (libxposed API 102)");
    }

    @Override
    public boolean onHotReloading(XposedModuleInterface.HotReloadingParam param) {
        Log.d(TAG, "onHotReloading: module is preparing to hot reload");
        return true;
    }

    @Override
    public void onHotReloaded(XposedModuleInterface.HotReloadedParam param) {
        Log.d(TAG, "onHotReloaded: module hot reload complete");
    }

    @Override
    public void onPackageLoaded(XposedModuleInterface.PackageLoadedParam param) {
        super.onPackageLoaded(param);
        if (!param.getPackageName().equals("com.gvxhgw.qwporr")) return;

        Log.d(TAG, "CW Pharma package loaded");

        try {
            ClassLoader classLoader = param.getDefaultClassLoader();
            
            // Hook PDF Viewer Activities
            XposedInterface.Hooker activityOnCreateHooker = new XposedInterface.Hooker() {
                @Override
                public Object intercept(XposedInterface.Chain chain) throws Throwable {
                    Object result = chain.proceed(); // execute original onCreate
                    Activity activity = (Activity) chain.getThisObject();
                    
                    Intent intent = activity.getIntent();
                    String pdfUrl = intent.getStringExtra("url");
                    Uri localUri = intent.getParcelableExtra("uri");
                    String key = intent.getStringExtra("key");
                    String title = intent.getStringExtra("title");
                    boolean isEncrypted = intent.getBooleanExtra("encrypted", false);

                    Log.d(TAG, "PDF opened — url=" + pdfUrl + " uri=" + localUri + " key=" + key + " encrypted=" + isEncrypted);

                    if ((pdfUrl == null || pdfUrl.isEmpty()) && localUri == null) {
                        return result;
                    }

                    syncPdfToModule(activity, pdfUrl, localUri, key, title, isEncrypted);
                    injectDownloadButton(activity, pdfUrl, localUri, key, title, isEncrypted);
                    return result;
                }
            };

            try {
                Class<?> pdfViewerClassOld = classLoader.loadClass("com.appx.core.activity.PdfViewerActivity");
                Method onCreateOld = pdfViewerClassOld.getDeclaredMethod("onCreate", Bundle.class);
                hook(onCreateOld).intercept(activityOnCreateHooker);
            } catch (Throwable t) { Log.e(TAG, "Error hooking old PDF viewer", t); }

            try {
                Class<?> pdfViewerClassNew = classLoader.loadClass("com.appx.core.activity.NewPDFViewerActivity");
                Method onCreateNew = pdfViewerClassNew.getDeclaredMethod("onCreate", Bundle.class);
                hook(onCreateNew).intercept(activityOnCreateHooker);
            } catch (Throwable t) { Log.e(TAG, "Error hooking new PDF viewer", t); }

            // Hook Activity onResume to show welcome popup once
            XposedInterface.Hooker activityOnResumeHooker = new XposedInterface.Hooker() {
                @Override
                public Object intercept(XposedInterface.Chain chain) throws Throwable {
                    Object result = chain.proceed();
                    if (hasShownPopup) return result; // Quick exit to save CPU

                    Activity activity = (Activity) chain.getThisObject();
                    
                    // Skip splash screens so the popup doesn't get destroyed when the logo disappears
                    String actName = activity.getClass().getSimpleName().toLowerCase();
                    if (actName.contains("splash") || actName.contains("launch") || actName.contains("start")) {
                        return result;
                    }
                    
                    hasShownPopup = true;
                    showWelcomePopup(activity);
                    return result;
                }
            };
            try {
                Method onResumeMethod = Activity.class.getDeclaredMethod("onResume");
                hook(onResumeMethod).intercept(activityOnResumeHooker);
            } catch (Throwable t) { Log.e(TAG, "Error hooking Activity.onResume", t); }

            // Crash + screenshot-block fix: the app's (void) isScreenshotEnabled()
            // reads the current window FLAG_SECURE bit and, when the "ACTIVATE_SCREENSHOT"
            // preference is set, dereferences a crashViewModel field that is still null
            // during onCreate -> NullPointerException on launch. No-op it so the app
            // neither crashes nor blocks screenshots.
            XposedInterface.Hooker noOpHooker = chain -> null;
            try {
                Class<?> mainActivityClass = classLoader.loadClass("com.appx.core.activity.MainActivity");
                Method isScreenshotEnabledMain = mainActivityClass.getDeclaredMethod("isScreenshotEnabled");
                hook(isScreenshotEnabledMain).intercept(noOpHooker);
                Log.d(TAG, "Successfully disabled MainActivity.isScreenshotEnabled (crash fix).");
            } catch (Throwable t) { Log.e(TAG, "Error hooking MainActivity.isScreenshotEnabled", t); }

            try {
                Class<?> splashActivityClass = classLoader.loadClass("com.appx.core.activity.SplashActivity");
                Method isScreenshotEnabledSplash = splashActivityClass.getDeclaredMethod("isScreenshotEnabled");
                hook(isScreenshotEnabledSplash).intercept(noOpHooker);
                Log.d(TAG, "Successfully disabled SplashActivity.isScreenshotEnabled (crash fix).");
            } catch (Throwable t) { Log.e(TAG, "Error hooking SplashActivity.isScreenshotEnabled", t); }

            // Content gate bypass: the boolean isScreenshotEnabled() variants return
            // true (and show the "Please disable screenshot" toast) when the window
            // has FLAG_SECURE + ACTIVATE_SCREENSHOT, which blocks opening paid PDFs /
            // videos. Force them to return false so content always opens.
            XposedInterface.Hooker screenshotGateBypass = chain -> Boolean.FALSE;
            String[] screenshotGateClasses = {
                "com.appx.core.activity.PreviousLiveActivity",
                "com.appx.core.activity.FolderCoursesContentsActivity",
                "com.appx.core.activity.PaidCourseRecordActivity",
                "com.appx.core.fragment.DemoFragment",
                "com.appx.core.fragment.OTTFragment",
                "com.appx.core.fragment.FolderCourseContentsFragment",
                "com.appx.core.fragment.LiveUpcomingCourseFragment",
                "com.appx.core.fragment.TimeTableVideoFragment",
                "com.appx.core.fragment.RecentClassesFragment",
                "com.appx.core.fragment.ThirdHomeFragment",
                "com.appx.core.fragment.BonusContentsFragment",
                "com.appx.core.fragment.RecordedUpcomingFragment"
            };
            for (String clazzName : screenshotGateClasses) {
                try {
                    Class<?> clazz = classLoader.loadClass(clazzName);
                    Method gate = clazz.getDeclaredMethod("isScreenshotEnabled");
                    hook(gate).intercept(screenshotGateBypass);
                    Log.d(TAG, "Bypassed screenshot gate: " + clazzName);
                } catch (Throwable t) { Log.e(TAG, "Error hooking screenshot gate " + clazzName, t); }
            }

            // Disable the app's screenshot-detection restart.
            // When the app detects a screenshot bypass it calls Appx.a(): it shows
            // "Screenshot Disabled. Restarting app", sets ACTIVATE_SCREENSHOT=false
            // and restarts to SplashActivity. No-op it so the app never reacts to the
            // (external) screenshot module you use.
            try {
                Class<?> appxClass = classLoader.loadClass("com.appx.core.Appx");
                Method screenshotRestart = appxClass.getDeclaredMethod("a");
                hook(screenshotRestart).intercept(noOpHooker);
                Log.d(TAG, "Disabled Appx screenshot-detection restart.");
            } catch (Throwable t) { Log.e(TAG, "Error hooking Appx screenshot detection", t); }

        } catch (Throwable t) {
            Log.e(TAG, "onPackageLoaded Error", t);
        }
    }

    private void syncPdfToModule(Activity activity, String pdfUrl, Uri localUri, String key, String title, boolean isEncrypted) {
        try {
            android.content.ContentValues values = new android.content.ContentValues();
            values.put("title", title != null ? title : "Unknown PDF");
            values.put("url", pdfUrl != null ? pdfUrl : "");
            values.put("uri", localUri != null ? localUri.toString() : "");
            values.put("decryption_key", key != null ? key : "");
            values.put("is_encrypted", isEncrypted ? 1 : 0);
            values.put("timestamp", System.currentTimeMillis());

            Uri providerUri = Uri.parse("content://com.cwpdf.saver.provider/pdfs");
            activity.getContentResolver().insert(providerUri, values);
            
            Log.d(TAG, "Successfully synced PDF to module UI: " + title);
            
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(activity, "PDF synced to CW PDF Saver app!", Toast.LENGTH_SHORT).show();
            });
        } catch (Exception e) {
            Log.e(TAG, "Failed to sync PDF to module", e);
        }
    }

    private void injectDownloadButton(final Activity activity, final String pdfUrl, final Uri localUri, final String key, final String title, final boolean isEncrypted) {
        try {
            View root = activity.findViewById(android.R.id.content);
            if (root == null || root.findViewWithTag("cw_download_bar") != null) return;

            float dp = dpToPx(activity, 1);
            int barHeight = (int)(48 * dp);

            // Bottom bar that spans from the START (left edge) up to the existing
            // fab_menu button on the right, without overlapping it.
            android.widget.LinearLayout bar = new android.widget.LinearLayout(activity);
            bar.setTag("cw_download_bar");
            bar.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            bar.setGravity(Gravity.CENTER);
            bar.setClickable(true);

            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setColor(0xFF0061A4);
            bg.setCornerRadius((int)(24 * dp));
            bar.setBackground(bg);
            bar.setElevation(6 * dp);

            android.widget.TextView label = new android.widget.TextView(activity);
            label.setText("⬇  Download PDF");
            label.setTextSize(16);
            label.setTextColor(0xFFFFFFFF);
            label.setGravity(Gravity.CENTER);
            bar.addView(label);

            // Parent of android.R.id.content is a FrameLayout -> FrameLayout.LayoutParams
            // honors gravity, which fixes the top-left placement.
            FrameLayout.LayoutParams barParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, barHeight
            );
            barParams.gravity = Gravity.BOTTOM | Gravity.START;
            barParams.setMargins((int)(14 * dp), 0, (int)(78 * dp), (int)(14 * dp));
            bar.setLayoutParams(barParams);

            bar.setOnClickListener(v -> {
                Toast.makeText(activity, "Downloading PDF...", Toast.LENGTH_SHORT).show();
                DownloadEngine.startDownload(activity, pdfUrl, localUri, key, title, isEncrypted, new DownloadEngine.DownloadCallback() {
                    @Override
                    public void onSuccess(String fileName) {
                        new Handler(Looper.getMainLooper()).post(() ->
                            Toast.makeText(activity, "Saved: " + fileName, Toast.LENGTH_LONG).show());
                    }
                    @Override
                    public void onError(String errorMsg) {
                        new Handler(Looper.getMainLooper()).post(() ->
                            Toast.makeText(activity, "Download failed: " + errorMsg, Toast.LENGTH_LONG).show());
                    }
                });
            });

            // Measure the existing fab_menu so the bar ends exactly before it.
            try {
                int fabMenuId = activity.getResources().getIdentifier("fab_menu", "id", activity.getPackageName());
                View fabMenu = fabMenuId != 0 ? activity.findViewById(fabMenuId) : null;
                if (fabMenu != null) {
                    bar.post(() -> {
                        int rightMargin = root.getWidth() - fabMenu.getLeft() + (int)(10 * dp);
                        barParams.setMargins((int)(14 * dp), 0, rightMargin, (int)(14 * dp));
                        bar.setLayoutParams(barParams);
                    });
                }
            } catch (Throwable ignored) {}

            // Hide the bar while the PDF is being scrolled, show it again after.
            // Attach to the CONTENT ROOT (parent), NOT the PDFView: PDFView manages
            // its own touch internally via setOnTouchListener, so touching it there
            // would break scrolling/pinch. On the parent we just observe without
            // consuming (return false), so scroll still reaches the PDFView.
            try {
                root.setOnTouchListener((v, event) -> {
                    int action = event.getActionMasked();
                    if (action == android.view.MotionEvent.ACTION_MOVE) {
                        bar.setVisibility(View.GONE);
                    } else if (action == android.view.MotionEvent.ACTION_UP
                            || action == android.view.MotionEvent.ACTION_CANCEL) {
                        bar.postDelayed(() -> bar.setVisibility(View.VISIBLE), 300);
                    }
                    return false;
                });
            } catch (Throwable ignored) {}

            if (root instanceof ViewGroup) {
                ((ViewGroup) root).addView(bar, barParams);
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error injecting download button", t);
        }
    }

    private int dpToPx(Context context, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    private void showWelcomePopup(Activity activity) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                if (activity.isFinishing() || activity.isDestroyed()) return;

                // Create a custom MD3 style dialog from scratch to avoid host app theme dependencies
                android.app.Dialog dialog = new android.app.Dialog(activity);
                dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                }

                android.widget.LinearLayout container = new android.widget.LinearLayout(activity);
                container.setOrientation(android.widget.LinearLayout.VERTICAL);
                container.setPadding(dpToPx(activity, 24), dpToPx(activity, 24), dpToPx(activity, 24), dpToPx(activity, 24));
                
                android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
                int nightModeFlags = activity.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
                boolean isDarkMode = nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES;
                bg.setColor(isDarkMode ? 0xFF2D2F31 : 0xFFF3F4F9); // MD3 surface color
                bg.setCornerRadius(dpToPx(activity, 28)); // MD3 Dialog corner radius
                container.setBackground(bg);

                // Title
                android.widget.TextView title = new android.widget.TextView(activity);
                title.setText("CW Pharmacy PDF Saver");
                title.setTextSize(24);
                title.setTextColor(isDarkMode ? 0xFFE3E2E6 : 0xFF1A1C1E);
                title.setTypeface(null, android.graphics.Typeface.BOLD);
                container.addView(title);

                // Message
                android.widget.TextView message = new android.widget.TextView(activity);
                message.setText("Module is active! 🚀\n\nMade by myzanori.\nKnowledge must be free, accessible, and shareable for all.");
                message.setTextSize(14);
                message.setTextColor(isDarkMode ? 0xFFC4C6D0 : 0xFF44474E);
                message.setPadding(0, dpToPx(activity, 16), 0, dpToPx(activity, 24));
                container.addView(message);

                // Buttons container
                android.widget.LinearLayout buttonContainer = new android.widget.LinearLayout(activity);
                buttonContainer.setOrientation(android.widget.LinearLayout.VERTICAL);
                buttonContainer.setGravity(Gravity.CENTER_HORIZONTAL);

                android.widget.LinearLayout.LayoutParams btnParams = new android.widget.LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(activity, 48)
                );
                btnParams.bottomMargin = dpToPx(activity, 8);

                // Telegram Button (Primary Filled)
                android.widget.Button btnTelegram = new android.widget.Button(activity);
                btnTelegram.setText("Join Telegram");
                btnTelegram.setTextColor(isDarkMode ? 0xFF00325B : 0xFFFFFFFF);
                btnTelegram.setAllCaps(false);
                android.graphics.drawable.GradientDrawable btnTelBg = new android.graphics.drawable.GradientDrawable();
                btnTelBg.setColor(isDarkMode ? 0xFFD1E4FF : 0xFF0061A4);
                btnTelBg.setCornerRadius(dpToPx(activity, 24)); // Pill shape
                btnTelegram.setBackground(btnTelBg);
                btnTelegram.setOnClickListener(v -> {
                    activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/+OQA0X-ECCHI4ZmU1")));
                });
                buttonContainer.addView(btnTelegram, btnParams);

                // GitHub Button (Secondary Tonal)
                android.widget.Button btnGithub = new android.widget.Button(activity);
                btnGithub.setText("Star on GitHub");
                btnGithub.setTextColor(isDarkMode ? 0xFFE3E2E6 : 0xFF1A1C1E);
                btnGithub.setAllCaps(false);
                android.graphics.drawable.GradientDrawable btnGitBg = new android.graphics.drawable.GradientDrawable();
                btnGitBg.setColor(isDarkMode ? 0xFF44474E : 0xFFE0E2EC);
                btnGitBg.setCornerRadius(dpToPx(activity, 24));
                btnGithub.setBackground(btnGitBg);
                btnGithub.setOnClickListener(v -> {
                    activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/myzanori/CW-pharma")));
                });
                buttonContainer.addView(btnGithub, btnParams);

                // Close Button (Text style)
                android.widget.Button btnClose = new android.widget.Button(activity);
                btnClose.setText("Close");
                btnClose.setTextColor(isDarkMode ? 0xFFAEC6FF : 0xFF0061A4);
                btnClose.setAllCaps(false);
                btnClose.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                btnClose.setOnClickListener(v -> dialog.dismiss());
                buttonContainer.addView(btnClose, btnParams);

                container.addView(buttonContainer);

                dialog.setContentView(container);
                dialog.setCancelable(true);
                
                int width = (int)(activity.getResources().getDisplayMetrics().widthPixels * 0.85);
                dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
                
                dialog.show();

            } catch (Exception e) {
                Log.e(TAG, "Failed to show popup", e);
            }
        }, 500); // 500ms delay to ensure activity window is fully attached
    }
}
