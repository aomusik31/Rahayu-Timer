package com.rahayu.rctimer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements RemoteAdapter.ActionListener {

    private DrawerLayout drawerLayout;
    private RecyclerView recyclerView;
    private RemoteAdapter adapter;
    private LinearLayout switchesContainer;
    private EditText inputDefaultTimer;
    private TimerReceiver timerReceiver;
    
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        prefs = getSharedPreferences("RahayuPrefs", MODE_PRIVATE);

        // UI Initialization
        drawerLayout = findViewById(R.id.drawer_layout);
        recyclerView = findViewById(R.id.recycler_timers);
        switchesContainer = findViewById(R.id.switches_container);
        inputDefaultTimer = findViewById(R.id.input_default_timer);
        Button btnSaveSettings = findViewById(R.id.btn_save_settings);
        View btnMenu = findViewById(R.id.btn_menu);

        // RecyclerView Setup
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new RemoteAdapter(this, new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);

        // Sidebar Toggles Generation
        setupSidebar();

        // Menu Button
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Save Settings
        btnSaveSettings.setOnClickListener(v -> saveSettings());

        // Start Service
        Intent serviceIntent = new Intent(this, TimerService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        
        // Request Overlay Permission
        checkOverlayPermission();
    }

    private void setupSidebar() {
        switchesContainer.removeAllViews();
        for (int i = 1; i <= 10; i++) {
            Switch sw = new Switch(this);
            sw.setText("Tampilkan Remot " + i);
            sw.setTextColor(getResources().getColor(R.color.brown_text));
            sw.setTextSize(16);
            sw.setPadding(0, 10, 0, 10);
            
            // Load state
            boolean isVisible = prefs.getBoolean("visible_remot_" + i, true);
            sw.setChecked(isVisible);
            
            final int remoteId = i;
            sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean("visible_remot_" + remoteId, isChecked).apply();
                updateVisibleList();
            });
            
            switchesContainer.addView(sw);
        }
    }
    
    private void updateVisibleList() {
        // Filter list based on preferences
        // Since Service holds the master data, actually we mostly just filter what we SHOW in adapter.
        // However, TimerService.remotes has all models. We create a subset.
        // Note: For simplicity, we are just syncing adapter. In a real MVVM, we'd observe LiveData.
        
        List<RemoteModel> allModels = TimerService.remotes;
        if (allModels == null || allModels.isEmpty()) return;

        List<RemoteModel> visibleModels = new ArrayList<>();
        for (RemoteModel m : allModels) {
            boolean isVisible = prefs.getBoolean("visible_remot_" + m.getId(), true);
            m.setVisible(isVisible); // Update model property just in case
            if (isVisible) {
                visibleModels.add(m);
            }
        }
        adapter.updateData(visibleModels);
    }

    private void saveSettings() {
        String timerStr = inputDefaultTimer.getText().toString();
        if (!timerStr.isEmpty()) {
            int mins = Integer.parseInt(timerStr);
            // Update all defaults logic if needed, or just save preference
            Toast.makeText(this, "Pengaturan Disimpan", Toast.LENGTH_SHORT).show();
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    private void checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 101);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        timerReceiver = new TimerReceiver();
        registerReceiver(timerReceiver, new IntentFilter(TimerService.ACTION_UPDATE_UI), Context.RECEIVER_NOT_EXPORTED);
        updateVisibleList(); // Initial sync
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(timerReceiver);
    }

    // Adapter Actions
    @Override
    public void onStartPause(int remoteId) {
        // Toggle logic: If running -> Pause. If not -> Start.
        // But adapter button text changes. Here we just send appropriate command.
        // We check current local state or let service handle toggle?
        // Service command 'CMD_START' forces start. We need toggle logic.
        // Easier: Just check the model in the list.
        RemoteModel m = findRemoteById(remoteId);
        if (m != null) {
            String cmd = m.isRunning() ? TimerService.CMD_PAUSE : TimerService.CMD_START;
            sendCommand(cmd, remoteId);
        }
    }

    @Override
    public void onReset(int remoteId) {
        sendCommand(TimerService.CMD_RESET, remoteId);
    }

    @Override
    public void onStopAlarm(int remoteId) {
        sendCommand(TimerService.CMD_STOP_ALARM, remoteId);
    }

    @Override
    public void onUpdateDefaultTime(int remoteId, long newTimeMillis) {
        // Directly update the object in memory (Service holds the ref)
        RemoteModel m = findRemoteById(remoteId);
        if (m != null) {
            m.setDefaultTimeMillis(newTimeMillis);
            m.setRemainingTimeMillis(newTimeMillis); // Also reset current
            adapter.notifyDataSetChanged();
        }
    }
    
    private RemoteModel findRemoteById(int id) {
        // Quick lookup from service static list (Not clean architecture but functional for snippet)
        for (RemoteModel m : TimerService.remotes) {
            if (m.getId() == id) return m;
        }
        return null;
    }

    private void sendCommand(String cmd, int remoteId) {
        Intent intent = new Intent(this, TimerService.class);
        intent.setAction(TimerService.ACTION_CMD);
        intent.putExtra("CMD", cmd);
        intent.putExtra(TimerService.EXTRA_REMOTE_ID, remoteId);
        startService(intent);
    }

    // Broadcast Receiver
    private class TimerReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TimerService.ACTION_UPDATE_UI.equals(intent.getAction())) {
                // Refresh Adapter
                // Note: notifyDataSetChanged is heavy if called every second. 
                // But efficient enough for 10 items.
                updateVisibleList(); 
            }
        }
    }
}
