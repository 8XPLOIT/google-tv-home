package io.github.samuelfonseca.tvhome;

import android.service.notification.NotificationListenerService;

/**
 * Does nothing itself. Android only lets an app see other apps' media sessions (for the
 * now-playing card) if it has an enabled notification listener. Enabled over adb with
 * {@code cmd notification allow_listener io.github.samuelfonseca.tvhome/io.github.samuelfonseca.tvhome.MediaListener}.
 */
public class MediaListener extends NotificationListenerService {
}
