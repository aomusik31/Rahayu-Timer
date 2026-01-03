package com.rahayu.rctimer;

public class RemoteModel {
    private int id;
    private String name;
    private long remainingTimeMillis;
    private long defaultTimeMillis;
    private boolean isRunning;
    private boolean isAlarming;
    private boolean isVisible; // For sidebar toggle

    public RemoteModel(int id, String name, long defaultTimeMillis) {
        this.id = id;
        this.name = name;
        this.defaultTimeMillis = defaultTimeMillis;
        this.remainingTimeMillis = defaultTimeMillis;
        this.isRunning = false;
        this.isAlarming = false;
        this.isVisible = true;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public long getRemainingTimeMillis() { return remainingTimeMillis; }
    public void setRemainingTimeMillis(long time) { this.remainingTimeMillis = time; }
    public long getDefaultTimeMillis() { return defaultTimeMillis; }
    public void setDefaultTimeMillis(long time) { this.defaultTimeMillis = time; }
    public boolean isRunning() { return isRunning; }
    public void setRunning(boolean running) { isRunning = running; }
    public boolean isAlarming() { return isAlarming; }
    public void setAlarming(boolean alarming) { isAlarming = alarming; }
    public boolean isVisible() { return isVisible; }
    public void setVisible(boolean visible) { isVisible = visible; }
}
