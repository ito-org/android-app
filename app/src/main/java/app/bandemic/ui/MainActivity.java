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
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import app.bandemic.R;
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
    private TracingService.ServiceStatusListener serviceStatusListener;

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

        nearbyDevicesFragment = (NearbyDevicesFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_nearby_devices);
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

    BeaconCache.NearbyDevicesListener nearbyDevicesListener = new BeaconCache.NearbyDevicesListener() {
        @Override
        public void onNearbyDevicesChanged(double[] distances) {
            runOnUiThread(() -> {
                nearbyDevicesFragment.model.distances.setValue(distances);
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
            serviceBinder.addNearbyDevicesListener(nearbyDevicesListener);
            runOnUiThread(() -> {
                // Get nearby devices once in case we missed some updates
                nearbyDevicesFragment.model.distances.setValue(serviceBinder.getNearbyDevices());
                nearbyDevicesFragment.skipAnimations();
            });

            serviceStatusListener = serviceStatus -> {
                Log.i(TAG, "Service status: " + serviceStatus);
                runOnUiThread(() -> {
                    switch (serviceStatus) {
                        case TracingService.STATUS_BLUETOOTH_NOT_ENABLED:
                            Toast toast = Toast.makeText(MainActivity.this, "Bluetooth needs to be enabled", Toast.LENGTH_LONG);
                            toast.show();
                            break;
                        case TracingService.STATUS_LOCATION_NOT_ENABLED:
                            Toast toast2 = Toast.makeText(MainActivity.this, "Location needs to be enabled", Toast.LENGTH_LONG);
                            toast2.show();
                            break;
                        case TracingService.STATUS_RUNNING:
                            Toast toast3 = Toast.makeText(MainActivity.this, "Service running", Toast.LENGTH_SHORT);
                            toast3.show();
                            break;
                    }
                });
            };
            serviceBinder.addServiceStatusListener(serviceStatusListener);
            serviceStatusListener.serviceStatusChanged(serviceBinder.getServiceStatus());
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.i(TAG, "Service disconnected");
            serviceBinder = null;
        }
    };

}
