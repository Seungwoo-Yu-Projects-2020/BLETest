package com.swy2k.bletest;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Stack;
import java.util.UUID;

import androidx.annotation.NonNull;

public abstract class ConnectionThread {
    private final BluetoothDevice bluetoothDevice;
    private BluetoothGatt bluetoothGatt;
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    private final HandlerThread connectionThread;
    private final Handler connectionThreadHandler;

    private boolean isConnected = false;
    private boolean readAsByte = true;

    private UUID serviceUUID;
    private UUID writeCharacteristicUUID;
    private UUID readCharacteristicUUID;
    private UUID notificationCharacteristicUUID;
    private UUID writeDescriptorUUID;
    private UUID readDescriptorUUID;

    private BluetoothGattService service;
    private BluetoothGattCharacteristic writeCharacteristic;
    private BluetoothGattCharacteristic readCharacteristic;
    private BluetoothGattCharacteristic notificationCharacteristic;
    private BluetoothGattDescriptor writeDescriptor;
    private BluetoothGattDescriptor readDescriptor;

    private OnConnectionListener onConnectionListener;
    private OnServiceListener onServiceListener;
    private final Stack<OnCharacteristicReadListener> onCharacteristicReadListenerStack = new Stack<>();
    private final Stack<OnCharacteristicWriteListener> onCharacteristicWriteListenerStack = new Stack<>();
    private final Stack<OnDescriptorReadListener> onDescriptorReadListenerStack = new Stack<>();
    private final Stack<OnDescriptorWriteListener> onDescriptorWriteListenerStack = new Stack<>();

    private final HashMap<Integer, OnNotificationCharacteristicListener> notificationCallbacks = new HashMap<>();
    private final HashMap<Integer, BluetoothGattCharacteristic> notificationHolder = new HashMap<>();

    private final BluetoothGattCallback callback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                isConnected = true;
                onConnectionListener.onSuccess();
                bluetoothGatt.discoverServices();
            } else {
                isConnected = false;
                onConnectionListener.onError(status, newState);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if(status == BluetoothGatt.GATT_SUCCESS) {
                onServiceListener.onSuccess();
            } else {
                onServiceListener.onError(status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            try {
                if(status == BluetoothGatt.GATT_SUCCESS) {
                    if (readAsByte) {
                        onCharacteristicReadListenerStack.pop().onSuccess(characteristic.getValue());
                    } else {
                        onCharacteristicReadListenerStack.pop().onSuccess(new String(characteristic.getValue()));
                    }
                } else {
                    onCharacteristicReadListenerStack.pop().onError(status);
                }
            } catch (EmptyStackException ignored) { }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            try {
                if(status == BluetoothGatt.GATT_SUCCESS) {
                    onCharacteristicWriteListenerStack.pop().onSuccess();
                } else {
                    onCharacteristicWriteListenerStack.pop().onError(status);
                }
            } catch (EmptyStackException ignored) { }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            try {
                int index = indexOf(notificationHolder.values(), characteristic);
                if(index > -1) {
                    OnNotificationCharacteristicListener listener = notificationCallbacks.get(index);
                    if(listener != null) {
                        if (readAsByte) {
                            listener.onSuccess(characteristic.getValue());
                        } else {
                            listener.onSuccess(new String(characteristic.getValue()));
                        }
                    }
                }
            } catch (EmptyStackException ignored) { }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            try {
                if(status == BluetoothGatt.GATT_SUCCESS) {
                    if (readAsByte) {
                        onDescriptorReadListenerStack.pop().onSuccess(descriptor.getValue());
                    } else {
                        onDescriptorReadListenerStack.pop().onSuccess(new String(descriptor.getValue()));
                    }
                } else {
                    onDescriptorReadListenerStack.pop().onError(status);
                }
            } catch (EmptyStackException ignored) { }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            try {
                if(status == BluetoothGatt.GATT_SUCCESS) {
                    onDescriptorWriteListenerStack.pop().onSuccess();
                } else {
                    onDescriptorWriteListenerStack.pop().onError(status);
                }
            } catch (EmptyStackException ignored) { }
        }
    };

    public ConnectionThread(@NonNull Context context, @NonNull BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;

        connectionThread = new HandlerThread("connection-thread");
        connectionThread.start();
        connectionThreadHandler = new Handler(connectionThread.getLooper());
        connectionThreadHandler.post(() -> {
            bluetoothGatt = this.bluetoothDevice.connectGatt(context, false, callback);
            onThreadCreated();
        });
    }

    public ConnectionThread(@NonNull Context context, @NonNull BluetoothDevice bluetoothDevice, boolean readAsByte) {
        this(context, bluetoothDevice);
        this.readAsByte = readAsByte;
    }

    public abstract void onThreadCreated();

    public void startConnection() {
        if(!isConnected) {
            bluetoothGatt.connect();
        }
    }

    public void stopConnection() {
        if(isConnected) {
            bluetoothGatt.disconnect();
        }
    }

    public Handler getConnectionHandler() {
        return connectionThreadHandler;
    }

    public void setReadAsByte(boolean readAsByte) {
        this.readAsByte = readAsByte;
    }

    public void setService(UUID serviceUUID) {
        service = bluetoothGatt.getService(serviceUUID);
    }

    public void writeCharacteristic(byte[] data) {
        if(isConnected) {
            if (writeCharacteristic == null || !writeCharacteristic.getUuid().equals(writeCharacteristicUUID)) {
                if (service == null || !service.getUuid().equals(serviceUUID)) {
                    service = bluetoothGatt.getService(serviceUUID);
                }
                writeCharacteristic = service.getCharacteristic(writeCharacteristicUUID);
            }
            writeCharacteristic.setValue(data);
            bluetoothGatt.writeCharacteristic(writeCharacteristic);
        }
    }

    public void writeCharacteristic(String data) {
        if(isConnected) {
            writeCharacteristic(data.getBytes());
        }
    }

    public void writeCharacteristic(byte[] data, OnCharacteristicWriteListener listener) {
        if(isConnected) {
            onCharacteristicWriteListenerStack.push(listener);
            writeCharacteristic(data);
        }
    }

    public void writeCharacteristic(String data, OnCharacteristicWriteListener listener) {
        if(isConnected) {
            onCharacteristicWriteListenerStack.push(listener);
            writeCharacteristic(data);
        }
    }

    public void writeCharacteristicWithUUIDs(byte[] data, UUID serviceUUID, UUID writeCharacteristicUUID, boolean setNewUUIDAsDefault) {
        if(isConnected) {
            BluetoothGattService service = bluetoothGatt.getService(serviceUUID);
            BluetoothGattCharacteristic characteristic = null;
            if (service != null) {
                characteristic = service.getCharacteristic(writeCharacteristicUUID);
            }

            if (service != null && characteristic != null) {
                bluetoothGatt.writeCharacteristic(characteristic);
                if (setNewUUIDAsDefault) {
                    this.service = service;
                    writeCharacteristic = characteristic;
                }
            }
        }
    }

    public void writeCharacteristicWithUUIDs(String data, UUID serviceUUID, UUID writeCharacteristicUUID, boolean setNewUUIDAsDefault) {
        if(isConnected) {
            writeCharacteristicWithUUIDs(data.getBytes(), serviceUUID, writeCharacteristicUUID, setNewUUIDAsDefault);
        }
    }

    public void writeCharacteristicWithUUIDs(byte[] data, OnCharacteristicWriteListener listener, UUID serviceUUID, UUID writeCharacteristicUUID, boolean setNewUUIDAsDefault) {
        if(isConnected) {
            onCharacteristicWriteListenerStack.push(listener);
            writeCharacteristicWithUUIDs(data, serviceUUID, writeCharacteristicUUID, setNewUUIDAsDefault);
        }
    }

    public void writeCharacteristicWithUUIDs(String data, OnCharacteristicWriteListener listener, UUID serviceUUID, UUID writeCharacteristicUUID, boolean setNewUUIDAsDefault) {
        if(isConnected) {
            onCharacteristicWriteListenerStack.push(listener);
            writeCharacteristicWithUUIDs(data, serviceUUID, writeCharacteristicUUID, setNewUUIDAsDefault);
        }
    }

    public void readCharacteristic() {
        if(isConnected) {
            if (readCharacteristic == null || !readCharacteristic.getUuid().equals(readCharacteristicUUID)) {
                if (service == null || !service.getUuid().equals(serviceUUID)) {
                    service = bluetoothGatt.getService(serviceUUID);
                }
                readCharacteristic = service.getCharacteristic(readCharacteristicUUID);
            }
            bluetoothGatt.readCharacteristic(readCharacteristic);
        }
    }

    public void readCharacteristic(OnCharacteristicReadListener listener) {
        if(isConnected) {
            onCharacteristicReadListenerStack.push(listener);
            readCharacteristic();
        }
    }

    public void readCharacteristicWithUUIDs(UUID serviceUUID, UUID readCharacteristicUUID, boolean setNewUUIDAsDefault) {
        if(isConnected) {
            BluetoothGattService service = bluetoothGatt.getService(serviceUUID);
            BluetoothGattCharacteristic characteristic = null;
            if (service != null) {
                characteristic = service.getCharacteristic(readCharacteristicUUID);
            }

            if (service != null && characteristic != null) {
                bluetoothGatt.readCharacteristic(characteristic);
                if (setNewUUIDAsDefault) {
                    this.service = service;
                    readCharacteristic = characteristic;
                }
            }
        }
    }

    public void readCharacteristicWithUUIDs(OnCharacteristicReadListener listener, UUID serviceUUID, UUID readCharacteristicUUID, boolean setNewUUIDAsDefault) {
        if(isConnected) {
            onCharacteristicReadListenerStack.push(listener);
            readCharacteristicWithUUIDs(serviceUUID, readCharacteristicUUID, setNewUUIDAsDefault);
        }
    }

    public BluetoothGattCharacteristic setNotificationCharacteristic() {
        if(isConnected) {
            if (notificationCharacteristic == null || !notificationCharacteristic.getUuid().equals(notificationCharacteristicUUID)) {
                if (service == null || !service.getUuid().equals(serviceUUID)) {
                    service = bluetoothGatt.getService(serviceUUID);
                }
                notificationCharacteristic = service.getCharacteristic(notificationCharacteristicUUID);
            }
            bluetoothGatt.setCharacteristicNotification(notificationCharacteristic, true);
            notificationHolder.put(notificationHolder.size(), notificationCharacteristic);
            return notificationCharacteristic;
        }
        return null;
    }

    public BluetoothGattCharacteristic setNotificationCharacteristic(OnNotificationCharacteristicListener listener) {
        if(isConnected) {
            notificationCallbacks.put(notificationHolder.size(), listener);
        }
        return setNotificationCharacteristic();
    }

    public BluetoothGattCharacteristic setNotificationCharacteristicWithUUIDs(UUID serviceUUID, UUID notificationCharacteristicUUID, boolean setNewUUIDAsDefault) {
        if(isConnected) {
            BluetoothGattService service = bluetoothGatt.getService(serviceUUID);
            BluetoothGattCharacteristic characteristic = null;
            if (service != null) {
                characteristic = service.getCharacteristic(notificationCharacteristicUUID);
            }

            if (service != null && characteristic != null) {
                bluetoothGatt.setCharacteristicNotification(characteristic, true);
                notificationHolder.put(notificationHolder.size(), characteristic);
                if (setNewUUIDAsDefault) {
                    this.service = service;
                    notificationCharacteristic = characteristic;
                }
                return characteristic;
            }
        }
        return null;
    }

    public BluetoothGattCharacteristic setNotificationCharacteristicWithUUIDs(OnNotificationCharacteristicListener listener, UUID serviceUUID, UUID notificationCharacteristicUUID, boolean setNewUUIDAsDefault) {
        if(isConnected) {
            notificationCallbacks.put(notificationHolder.size(), listener);
        }
        return setNotificationCharacteristicWithUUIDs(serviceUUID, notificationCharacteristicUUID, setNewUUIDAsDefault);
    }

    public void removeNotificationCharacteristic() {
        if(isConnected) {
            final int index = indexOf(notificationHolder.values(), notificationCharacteristic);
            if (index < 0) return;
            notificationHolder.remove(index);
            bluetoothGatt.setCharacteristicNotification(notificationHolder.remove(index), false);
        }
    }

    public void removeNotificationCharacteristic(BluetoothGattCharacteristic target) {
        if(isConnected) {
            final int index = indexOf(notificationHolder.values(), target);
            if (index < 0) return;
            notificationHolder.remove(index);
            bluetoothGatt.setCharacteristicNotification(notificationHolder.remove(index), false);
        }
    }

    public BluetoothGattDescriptor writeDescriptor(byte[] data) {
        if(isConnected) {
            if (writeDescriptor == null || !writeDescriptor.getUuid().equals(writeDescriptorUUID)) {
                if (service == null || !service.getUuid().equals(serviceUUID)) {
                    service = bluetoothGatt.getService(serviceUUID);
                }
                if (notificationCharacteristic == null || !notificationCharacteristic.getUuid().equals(notificationCharacteristicUUID)) {
                    notificationCharacteristic = service.getCharacteristic(notificationCharacteristicUUID);
                }
                writeDescriptor = notificationCharacteristic.getDescriptor(writeDescriptorUUID);
            }
            writeDescriptor.setValue(data);
            bluetoothGatt.writeDescriptor(writeDescriptor);
        }
        return writeDescriptor;
    }

    public BluetoothGattDescriptor writeDescriptor(byte[] data, OnDescriptorWriteListener listener) {
        if(isConnected) {
            onDescriptorWriteListenerStack.push(listener);
            return writeDescriptor(data);
        }
        return null;
    }

    public BluetoothGattDescriptor writeDescriptor(String data) {
        return writeDescriptor(data.getBytes());
    }

    public BluetoothGattDescriptor writeDescriptor(String data, OnDescriptorWriteListener listener) {
        if(isConnected) {
            onDescriptorWriteListenerStack.push(listener);
        }
        return writeDescriptor(data.getBytes());
    }

    public BluetoothGattDescriptor writeDescriptorWithUUIDs(byte[] data, UUID notificationCharacteristicUUID, UUID serviceUUID, UUID writeDescriptorUUID, boolean setNewUUIDAsDefault) {
        if(isConnected) {
            BluetoothGattService service = bluetoothGatt.getService(serviceUUID);
            BluetoothGattCharacteristic notificationCharacteristic = null;
            BluetoothGattDescriptor writeDescriptor = null;
            if (service != null) {
                notificationCharacteristic = service.getCharacteristic(notificationCharacteristicUUID);
                if (notificationCharacteristic != null) {
                    writeDescriptor = notificationCharacteristic.getDescriptor(writeDescriptorUUID);
                }
            }

            if (service != null && notificationCharacteristic != null && writeDescriptor != null) {
                writeDescriptor.setValue(data);
                notificationCharacteristic.addDescriptor(writeDescriptor);
                if (indexOf(notificationHolder.values(), notificationCharacteristic) < 0) {
                    bluetoothGatt.setCharacteristicNotification(notificationCharacteristic, true);
                    notificationHolder.put(notificationHolder.size(), notificationCharacteristic);
                    if (setNewUUIDAsDefault) {
                        this.service = service;
                        this.notificationCharacteristic = notificationCharacteristic;
                        this.writeDescriptor = writeDescriptor;
                    }
                }
            }
        }

        return writeDescriptor;
    }

    public BluetoothGattDescriptor writeDescriptorWithUUIDs(byte[] data, OnDescriptorWriteListener listener, UUID notificationCharacteristicUUID, UUID serviceUUID, UUID writeDescriptorUUID, boolean setNewUUIDAsDefault) {
        if(isConnected) {
            onDescriptorWriteListenerStack.push(listener);
        }
        return writeDescriptorWithUUIDs(data, notificationCharacteristicUUID, serviceUUID, writeDescriptorUUID, setNewUUIDAsDefault);
    }

    public BluetoothGattDescriptor writeDescriptorWithUUIDs(String data, UUID notificationCharacteristicUUID, UUID serviceUUID, UUID writeDescriptorUUID, boolean setNewUUIDAsDefault) {
        return writeDescriptorWithUUIDs(data.getBytes(), notificationCharacteristicUUID, serviceUUID, writeDescriptorUUID, setNewUUIDAsDefault);
    }

    public BluetoothGattDescriptor writeDescriptorWithUUIDs(String data, OnDescriptorWriteListener listener, UUID notificationCharacteristicUUID, UUID serviceUUID, UUID writeDescriptorUUID, boolean setNewUUIDAsDefault) {
        if(isConnected) {
            onDescriptorWriteListenerStack.push(listener);
        }
        return writeDescriptorWithUUIDs(data.getBytes(), notificationCharacteristicUUID, serviceUUID, writeDescriptorUUID, setNewUUIDAsDefault);
    }

    public void readDescriptor() {
        if(isConnected) {
            if (readDescriptor == null || !readDescriptor.getUuid().equals(readDescriptorUUID)) {
                if (service == null || !service.getUuid().equals(serviceUUID)) {
                    service = bluetoothGatt.getService(serviceUUID);
                }
                if (notificationCharacteristic == null || !notificationCharacteristic.getUuid().equals(notificationCharacteristicUUID)) {
                    notificationCharacteristic = service.getCharacteristic(notificationCharacteristicUUID);
                }
                readDescriptor = notificationCharacteristic.getDescriptor(readDescriptorUUID);
            }
            bluetoothGatt.readDescriptor(readDescriptor);
        }
    }

    public void readDescriptor(OnDescriptorReadListener listener) {
        if(isConnected) {
            onDescriptorReadListenerStack.push(listener);
        }
        readDescriptor();
    }

    public void readDescriptor(BluetoothGattDescriptor target) {
        bluetoothGatt.readDescriptor(target);
    }

    public void readDescriptor(BluetoothGattDescriptor target, OnDescriptorReadListener listener) {
        if(isConnected) {
            onDescriptorReadListenerStack.push(listener);
        }
        bluetoothGatt.readDescriptor(target);
    }

    public void runOnUiThread(Runnable runnable) {
        mainThreadHandler.post(runnable);
    }

    public void setServiceUUID(UUID uuid) {
        serviceUUID = uuid;
    }

    public void setWriteCharacteristicUUID(UUID uuid) {
        writeCharacteristicUUID = uuid;
    }

    public void setReadCharacteristicUUID(UUID uuid) {
        readCharacteristicUUID = uuid;
    }

    public void setNotificationCharacteristicUUID(UUID uuid) {
        notificationCharacteristicUUID = uuid;
    }

    public void setWriteDescriptorUUID(UUID uuid) {
        writeDescriptorUUID = uuid;
    }

    public void setReadDescriptorUUID(UUID uuid) {
        readDescriptorUUID = uuid;
    }

    public void setOnConnectionListener(OnConnectionListener listener) {
        onConnectionListener = listener;
    }

    public void setOnServiceListener(OnServiceListener listener) {
        onServiceListener = listener;
    }

    public void addOnCharacteristicReadListener(OnCharacteristicReadListener listener) {
        onCharacteristicReadListenerStack.push(listener);
    }

    public void addOnCharacteristicWriteListener(OnCharacteristicWriteListener listener) {
        onCharacteristicWriteListenerStack.push(listener);
    }

    public void addOnDescriptorReadListener(OnDescriptorReadListener listener) {
        onDescriptorReadListenerStack.push(listener);
    }

    public void addOnDescriptorWriteListener(OnDescriptorWriteListener listener) {
        onDescriptorWriteListenerStack.push(listener);
    }

    public void exit() {
        if(bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
        if(!connectionThread.isInterrupted()) {
            connectionThread.quit();
        }
    }

    private <T> int indexOf(Collection<T> list, T target) {
        return new ArrayList<>(list).indexOf(target);
    }

    public abstract static class OnConnectionListener {
        public abstract void onSuccess();
        public abstract void onError(int status, int newState);
    }

    public abstract static class OnServiceListener {
        public abstract void onSuccess();
        public abstract void onError(int status);
    }

    public abstract static class OnCharacteristicReadListener {
        public void onSuccess(String readData) { }
        public void onSuccess(byte[] readData) { }
        public abstract void onError(int status);
    }

    public abstract static class OnCharacteristicWriteListener {
        public void onSuccess() { }
        public abstract void onError(int status);
    }

    public static class OnNotificationCharacteristicListener {
        public void onSuccess(String readData) { }
        public void onSuccess(byte[] readData) { }
    }

    public abstract static class OnDescriptorReadListener {
        public void onSuccess(String readData) { }
        public void onSuccess(byte[] readData) { }
        public abstract void onError(int status);
    }

    public abstract static class OnDescriptorWriteListener {
        public void onSuccess() { }
        public abstract void onError(int status);
    }
}
