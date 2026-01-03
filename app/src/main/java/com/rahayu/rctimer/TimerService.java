package com.rahayu.rctimer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import java.util.ArrayList;
import java.util.List;

public class TimerService extends Service {
    public static final String ACTION_UPDATE_UI = "com.rahayu.rctimer.UPDATE_UI";
    public static final String CHANNEL_ID = "TimerServiceChannel";
    
    // Commands
    public static final String ACTION_CMD = "com.rahayu.rctimer.CMD";
    public static final String CMD_START = "START";
    public static final String CMD_PAUSE = "PAUSE";
    public static final String CMD_RESET = "RESET";
    public static final String CMD_STOP_ALARM = "STOP_ALARM";
    public static final String EXTRA_REMOTE_ID = "REMOTE_ID";

    // State
    public static List<RemoteModel> remotes = new ArrayList<>();
    private Handler handler;
    private Runnable runnable;
    private SoundManager soundManager;

    @Override
    public void onCreate() {
        super.onCreate();
        soundManager = SoundManager.getInstance(this);
        initializeData();
        createNotificationChannel();
        startForeground(1, createNotification("RAHAYU RC TIMER Ready"));
        startTimerLoop();
    }

    private void initializeData() {
        if (remotes.isEmpty()) {
            // Default init 10 remotes
            for (int i = 1; i <= 10; i++) {
                remotes.add(new RemoteModel(i, "REMOT " + i, 15 * 60 * 1000L));
            }
        }
    }

    private void startTimerLoop() {
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                boolean anyRunning = false;
                long now = System.currentTimeMillis();

                for (RemoteModel r : remotes) {
                    if (r.isRunning()) {
                        anyRunning = true;
                        long left = r.getRemainingTimeMillis() - 1000;
                        if (left <= 0) {
                            left = 0;
                            r.setRunning(false);
                            r.setAlarming(true);
                            soundManager.triggerAlarm(r.getId());
                        }
                        r.setRemainingTimeMillis(left);
                    }
                }

                // Broadcast Update
                Intent intent = new Intent(ACTION_UPDATE_UI);
                sendBroadcast(intent);

                // Update Notification if needed (simplified)
                if (anyRunning) {
                   // updateNotification("Timer running...");
                }

                handler.postDelayed(this, 1000);
            }
        };
        handler.post(runnable);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(ACTION_CMD)) {
                String cmd = intent.getStringExtra("CMD");
                int id = intent.getIntExtra(EXTRA_REMOTE_ID, -1);
                handleCommand(cmd, id);
            }
        }
        return START_STICKY; 
    }

    private void handleCommand(String cmd, int id) {
        RemoteModel target = null;
        for (RemoteModel r : remotes) {
            if (r.getId() == id) {
                target = r;
                break;
            }
        }

        if (target != null) {
            switch (cmd) {
                case CMD_START:
                    target.setRunning(true);
                    target.setAlarming(false);
                    break;
                case CMD_PAUSE:
                    target.setRunning(false);
                    break;
                case CMD_RESET:
                    target.setRunning(false);
                    target.setAlarming(false);
                    target.setRemainingTimeMillis(target.getDefaultTimeMillis());
                    soundManager.stopAlarm(id);
                    break;
                case CMD_STOP_ALARM:
                    target.setAlarming(false);
                    soundManager.stopAlarm(id);
                    break;
            }
            // Trigger immediate UI update
            sendBroadcast(new Intent(ACTION_UPDATE_UI));
        }
    }

    private Notification createNotification(String content) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("RAHAYU RC TIMER Main")
                .setContentText(content)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Rahayu Timer Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // Using intent commands for simplicity
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if(handler != null && runnable != null) handler.removeCallbacks(runnable);
    }
}
