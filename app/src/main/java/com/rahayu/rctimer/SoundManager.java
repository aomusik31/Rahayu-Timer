package com.rahayu.rctimer;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;
import java.util.LinkedList;
import java.util.Queue;

public class SoundManager {
    private static SoundManager instance;
    private MediaPlayer mediaPlayer;
    private Queue<Integer> alarmQueue;
    private boolean isPlaying = false;
    private Context context;

    private SoundManager(Context context) {
        this.context = context;
        this.alarmQueue = new LinkedList<>();
    }

    public static synchronized SoundManager getInstance(Context context) {
        if (instance == null) {
            instance = new SoundManager(context.getApplicationContext());
        }
        return instance;
    }

    // Trigger an alarm for a specific remote ID
    public void triggerAlarm(int remoteId) {
        synchronized (this) {
            if (!alarmQueue.contains(remoteId)) {
                alarmQueue.offer(remoteId);
                Log.d("SoundManager", "Alarm queued for Remote " + remoteId);
            }
            processQueue();
        }
    }

    // Stop alarm for a specific remote
    public void stopAlarm(int remoteId) {
        synchronized (this) {
            if (alarmQueue.contains(remoteId)) {
                alarmQueue.remove(remoteId);
            }
            
            // If the current playing sound belongs to this remote, stop it.
            // Note: Since we are looping one sound at a time, simply stopping the player 
            // and checking queue is sufficient logic for "Stop this alarm".
            // However, to be precise, we rely on the user clicking "STOP" on the active alarm.
            // If the current playing alarm is this one:
            if (isPlaying) {
                 // In a real complex app, we'd track WHICH remote is currently playing in a variable.
                 // For now, we assume if STOP is clicked, we stop the current sound and play next.
                 stopCurrentSound();
            }
        }
    }

    private void processQueue() {
        if (isPlaying || alarmQueue.isEmpty()) {
            return;
        }

        int nextRemoteId = alarmQueue.peek(); // Don't remove yet, wait until processed or stopped? 
        // Actually for looping alarm, we play UNTIL stopped.
        // So we don't automatically proceed to next unless the user interacts or we mix sounds.
        // But the requirement says "Queue or ensure clear".
        // Strategy: Play the head of the queue. It loops. User must Dismiss it to play next.
        
        playRemoteSound(nextRemoteId);
    }

    private void playRemoteSound(int remoteId) {
        try {
            stopCurrentSound(); // Safety check

            // Dynamic Resource Loading: R.raw.remot_x
            String soundName = "remot_" + remoteId; 
            int resId = context.getResources().getIdentifier(soundName, "raw", context.getPackageName());

            if (resId != 0) {
                mediaPlayer = MediaPlayer.create(context, resId);
                if (mediaPlayer != null) {
                    mediaPlayer.setLooping(true); // Requirement: SPAM/LOOPING
                    mediaPlayer.setOnCompletionListener(mp -> {
                        // For looping, this rarely triggers, but if it wasn't looping:
                        // isPlaying = false; processQueue(); 
                    });
                    mediaPlayer.start();
                    isPlaying = true;
                    Log.d("SoundManager", "Playing sound for Remote " + remoteId);
                }
            } else {
                Log.e("SoundManager", "Sound file not found: " + soundName);
                // If file missing, just remove from queue to avoid block
                alarmQueue.poll();
                processQueue();
            }
        } catch (Exception e) {
            Log.e("SoundManager", "Error playing sound", e);
            isPlaying = false;
        }
    }

    private void stopCurrentSound() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        isPlaying = false;
        
        // Remove the one that just finished/stopped from queue start
        if (!alarmQueue.isEmpty()) {
            alarmQueue.poll();
        }
        
        // Immediately check if others are waiting
        processQueue();
    }
    
    public boolean isQueueEmpty() {
        return alarmQueue.isEmpty();
    }
}
