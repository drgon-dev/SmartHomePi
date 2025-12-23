package com.example.myapplication;

import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class DeviceSettingsActivity extends AppCompatActivity {

    private Device.DeviceType deviceType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_settings);

        String deviceId = getIntent().getStringExtra("DEVICE_ID");
        String deviceName = getIntent().getStringExtra("DEVICE_NAME");

        String type = getIntent().getStringExtra("DEVICE_TYPE");
        if (type != null) {
            deviceType = Device.DeviceType.valueOf(type);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Настройки: " + deviceName);
        }

        TextView deviceInfo = findViewById(R.id.deviceInfo);
        deviceInfo.setText("Устройство: " + deviceName + "\nID: " + deviceId);

        setupDeviceSpecificSettings();

        Button saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(v -> {
            saveSettings();
            Toast.makeText(this, "Настройки сохранены", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void setupDeviceSpecificSettings() {
        SeekBar brightnessSeekBar = findViewById(R.id.brightnessSeekBar);
        Spinner colorSpinner = findViewById(R.id.colorSpinner);

        if (deviceType == Device.DeviceType.LIGHT) {
            // Настройки для лампы
            brightnessSeekBar.setVisibility(android.view.View.VISIBLE);
            colorSpinner.setVisibility(android.view.View.VISIBLE);

            // Заполняем список цветов
            String[] colors = {"Белый", "Теплый белый", "Холодный белый", "Красный", "Зеленый", "Синий"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, colors);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            colorSpinner.setAdapter(adapter);

        } else if (deviceType == Device.DeviceType.THERMOSTAT) {
            // Скрываем элементы для термостата (или меняем на другие)
            brightnessSeekBar.setVisibility(android.view.View.GONE);
            colorSpinner.setVisibility(android.view.View.GONE);
            // Можно добавить другие элементы
        }
    }

    private void saveSettings() {
        SeekBar brightnessSeekBar = findViewById(R.id.brightnessSeekBar);
        Spinner colorSpinner = findViewById(R.id.colorSpinner);

        if (deviceType == Device.DeviceType.LIGHT) {
            int brightness = brightnessSeekBar.getProgress();
            String selectedColor = colorSpinner.getSelectedItem().toString();

            // Сохраняем настройки (например, в SharedPreferences или БД)
            // или отправляем команду на устройство
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}