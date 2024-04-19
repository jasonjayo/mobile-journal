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
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import java.util.Calendar;
import java.util.Random;

public class HomeActivity extends AppCompatActivity {

    private Handler handler = new Handler(Looper.getMainLooper());
    private Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        createNotificationChannel();
        scheduleFixedNotifications();
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

    private void scheduleFixedNotifications() {
        scheduleNotificationAt(10, 0, "Good morning!");
        scheduleNotificationAt(16, 0, "Don't forget about us!");
    }

    private void scheduleNotificationAt(int hour, int minute, String title) {

        Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMinute = calendar.get(Calendar.MINUTE);

        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        if (currentHour > hour || (currentHour == hour && currentMinute >= minute)) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        long delay = calendar.getTimeInMillis() - System.currentTimeMillis();

        handler.postDelayed(() -> {
            sendNotification(title);
            scheduleFixedNotifications();
        }, delay);
    }

    private void sendNotification(String title) {
        // Randomly select a question
        String[] prompts = Prompts.possiblePrompts.values().toArray(new String[0]);
        String[] questions = Prompts.possibleQuestions.values().toArray(new String[0]);
        String content;
        if (random.nextBoolean()) {
            content = prompts[random.nextInt(prompts.length)];
        } else {
            content = questions[random.nextInt(questions.length)];
        }

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        Notification.Builder builder = new Notification.Builder(this, "MY_CHANNEL")
                .setSmallIcon(R.drawable.journal_app_logo)
                .setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(true);
        notificationManager.notify(0, builder.build());
    }

    public void openProfile(View view) {
        NavController navController = Navigation.findNavController(this, R.id.fragmentContainerView2);
        navController.navigate(R.id.profile);
    }

    public void openDashboard(View view) {
        NavController navController = Navigation.findNavController(this, R.id.fragmentContainerView2);
        navController.navigate(R.id.dashboard);
    }

    public void openMap(View v) {
        Intent i = new Intent(this, MapActivity.class);
        startActivity(i);
    }
}
