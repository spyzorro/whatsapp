package com.example.whatsvoicebubble;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;
import java.util.Locale;

public class LiveSpeechRecognizerHelper {
    public interface Callback {
        void onMessage(String message);
        void onFinalText(String text);
        void onError(String error);
    }

    public static void listenFromMic(Context ctx, int seconds, Callback cb) {
        Handler main = new Handler(Looper.getMainLooper());
        main.post(() -> {
            try {
                if (!SpeechRecognizer.isRecognitionAvailable(ctx)) {
                    cb.onError("خدمة التعرف على الكلام غير متاحة على هذا الجهاز. استخدم Backend أو اختار ملف صوت.");
                    return;
                }

                SpeechRecognizer sr = SpeechRecognizer.createSpeechRecognizer(ctx.getApplicationContext());
                StringBuilder finalText = new StringBuilder();

                sr.setRecognitionListener(new RecognitionListener() {
                    @Override public void onReadyForSpeech(Bundle params) { cb.onMessage("شغّل الريكورد في واتساب الآن. جاري السماع من الميكروفون..."); }
                    @Override public void onBeginningOfSpeech() { cb.onMessage("سمعت صوت. جاري التحويل المباشر..."); }
                    @Override public void onRmsChanged(float rmsdB) {}
                    @Override public void onBufferReceived(byte[] buffer) {}
                    @Override public void onEndOfSpeech() { cb.onMessage("انتهى السماع. جاري تجهيز النص..."); }
                    @Override public void onError(int error) {
                        try { sr.destroy(); } catch (Exception ignored) {}
                        String partial = finalText.toString().trim();
                        if (partial.length() > 0) cb.onFinalText(partial);
                        else cb.onError(errorToText(error));
                    }
                    @Override public void onResults(Bundle results) {
                        ArrayList<String> list = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                        if (list != null && list.size() > 0) finalText.append(list.get(0)).append('\n');
                        try { sr.destroy(); } catch (Exception ignored) {}
                        String out = finalText.toString().trim();
                        if (out.length() == 0) cb.onError("لم يتم التعرف على كلام واضح. ارفع صوت الريكورد وقرب الموبايل وجرب مرة تانية.");
                        else cb.onFinalText(out);
                    }
                    @Override public void onPartialResults(Bundle partialResults) {
                        ArrayList<String> list = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                        if (list != null && list.size() > 0) cb.onMessage("نص مؤقت:\n" + list.get(0));
                    }
                    @Override public void onEvent(int eventType, Bundle params) {}
                });

                Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                String lang = AppSettings.getSpeechLanguage(ctx);
                if (lang == null || lang.length() == 0) lang = Locale.getDefault().toLanguageTag();
                i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang);
                i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
                i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
                sr.startListening(i);

                main.postDelayed(() -> {
                    try { sr.stopListening(); } catch (Exception ignored) {}
                }, Math.max(5, seconds) * 1000L);
            } catch (Exception e) {
                cb.onError(e.getMessage() == null ? e.toString() : e.getMessage());
            }
        });
    }

    private static String errorToText(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO: return "مشكلة في الميكروفون.";
            case SpeechRecognizer.ERROR_CLIENT: return "خدمة التعرف توقفت. جرب مرة تانية.";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: return "صلاحية الميكروفون غير مفعلة.";
            case SpeechRecognizer.ERROR_NETWORK: return "مشكلة شبكة في خدمة التعرف على الكلام.";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: return "انتهت مهلة الشبكة.";
            case SpeechRecognizer.ERROR_NO_MATCH: return "لم أسمع كلام واضح.";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: return "خدمة التعرف مشغولة. جرب بعد لحظات.";
            case SpeechRecognizer.ERROR_SERVER: return "مشكلة في سيرفر التعرف على الكلام.";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: return "لم يتم سماع صوت. شغّل الريكورد وارفع الصوت.";
            default: return "فشل التعرف على الكلام: " + error;
        }
    }
}
