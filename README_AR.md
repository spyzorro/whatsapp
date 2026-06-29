# Whats Voice Bubble

تطبيق أندرويد يعرض فقاعة فوق واتساب، وعند الضغط عليها يحاول جلب أحدث ريكورد واتساب محفوظ على الجهاز وتحويله إلى نص بدون عمل Share للريكورد.

## المهم

التطبيق لا يكسر تشفير واتساب ولا يسحب الريكورد من سيرفرات واتساب. لكي يعمل بدون Share، يجب أن يكون الريكورد محمّلًا ومحفوظًا على الجهاز.

## المميزات الموجودة

- فقاعة عائمة فوق واتساب.
- Accessibility Service لمعرفة أن واتساب مفتوح.
- قراءة أحدث Voice Note محفوظ من MediaStore.
- نافذة عائمة تعرض النص فوق الشات.
- زر نسخ النص.
- زر فحص التحديث من GitHub Releases.
- GitHub Actions يبني APK تلقائيًا عند كل Push.
- عند عمل Tag مثل `v3.0.0` يتم إنشاء Release ورفع APK تلقائيًا.

## التحديث عن طريق GitHub

زر **فحص التحديث من GitHub** داخل التطبيق يقرأ آخر Release من:

`https://api.github.com/repos/spyzorro/-/releases/latest`

لو رقم الإصدار في التاج أعلى من `versionCode` الحالي، يفتح رابط تحميل APK من آخر Release.

الصيغة المقترحة للتاج:

```bash
git tag v3.0.0
git push origin v3.0.0
```

مهم: الرقم الأول بعد `v` يعتبر `versionCode` داخل التطبيق. مثال: `v3.0.0` يعني versionCode = 3.

## البناء على GitHub Actions

أي Push على `main` سيبني APK ويرفعه كـ Artifact.

أي Push لتاج يبدأ بـ `v` سيبني APK وينشئ Release ويرفع `WhatsVoiceBubble-debug.apk`.

## ربط تحويل الصوت لنص

افتح الملف:

`app/src/main/java/com/example/whatsvoicebubble/TranscriptionClient.java`

وغيّر:

```java
private static final String BACKEND_TRANSCRIBE_URL = "https://YOUR_BACKEND_DOMAIN/transcribe";
```

لا تضع مفتاح OpenAI أو Google داخل تطبيق الأندرويد. الأفضل عمل Backend يستقبل ملف الصوت ويرجع النص.
