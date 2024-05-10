package com.example.emailpasswordauth;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
    // Handler to post delayed tasks for scheduling notifications
    private Handler handler = new Handler(Looper.getMainLooper());
    // Random generator for selecting the prompts
    private Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        createNotificationChannel();
        scheduleFixedNotifications();
        // temp
        sendNotification("☁ Time to Reflect");
    }

    // Creating the the notification channel
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Reminders";
            String description = "Reminders";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("Reminders", name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    // Scheduling fixed notifications using scheduleNotificationAt method
    private void scheduleFixedNotifications() {
        scheduleNotificationAt(10, 0, "Good morning!");
        scheduleNotificationAt(16, 0, "Don't forget about us!");
    }

    // Scheduling notifications at a specific time we want
    private void scheduleNotificationAt(int hour, int minute, String title) {

        Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMinute = calendar.get(Calendar.MINUTE);
        // Set the time for the notification
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        // If the scheduled time has already passed, set it for the next day
        if (currentHour > hour || (currentHour == hour && currentMinute >= minute)) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        // Calculate the delay until the scheduled time
        long delay = calendar.getTimeInMillis() - System.currentTimeMillis();
        // Post a delayed task to send the notification
        handler.postDelayed(() -> {
            sendNotification(title);
            // Reschedule notifications for the next day
            scheduleFixedNotifications();
        }, delay);
    }

    // Method to send a notification with random prompt
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

        // create intent for notification to launch new journal entry activity
        Intent newEntryIntent = new Intent(this, CreateJournalEntry.class);
        PendingIntent pendingNewEntryIntent = PendingIntent.getActivity(this, 0, newEntryIntent, PendingIntent.FLAG_IMMUTABLE);

        // Build and display the notification
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        Notification.Builder builder = new Notification.Builder(this, "MY_CHANNEL")
                .setSmallIcon(R.drawable.journal_app_logo)
                .setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(true)
                .setContentIntent(pendingNewEntryIntent);
        notificationManager.notify(0, builder.build());
    }

    // Method to open profile fragment
    public void openProfile(View view) {
        NavController navController = Navigation.findNavController(this, R.id.fragmentContainerView2);
        navController.navigate(R.id.profile);
    }

    // Method to open dashboard fragment
    public void openDashboard(View view) {
        NavController navController = Navigation.findNavController(this, R.id.fragmentContainerView2);
        navController.navigate(R.id.dashboard);
    }

    // Method to open map activity
    public void openMap(View v) {
        Intent i = new Intent(this, MapActivity.class);
        startActivity(i);
    }
}
