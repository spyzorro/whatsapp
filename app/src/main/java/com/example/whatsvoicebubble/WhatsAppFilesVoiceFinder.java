package com.example.whatsvoicebubble;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

public class WhatsAppFilesVoiceFinder {
    public static VoiceCandidate findLatestCandidate(Context ctx, long maxAgeMs) {
        long minDateModifiedSeconds = (System.currentTimeMillis() - maxAgeMs) / 1000L;
        Uri collection = MediaStore.Files.getContentUri("external");

        String[] projection = new String[]{
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.DATE_MODIFIED,
                MediaStore.Files.FileColumns.RELATIVE_PATH,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.SIZE
        };

        String selection = MediaStore.Files.FileColumns.DATE_MODIFIED + " >= ? AND " +
                MediaStore.Files.FileColumns.SIZE + " > ?";
        String[] args = new String[]{String.valueOf(minDateModifiedSeconds), "1024"};
        String sort = MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC";

        try (Cursor c = ctx.getContentResolver().query(collection, projection, selection, args, sort)) {
            if (c == null) return null;
            int idCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID);
            int nameCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME);
            int modCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED);
            int pathCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.RELATIVE_PATH);
            int mimeCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE);

            while (c.moveToNext()) {
                String name = safe(c.getString(nameCol)).toLowerCase();
                String path = safe(c.getString(pathCol)).toLowerCase();
                String mime = safe(c.getString(mimeCol)).toLowerCase();
                if (WhatsAppVoiceFinder.looksLikeWhatsAppVoice(name, path, mime)) {
                    long id = c.getLong(idCol);
                    long modified = c.getLong(modCol) * 1000L;
                    Uri uri = ContentUris.withAppendedId(collection, id);
                    return new VoiceCandidate(uri, "MediaStore Files - ملف واتساب الصوتي", modified);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String safe(String s) { return s == null ? "" : s; }
}
