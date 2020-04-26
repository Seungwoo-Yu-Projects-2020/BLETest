package com.swy2k.bletest.managers;

import com.swy2k.bletest.MainActivityViewModel;

import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

public class CommunicationLogAggregator {
    public static StringBuilder getCommunicationLogByAddress(@NonNull MainActivityViewModel viewModel, @NonNull String macAddress) {
        final LiveData<HashMap<String, StringBuilder>> data = viewModel.getCommunicationLog();
        if(data == null) return null;
        final HashMap<String, StringBuilder> logs = data.getValue();
        if(logs == null) return null;

        return logs.get(macAddress);
    }

    public static void setCommunicationLogByAddress(@NonNull MainActivityViewModel viewModel, @NonNull String macAddress, @NonNull String log) {
        final LiveData<HashMap<String, StringBuilder>> data = viewModel.getCommunicationLog();
        if(data == null) return;
        final HashMap<String, StringBuilder> hashMap = data.getValue();
        if(hashMap == null) return;

        StringBuilder logs = hashMap.get(macAddress);
        if (logs == null) {
            logs = new StringBuilder(log);
        } else {
            logs.append("\n").append(log);
        }
        hashMap.put(macAddress, logs);

        viewModel.setCommunicationLog(hashMap);
    }
}
