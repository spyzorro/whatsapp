package com.example.whatsvoicebubble;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BubbleOverlayService extends Service {
    private WindowManager wm;
    private Button bubble;
    private LinearLayout resultBox;
    private volatile boolean busy = false;

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        showBubble();
        return START_STICKY;
    }

    private WindowManager.LayoutParams overlayParams(int w, int h, int gravity) {
        int type = android.os.Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                w, h, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        p.gravity = gravity;
        p.x = 24;
        p.y = 260;
        return p;
    }

    private void showBubble() {
        if (bubble != null) return;
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        bubble = new Button(this);
        bubble.setText("نص");
        bubble.setTextSize(14);
        WindowManager.LayoutParams p = overlayParams(150, 150, Gravity.TOP | Gravity.START);

        bubble.setOnClickListener(v -> showActionMenu());
        bubble.setOnTouchListener(new DragTouchListener(p));
        wm.addView(bubble, p);
    }

    private void showActionMenu() {
        showResult("اختار طريقة التحويل:\n\n" +
                "• تحويل ذكي: يجرب آخر ريكورد محفوظ + فولدر واتساب المختار + آخر ملف يدوي.\n" +
                "• اسمع الريكورد بدون سيرفر: شغّل الريكورد والتطبيق يحوله من الميكروفون.\n" +
                "• تسجيل ثم Backend: شغّل الريكورد، التطبيق يسجله ثم يرسله للسيرفر.", false);

        addButtonToResult("تحويل ذكي بكل الطرق", v -> transcribeSmartAllSources());
        addButtonToResult("اسمع الريكورد بدون سيرفر", v -> listenLiveWithoutBackend());
        addButtonToResult("تسجيل الريكورد ثم Backend", v -> recordPlaybackThenBackend());
        addButtonToResult("إغلاق الفقاعة", v -> stopSelfAndRemove());
    }

    private void transcribeSmartAllSources() {
        if (busy) {
            showResult("في عملية تحويل شغالة حاليًا. استنى لما تخلص أو اقفل النافذة وجرب تاني.", false);
            return;
        }
        busy = true;
        showResult("جاري تجربة أكثر من طريقة للوصول للريكورد...", false);
        new Thread(() -> {
            try {
                VoiceCandidate c = findBestCandidate();
                if (c != null) {
                    postResult("تم العثور على ريكورد من: " + c.source + "\nجاري التحويل لنص...", false);
                    String text = TranscriptionClient.transcribe(this, c.uri);
                    postResult(text, true);
                    busy = false;
                    return;
                }

                if (AppSettings.getBackendUrl(this).length() == 0) {
                    postResult("لم أجد ملف ريكورد محفوظ.\n\nهبدأ وضع التحويل المباشر من الميكروفون بدون Backend. شغّل الريكورد في واتساب وارفع الصوت.", false);
                    busy = false;
                    listenLiveWithoutBackend();
                } else {
                    postResult("لم أجد ملف ريكورد محفوظ.\n\nهبدأ وضع تسجيل الصوت أثناء تشغيل الريكورد ثم إرساله للـ Backend. شغّل الريكورد في واتساب وارفع الصوت.", false);
                    busy = false;
                    recordPlaybackThenBackend();
                }
            } catch (Exception e) {
                postResult("فشل التحويل: " + safe(e.getMessage()), false);
                busy = false;
            }
        }).start();
    }

    private VoiceCandidate findBestCandidate() {
        long age = 24 * 60 * 60 * 1000L;
        VoiceCandidate best = null;
        best = newer(best, WhatsAppVoiceFinder.findLatestCandidate(this, age));
        best = newer(best, WhatsAppFilesVoiceFinder.findLatestCandidate(this, age));
        best = newer(best, SafVoiceFinder.findLatestFromSavedTree(this, 7 * age));

        String last = AppSettings.getLastAudioUri(this);
        if (last != null && last.length() > 0) {
            try { best = newer(best, new VoiceCandidate(Uri.parse(last), "آخر ملف صوت اختارته يدويًا", System.currentTimeMillis())); }
            catch (Exception ignored) {}
        }
        return best;
    }

    private VoiceCandidate newer(VoiceCandidate a, VoiceCandidate b) {
        if (b == null) return a;
        if (a == null) return b;
        return b.modified >= a.modified ? b : a;
    }

    private void listenLiveWithoutBackend() {
        if (busy) {
            showResult("في عملية شغالة حاليًا.", false);
            return;
        }
        busy = true;
        showResult("خلال ثواني شغّل الريكورد في واتساب وارفع صوت الموبايل.\n\nهذه الطريقة لا تحتاج Backend، لكنها تعتمد على وضوح الصوت من السماعة.", false);
        LiveSpeechRecognizerHelper.listenFromMic(this, 60, new LiveSpeechRecognizerHelper.Callback() {
            @Override public void onMessage(String message) { postResult(message, false); }
            @Override public void onFinalText(String text) { postResult(text, true); busy = false; }
            @Override public void onError(String error) { postResult("فشل التحويل المباشر: " + error, false); busy = false; }
        });
    }

    private void recordPlaybackThenBackend() {
        if (busy) {
            showResult("في عملية شغالة حاليًا.", false);
            return;
        }
        busy = true;
        showResult("شغّل الريكورد في واتساب وارفع صوت الموبايل. التطبيق هيسجل الصوت ثم يرسله للـ Backend.", false);
        AudioFallbackRecorder.recordMicForWhatsAppPlayback(this, 60, new AudioFallbackRecorder.Callback() {
            @Override public void onTick(String message) { postResult(message, false); }

            @Override public void onRecorded(Uri uri) {
                new Thread(() -> {
                    try {
                        postResult("تم تسجيل الصوت. جاري التحويل لنص...", false);
                        String text = TranscriptionClient.transcribe(BubbleOverlayService.this, uri);
                        postResult(text, true);
                    } catch (Exception e) {
                        postResult("فشل تحويل التسجيل: " + safe(e.getMessage()), false);
                    } finally {
                        busy = false;
                    }
                }).start();
            }

            @Override public void onError(String error) {
                postResult("لم أقدر أسجل الريكورد: " + error + "\n\nارفع صوت الموبايل وشغّل الريكورد واضغط الفقاعة مرة ثانية.", false);
                busy = false;
            }
        });
    }

    private void stopSelfAndRemove() {
        try { if (resultBox != null) wm.removeView(resultBox); } catch (Exception ignored) {}
        try { if (bubble != null) wm.removeView(bubble); } catch (Exception ignored) {}
        resultBox = null;
        bubble = null;
        stopSelf();
    }

    private String safe(String s) { return s == null || s.length() == 0 ? "خطأ غير معروف" : s; }

    private void postResult(String s, boolean finalText) {
        android.os.Handler h = new android.os.Handler(getMainLooper());
        h.post(() -> showResult(s, finalText));
    }

    private void addButtonToResult(String label, android.view.View.OnClickListener listener) {
        if (resultBox == null) return;
        Button b = new Button(this);
        b.setText(label);
        b.setOnClickListener(listener);
        resultBox.addView(b);
    }

    private void showResult(String text, boolean finalText) {
        if (wm == null) wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (resultBox != null) {
            try { wm.removeView(resultBox); } catch (Exception ignored) {}
        }

        resultBox = new LinearLayout(this);
        resultBox.setOrientation(LinearLayout.VERTICAL);
        resultBox.setPadding(28, 24, 28, 24);
        resultBox.setBackgroundColor(finalText ? 0xF2E8FFF1 : 0xF2FFFFFF);

        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(16);
        resultBox.addView(tv);

        Button retry = new Button(this);
        retry.setText("فتح الطرق");
        retry.setOnClickListener(v -> {
            busy = false;
            showActionMenu();
        });
        resultBox.addView(retry);

        Button close = new Button(this);
        close.setText("إغلاق");
        close.setOnClickListener(v -> {
            if (resultBox != null) {
                try { wm.removeView(resultBox); } catch (Exception ignored) {}
                resultBox = null;
            }
        });
        resultBox.addView(close);

        WindowManager.LayoutParams p = overlayParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.START);
        wm.addView(resultBox, p);
    }

    private class DragTouchListener implements android.view.View.OnTouchListener {
        private final WindowManager.LayoutParams p;
        private int startX, startY;
        private float downX, downY;
        DragTouchListener(WindowManager.LayoutParams p) { this.p = p; }
        @Override public boolean onTouch(android.view.View v, MotionEvent e) {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startX = p.x; startY = p.y; downX = e.getRawX(); downY = e.getRawY(); return false;
                case MotionEvent.ACTION_MOVE:
                    p.x = startX + (int) (e.getRawX() - downX);
                    p.y = startY + (int) (e.getRawY() - downY);
                    wm.updateViewLayout(v, p); return true;
            }
            return false;
        }
    }
}
