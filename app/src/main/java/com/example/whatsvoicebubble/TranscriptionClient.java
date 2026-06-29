package com.example.whatsvoicebubble;

import android.content.Context;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class TranscriptionClient {
    public static String transcribe(Context ctx, Uri audioUri) throws Exception {
        String backendUrl = AppSettings.getBackendUrl(ctx);
        if (backendUrl.length() == 0) {
            return "تم التقاط الصوت بنجاح، لكن التحويل لنص محتاج رابط Backend.\n\nافتح التطبيق الرئيسي وضع رابط السيرفر في خانة Backend URL ثم جرّب مرة ثانية.";
        }

        byte[] audio = readAll(ctx.getContentResolver().openInputStream(audioUri));
        if (audio.length == 0) throw new Exception("ملف الصوت فارغ أو غير قابل للقراءة");

        HttpURLConnection conn = (HttpURLConnection) new URL(backendUrl).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(180000);
        conn.setRequestProperty("Content-Type", guessContentType(audioUri));
        conn.setRequestProperty("X-File-Name", audioUri.toString());
        try (OutputStream os = conn.getOutputStream()) { os.write(audio); }

        int code = conn.getResponseCode();
        InputStream is = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
        String body = new String(readAll(is), "UTF-8").trim();
        if (code < 200 || code >= 300) throw new Exception(body.length() == 0 ? "Backend error: " + code : body);
        return body.length() == 0 ? "لم يرجع السيرفر أي نص." : body;
    }

    private static String guessContentType(Uri uri) {
        String s = uri == null ? "" : uri.toString().toLowerCase();
        if (s.endsWith(".m4a") || s.contains("m4a")) return "audio/mp4";
        if (s.endsWith(".mp3") || s.contains("mp3")) return "audio/mpeg";
        if (s.endsWith(".wav") || s.contains("wav")) return "audio/wav";
        if (s.endsWith(".opus") || s.contains("opus")) return "audio/ogg";
        return "audio/ogg";
    }

    private static byte[] readAll(InputStream is) throws Exception {
        if (is == null) return new byte[0];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) != -1) out.write(buf, 0, n);
        return out.toByteArray();
    }
}
