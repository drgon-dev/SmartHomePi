package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;

public class DeviceSettingsActivity extends AppCompatActivity {

    private Device.DeviceType deviceType;
    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_settings);

        deviceId = getIntent().getStringExtra("DEVICE_ID");
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

            Intent resultIntent = new Intent();
            resultIntent.putExtra("DEVICE_ID", deviceId);
            resultIntent.putExtra("SETTINGS_CHANGED", true);

            if (deviceType == Device.DeviceType.LIGHT) {
                SeekBar brightnessSeekBar = findViewById(R.id.brightnessSeekBar);
                Spinner colorSpinner = findViewById(R.id.colorSpinner);

                resultIntent.putExtra("BRIGHTNESS", brightnessSeekBar.getProgress());
                resultIntent.putExtra("COLOR", colorSpinner.getSelectedItem().toString());

            } else if (deviceType == Device.DeviceType.THERMOSTAT) {
                Spinner acModeSpinner = findViewById(R.id.acModeSpinner);
                SeekBar acTempSeekBar = findViewById(R.id.acTempSeekBar);
                Spinner acFanSpinner = findViewById(R.id.acFanSpinner);

                resultIntent.putExtra("AC_MODE", acModeSpinner.getSelectedItem().toString());
                resultIntent.putExtra("AC_TEMPERATURE", acTempSeekBar.getProgress());
                resultIntent.putExtra("AC_FAN_SPEED", acFanSpinner.getSelectedItem().toString());
            }

            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    private void setupDeviceSpecificSettings() {
        Group lightSettingsGroup = findViewById(R.id.lightSettingsGroup);
        Group acSettingsGroup = findViewById(R.id.acSettingsGroup);

        if (deviceType == Device.DeviceType.LIGHT) {
            lightSettingsGroup.setVisibility(android.view.View.VISIBLE);
            acSettingsGroup.setVisibility(android.view.View.GONE);

            SeekBar brightnessSeekBar = findViewById(R.id.brightnessSeekBar);
            Spinner colorSpinner = findViewById(R.id.colorSpinner);

            int currentBrightness = getIntent().getIntExtra("BRIGHTNESS", 75);
            String currentColor = getIntent().getStringExtra("COLOR");

            brightnessSeekBar.setProgress(currentBrightness);

            String[] colors = {"Белый", "Теплый белый", "Холодный белый", "Красный", "Зеленый", "Синий"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, colors);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            colorSpinner.setAdapter(adapter);

            if (currentColor != null) {
                for (int i = 0; i < colors.length; i++) {
                    if (colors[i].equals(currentColor)) {
                        colorSpinner.setSelection(i);
                        break;
                    }
                }
            }

        } else if (deviceType == Device.DeviceType.THERMOSTAT) {
            lightSettingsGroup.setVisibility(android.view.View.GONE);
            acSettingsGroup.setVisibility(android.view.View.VISIBLE);


            setupAirConditionerSettings();

        } else {
            lightSettingsGroup.setVisibility(android.view.View.GONE);
            acSettingsGroup.setVisibility(android.view.View.GONE);
        }
    }

    private void setupAirConditionerSettings() {


        String currentMode = getIntent().getStringExtra("AC_MODE");
        int currentTemperature = getIntent().getIntExtra("AC_TEMPERATURE", 22);
        String currentFanSpeed = getIntent().getStringExtra("AC_FAN_SPEED");

        Spinner acModeSpinner = findViewById(R.id.acModeSpinner);
        String[] modes = {"AUTO", "COOL", "DRY", "HEAT", "FAN"};
        ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, modes);
        modeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        acModeSpinner.setAdapter(modeAdapter);


        if (currentMode != null) {
            for (int i = 0; i < modes.length; i++) {
                if (modes[i].equals(currentMode)) {
                    acModeSpinner.setSelection(i);
                    break;
                }
            }
        }


        SeekBar acTempSeekBar = findViewById(R.id.acTempSeekBar);
        TextView acTempLabel = findViewById(R.id.acTempLabel);
        TextView acFanLabel = findViewById(R.id.acFanLabel);

        acTempSeekBar.setProgress(currentTemperature);
        acTempLabel.setText("Температура: " + currentTemperature + "°C");

        acTempSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                acTempLabel.setText("Температура: " + progress + "°C");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });


        Spinner acFanSpinner = findViewById(R.id.acFanSpinner);
        String[] fanSpeeds = {"AUTO", "LOW", "MEDIUM", "HIGH"};
        ArrayAdapter<String> fanAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, fanSpeeds);
        fanAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        acFanSpinner.setAdapter(fanAdapter);


        if (currentFanSpeed != null) {
            for (int i = 0; i < fanSpeeds.length; i++) {
                if (fanSpeeds[i].equals(currentFanSpeed)) {
                    acFanSpinner.setSelection(i);
                    break;
                }
            }
        }


        acModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                String selectedMode = (String) parent.getItemAtPosition(position);


                if ("FAN".equals(selectedMode)) {
                    acTempLabel.setEnabled(false);
                    acTempSeekBar.setEnabled(false);
                    acTempLabel.setText("Температура: --°C");
                } else {
                    acTempLabel.setEnabled(true);
                    acTempSeekBar.setEnabled(true);
                    acTempLabel.setText("Температура: " + acTempSeekBar.getProgress() + "°C");
                }


                if ("AUTO".equals(selectedMode)) {
                    acFanLabel.setEnabled(false);
                    acFanSpinner.setEnabled(false);
                    acFanSpinner.setSelection(0);
                } else {
                    acFanLabel.setEnabled(true);
                    acFanSpinner.setEnabled(true);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        acModeSpinner.post(() -> {
            String selectedMode = (String) acModeSpinner.getSelectedItem();

            if ("FAN".equals(selectedMode)) {
                acTempLabel.setEnabled(false);
                acTempSeekBar.setEnabled(false);
                acTempLabel.setText("Температура: --°C");
            }

            if ("AUTO".equals(selectedMode)) {
                acFanLabel.setEnabled(false);
                acFanSpinner.setEnabled(false);
            }
        });
    }

    private void saveSettings() {

    }

    @Override
    public boolean onSupportNavigateUp() {
        setResult(RESULT_CANCELED);
        finish();
        return true;
    }
}