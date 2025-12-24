package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    public interface OnDeviceClickListener {
        void onDeviceClick(Device device);
        void onSettingsClick(Device device);
    }

    private List<Device> devices;
    private OnDeviceClickListener listener;

    public DeviceAdapter(List<Device> devices, OnDeviceClickListener listener) {
        this.devices = devices;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        Device device = devices.get(position);
        holder.bind(device, listener);
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    public void updateDeviceStatus(int position, boolean isActive) {
        devices.get(position).setActive(isActive);
        notifyItemChanged(position);
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        private TextView deviceName;
        private TextView deviceStatus;
        private ImageView deviceIcon;
        private SwitchCompat deviceToggle;
        private ImageButton settingsButton;
        private CardView deviceCard;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.deviceName);
            deviceStatus = itemView.findViewById(R.id.deviceStatus);
            deviceIcon = itemView.findViewById(R.id.deviceIcon);
            deviceToggle = itemView.findViewById(R.id.deviceToggle);
            settingsButton = itemView.findViewById(R.id.settingsButton);
            deviceCard = itemView.findViewById(R.id.deviceCard);
        }

        public void bind(final Device device, final OnDeviceClickListener listener) {
            deviceName.setText(device.getName());
            deviceStatus.setText(device.getStatus());
            deviceIcon.setImageResource(device.getIconRes());
            deviceToggle.setChecked(device.isActive());

            deviceToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                device.setActive(isChecked);
                device.updateStatus(); // Обновляем статус
                deviceStatus.setText(device.getStatus()); // Обновляем текст статуса
                if (listener != null) {
                    listener.onDeviceClick(device);
                }
            });

            settingsButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSettingsClick(device);
                }
            });

            deviceCard.setOnClickListener(v -> {
                deviceToggle.setChecked(!deviceToggle.isChecked());
            });
        }
    }
}