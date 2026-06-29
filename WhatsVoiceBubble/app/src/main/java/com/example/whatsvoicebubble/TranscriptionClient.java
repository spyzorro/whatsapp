package com.example.whatsvoicebubble;

import android.content.Context;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class TranscriptionClient {
    // مهم: لا تضع مفتاح OpenAI أو Google داخل تطبيق أندرويد مباشر.
    // اعمل Backend بسيط يستقبل audio bytes ويرجع النص، وضع رابط backend هنا.
    private static final String BACKEND_TRANSCRIBE_URL = "https://YOUR_BACKEND_DOMAIN/transcribe";

    public static String transcribe(Context ctx, Uri audioUri) throws Exception {
        byte[] audio = readAll(ctx.getContentResolver().openInputStream(audioUri));
        if (audio.length == 0) throw new Exception("ملف الصوت فارغ أو غير قابل للقراءة");

        // مؤقتًا لو لسه ماعملتش backend، رجّع رسالة واضحة بدل crash.
        if (BACKEND_TRANSCRIBE_URL.contains("YOUR_BACKEND_DOMAIN")) {
            return "تم التقاط ملف الريكورد بنجاح.\n\nالخطوة التالية: اربط التطبيق بـ backend تحويل الصوت لنص. لا تضع API Key داخل التطبيق نفسه.";
        }

        HttpURLConnection conn = (HttpURLConnection) new URL(BACKEND_TRANSCRIBE_URL).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(120000);
        conn.setRequestProperty("Content-Type", "audio/ogg");
        try (OutputStream os = conn.getOutputStream()) { os.write(audio); }

        int code = conn.getResponseCode();
        InputStream is = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
        String body = new String(readAll(is));
        if (code < 200 || code >= 300) throw new Exception(body);
        return body;
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
