package com.example.myapplication;

public class Device {
    private String id;
    private String name;
    private DeviceType type;
    private String status;
    private boolean isActive;
    private int iconRes;


    private int brightness = 75;
    private String color = "Белый";
    private String acMode = "AUTO";
    private int acTemperature = 22;
    private String acFanSpeed = "AUTO";

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

    // Геттеры и сеттеры для настроек
    public int getBrightness() { return brightness; }
    public void setBrightness(int brightness) { this.brightness = brightness; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getAcMode() { return acMode; }
    public void setAcMode(String acMode) { this.acMode = acMode; }

    public int getAcTemperature() { return acTemperature; }
    public void setAcTemperature(int acTemperature) { this.acTemperature = acTemperature; }

    public String getAcFanSpeed() { return acFanSpeed; }
    public void setAcFanSpeed(String acFanSpeed) { this.acFanSpeed = acFanSpeed; }

    public void setActive(boolean active) {
        isActive = active;
        updateStatus();
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // Метод для обновления статуса на основе текущих настроек
    public void updateStatus() {
        switch (type) {
            case LIGHT:
                if (isActive) {
                    status = "Включено • " + brightness + "% • " + color;
                } else {
                    status = "Выключено • " + brightness + "% • " + color;
                }
                break;
            case THERMOSTAT:
                // Переводим режим на русский для отображения
                String modeDisplay;
                switch (acMode) {
                    case "COOL": modeDisplay = "Охлаждение"; break;
                    case "HEAT": modeDisplay = "Обогрев"; break;
                    case "DRY": modeDisplay = "Осушение"; break;
                    case "FAN": modeDisplay = "Вентиляция"; break;
                    case "AUTO": modeDisplay = "Авто"; break;
                    default: modeDisplay = acMode;
                }

                if (isActive) {
                    if ("FAN".equals(acMode)) {
                        status = "Включено • " + modeDisplay + " • " + acFanSpeed;
                    } else {
                        status = "Включено • " + acTemperature + "°C • " + modeDisplay + " • " + acFanSpeed;
                    }
                } else {
                    if ("FAN".equals(acMode)) {
                        status = "Выключено • " + modeDisplay + " • " + acFanSpeed;
                    } else {
                        status = "Выключено • " + acTemperature + "°C • " + modeDisplay + " • " + acFanSpeed;
                    }
                }
                break;
            default:
                // Для других устройств оставляем текущий статус
                break;
        }
    }
}