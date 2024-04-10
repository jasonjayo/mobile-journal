package com.example.emailpasswordauth;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class HomeActivity extends AppCompatActivity {

    private Handler handler = new Handler(Looper.getMainLooper());
    private Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        createNotificationChannel();
        scheduleRandomNotification();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "TEST_CHANNEL";
            String description = "Test Channel";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("MY_CHANNEL", name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void scheduleRandomNotification() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendNotification();
                int delay = (random.nextInt(4) + 1) * 60 * 60 * 1000;
                handler.postDelayed(this, delay);
            }
        }, 0);
    }

    private void sendNotification() {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        Notification.Builder builder = new Notification.Builder(this, "MY_CHANNEL")
                .setSmallIcon(R.drawable.journal_app_logo)
                .setContentTitle("App Reminder")
                .setContentText("Don't forget to use the app!")
                .setAutoCancel(true);
        notificationManager.notify(0, builder.build());
    }

    public void openMap(View v) {
        Intent i = new Intent(this, MapActivity.class);
        startActivity(i);
    }
}
