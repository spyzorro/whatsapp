package com.example.whatsvoicebubble;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

public class WhatsAppVoiceFinder {
    public static Uri findLatestVoiceNote(Context ctx, long maxAgeMs) {
        long minDateAddedSeconds = (System.currentTimeMillis() - maxAgeMs) / 1000L;
        Uri collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        String[] projection = new String[]{
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.RELATIVE_PATH,
                MediaStore.Audio.Media.MIME_TYPE
        };

        String selection = MediaStore.Audio.Media.DATE_ADDED + " >= ? AND (" +
                MediaStore.Audio.Media.RELATIVE_PATH + " LIKE ? OR " +
                MediaStore.Audio.Media.DISPLAY_NAME + " LIKE ? OR " +
                MediaStore.Audio.Media.MIME_TYPE + " LIKE ? )";

        String[] args = new String[]{
                String.valueOf(minDateAddedSeconds),
                "%WhatsApp%Voice%",
                "%opus%",
                "%ogg%"
        };

        String sort = MediaStore.Audio.Media.DATE_ADDED + " DESC";

        try (Cursor c = ctx.getContentResolver().query(collection, projection, selection, args, sort)) {
            if (c == null) return null;
            int idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            int nameCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
            int pathCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH);
            while (c.moveToNext()) {
                String name = safe(c.getString(nameCol)).toLowerCase();
                String path = safe(c.getString(pathCol)).toLowerCase();
                boolean looksLikeWhatsApp = path.contains("whatsapp") || path.contains("com.whatsapp");
                boolean looksLikeVoice = path.contains("voice") || name.endsWith(".opus") || name.endsWith(".ogg");
                if (looksLikeWhatsApp && looksLikeVoice) {
                    long id = c.getLong(idCol);
                    return Uri.withAppendedPath(collection, String.valueOf(id));
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String safe(String s) { return s == null ? "" : s; }
}
