package com.example.infectiontracker;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private final int MY_QR_CODE_SCAN = 2;

    private final boolean showContactLogger = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermissions();

        if(showContactLogger) {
            startActivity(new Intent(this, ContactLogger.class));
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
            case MY_QR_CODE_SCAN: {
                if(resultCode == Activity.RESULT_OK) {
                    Snackbar.make(findViewById(R.id.myCoordinatorLayout),
                            R.string.qr_added_contact, Snackbar.LENGTH_SHORT).show();
                }
            } break;
        }
    }

    private void startTracingService() {
        Intent intent = new Intent(this, TracingService.class);
        startService(intent);
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

    public void onAddQrCode(View v) {
        startActivityForResult(new Intent(this, QrContact.class), MY_QR_CODE_SCAN);
    }

}
