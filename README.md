# Whats Voice Bubble v1.3.0

Android starter app for converting WhatsApp voice notes to text using a floating bubble.

## v1.3.0 changes

- Removed Accessibility Service to avoid Android restricted settings warnings for sideloaded APKs.
- Bubble now runs manually/always without Accessibility.
- Added multiple conversion routes:
  - MediaStore Audio lookup.
  - MediaStore Files lookup.
  - SAF folder picker for WhatsApp Voice Notes.
  - Manual audio file picker.
  - Live microphone speech recognition without backend.
  - Microphone recording fallback sent to backend.
- GitHub update checker remains wired to `spyzorro/whatsapp`.

## Build

Upload repo contents to the root of `spyzorro/whatsapp` and run GitHub Actions `Build APK`.

## Backend

Run `backend/server.py` with `OPENAI_API_KEY` and set the `/transcribe` URL inside the app.
