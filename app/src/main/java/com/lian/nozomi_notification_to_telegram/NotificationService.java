package com.lian.nozomi_notification_to_telegram;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.telephony.SmsManager;
import android.text.SpannableString;
import android.util.Log;
import android.widget.Toast;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;

import okhttp3.OkHttpClient;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class NotificationService extends NotificationListenerService {
    public static final String ACTION_INCOMING_MSG = "com.lian.nozomi_notification_to_telegram.INCOMING_MSG";
    public static final String SPECIAL_MUSIC = "com.google.android.music org.lineageos.eleven com.spotify.music deezer.android.app deezer.android.tv";
    private static SharedPreferences mPreferences;
    private ArrayList<String> specialMusicPlayer;
    private String lastPost = "";

    @Override
    public void onCreate() {
        super.onCreate();
        mPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        specialMusicPlayer = new ArrayList<>(Arrays.asList(SPECIAL_MUSIC.split(" ")));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.onNotificationRemoved(sbn);
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        Notification noti = sbn.getNotification();
        Bundle extras = noti.extras;
        String title = null;
        String pack = sbn.getPackageName();
        String msg = null;
        String msg1 = (String) noti.tickerText;
        Object obj = extras.get(Notification.EXTRA_TEXT);
        String msg2 = null;

        try {
            if (title == null || title == "") {
                SpannableString sp = (SpannableString) extras.get("android.title");
                title = Objects.requireNonNull(sp).toString();
            }
        } catch (Exception e) {
            title = extras.getString(Notification.EXTRA_TITLE);
        }

        if (obj != null) {
            msg2 = obj.toString();
        }
        String msg3 = null;
        String msg4 = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            msg3 = extras.getString(Notification.EXTRA_BIG_TEXT);
        }

        String TAG = "NotificationService";
        try {
            SpannableString sp = (SpannableString) extras.get("android.text");
            Log.d(TAG, "title    " + title);
            Log.d(TAG, "pack     " + pack);
            Log.d(TAG, "ticker   " + msg1);
            Log.d(TAG, "text     " + msg2);
            Log.d(TAG, "big.text " + msg3);
            if (sp != null) {
                msg4 = sp.toString();
            }
            Log.d(TAG, "android.text " + msg4);
        } catch (Exception ignored) {
        }

        msg = msg1; // ticker text is default

        if (msg4 != null && msg4.length() > 0) { // android.text (for old androids)
            msg = msg4;
        }
        if (msg2 != null && msg2.length() > 0) { // extra text
            msg = msg2;
        }
        if (msg3 != null && msg3.length() > 0) { // favourit big text, if exists
            msg = msg3;
        }

        try {
            ApplicationInfo appi = this.getPackageManager().getApplicationInfo(pack, 0);
            Drawable icon = getPackageManager().getApplicationIcon(appi);
            if (specialMusicPlayer.indexOf(pack) < 0) {
                pack = getPackageManager().getApplicationLabel(appi).toString();
            } else {
                pack = "Music";
            }

        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }

        // catch not normal message -----------------------------
        if (!pack.equals("Music") && !sbn.isClearable()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (pack.equals("Music") && sbn.isGroup()) {
                Log.d(TAG, "is group");
                return;
            }
        }
        if (title == null) title = pack;

        title = title.trim();
        if (title.endsWith(":")) {
            title = title.substring(0, title.lastIndexOf(":"));
        }

        try {
            Log.d(TAG, "title: " + title);
            Log.d(TAG, "msg: " + msg);
            Log.d(TAG, "app: " + pack);

        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }

        if (msg == null) return;
        if ((title + msg).equals(lastPost)) return;

        lastPost = title + msg;

        Intent i = new Intent(ACTION_INCOMING_MSG);
        i.putExtra("notification_event", msg);
        sendBroadcast(i);
        if (!mPreferences.getBoolean("with_source", true)) pack = "";

        sendTelegram(title, msg, pack);
    }

    private void sendTelegram(String... strings) {
        // Hack - should be done using an async task !!
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        String title = strings[0];
        String message = strings[1];
        int chatId;
        try {
             chatId = Integer.parseInt(Objects.requireNonNull(mPreferences.getString("telegramChatId", "")));
        } catch (NumberFormatException e) {
            Toast t = Toast.makeText(this, "telegram chat id must be a number", Toast.LENGTH_SHORT);
            t.show();
            return;
        }

        String botToken = mPreferences.getString("telegramBotToken", "");
        if (Objects.requireNonNull(botToken).equals("")) {
            Toast t = Toast.makeText(this, "telegram information not entered", Toast.LENGTH_SHORT);
            t.show();
            return;
        }
        TelegramBot bot = new TelegramBot(botToken);
        SendMessage request = new SendMessage(chatId, title + ": " + message);
        bot.execute(request);
    }

}
