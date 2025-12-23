package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements DeviceAdapter.OnDeviceClickListener {

    private RecyclerView recyclerView;
    private DeviceAdapter adapter;
    private List<Device> deviceList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        // Пример данных устройств
        deviceList.add(new Device(
                "1",
                "Главный свет",
                Device.DeviceType.LIGHT,
                "Включено • 75%",
                true,
                R.drawable.ic_light
        ));

        deviceList.add(new Device(
                "2",
                "Кондиционер",
                Device.DeviceType.THERMOSTAT,
                "22°C • Охлаждение",
                true,
                R.mipmap.ic_termostat
        ));

//        deviceList.add(new Device(
//                "3",
//                "Камера у входа",
//                Device.DeviceType.SECURITY_CAMERA,
//                "Онлайн • Детекция движения",
//                true,
//                R.drawable.ic_camera
//        ));
//
//        deviceList.add(new Device(
//                "4",
//                "Умный замок",
//                Device.DeviceType.LOCK,
//                "Закрыто",
//                false,
//                R.drawable.ic_lock
//        ));
//
//        deviceList.add(new Device(
//                "5",
//                "Жалюзи в гостиной",
//                Device.DeviceType.BLINDS,
//                "Подняты • 10:00-18:00",
//                true,
//                R.drawable.ic_blinds
//        ));
//
//        deviceList.add(new Device(
//                "6",
//                "Розетка в спальне",
//                Device.DeviceType.SOCKET,
//                "Выключено",
//                false,
//                R.drawable.ic_socket
//        ));
//
//        deviceList.add(new Device(
//                "7",
//                "Датчик температуры",
//                Device.DeviceType.SENSOR,
//                "23.5°C • 45% влажности",
//                true,
//                R.drawable.ic_sensor
//        ));
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

    @Override
    public void onDeviceClick(Device device) {

        String message = device.getName() + ": " +
                (device.isActive() ? "Включено" : "Выключено");
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();


        sendCommandToDevice(device);
    }

    @Override
    public void onSettingsClick(Device device) {

        Intent intent = new Intent(this, DeviceSettingsActivity.class);
        intent.putExtra("DEVICE_ID", device.getId());
        intent.putExtra("DEVICE_NAME", device.getName());
        intent.putExtra("DEVICE_TYPE", device.getType().toString());
        startActivity(intent);
    }

    private void sendCommandToDevice(Device device) {

    }
}