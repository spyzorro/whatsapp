package com.example.whatsvoicebubble;

import android.net.Uri;

public class VoiceCandidate {
    public final Uri uri;
    public final String source;
    public final long modified;

    public VoiceCandidate(Uri uri, String source, long modified) {
        this.uri = uri;
        this.source = source == null ? "مصدر غير معروف" : source;
        this.modified = modified;
    }
}
