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

        bubble.setOnClickListener(v -> transcribeLatestWhatsAppVoiceNote());
        bubble.setOnTouchListener(new DragTouchListener(p));
        wm.addView(bubble, p);
    }

    private void transcribeLatestWhatsAppVoiceNote() {
        showResult("جاري البحث عن آخر ريكورد واتساب محفوظ...");
        new Thread(() -> {
            try {
                Uri audio = WhatsAppVoiceFinder.findLatestVoiceNote(this, 60 * 60 * 1000L);
                if (audio == null) {
                    postResult("لم أجد ريكورد واتساب محفوظ خلال آخر ساعة. افتح الشات واضغط تحميل للريكورد ثم جرّب مرة ثانية.");
                    return;
                }
                postResult("تم العثور على الريكورد. جاري التحويل لنص...");
                String text = TranscriptionClient.transcribe(this, audio);
                postResult(text);
            } catch (Exception e) {
                postResult("فشل التحويل: " + e.getMessage());
            }
        }).start();
    }

    private void postResult(String s) { android.os.Handler h = new android.os.Handler(getMainLooper()); h.post(() -> showResult(s)); }

    private void showResult(String text) {
        if (wm == null) wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (resultBox != null) wm.removeView(resultBox);

        resultBox = new LinearLayout(this);
        resultBox.setOrientation(LinearLayout.VERTICAL);
        resultBox.setPadding(28, 24, 28, 24);
        resultBox.setBackgroundColor(0xEEFFFFFF);

        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(16);
        resultBox.addView(tv);

        Button copy = new Button(this);
        copy.setText("نسخ النص");
        copy.setOnClickListener(v -> {
            android.content.ClipboardManager cb = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            cb.setPrimaryClip(android.content.ClipData.newPlainText("transcript", tv.getText()));
        });
        resultBox.addView(copy);

        Button close = new Button(this);
        close.setText("إغلاق");
        close.setOnClickListener(v -> { if (resultBox != null) { wm.removeView(resultBox); resultBox = null; } });
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
