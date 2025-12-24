package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements DeviceAdapter.OnDeviceClickListener {
    private BluetoothManager bluetoothManager;
    private RecyclerView recyclerView;
    private DeviceAdapter adapter;
    private List<Device> deviceList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothManager = new BluetoothManager(this, this);

        if (bluetoothManager.enableBluetooth()) {
            // Bluetooth включен или процесс включения запущен
            Toast.makeText(this, "Bluetooth включается...", Toast.LENGTH_SHORT).show();
        }
        else {
            // Необходимо предоставить разрешения
            Toast.makeText(this, "Предоставьте разрешения для Bluetooth", Toast.LENGTH_SHORT).show();
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Умный дом");
            getSupportActionBar().setSubtitle("Все устройства");
        }

        recyclerView = findViewById(R.id.devicesRecyclerView);
        setupDeviceList();
        setupRecyclerView();
    }

    private void setupDeviceList() {
        deviceList = new ArrayList<>();

        // Создаем лампу с начальными настройками
        Device light = new Device(
                "1",
                "Главный свет",
                Device.DeviceType.LIGHT,
                "Включено • 75% • Белый",
                true,
                R.drawable.ic_light
        );
        light.setBrightness(75);
        light.setColor("Белый");
        deviceList.add(light);

        // Создаем кондиционер с начальными настройками
        Device ac = new Device(
                "2",
                "Кондиционер",
                Device.DeviceType.THERMOSTAT,
                "Включено • 22°C • Авто • AUTO",
                true,
                R.mipmap.ic_termostat
        );
        ac.setAcMode("AUTO");
        ac.setAcTemperature(22);
        ac.setAcFanSpeed("AUTO");
        deviceList.add(ac);

        deviceList.add(new Device(
                "3",
                "Камера",
                Device.DeviceType.SECURITY_CAMERA,
                "Онлайн • Детекция движения",
                true,
                R.mipmap.ic_camera
        ));

        deviceList.add(new Device(
                "4",
                "Жалюзи в гостиной",
                Device.DeviceType.BLINDS,
                "Подняты • 10:00-18:00",
                true,
                R.mipmap.ic_blinds
        ));

        deviceList.add(new Device(
                "5",
                "Умная розетка",
                Device.DeviceType.SOCKET,
                "Выключено",
                false,
                R.mipmap.ic_socket
        ));
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new DeviceAdapter(deviceList, this);
        recyclerView.setAdapter(adapter);

        DividerItemDecoration divider = new DividerItemDecoration(
                recyclerView.getContext(),
                DividerItemDecoration.VERTICAL
        );
        recyclerView.addItemDecoration(divider);
    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        // Передаем результат в BluetoothManager
//        bluetoothManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        // Передаем результат в BluetoothManager
//        bluetoothManager.onActivityResult(requestCode, resultCode);
//
//        if (requestCode == BluetoothManager.REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
//            // Bluetooth включен, можно подключаться к устройствам
//            connectToRaspberryPi();
//        }
//    }

    @Override
    public void onDeviceClick(Device device) {
        // Обновляем статус устройства после изменения состояния
        device.updateStatus();

        // Находим позицию устройства в списке
        int position = deviceList.indexOf(device);
        if (position != -1) {
            adapter.notifyItemChanged(position);
        }

        String message = device.getName() + ": " +
                (device.isActive() ? "Включено" : "Выключено");
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

        // Отправляем команду на устройство
        sendCommandToDevice(device);
    }

    @Override
    public void onSettingsClick(Device device) {
        // Открываем активность настроек с ожиданием результата
        Intent intent = new Intent(this, DeviceSettingsActivity.class);
        intent.putExtra("DEVICE_ID", device.getId());
        intent.putExtra("DEVICE_NAME", device.getName());
        intent.putExtra("DEVICE_TYPE", device.getType().toString());

        // Передаем текущие настройки устройства
        if (device.getType() == Device.DeviceType.LIGHT) {
            intent.putExtra("BRIGHTNESS", device.getBrightness());
            intent.putExtra("COLOR", device.getColor());
        } else if (device.getType() == Device.DeviceType.THERMOSTAT) {
            intent.putExtra("AC_MODE", device.getAcMode());
            intent.putExtra("AC_TEMPERATURE", device.getAcTemperature());
            intent.putExtra("AC_FAN_SPEED", device.getAcFanSpeed());
        }

        startActivityForResult(intent, 1);
    }

    private void sendCommandToDevice(Device device) {
        // Реализация отправки команды на устройство
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            String deviceId = data.getStringExtra("DEVICE_ID");
            boolean settingsChanged = data.getBooleanExtra("SETTINGS_CHANGED", false);

            if (settingsChanged) {
                // Находим устройство по ID
                for (Device device : deviceList) {
                    if (device.getId().equals(deviceId)) {
                        // Обновляем настройки из данных
                        if (device.getType() == Device.DeviceType.LIGHT) {
                            int brightness = data.getIntExtra("BRIGHTNESS", 75);
                            String color = data.getStringExtra("COLOR");

                            device.setBrightness(brightness);
                            device.setColor(color != null ? color : "Белый");
                        } else if (device.getType() == Device.DeviceType.THERMOSTAT) {
                            String acMode = data.getStringExtra("AC_MODE");
                            int acTemperature = data.getIntExtra("AC_TEMPERATURE", 22);
                            String acFanSpeed = data.getStringExtra("AC_FAN_SPEED");

                            if (acMode != null) device.setAcMode(acMode);
                            device.setAcTemperature(acTemperature);
                            if (acFanSpeed != null) device.setAcFanSpeed(acFanSpeed);
                        }

                        // Обновляем статус
                        device.updateStatus();

                        // Находим позицию и обновляем элемент в адаптере
                        int position = deviceList.indexOf(device);
                        if (position != -1) {
                            adapter.notifyItemChanged(position);
                        }

                        break;
                    }
                }
            }
        }
    }
}