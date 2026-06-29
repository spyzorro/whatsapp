# Backend تحويل الصوت لنص

التطبيق لا يضع مفتاح OpenAI داخل APK. الأفضل تشغيل هذا السيرفر على Render / Railway / VPS ثم وضع الرابط داخل التطبيق:

```text
https://your-domain.com/transcribe
```

## التشغيل محليًا

```bash
cd backend
pip install -r requirements.txt
set OPENAI_API_KEY=ضع_المفتاح_هنا
python server.py
```

على لينكس/ماك:

```bash
export OPENAI_API_KEY=ضع_المفتاح_هنا
python server.py
```

## التشغيل على سيرفر

الأمر المقترح:

```bash
gunicorn server:app --bind 0.0.0.0:$PORT
```
