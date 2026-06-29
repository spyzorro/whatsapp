package com.example.whatsvoicebubble;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

public class SafVoiceFinder {
    public static VoiceCandidate findLatestFromSavedTree(Context ctx, long maxAgeMs) {
        String tree = AppSettings.getVoiceTreeUri(ctx);
        if (tree == null || tree.length() == 0) return null;
        try {
            Uri treeUri = Uri.parse(tree);
            String rootDocId = DocumentsContract.getTreeDocumentId(treeUri);
            Holder h = new Holder();
            long min = System.currentTimeMillis() - maxAgeMs;
            scanChildren(ctx, treeUri, rootDocId, 0, min, h);
            if (h.uri != null) return new VoiceCandidate(h.uri, "SAF - فولدر واتساب المختار", h.modified);
        } catch (Exception ignored) {}
        return null;
    }

    private static void scanChildren(Context ctx, Uri treeUri, String parentDocId, int depth, long minModified, Holder holder) {
        if (depth > 5) return;
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId);
        String[] projection = new String[]{
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                DocumentsContract.Document.COLUMN_SIZE
        };
        try (Cursor c = ctx.getContentResolver().query(childrenUri, projection, null, null, null)) {
            if (c == null) return;
            int idCol = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID);
            int nameCol = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME);
            int mimeCol = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE);
            int modCol = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED);
            int sizeCol = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE);

            while (c.moveToNext()) {
                String id = c.getString(idCol);
                String name = safe(c.getString(nameCol));
                String mime = safe(c.getString(mimeCol));
                long modified = c.getLong(modCol);
                long size = c.getLong(sizeCol);
                if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) {
                    scanChildren(ctx, treeUri, id, depth + 1, minModified, holder);
                } else if (size > 1024 && modified >= minModified && looksLikeAudio(name, mime)) {
                    if (modified > holder.modified) {
                        holder.modified = modified;
                        holder.uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private static boolean looksLikeAudio(String name, String mime) {
        name = safe(name).toLowerCase();
        mime = safe(mime).toLowerCase();
        return mime.startsWith("audio") || name.endsWith(".opus") || name.endsWith(".ogg") || name.endsWith(".m4a") || name.endsWith(".mp3") || name.startsWith("ptt-");
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private static class Holder { Uri uri; long modified; }
}
