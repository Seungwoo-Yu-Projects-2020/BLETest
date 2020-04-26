package com.swy2k.bletest.managers;

import android.util.Log;

import com.swy2k.bletest.MainActivityViewModel;
import com.swy2k.bletest.recyclerviews.BLEList.RecyclerViewItem;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

public class FoundDeviceAggregator {
    public static int indexOfNotConnectibleDeviceItem(@NonNull MainActivityViewModel viewModel) {
        final LiveData<List<RecyclerViewItem>> data = viewModel.getFoundDevices();
        if(data == null) return -1;
        final List<RecyclerViewItem> devices = data.getValue();
        if(devices == null) return -1;

        Log.e("init", "init");

        for(int i = 0; i < devices.size(); i++) {
            if(devices.get(i).getStatus() != RecyclerViewItem.STATUS_CONNECTIBLE) return i;
        }

        return -1;
    }

    public static RecyclerViewItem getNotConnectibleDeviceItem(@NonNull MainActivityViewModel viewModel) {
        final LiveData<List<RecyclerViewItem>> data = viewModel.getFoundDevices();
        if(data == null) return null;
        final List<RecyclerViewItem> devices = data.getValue();
        if(devices == null) return null;

        for(int i = 0; i < devices.size(); i++) {
            if(devices.get(i).getStatus() != RecyclerViewItem.STATUS_CONNECTIBLE) return devices.get(i);
        }

        return null;
    }

    public static void setItemState(@NonNull MainActivityViewModel viewModel, int index, int state) {
        final LiveData<List<RecyclerViewItem>> data = viewModel.getFoundDevices();
        if(data == null) return;
        final List<RecyclerViewItem> devices = data.getValue();
        if(devices == null) return;
        final RecyclerViewItem device = devices.get(index);
        if(device == null) return;

        device.setStatus(state);
        devices.set(index, device);
        viewModel.setFoundDevices(devices);
    }
}
