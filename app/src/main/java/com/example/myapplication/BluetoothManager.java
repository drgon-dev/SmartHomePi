package com.example.myapplication;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothManager {
    private static final String TAG = "BluetoothManager";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // Стандартный UUID для SPP

    public static final String MESSAGE_CONNECTING = "Подключение к Raspberry Pi...";
    public static final String MESSAGE_CONNECTED = "Подключено к Raspberry Pi";
    public static final String MESSAGE_SENT = "Сообщение отправлено";
    public static final String MESSAGE_ERROR = "Ошибка подключения";
    public static final String MESSAGE_NO_BLUETOOTH = "Bluetooth не поддерживается";
    public static final String MESSAGE_BLUETOOTH_OFF = "Включите Bluetooth";

    private BluetoothSocket socket;
    private final Context context;
    private final Activity activity;
    private OutputStream outputStream;
    private BluetoothAdapter bluetoothAdapter;
    static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSIONS = 2;
    private Handler handler;

    public BluetoothManager(Context context, Activity activity) {
        this.context = context;
        this.activity = activity;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    /**
     * Подключение к Raspberry Pi по MAC-адресу
     * @param deviceMac MAC-адрес Raspberry Pi в формате "XX:XX:XX:XX:XX:XX"
     * @return true если подключение успешно
     */
    public boolean connectToDevice(String deviceMac) {
        try {
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceMac);

            // Создаем socket для подключения
            socket = device.createRfcommSocketToServiceRecord(MY_UUID);

            // Подключаемся
            socket.connect();
            outputStream = socket.getOutputStream();

            Log.i(TAG, "Подключился к устройству RPi");
            return true;

        } catch (IOException e) {
            Log.e(TAG, "Ошибка подключения: " + e.getMessage());
            closeConnection();
            return false;
        }
    }

    /**
     * Отправка данных на Raspberry Pi
     * @param data строка для отправки
     * @return true если отправка успешна
     */
    public boolean sendData(String data) {
        if (socket == null || !socket.isConnected() || outputStream == null) {
            Log.e(TAG, "Нет подключения к устройству");
            return false;
        }

        try {
            byte[] buffer = data.getBytes();
            outputStream.write(buffer);
            outputStream.flush();

            Log.i(TAG, "Данные отправлены: " + data);
            return true;

        } catch (IOException e) {
            Log.e(TAG, "Ошибка отправки данных: " + e.getMessage());
            closeConnection();
            return false;
        }
    }

    /**
     * Отправка сигнала
     * @param signal команда для Raspberry Pi
     */
    public void sendSignal(String signal) {
        new Thread(() -> {
            if (sendData(signal + "\n")) { // Добавляем символ новой строки для парсинга на RPi
                Log.i(TAG, "Сигнал отправлен: " + signal);
            }
        }).start();
    }

    /**
     * Закрытие соединения
     */
    public void closeConnection() {
        try {
            if (outputStream != null) {
                outputStream.close();
            }
            if (socket != null) {
                socket.close();
            }
            Log.i(TAG, "Соединение закрыто");
        } catch (IOException e) {
            Log.e(TAG, "Ошибка при закрытии соединения: " + e.getMessage());
        }
    }

    /**
     * Проверка поддержки Bluetooth
     */
    public boolean isBluetoothSupported() {
        return bluetoothAdapter != null;
    }

    /**
     * Включение Bluetooth
     */
    public boolean enableBluetooth() {
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Устройство не поддерживает Bluetooth");
            return false;
        }

        // Если Bluetooth уже включен
        if (bluetoothAdapter.isEnabled()) {
            Log.i(TAG, "Bluetooth уже включен");
            return true;
        }

        // Проверяем и запрашиваем необходимые разрешения
        if (!checkAndRequestPermissions()) {
            Log.w(TAG, "Необходимые разрешения не предоставлены");
            return false;
        }

        // Включаем Bluetooth
        try {
            // Для Android 12+ используем Intent
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                if (activity != null) {
                    activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    // Возвращаем true, так как процесс запущен (результат будет в onActivityResult)
                    return true;
                }
            } else {
                // Для старых версий используем прямой вызов
                boolean result = bluetoothAdapter.enable();
                Log.i(TAG, "Попытка включить Bluetooth: " + result);
                return result;
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Ошибка безопасности при включении Bluetooth: " + e.getMessage());
        }

        return false;
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                Log.i(TAG, "Все разрешения предоставлены");
                // После получения разрешений можно попробовать снова включить Bluetooth
                enableBluetooth();
            } else {
                Log.w(TAG, "Не все разрешения предоставлены");
            }
        }
    }

    /**
     * Обработка результата включения Bluetooth через Intent
     * @param requestCode код запроса
     * @param resultCode результат
     */
    public void onActivityResult(int requestCode, int resultCode) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                Log.i(TAG, "Bluetooth успешно включен пользователем");
            } else {
                Log.w(TAG, "Пользователь отказался включать Bluetooth");
            }
        }
    }

    private boolean checkAndRequestPermissions() {
        // Список необходимых разрешений
        String[] permissions;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ (API 31+)
            permissions = new String[]{
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0+ до Android 12
            permissions = new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            };
        } else {
            // Для версий ниже Android 6.0 разрешения уже предоставлены
            return true;
        }

        // Проверяем, какие разрешения уже предоставлены
        boolean allPermissionsGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        // Если не все разрешения предоставлены, запрашиваем их
        if (!allPermissionsGranted && activity != null) {
            ActivityCompat.requestPermissions(activity, permissions, REQUEST_PERMISSIONS);
            return false; // Возвращаем false, так как разрешения еще не предоставлены
        }

        return allPermissionsGranted;
    }

    private void sendCallbackMessage(String message) {
        Log.i(TAG, message);
    }
    private class ConnectAndSendRunnable implements Runnable {
        @Override
        public void run() {
            try {
                // Шаг 1: Подключение
                sendCallbackMessage(MESSAGE_CONNECTING);

                // Получаем устройство по MAC-адресу
                String raspberryPiMacAddress = "XX:XX:XX:XX:XX:XX";
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(raspberryPiMacAddress);

                // Создаем socket
                socket = device.createRfcommSocketToServiceRecord(MY_UUID);

                // Отменяем поиск для улучшения производительности
                bluetoothAdapter.cancelDiscovery();

                // Подключаемся
                socket.connect();
                outputStream = socket.getOutputStream();

                // Шаг 2: Отправка

                // Закрываем соединение после отправки
                closeConnection();

            } catch (IOException e) {
                Log.e(TAG, "Ошибка при подключении/отправке: " + e.getMessage());
                sendCallbackMessage(MESSAGE_ERROR + ": " + e.getMessage());
                closeConnection();
            } catch (SecurityException e) {
                Log.e(TAG, "Ошибка безопасности: " + e.getMessage());
                sendCallbackMessage("Ошибка безопасности: " + e.getMessage());
            }
        }
    }

}