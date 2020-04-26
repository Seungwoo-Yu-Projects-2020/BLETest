package com.swy2k.bletest.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.swy2k.bletest.ConnectionThread;
import com.swy2k.bletest.MainActivityViewModel;
import com.swy2k.bletest.R;
import com.swy2k.bletest.databinding.FragmentBleConsoleBinding;
import com.swy2k.bletest.managers.CommunicationLogAggregator;
import com.swy2k.bletest.managers.FoundDeviceAggregator;
import com.swy2k.bletest.recyclerviews.BLEConsole.RecyclerViewAdapter;
import com.swy2k.bletest.recyclerviews.BLEList.RecyclerViewItem;
import com.swy2k.bletest.recyclerviews.LinearLayoutManagerWrapper;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

public class BLEConsoleFragment extends Fragment {
    private FragmentBleConsoleBinding binding;
    private NavController navController;
    private MainActivityViewModel mainActivityViewModel;

    private RecyclerViewAdapter recyclerViewAdapter;

    private AtomicReference<ConnectionThread> connectionThread = new AtomicReference<>();
    private String targetMacAddress;
    private StringBuilder logs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBleConsoleBinding.inflate(inflater, container, false);
        navController = Navigation.findNavController(requireParentFragment().requireView());
        mainActivityViewModel = new ViewModelProvider(navController.getViewModelStoreOwner(R.id.main_nav)).get(MainActivityViewModel.class);

        recyclerViewAdapter = new RecyclerViewAdapter(getLayoutInflater(), (ViewGroup) binding.getRoot());

        binding.consoleRecyclerviewSurface.setLayoutManager(new LinearLayoutManagerWrapper(requireContext(), LinearLayoutManager.VERTICAL, false));
        binding.consoleRecyclerviewSurface.setAdapter(recyclerViewAdapter);
        binding.consoleRecyclerviewSurface.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));

        if(mainActivityViewModel.getFoundDevices() != null && mainActivityViewModel.getFoundDevices().getValue() != null
                && mainActivityViewModel.getCommunicationLog() != null && mainActivityViewModel.getCommunicationLog().getValue() != null) {
            if(FoundDeviceAggregator.indexOfNotConnectibleDeviceItem(mainActivityViewModel) > -1) {
                final RecyclerViewItem item = FoundDeviceAggregator.getNotConnectibleDeviceItem(mainActivityViewModel);
                if(item != null) {
                    targetMacAddress = item.getMacAddress();
                    logs = CommunicationLogAggregator.getCommunicationLogByAddress(mainActivityViewModel, targetMacAddress);
                }
            }
        }

        if(logs != null && logs.length() > 0) {
            recyclerViewAdapter.addAllItem(Arrays.asList(logs.toString().split("\n")));
        }

        binding.sendButton.setOnClickListener(v -> {
            if(binding.textInput.getText() != null && !binding.textInput.getText().toString().equals("")
                    && targetMacAddress != null && !targetMacAddress.equals("") && connectionThread.get() != null) {
                Log.e("sendData", binding.textInput.getText().toString());
                connectionThread.get().writeCharacteristic(binding.textInput.getText().toString());
                CommunicationLogAggregator.setCommunicationLogByAddress(mainActivityViewModel, targetMacAddress, "<'>'> " + binding.textInput.getText().toString());
                binding.textInput.setText("");
            }
        });

        mainActivityViewModel.getCommunicationLog().observe(getViewLifecycleOwner(), ignored -> {
            final StringBuilder stringBuilder = CommunicationLogAggregator.getCommunicationLogByAddress(mainActivityViewModel, targetMacAddress);
            if(stringBuilder != null && stringBuilder.length() > 0) {
                recyclerViewAdapter.clearItem();
                recyclerViewAdapter.addAllItem(Arrays.asList(stringBuilder.toString().split("\n")));
            }
        });

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();

        if(mainActivityViewModel.getConnectionThread() != null && mainActivityViewModel.getConnectionThread().getValue() != null
                && mainActivityViewModel.getFoundDevices() != null && mainActivityViewModel.getFoundDevices().getValue() != null) {
            if(FoundDeviceAggregator.indexOfNotConnectibleDeviceItem(mainActivityViewModel) > -1) {
                final RecyclerViewItem item = FoundDeviceAggregator.getNotConnectibleDeviceItem(mainActivityViewModel);
                if(item != null) {
                    connectionThread.set(mainActivityViewModel.getConnectionThread().getValue());
                    connectionThread.get().setServiceUUID(UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"));
                    connectionThread.get().setWriteCharacteristicUUID(UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb"));
                    connectionThread.get().setNotificationCharacteristicUUID(UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb"));
                    connectionThread.get().setReadAsByte(false);
                    connectionThread.get().setNotificationCharacteristic(new ConnectionThread.OnNotificationCharacteristicListener(){
                        @Override
                        public void onSuccess(String readData) {
                            Log.e("readData1", readData);
                            CommunicationLogAggregator.setCommunicationLogByAddress(mainActivityViewModel, targetMacAddress, "<'<'> " + readData);
                        }
                    });
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if(connectionThread.get() != null && mainActivityViewModel != null) {
            mainActivityViewModel.setConnectionThread(connectionThread.get());
        }
    }
}
