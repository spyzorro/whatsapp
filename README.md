# WhatsVoiceBubble GitHub Build Package

المشروع داخل مجلد `WhatsVoiceBubble`.

GitHub Actions داخل `.github/workflows/build-apk.yml` وسيبني APK تلقائيًا.

- Push على `main` => APK كـ Artifact.
- Tag يبدأ بـ `v` مثل `v3.0.0` => Release + APK.

زر التحديث داخل التطبيق يعتمد على آخر Release في GitHub.
لو غيرت اسم الريبو، عدّل `GITHUB_REPO` داخل:

`WhatsVoiceBubble/app/src/main/java/com/example/whatsvoicebubble/UpdateChecker.java`
