package com.example.whatsvoicebubble;

import android.content.Context;
import android.media.MediaRecorder;
import android.net.Uri;

import java.io.File;

public class AudioFallbackRecorder {
    public interface Callback {
        void onTick(String message);
        void onRecorded(Uri uri);
        void onError(String error);
    }

    public static void recordMicForWhatsAppPlayback(Context ctx, int seconds, Callback cb) {
        new Thread(() -> {
            MediaRecorder recorder = null;
            try {
                File dir = new File(ctx.getCacheDir(), "fallback_recordings");
                if (!dir.exists()) dir.mkdirs();
                File out = new File(dir, "whatsapp_playback_" + System.currentTimeMillis() + ".m4a");

                cb.onTick("مش لاقي ملف ريكورد محفوظ. افتح الريكورد في واتساب الآن. التسجيل هيبدأ بعد 3 ثواني...");
                Thread.sleep(3000);

                recorder = new MediaRecorder();
                recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                recorder.setAudioEncodingBitRate(128000);
                recorder.setAudioSamplingRate(44100);
                recorder.setOutputFile(out.getAbsolutePath());
                recorder.prepare();
                recorder.start();

                cb.onTick("جاري سماع الريكورد من صوت الموبايل لمدة " + seconds + " ثانية. شغّل الريكورد الآن لو لسه مش شغال...");
                Thread.sleep(seconds * 1000L);

                try { recorder.stop(); } catch (Exception ignored) {}
                try { recorder.release(); } catch (Exception ignored) {}
                recorder = null;

                if (!out.exists() || out.length() < 1024) {
                    cb.onError("لم يتم تسجيل صوت واضح. ارفع صوت الموبايل وشغّل الريكورد ثم جرّب مرة ثانية.");
                    return;
                }
                cb.onRecorded(Uri.fromFile(out));
            } catch (Exception e) {
                cb.onError(e.getMessage() == null ? e.toString() : e.getMessage());
            } finally {
                if (recorder != null) {
                    try { recorder.release(); } catch (Exception ignored) {}
                }
            }
        }).start();
    }
}
