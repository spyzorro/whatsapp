package com.example.whatsvoicebubble;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker {
    private static final String GITHUB_OWNER = "spyzorro";
    private static final String GITHUB_REPO_NAME = "whatsapp";
    private static final String GITHUB_REPO = GITHUB_OWNER + "/" + GITHUB_REPO_NAME;
    private static final String RELEASES_LATEST_API = "https://api.github.com/repos/" + GITHUB_REPO + "/releases/latest";
    private static final String RELEASES_LIST_API = "https://api.github.com/repos/" + GITHUB_REPO + "/releases";
    private static final String RELEASES_PAGE = "https://github.com/" + GITHUB_REPO + "/releases/latest";

    public interface Callback {
        void onResult(UpdateInfo info);
        void onError(String error);
    }

    public static class UpdateInfo {
        public boolean available;
        public String versionName = "";
        public int versionCode;
        public String notes = "";
        public String pageUrl = RELEASES_PAGE;
        public String apkUrl = "";
        public String apkName = "WhatsVoiceBubble-update.apk";
        public String source = "GitHub";
    }

    public static void check(Activity activity, Callback callback) {
        new Thread(() -> {
            try {
                ReleaseResponse latest = requestJson(RELEASES_LATEST_API);
                JSONObject releaseJson;

                if (latest.code == 404) {
                    ReleaseResponse list = requestJson(RELEASES_LIST_API);
                    if (list.code == 404) {
                        throw new Exception("الريبو غير موجود أو الاسم غلط: " + GITHUB_REPO);
                    }
                    if (list.code < 200 || list.code >= 300) {
                        throw new Exception(cleanGithubError(list.body, list.code));
                    }
                    JSONArray arr = new JSONArray(list.body);
                    if (arr.length() == 0) {
                        throw new Exception("مفيش Release منشور على GitHub حتى الآن. اعمل Tag مثل v" + BuildConfig.VERSION_NAME + " أو انشر Release فيه ملف APK.");
                    }
                    releaseJson = pickFirstReleaseWithApk(arr);
                    if (releaseJson == null) releaseJson = arr.getJSONObject(0);
                } else if (latest.code >= 200 && latest.code < 300) {
                    releaseJson = new JSONObject(latest.body);
                } else {
                    throw new Exception(cleanGithubError(latest.body, latest.code));
                }

                UpdateInfo info = parseRelease(releaseJson);
                info.available = isNewer(info.versionName, info.versionCode, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE);
                callback.onResult(info);
            } catch (Exception e) {
                callback.onError(e.getMessage() == null ? e.toString() : e.getMessage());
            }
        }).start();
    }

    public static void downloadAndInstall(Activity activity, UpdateInfo info) {
        if (info == null || info.apkUrl == null || info.apkUrl.length() == 0) {
            Toast.makeText(activity, "لم أجد ملف APK داخل GitHub Release. افتح صفحة Releases وتأكد إن الـ APK مرفوع كـ asset.", Toast.LENGTH_LONG).show();
            openReleasePage(activity, info);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PackageManager pm = activity.getPackageManager();
            if (!pm.canRequestPackageInstalls()) {
                Toast.makeText(activity, "اسمح للتطبيق بتثبيت تحديثات APK ثم اضغط فحص التحديث مرة ثانية", Toast.LENGTH_LONG).show();
                Intent settings = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + activity.getPackageName()));
                activity.startActivity(settings);
                return;
            }
        }

        try {
            DownloadManager dm = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
            String fileName = safeFileName(info.apkName.length() == 0 ? "WhatsVoiceBubble-update.apk" : info.apkName);
            if (!fileName.toLowerCase().endsWith(".apk")) fileName = "WhatsVoiceBubble-update.apk";

            DownloadManager.Request req = new DownloadManager.Request(Uri.parse(info.apkUrl));
            req.setTitle("WhatsVoiceBubble update");
            req.setDescription("جاري تحميل التحديث من GitHub");
            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            req.setMimeType("application/vnd.android.package-archive");
            req.setAllowedOverMetered(true);
            req.setAllowedOverRoaming(true);
            req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

            long downloadId = dm.enqueue(req);
            Toast.makeText(activity, "بدأ تحميل التحديث من GitHub", Toast.LENGTH_LONG).show();

            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override public void onReceive(Context context, Intent intent) {
                    long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
                    if (id != downloadId) return;
                    try {
                        Uri apkUri = dm.getUriForDownloadedFile(downloadId);
                        if (apkUri == null) {
                            Toast.makeText(activity, "اتحمل التحديث لكن لم أجد ملف APK", Toast.LENGTH_LONG).show();
                            return;
                        }
                        Intent install = new Intent(Intent.ACTION_VIEW);
                        install.setDataAndType(apkUri, "application/vnd.android.package-archive");
                        install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        activity.startActivity(install);
                    } catch (Exception e) {
                        Toast.makeText(activity, "تم التحميل. افتح ملف APK من Downloads للتثبيت", Toast.LENGTH_LONG).show();
                    } finally {
                        try { activity.unregisterReceiver(this); } catch (Exception ignored) {}
                    }
                }
            };

            if (Build.VERSION.SDK_INT >= 33) {
                activity.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED);
            } else {
                activity.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
            }
        } catch (Exception e) {
            Toast.makeText(activity, "فشل تحميل التحديث: " + safe(e.getMessage()), Toast.LENGTH_LONG).show();
            openReleasePage(activity, info);
        }
    }

    public static void openReleasePage(Activity activity, UpdateInfo info) {
        String url = info != null && info.pageUrl != null && info.pageUrl.length() > 0 ? info.pageUrl : RELEASES_PAGE;
        activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    private static UpdateInfo parseRelease(JSONObject json) throws Exception {
        String tag = json.optString("tag_name", "");
        UpdateInfo info = new UpdateInfo();
        info.versionName = cleanVersionName(tag);
        info.versionCode = parseVersionCode(tag);
        info.notes = json.optString("body", "");
        info.pageUrl = json.optString("html_url", RELEASES_PAGE);
        JSONObject apk = findApkAsset(json.optJSONArray("assets"));
        if (apk != null) {
            info.apkUrl = apk.optString("browser_download_url", "");
            info.apkName = apk.optString("name", "WhatsVoiceBubble-update.apk");
        }
        return info;
    }

    private static JSONObject pickFirstReleaseWithApk(JSONArray arr) throws Exception {
        for (int i = 0; i < arr.length(); i++) {
            JSONObject r = arr.getJSONObject(i);
            if (findApkAsset(r.optJSONArray("assets")) != null) return r;
        }
        return null;
    }

    private static JSONObject findApkAsset(JSONArray assets) throws Exception {
        if (assets == null) return null;
        for (int i = 0; i < assets.length(); i++) {
            JSONObject a = assets.getJSONObject(i);
            String name = a.optString("name", "").toLowerCase();
            if (name.endsWith(".apk")) return a;
        }
        return null;
    }

    private static boolean isNewer(String latestName, int latestCode, String currentName, int currentCode) {
        if (latestCode > 0 && currentCode > 0) {
            if (latestCode > currentCode) return true;
            if (latestCode < currentCode) return false;
        }
        int[] latest = versionParts(latestName);
        int[] current = versionParts(currentName);
        for (int i = 0; i < Math.max(latest.length, current.length); i++) {
            int a = i < latest.length ? latest[i] : 0;
            int b = i < current.length ? current[i] : 0;
            if (a > b) return true;
            if (a < b) return false;
        }
        return false;
    }

    private static int[] versionParts(String v) {
        try {
            String cleaned = v == null ? "" : v.toLowerCase().replace("version", "").replace("v", "").trim();
            String[] p = cleaned.split("\\.");
            int[] out = new int[p.length];
            for (int i = 0; i < p.length; i++) {
                String n = p[i].replaceAll("[^0-9]", "");
                out[i] = n.length() == 0 ? 0 : Integer.parseInt(n);
            }
            return out;
        } catch (Exception e) { return new int[]{0}; }
    }

    private static int parseVersionCode(String tag) {
        int[] p = versionParts(tag);
        if (p.length == 1) return p[0];
        return 0;
    }

    private static String cleanVersionName(String tag) {
        if (tag == null || tag.trim().length() == 0) return "0";
        tag = tag.trim();
        return tag.startsWith("v") || tag.startsWith("V") ? tag.substring(1) : tag;
    }

    private static ReleaseResponse requestJson(String api) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(api).openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(20000);
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        conn.setRequestProperty("User-Agent", "WhatsVoiceBubble-Android");
        int code = conn.getResponseCode();
        InputStream is = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
        return new ReleaseResponse(code, readText(is));
    }

    private static class ReleaseResponse {
        int code; String body;
        ReleaseResponse(int c, String b) { code = c; body = b == null ? "" : b; }
    }

    private static String cleanGithubError(String body, int code) {
        try {
            JSONObject j = new JSONObject(body);
            String msg = j.optString("message", "");
            if (code == 404) return "GitHub رجع 404: تأكد إن الريبو موجود وإن فيه Release منشور: " + GITHUB_REPO;
            return "GitHub error " + code + ": " + msg;
        } catch (Exception e) {
            return "GitHub error " + code + ": " + body;
        }
    }

    private static String readText(InputStream is) throws Exception {
        if (is == null) return "";
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line).append('\n');
        return sb.toString();
    }

    private static String safeFileName(String s) {
        if (s == null) return "WhatsVoiceBubble-update.apk";
        return s.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static String safe(String s) { return s == null || s.length() == 0 ? "خطأ غير معروف" : s; }
}
