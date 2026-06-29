# WhatsVoiceBubble

تطبيق أندرويد بفقاعة عائمة فوق واتساب لتحويل آخر ريكورد محفوظ إلى نص، مع زر فحص تحديث من GitHub Releases.

## Build APK

GitHub Actions يبني APK تلقائيًا مع كل push.

بعد نجاح الـ Action، حمّل الـ APK من:

Actions → آخر Run → Artifacts → WhatsVoiceBubble-debug-apk

## تحديث التطبيق

1. زوّد `versionCode` و `versionName` في `app/build.gradle`.
2. اعمل Tag مثل `v1.2.0`.
3. GitHub Actions سينشئ Release ويرفع APK.
4. داخل التطبيق اضغط زر فحص التحديث.

## مهم

الريكورد لازم يكون متحمّل فعلًا على الجهاز؛ التطبيق لا يستطيع تحميل ريكورد غير محفوظ من سيرفرات واتساب مباشرة.
