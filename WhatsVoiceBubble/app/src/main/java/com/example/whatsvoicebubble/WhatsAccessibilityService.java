package com.example.whatsvoicebubble;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;

public class WhatsAccessibilityService extends AccessibilityService {
    private static final String WA = "com.whatsapp";
    private static final String WAB = "com.whatsapp.w4b";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;
        String pkg = event.getPackageName().toString();
        if (WA.equals(pkg) || WAB.equals(pkg)) {
            Intent i = new Intent(this, BubbleOverlayService.class);
            i.putExtra("source", "whatsapp_visible");
            startService(i);
        }
    }

    @Override public void onInterrupt() {}
}
