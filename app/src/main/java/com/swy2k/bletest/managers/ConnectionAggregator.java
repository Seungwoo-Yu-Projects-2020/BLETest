package com.swy2k.bletest.managers;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import com.swy2k.bletest.ConnectionThread;
import com.swy2k.bletest.MainActivityViewModel;

import androidx.annotation.NonNull;

public class ConnectionAggregator {
    public static void startConnection(@NonNull MainActivityViewModel viewModel, @NonNull Context context, @NonNull BluetoothDevice bluetoothDevice) {
        if(viewModel.getConnectionThread() != null && viewModel.getConnectionThread().getValue() != null) return;

        ConnectionThread connectionThread = new ConnectionThread(context, bluetoothDevice) {
            @Override
            public void onThreadCreated() {
                this.startConnection();
            }
        };
        viewModel.setConnectionThread(connectionThread);
    }

    public static void stopConnection(@NonNull MainActivityViewModel viewModel) {
        if(viewModel.getConnectionThread() == null || viewModel.getConnectionThread().getValue() == null) return;

        viewModel.getConnectionThread().getValue().stopConnection();
    }

    public static void exitConnection(@NonNull MainActivityViewModel viewModel) {
        if(viewModel.getConnectionThread() == null || viewModel.getConnectionThread().getValue() == null) return;

        viewModel.getConnectionThread().getValue().exit();
        viewModel.setConnectionThread(null);
    }
}
