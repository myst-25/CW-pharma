package com.cwpdf.saver.util;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class DownloadEngine {
    private static final String TAG = "CWPDFSaver_Download";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface DownloadCallback {
        void onSuccess(String fileName);
        void onError(String errorMsg);
    }

    public static void startDownload(Context context, String pdfUrl, Uri localUri, String key, String title, boolean isEncrypted, DownloadCallback callback) {
        executor.execute(() -> {
            try {
                byte[] rawBytes = null;

                if (localUri != null) {
                    rawBytes = readUriBytes(context, localUri);
                } else if (pdfUrl != null && !pdfUrl.isEmpty()) {
                    rawBytes = downloadBytes(pdfUrl);
                }

                if (rawBytes == null) {
                    if (callback != null) callback.onError("Download failed: Empty response or inaccessible file.");
                    return;
                }

                byte[] pdfBytes = null;
                if (isEncrypted && key != null && !key.isEmpty()) {
                    pdfBytes = decryptAesCbc(rawBytes, key);
                } else {
                    boolean isPdf = rawBytes.length >= 4 && rawBytes[0] == 0x25 && rawBytes[1] == 0x50 && rawBytes[2] == 0x44 && rawBytes[3] == 0x46;
                    if (isPdf) {
                        pdfBytes = rawBytes;
                    } else {
                        pdfBytes = decryptXorHeader(rawBytes);
                    }
                }

                if (pdfBytes == null) {
                    if (callback != null) callback.onError("Decryption failed");
                    return;
                }

                File downDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                String safeTitle = (title != null && !title.isEmpty()) ? title.replaceAll("[^a-zA-Z0-9._-]", "_") : "document";
                File outFile = new File(downDir, safeTitle + "_myzanori_" + System.currentTimeMillis() + ".pdf");
                
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    fos.write(pdfBytes);
                }
                
                if (callback != null) callback.onSuccess(outFile.getName());

            } catch (Exception e) {
                Log.e(TAG, "Download error", e);
                if (callback != null) callback.onError("Error: " + e.getMessage());
            }
        });
    }

    private static byte[] readUriBytes(Context context, Uri uri) throws IOException {
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (is == null) return null;
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[16384];
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            return buffer.toByteArray();
        }
    }

    private static byte[] downloadBytes(String url) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .header("Referer", "https://player.akamai.net.in/")
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Log.e(TAG, "HTTP error: " + response.code());
                return null;
            }
            ResponseBody body = response.body();
            return body != null ? body.bytes() : null;
        }
    }

    private static byte[] decryptAesCbc(byte[] encryptedContent, String password) throws Exception {
        byte[] ivBytes = Arrays.copyOfRange(encryptedContent, 0, 16);
        byte[] cipherText = Arrays.copyOfRange(encryptedContent, 16, encryptedContent.length);

        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(password.getBytes("UTF-8"));
        byte[] keyBytes = md.digest();

        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");
        IvParameterSpec ivParameterSpec = new IvParameterSpec(ivBytes);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);

        return cipher.doFinal(cipherText);
    }

    private static byte[] decryptXorHeader(byte[] rawBytes) {
        for (int i = 0; i < 28 && i < rawBytes.length; i++) {
            rawBytes[i] = (byte) (rawBytes[i] ^ 9);
        }
        return rawBytes;
    }
}
