package com.lian.nozomi_notification_to_telegram;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Set;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity {
    private TextView txtView;
    private EditText telegramBotToken;
    private EditText telegramChatId;

    private ScrollView scrollView;

    private NotificationReceiver nReceiver;
    private SharedPreferences mPreferences;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txtView = (TextView) findViewById(R.id.textView);
        nReceiver = new NotificationReceiver();
        mPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        telegramBotToken = (EditText) findViewById(R.id.telegramBotToken);
        telegramChatId = (EditText) findViewById(R.id.telegramChatId);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        scrollView = (ScrollView) findViewById(R.id.primaryContent);
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(ScrollView.FOCUS_UP);
            }
        });
        createNotificationChannel();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Set<String> packs = NotificationManagerCompat.getEnabledListenerPackages(getApplicationContext());
        boolean readNotiPermissions = packs.contains(getPackageName());
        if (readNotiPermissions == false) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                    startActivity(intent);
                }
            });
            alertDialogBuilder.setMessage("please allow necessary permissions");
            alertDialogBuilder.show();
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(NotificationService.ACTION_INCOMING_MSG);
        registerReceiver(nReceiver, filter);

        String telegramBotToken_ = "";
        String telegramChatId_ = "";

        if (mPreferences.contains("telegramBotToken")) {
            telegramBotToken_ = mPreferences.getString("telegramBotToken", telegramBotToken_);
        }

        if (mPreferences.contains("telegramChatId")) {
            telegramChatId_ = mPreferences.getString("telegramChatId", telegramChatId_);
        }

        telegramBotToken.setText(telegramBotToken_);
        telegramChatId.setText(telegramChatId_);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(nReceiver);

        String telegramBotToken_ = telegramBotToken.getText().toString();
        String telegramChatId_ = telegramChatId.getText().toString();

        mPreferences.edit().putString("telegramBotToken", telegramBotToken_).apply();
        mPreferences.edit().putString("telegramChatId", telegramChatId_).apply();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        CharSequence name = "nozomi";
        String description = "nozomi";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel("nozomi", name, importance);
        channel.setDescription(description);
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    public void buttonClicked(View v) {
        String telegramBotToken_ = telegramBotToken.getText().toString();
        String telegramChatId_ = telegramChatId.getText().toString();

        mPreferences.edit().putString("telegramBotToken", telegramBotToken_).apply();
        mPreferences.edit().putString("telegramChatId", telegramChatId_).apply();

        Notification.Builder notificationBuilder = new Notification.Builder(this, "nozomi")
                .setContentTitle("Test Title")
                .setContentText("I am a test message.")
                .setTicker("I am a test ticker")
                .setSmallIcon(R.drawable.ic_stat_ac_unit)
                .setAutoCancel(true);
        Notification notification = notificationBuilder.build();


        Log.d("MainActivity", notification.toString());
        NotificationManager nManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nManager.notify("nozomi", 328_165_312, notification);
    }

    class NotificationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(NotificationService.ACTION_INCOMING_MSG)) {
                String temp = intent.getStringExtra("notification_event") + ": " + txtView.getText();
                txtView.setText(temp);

            } else {
                Logger.getAnonymousLogger().warning("invalid action received");
            }
        }
    }
}

