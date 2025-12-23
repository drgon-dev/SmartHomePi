package com.example.myapplication;

public class Device {
    private String id;
    private String name;
    private DeviceType type;
    private String status;
    private boolean isActive;
    private int iconRes;

    public enum DeviceType {
        LIGHT, THERMOSTAT, SECURITY_CAMERA, LOCK, BLINDS, SOCKET, SENSOR
    }

    public Device(String id, String name, DeviceType type, String status, boolean isActive, int iconRes) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.status = status;
        this.isActive = isActive;
        this.iconRes = iconRes;
    }


    public String getId() { return id; }
    public String getName() { return name; }
    public DeviceType getType() { return type; }
    public String getStatus() { return status; }
    public boolean isActive() { return isActive; }
    public int getIconRes() { return iconRes; }

    public void setActive(boolean active) { isActive = active; }
    public void setStatus(String status) { this.status = status; }
}