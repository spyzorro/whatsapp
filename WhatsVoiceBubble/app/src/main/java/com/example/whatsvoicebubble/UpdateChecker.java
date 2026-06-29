package com.example.whatsvoicebubble;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker {
    // غيّر الريبو هنا لو نقلت المشروع لريبو باسم مختلف.
    private static final String GITHUB_REPO = "spyzorro/-";
    private static final String LATEST_RELEASE_API = "https://api.github.com/repos/" + GITHUB_REPO + "/releases/latest";

    public interface Callback {
        void onResult(UpdateInfo info);
        void onError(String error);
    }

    public static class UpdateInfo {
        public boolean available;
        public String versionName;
        public int versionCode;
        public String notes;
        public String pageUrl;
        public String apkUrl;
    }

    public static void check(Activity activity, Callback callback) {
        new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(LATEST_RELEASE_API).openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(20000);
                conn.setRequestProperty("Accept", "application/vnd.github+json");
                conn.setRequestProperty("User-Agent", "WhatsVoiceBubble-Android");

                int code = conn.getResponseCode();
                InputStream is = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
                String body = readText(is);
                if (code < 200 || code >= 300) throw new Exception(body);

                JSONObject json = new JSONObject(body);
                String tag = json.optString("tag_name", "");
                int latestCode = parseVersionCode(tag);
                String latestName = cleanVersionName(tag);

                UpdateInfo info = new UpdateInfo();
                info.versionCode = latestCode;
                info.versionName = latestName;
                info.notes = json.optString("body", "");
                info.pageUrl = json.optString("html_url", "https://github.com/" + GITHUB_REPO + "/releases/latest");
                info.apkUrl = findApkUrl(json.optJSONArray("assets"));
                info.available = latestCode > BuildConfig.VERSION_CODE;

                callback.onResult(info);
            } catch (Exception e) {
                callback.onError(e.getMessage() == null ? e.toString() : e.getMessage());
            }
        }).start();
    }

    public static void openDownloadPage(Activity activity, UpdateInfo info) {
        String url = info.apkUrl != null && info.apkUrl.length() > 0 ? info.apkUrl : info.pageUrl;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        activity.startActivity(intent);
    }

    private static String findApkUrl(JSONArray assets) throws Exception {
        if (assets == null) return "";
        for (int i = 0; i < assets.length(); i++) {
            JSONObject a = assets.getJSONObject(i);
            String name = a.optString("name", "").toLowerCase();
            if (name.endsWith(".apk")) return a.optString("browser_download_url", "");
        }
        return "";
    }

    private static int parseVersionCode(String tag) {
        // الصيغة المفضلة: v2 أو v2.0 أو v2.1.0. الرقم الأول بعد v يعتبر versionCode.
        try {
            String cleaned = tag.toLowerCase().replace("version", "").replace("v", "").trim();
            String first = cleaned.split("\\.")[0].replaceAll("[^0-9]", "");
            return first.length() == 0 ? 0 : Integer.parseInt(first);
        } catch (Exception e) {
            return 0;
        }
    }

    private static String cleanVersionName(String tag) {
        if (tag == null || tag.trim().length() == 0) return "غير معروف";
        return tag.startsWith("v") ? tag.substring(1) : tag;
    }

    private static String readText(InputStream is) throws Exception {
        if (is == null) return "";
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line).append('\n');
        return sb.toString();
    }
}
