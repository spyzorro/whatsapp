import os
import tempfile
from flask import Flask, request, Response
from openai import OpenAI

app = Flask(__name__)
client = OpenAI(api_key=os.environ.get("OPENAI_API_KEY"))

@app.post('/transcribe')
def transcribe():
    if not os.environ.get("OPENAI_API_KEY"):
        return Response("OPENAI_API_KEY غير موجود على السيرفر", status=500)

    audio = request.get_data()
    if not audio:
        return Response("لم يصل أي ملف صوت", status=400)

    content_type = request.headers.get('Content-Type', 'audio/ogg')
    ext = '.ogg'
    if 'mp4' in content_type or 'm4a' in content_type:
        ext = '.m4a'
    elif 'mpeg' in content_type or 'mp3' in content_type:
        ext = '.mp3'
    elif 'wav' in content_type:
        ext = '.wav'

    with tempfile.NamedTemporaryFile(delete=False, suffix=ext) as f:
        f.write(audio)
        path = f.name

    try:
        with open(path, 'rb') as f:
            result = client.audio.transcriptions.create(
                model=os.environ.get("TRANSCRIBE_MODEL", "gpt-4o-mini-transcribe"),
                file=f,
                response_format="text"
            )
        return Response(str(result).strip(), mimetype='text/plain; charset=utf-8')
    finally:
        try:
            os.remove(path)
        except OSError:
            pass

@app.get('/')
def home():
    return 'WhatsVoiceBubble backend is running. POST audio bytes to /transcribe', 200

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=int(os.environ.get('PORT', '8080')))
