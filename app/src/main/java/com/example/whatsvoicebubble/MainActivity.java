package com.example.whatsvoicebubble;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int REQ_AUDIO_FILE = 201;
    private static final int REQ_VOICE_TREE = 202;

    private TextView status;
    private EditText backendInput;
    private EditText languageInput;

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
                "تم تعديل النسخة لتعمل بدون Accessibility عشان Android مايعرضش إعداد محظور.\n\n" +
                "طرق التحويل المتاحة:\n" +
                "1) البحث في MediaStore Audio عن آخر ريكورد واتساب.\n" +
                "2) البحث في MediaStore Files عن ملفات OPUS/OGG/M4A.\n" +
                "3) اختيار فولدر WhatsApp Voice Notes مرة واحدة بصلاحية آمنة.\n" +
                "4) اختيار ملف صوت يدوي للتجربة.\n" +
                "5) تشغيل الريكورد وسماعه من الميكروفون بدون Backend.\n" +
                "6) تشغيل الريكورد وتسجيله ثم إرساله للـ Backend.\n\n" +
                "مهم: تحويل ملف صوت محفوظ لنص يحتاج Backend URL. التحويل المباشر من الميكروفون لا يحتاج Backend لكنه أقل دقة.");
        title.setTextSize(18);
        root.addView(title);

        status = new TextView(this);
        status.setText("\nالإصدار الحالي: " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")\n");
        status.setTextSize(16);
        root.addView(status);

        TextView backendLabel = new TextView(this);
        backendLabel.setText("رابط Backend التحويل لنص:");
        backendLabel.setTextSize(16);
        root.addView(backendLabel);

        backendInput = new EditText(this);
        backendInput.setSingleLine(true);
        backendInput.setHint("https://your-domain.com/transcribe");
        backendInput.setText(AppSettings.getBackendUrl(this));
        root.addView(backendInput);

        Button saveBackendBtn = new Button(this);
        saveBackendBtn.setText("حفظ رابط Backend");
        saveBackendBtn.setOnClickListener(v -> {
            AppSettings.setBackendUrl(this, backendInput.getText().toString());
            Toast.makeText(this, "تم حفظ رابط Backend", Toast.LENGTH_SHORT).show();
        });
        root.addView(saveBackendBtn);

        TextView languageLabel = new TextView(this);
        languageLabel.setText("لغة التحويل المباشر من الميكروفون مثل ar أو en-US:");
        languageLabel.setTextSize(16);
        root.addView(languageLabel);

        languageInput = new EditText(this);
        languageInput.setSingleLine(true);
        languageInput.setHint("ar");
        languageInput.setText(AppSettings.getSpeechLanguage(this));
        root.addView(languageInput);

        Button saveLangBtn = new Button(this);
        saveLangBtn.setText("حفظ اللغة");
        saveLangBtn.setOnClickListener(v -> {
            AppSettings.setSpeechLanguage(this, languageInput.getText().toString());
            Toast.makeText(this, "تم حفظ اللغة", Toast.LENGTH_SHORT).show();
        });
        root.addView(saveLangBtn);

        Button overlayBtn = new Button(this);
        overlayBtn.setText("تفعيل الظهور فوق التطبيقات");
        overlayBtn.setOnClickListener(v -> {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(i);
        });
        root.addView(overlayBtn);

        Button startBtn = new Button(this);
        startBtn.setText("تشغيل الفقاعة بدون Accessibility");
        startBtn.setOnClickListener(v -> {
            startService(new Intent(this, BubbleOverlayService.class));
            Toast.makeText(this, "تم تشغيل الفقاعة. افتح واتساب واضغط عليها.", Toast.LENGTH_LONG).show();
        });
        root.addView(startBtn);

        Button treeBtn = new Button(this);
        treeBtn.setText("اختيار فولدر ريكوردات واتساب مرة واحدة");
        treeBtn.setOnClickListener(v -> openVoiceTreePicker());
        root.addView(treeBtn);

        Button pickAudioBtn = new Button(this);
        pickAudioBtn.setText("اختيار ملف صوت يدوي وتحويله");
        pickAudioBtn.setOnClickListener(v -> openAudioPicker());
        root.addView(pickAudioBtn);

        Button updateBtn = new Button(this);
        updateBtn.setText("فحص التحديث وتحميله من GitHub");
        updateBtn.setOnClickListener(v -> {
            status.setText("جاري فحص التحديث من GitHub...");
            UpdateChecker.check(this, new UpdateChecker.Callback() {
                @Override public void onResult(UpdateChecker.UpdateInfo info) {
                    runOnUiThread(() -> {
                        if (info.available) {
                            status.setText("يوجد تحديث جديد: " + info.versionName + "\n" + info.notes + "\n\nجاري فتح التحميل من GitHub...");
                            UpdateChecker.downloadAndInstall(MainActivity.this, info);
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

        requestNeededPermissions();
        setContentView(scroll);
    }

    private void openAudioPicker() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("audio/*");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, REQ_AUDIO_FILE);
    }

    private void openVoiceTreePicker() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        startActivityForResult(i, REQ_VOICE_TREE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try { getContentResolver().takePersistableUriPermission(uri, flags); } catch (Exception ignored) {}

        if (requestCode == REQ_VOICE_TREE) {
            AppSettings.setVoiceTreeUri(this, uri.toString());
            status.setText("تم حفظ فولدر ريكوردات واتساب. افتح واتساب واضغط الفقاعة، أو اضغط تحويل ذكي.");
            return;
        }

        if (requestCode == REQ_AUDIO_FILE) {
            AppSettings.setLastAudioUri(this, uri.toString());
            status.setText("تم اختيار ملف الصوت. جاري التحويل...");
            new Thread(() -> {
                try {
                    String text = TranscriptionClient.transcribe(this, uri);
                    runOnUiThread(() -> status.setText(text));
                } catch (Exception e) {
                    runOnUiThread(() -> status.setText("فشل تحويل الملف: " + (e.getMessage() == null ? e.toString() : e.getMessage())));
                }
            }).start();
        }
    }

    private void requestNeededPermissions() {
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS}, 10);
        } else {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, 10);
        }
    }
}
