package io.github.samuelfonseca.tvhome;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/**
 * Makes the remote's Settings button open Settings again. The system sends that key to the
 * Google TV home's quick-settings panel, and only to that package (and only if the panel needs a
 * system permission), so with launcherx disabled the button does nothing and apps never see it.
 * An accessibility service gets keys before the system does. Enabled over adb by
 * {@code tools/set-home.sh}.
 */
public class SettingsKey extends AccessibilityService {
    @Override protected boolean onKeyEvent(KeyEvent event) {
        // The TCL IR remote's gear sends NOTIFICATION; Google's Bluetooth remote sends SETTINGS.
        int key = event.getKeyCode();
        if (key != KeyEvent.KEYCODE_NOTIFICATION && key != KeyEvent.KEYCODE_SETTINGS) return false;
        if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) {
            startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
        return true;
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override public void onInterrupt() {
    }
}
