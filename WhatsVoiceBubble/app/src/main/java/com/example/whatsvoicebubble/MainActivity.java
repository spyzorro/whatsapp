package com.example.whatsvoicebubble;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private TextView status;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 60, 40, 40);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("Whats Voice Bubble\n\n" +
                "التطبيق يحاول تحويل أحدث ريكورد واتساب محفوظ على الجهاز إلى نص بدون Share.\n\n" +
                "طريقة الاستخدام:\n" +
                "1) فعّل الظهور فوق التطبيقات.\n" +
                "2) فعّل Accessibility للتطبيق.\n" +
                "3) افتح واتساب.\n" +
                "4) اضغط الفقاعة عند الشات الذي يحتوي على الريكورد.\n\n" +
                "مهم: لو الريكورد غير محمّل على الجهاز، التطبيق الخارجي لا يستطيع تحميله من سيرفرات واتساب مباشرة.");
        title.setTextSize(18);
        root.addView(title);

        status = new TextView(this);
        status.setText("\nالإصدار الحالي: " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")\n");
        status.setTextSize(16);
        root.addView(status);

        Button overlayBtn = new Button(this);
        overlayBtn.setText("تفعيل الظهور فوق التطبيقات");
        overlayBtn.setOnClickListener(v -> {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(i);
        });
        root.addView(overlayBtn);

        Button accBtn = new Button(this);
        accBtn.setText("فتح إعدادات Accessibility");
        accBtn.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        root.addView(accBtn);

        Button startBtn = new Button(this);
        startBtn.setText("تشغيل الفقاعة الآن");
        startBtn.setOnClickListener(v -> {
            startService(new Intent(this, BubbleOverlayService.class));
            Toast.makeText(this, "تم تشغيل الفقاعة", Toast.LENGTH_SHORT).show();
        });
        root.addView(startBtn);

        Button updateBtn = new Button(this);
        updateBtn.setText("فحص التحديث من GitHub");
        updateBtn.setOnClickListener(v -> {
            status.setText("جاري فحص التحديث من GitHub...");
            UpdateChecker.check(this, new UpdateChecker.Callback() {
                @Override public void onResult(UpdateChecker.UpdateInfo info) {
                    runOnUiThread(() -> {
                        if (info.available) {
                            status.setText("يوجد تحديث جديد: " + info.versionName + "\n" + info.notes);
                            UpdateChecker.openDownloadPage(MainActivity.this, info);
                        } else {
                            status.setText("أنت على آخر إصدار. الإصدار الحالي: " + BuildConfig.VERSION_NAME);
                        }
                    });
                }

                @Override public void onError(String error) {
                    runOnUiThread(() -> status.setText("فشل فحص التحديث: " + error));
                }
            });
        });
        root.addView(updateBtn);

        requestStoragePermission();
        setContentView(scroll);
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{Manifest.permission.READ_MEDIA_AUDIO}, 10);
        } else {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 10);
        }
    }
}
