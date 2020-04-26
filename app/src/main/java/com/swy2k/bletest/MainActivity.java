package com.swy2k.bletest;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.swy2k.bletest.databinding.ActivityMainBinding;
import com.swy2k.bletest.managers.FoundDeviceAggregator;
import com.swy2k.bletest.recyclerviews.BLEList.RecyclerViewItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;
    private MainActivityViewModel mainActivityViewModel;

    private String[] requiredPermissions = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
    };

    private static final int PERMISSION_REQUEST_CODE = 1;

    private AlertDialog dialogForInsufficientPermittedPermission;
    private Snackbar snackbarForCurrentConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.main_nav_host);
        if(navHostFragment != null) {
            navController = navHostFragment.getNavController();
            mainActivityViewModel = new ViewModelProvider(navController.getViewModelStoreOwner(R.id.main_nav)).get(MainActivityViewModel.class);
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if(!navController.navigateUp()) {
                    finish();
                }
            }
        });

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            List<String> list = Arrays.asList(requiredPermissions);
            list.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
            requiredPermissions = list.toArray(new String[0]);
        }

        grantPermissions(checkPermissions(requiredPermissions));

        mainActivityViewModel.getFoundDevices().observe(this, ignored -> {
            final int index = FoundDeviceAggregator.indexOfNotConnectibleDeviceItem(mainActivityViewModel);
            if(index > -1) {
                RecyclerViewItem item = FoundDeviceAggregator.getNotConnectibleDeviceItem(mainActivityViewModel);
                if(item != null) {
                    String name;
                    String currentState = "";

                    if(item.getName() == null || item.getName().equals("")) {
                        name = getResources().getString(R.string.no_name);
                    } else {
                        name = item.getName();
                    }

                    switch (item.getStatus()) {
                        case RecyclerViewItem.STATUS_CONNECTIBLE:
                            return;
                        case RecyclerViewItem.STATUS_CONNECTING:
                            currentState = getResources().getString(R.string.connecting);
                            break;
                        case RecyclerViewItem.STATUS_CONNECTED:
                            currentState = getResources().getString(R.string.connected);
                            break;
                        case RecyclerViewItem.STATUS_ERROR:
                            currentState = getResources().getString(R.string.error);
                            break;
                    }

                    if(snackbarForCurrentConnection != null && snackbarForCurrentConnection.isShown()) {
                        snackbarForCurrentConnection.dismiss();
                    }
                    snackbarForCurrentConnection = Snackbar.make(binding.getRoot(), name + " " + item.getMacAddress() + " " + currentState, Snackbar.LENGTH_INDEFINITE);
                    if(item.getStatus() == RecyclerViewItem.STATUS_CONNECTED) {
                        snackbarForCurrentConnection.setAction(R.string.cancel_connection, v -> FoundDeviceAggregator.setItemState(mainActivityViewModel, index, RecyclerViewItem.STATUS_CONNECTIBLE));
                    } else if(item.getStatus() != RecyclerViewItem.STATUS_CONNECTING) {
                        snackbarForCurrentConnection.setDuration(BaseTransientBottomBar.LENGTH_LONG);
                    }
                    snackbarForCurrentConnection.show();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mainActivityViewModel.getConnectionThread() != null && mainActivityViewModel.getConnectionThread().getValue() != null) {
            mainActivityViewModel.getConnectionThread().getValue().exit();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == PERMISSION_REQUEST_CODE) {
            if(checkPermissions(requiredPermissions).length > 0) {
                showDialogForInsufficientPermittedPermission();
            }
        }
    }

    private String[] checkPermissions(String[] requiredPermissions) {
        if(requiredPermissions == null || requiredPermissions.length < 1) return null;
        final List<String> notGrantedPermissions = new ArrayList<>();

        for (String requiredPermission : requiredPermissions) {
            if (ActivityCompat.checkSelfPermission(this, requiredPermission) != PackageManager.PERMISSION_GRANTED) {
                notGrantedPermissions.add(requiredPermission);
            }
        }

        return notGrantedPermissions.toArray(new String[0]);
    }

    private void grantPermissions(String[] permissions) {
        if(permissions == null || permissions.length < 1) return;

        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
    }

    private void showDialogForInsufficientPermittedPermission() {
        if(dialogForInsufficientPermittedPermission == null) {
            dialogForInsufficientPermittedPermission = new AlertDialog.Builder(this)
                    .setTitle(R.string.insufficient_permission_dialog_title)
                    .setMessage(R.string.insufficient_permission_dialog_message)
                    .setCancelable(false)
                    .setPositiveButton(R.string.confirm, (i1, i2) -> finish()).create();
        }
        if(!dialogForInsufficientPermittedPermission.isShowing()) {
            dialogForInsufficientPermittedPermission.show();
        }
    }
}
