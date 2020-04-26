package com.swy2k.bletest.fragments;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.swy2k.bletest.ConnectionThread;
import com.swy2k.bletest.MainActivityViewModel;
import com.swy2k.bletest.R;
import com.swy2k.bletest.databinding.FragmentBleListBinding;
import com.swy2k.bletest.managers.ConnectionAggregator;
import com.swy2k.bletest.managers.FoundDeviceAggregator;
import com.swy2k.bletest.recyclerviews.LinearLayoutManagerWrapper;
import com.swy2k.bletest.recyclerviews.BLEList.RecyclerViewAdapter;
import com.swy2k.bletest.recyclerviews.BLEList.RecyclerViewItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

public class BLEListFragment extends Fragment {
    private FragmentBleListBinding binding;
    private NavController navController;
    private MainActivityViewModel mainActivityViewModel;

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;

    private RecyclerViewAdapter recyclerViewAdapter;

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if(recyclerViewAdapter != null && result.getDevice() != null) {
                RecyclerViewItem item = RecyclerViewItem.builder()
                        .name(result.getDevice().getName())
                        .macAddress(result.getDevice().getAddress())
                        .status(RecyclerViewItem.STATUS_CONNECTIBLE).build();

                recyclerViewAdapter.addItem(item);
            }
        }
    };

    private final Runnable stopScanCallback = new Runnable() {
        @Override
        public void run() {
            if(mainActivityViewModel.isSearching() != null && mainActivityViewModel.isSearching().getValue() != null
                    && mainActivityViewModel.isSearching().getValue()) {
                mainActivityViewModel.setSearching(false);
                bluetoothLeScanner.stopScan(scanCallback);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBleListBinding.inflate(inflater, container, false);
        navController = Navigation.findNavController(requireParentFragment().requireView());
        mainActivityViewModel = new ViewModelProvider(navController.getViewModelStoreOwner(R.id.main_nav)).get(MainActivityViewModel.class);
        binding.setMainActivityViewModel(mainActivityViewModel);
        binding.setLifecycleOwner(this);

        bluetoothManager = (BluetoothManager) requireContext().getSystemService(Context.BLUETOOTH_SERVICE);
        if(bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            bluetoothAdapter.enable();
        }

        if(bluetoothManager == null || bluetoothAdapter == null) {
            mainActivityViewModel.setBluetoothSupported(false);
        }

        binding.searchButton.setOnClickListener(v -> {
            if(mainActivityViewModel.isBluetoothSupported() != null && mainActivityViewModel.isBluetoothSupported().getValue() != null
                    && mainActivityViewModel.isSearching() != null && mainActivityViewModel.isSearching().getValue() != null
                    // Why does it keep showing Null Pointer Exception warning even the values are verified that they are not null?
                    && mainActivityViewModel.isBluetoothSupported().getValue() && !mainActivityViewModel.isSearching().getValue()) {
                recyclerViewAdapter.clearItem();
                mainActivityViewModel.setSearching(true);
                bluetoothLeScanner.startScan(scanCallback);
                binding.getRoot().postDelayed(stopScanCallback, 10000);
            }
        });

        binding.stopSearchingButton.setOnClickListener(v -> {
            if(mainActivityViewModel.isBluetoothSupported() != null && mainActivityViewModel.isBluetoothSupported().getValue() != null
                && mainActivityViewModel.isSearching() != null && mainActivityViewModel.isSearching().getValue() != null
                && mainActivityViewModel.isBluetoothSupported().getValue() && mainActivityViewModel.isSearching().getValue()) {
                binding.getRoot().removeCallbacks(stopScanCallback);
                binding.getRoot().post(stopScanCallback);
            }
        });

        binding.openConsoleButton.setOnClickListener(v -> navController.navigate(R.id.main_nav_action_to_ble_console));

        recyclerViewAdapter = new RecyclerViewAdapter(getLayoutInflater(), (ViewGroup) binding.getRoot()) {

            @Override
            public void onClick(RecyclerViewItem item) {
                int index = FoundDeviceAggregator.indexOfNotConnectibleDeviceItem(mainActivityViewModel);
                if(index > -1) {
                    FoundDeviceAggregator.setItemState(mainActivityViewModel, index, RecyclerViewItem.STATUS_CONNECTIBLE);
                }

                item.setStatus(RecyclerViewItem.STATUS_CONNECTING);
                recyclerViewAdapter.addItem(item);
                mainActivityViewModel.setFoundDevices(recyclerViewAdapter.getAllItem());

                if(mainActivityViewModel.getConnectionThread() != null && mainActivityViewModel.getConnectionThread().getValue() != null) {
                    mainActivityViewModel.getConnectionThread().getValue().exit();
                    mainActivityViewModel.setConnectionThread(null);
                }
                ConnectionAggregator.startConnection(mainActivityViewModel, requireContext(), bluetoothAdapter.getRemoteDevice(item.getMacAddress()));
                if(mainActivityViewModel.getConnectionThread() != null && mainActivityViewModel.getConnectionThread().getValue() != null) {
                    mainActivityViewModel.getConnectionThread().getValue().setOnConnectionListener(new ConnectionThread.OnConnectionListener() {
                        @Override
                        public void onSuccess() {
                            item.setStatus(RecyclerViewItem.STATUS_CONNECTED);
                            recyclerViewAdapter.addItem(item);
                            mainActivityViewModel.setFoundDevices(recyclerViewAdapter.getAllItem());
                        }

                        @Override
                        public void onError(int status, int newState) {
                            item.setStatus(RecyclerViewItem.STATUS_ERROR);
                            recyclerViewAdapter.addItem(item);
                            mainActivityViewModel.setFoundDevices(recyclerViewAdapter.getAllItem());
                            mainActivityViewModel.getConnectionThread().getValue().exit();
                        }
                    });
                }
            }
        };

        binding.listRecyclerviewSurface.setLayoutManager(new LinearLayoutManagerWrapper(requireContext(), LinearLayoutManager.VERTICAL, false));
        binding.listRecyclerviewSurface.setAdapter(recyclerViewAdapter);
        binding.listRecyclerviewSurface.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));

        if(mainActivityViewModel.getFoundDevices() != null) {
            if(mainActivityViewModel.getFoundDevices().getValue() != null && mainActivityViewModel.getFoundDevices().getValue().size() > 0) {
                recyclerViewAdapter.addAllItem(mainActivityViewModel.getFoundDevices().getValue());
            }
            mainActivityViewModel.getFoundDevices().observe(getViewLifecycleOwner(), list -> {
                recyclerViewAdapter.addAllItem(list);
                if(FoundDeviceAggregator.indexOfNotConnectibleDeviceItem(mainActivityViewModel) < 0 && mainActivityViewModel.getConnectionThread() != null
                        && mainActivityViewModel.getConnectionThread().getValue() != null) {
                    mainActivityViewModel.getConnectionThread().getValue().exit();
                }
            });
        }

        return binding.getRoot();
    }

    @Override
    public void onPause() {
        super.onPause();

        if(recyclerViewAdapter != null && recyclerViewAdapter.getAllItem().size() > 0) {
            mainActivityViewModel.setFoundDevices(recyclerViewAdapter.getAllItem());
        }
    }
}
