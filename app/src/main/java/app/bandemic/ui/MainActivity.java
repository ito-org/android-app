package app.bandemic.ui;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import app.bandemic.R;
import app.bandemic.fragments.ErrorMessageFragment;
import app.bandemic.fragments.NearbyDevicesFragment;
import app.bandemic.strict.service.BeaconCache;
import app.bandemic.strict.service.TracingService;
import app.bandemic.viewmodel.MainActivityViewModel;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    public static final String PREFERENCE_DATA_OK = "data_ok";

    private MainActivityViewModel mViewModel;
    private NearbyDevicesFragment nearbyDevicesFragment;
    private TracingService.TracingServiceBinder serviceBinder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mViewModel = new ViewModelProvider(this).get(MainActivityViewModel.class);

        SwipeRefreshLayout refreshLayout = findViewById(R.id.main_swipe_refresh_layout);
        refreshLayout.setOnRefreshListener(() -> {
            mViewModel.onRefresh();
        });
        mViewModel.eventRefresh().observe(this, refreshing -> {
            refreshLayout.setRefreshing(refreshing);
        });

        checkPermissions();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        if(!sharedPref.getBoolean(PREFERENCE_DATA_OK, false)) {
            startActivity(new Intent(this, Instructions.class));
        }
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
        else {
            startTracingService();
        }
    }

    private void startTracingService() {
        Intent intent = new Intent(this, TracingService.class);
        startService(intent);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    findViewById(R.id.card_permission_required).setVisibility(View.GONE);
                    // Start background service
                    startTracingService();
                } else {
                    // Don't start the discovery service
                    findViewById(R.id.card_permission_required).setVisibility(View.VISIBLE);
                    findViewById(R.id.ask_permission).setOnClickListener(view -> checkPermissions());
                }
                return;
            }
        }
    }

    private TracingService.ServiceStatusListener serviceStatusListener = serviceStatus -> {
        Log.i(TAG, "Service status: " + serviceStatus);
        runOnUiThread(() -> {
            if (serviceStatus == TracingService.STATUS_RUNNING) {
                if (nearbyDevicesFragment == null) {
                    nearbyDevicesFragment = new NearbyDevicesFragment();
                }
                if (nearbyDevicesFragment.model != null) {
                    nearbyDevicesFragment.model.distances.setValue(serviceBinder.getNearbyDevices());
                }
                nearbyDevicesFragment.skipAnimations();

                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                        .replace(R.id.fragment_nearby_devices, nearbyDevicesFragment)
                        .commit();
            } else {
                String errorMessage = "";
                if (serviceStatus == TracingService.STATUS_BLUETOOTH_NOT_ENABLED) {
                    errorMessage = getString(R.string.error_bluetooth_not_enabled);
                } else if (serviceStatus == TracingService.STATUS_LOCATION_NOT_ENABLED) {
                    errorMessage = getString(R.string.error_location_not_enabled);
                }
                ErrorMessageFragment errorMessageFragment = ErrorMessageFragment.newInstance(errorMessage);
                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                        .replace(R.id.fragment_nearby_devices, errorMessageFragment)
                        .commit();
            }
        });
    };

    BeaconCache.NearbyDevicesListener nearbyDevicesListener = new BeaconCache.NearbyDevicesListener() {
        @Override
        public void onNearbyDevicesChanged(double[] distances) {
            runOnUiThread(() -> {
                if (nearbyDevicesFragment != null) {
                    nearbyDevicesFragment.model.distances.setValue(distances);
                }
            });
        }
    };

    @Override
    protected void onStop() {
        serviceBinder.removeNearbyDevicesListener(nearbyDevicesListener);
        serviceBinder.removeServiceStatusListener(serviceStatusListener);
        unbindService(connection);
        super.onStop();
    }

    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.i(TAG, "Service connected");
            serviceBinder = (TracingService.TracingServiceBinder) service;

            serviceBinder.addServiceStatusListener(serviceStatusListener);
            serviceStatusListener.serviceStatusChanged(serviceBinder.getServiceStatus());

            serviceBinder.addNearbyDevicesListener(nearbyDevicesListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.i(TAG, "Service disconnected");
            serviceBinder = null;
        }
    };

}
