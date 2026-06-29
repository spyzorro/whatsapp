package com.example.whatsvoicebubble;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

public class WhatsAppVoiceFinder {
    public static Uri findLatestVoiceNote(Context ctx, long maxAgeMs) {
        VoiceCandidate c = findLatestCandidate(ctx, maxAgeMs);
        return c == null ? null : c.uri;
    }

    public static VoiceCandidate findLatestCandidate(Context ctx, long maxAgeMs) {
        long minDateAddedSeconds = (System.currentTimeMillis() - maxAgeMs) / 1000L;
        Uri collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        String[] projection = new String[]{
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.DATE_MODIFIED,
                MediaStore.Audio.Media.RELATIVE_PATH,
                MediaStore.Audio.Media.MIME_TYPE
        };

        String selection = MediaStore.Audio.Media.DATE_ADDED + " >= ?";
        String[] args = new String[]{String.valueOf(minDateAddedSeconds)};
        String sort = MediaStore.Audio.Media.DATE_ADDED + " DESC";

        try (Cursor c = ctx.getContentResolver().query(collection, projection, selection, args, sort)) {
            if (c == null) return null;
            int idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            int nameCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
            int dateCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED);
            int pathCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH);
            int mimeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE);

            while (c.moveToNext()) {
                String name = safe(c.getString(nameCol)).toLowerCase();
                String path = safe(c.getString(pathCol)).toLowerCase();
                String mime = safe(c.getString(mimeCol)).toLowerCase();
                if (looksLikeWhatsAppVoice(name, path, mime)) {
                    long id = c.getLong(idCol);
                    long modified = c.getLong(dateCol) * 1000L;
                    Uri uri = ContentUris.withAppendedId(collection, id);
                    return new VoiceCandidate(uri, "MediaStore Audio - آخر ريكورد واتساب", modified);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    static boolean looksLikeWhatsAppVoice(String name, String path, String mime) {
        name = safe(name).toLowerCase();
        path = safe(path).toLowerCase();
        mime = safe(mime).toLowerCase();
        boolean wa = path.contains("whatsapp") || path.contains("com.whatsapp") || path.contains("whatsapp voice notes");
        boolean voice = path.contains("voice") || path.contains("ptt") || name.endsWith(".opus") || name.endsWith(".ogg") || name.endsWith(".m4a") || name.startsWith("ptt-");
        boolean audio = mime.startsWith("audio") || mime.contains("ogg") || mime.contains("opus") || mime.contains("mp4");
        return wa && voice && (audio || name.endsWith(".opus") || name.endsWith(".ogg") || name.endsWith(".m4a"));
    }

    private static String safe(String s) { return s == null ? "" : s; }
}
