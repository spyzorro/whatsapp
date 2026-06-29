package com.example.whatsvoicebubble;

import android.content.Context;
import android.content.SharedPreferences;

public class AppSettings {
    private static final String PREFS = "wvb_settings";
    private static final String KEY_BACKEND = "backend_url";
    private static final String KEY_TREE_URI = "voice_tree_uri";
    private static final String KEY_LAST_AUDIO_URI = "last_audio_uri";
    private static final String KEY_SPEECH_LANGUAGE = "speech_language";

    public static String getBackendUrl(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return p.getString(KEY_BACKEND, "").trim();
    }

    public static void setBackendUrl(Context ctx, String url) {
        if (url == null) url = "";
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_BACKEND, url.trim())
                .apply();
    }

    public static String getVoiceTreeUri(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TREE_URI, "");
    }

    public static void setVoiceTreeUri(Context ctx, String uri) {
        if (uri == null) uri = "";
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_TREE_URI, uri)
                .apply();
    }

    public static String getLastAudioUri(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LAST_AUDIO_URI, "");
    }

    public static void setLastAudioUri(Context ctx, String uri) {
        if (uri == null) uri = "";
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LAST_AUDIO_URI, uri)
                .apply();
    }

    public static String getSpeechLanguage(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_SPEECH_LANGUAGE, "ar");
    }

    public static void setSpeechLanguage(Context ctx, String lang) {
        if (lang == null || lang.trim().length() == 0) lang = "ar";
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_SPEECH_LANGUAGE, lang.trim())
                .apply();
    }
}
