package com.swy2k.bletest;

import com.swy2k.bletest.recyclerviews.BLEList.RecyclerViewItem;

import java.util.HashMap;
import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MainActivityViewModel extends ViewModel {
    private final MutableLiveData<Boolean> isBluetoothSupported = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> isSearching = new MutableLiveData<>(false);
    private final MutableLiveData<List<RecyclerViewItem>> foundDevices = new MutableLiveData<>();
    private final MutableLiveData<HashMap<String, StringBuilder>> communicationLog = new MutableLiveData<>(new HashMap<>());
    private MutableLiveData<ConnectionThread> connection;

    public LiveData<Boolean> isBluetoothSupported() {
        return isBluetoothSupported;
    }

    public void setBluetoothSupported(boolean supported) {
        try {
            isBluetoothSupported.setValue(supported);
        } catch (IllegalStateException ignored) {
            isBluetoothSupported.postValue(supported);
        }
    }

    public LiveData<Boolean> isSearching() {
        return isSearching;
    }

    public void setSearching(boolean searching) {
        try {
            isSearching.setValue(searching);
        } catch (IllegalStateException ignored) {
            isSearching.postValue(searching);
        }
    }

    public LiveData<List<RecyclerViewItem>> getFoundDevices() {
        return foundDevices;
    }

    public void setFoundDevices(List<RecyclerViewItem> devices) {
        try {
            foundDevices.setValue(devices);
        } catch (IllegalStateException ignored) {
            foundDevices.postValue(devices);
        }
    }

    public LiveData<HashMap<String, StringBuilder>> getCommunicationLog() {
        return communicationLog;
    }

    public void setCommunicationLog(HashMap<String, StringBuilder> logs) {
        try {
            communicationLog.setValue(logs);
        } catch (IllegalStateException ignored) {
            communicationLog.postValue(logs);
        }
    }

    public LiveData<ConnectionThread> getConnectionThread() {
        return connection;
    }

    public void setConnectionThread(ConnectionThread connectionThread) {
        if(connection == null) {
            connection = new MutableLiveData<>();
        }
        try {
            connection.setValue(connectionThread);
        } catch (IllegalStateException ignored) {
            connection.postValue(connectionThread);
        }
    }
}
