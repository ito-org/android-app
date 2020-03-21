package com.example.infectiontracker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.samples.vision.barcodereader.BarcodeCapture;
import com.google.android.gms.samples.vision.barcodereader.BarcodeGraphic;
import com.google.android.gms.vision.barcode.Barcode;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import xyz.belvi.mobilevisionbarcodescanner.BarcodeRetriever;

    public class QrContact extends AppCompatActivity implements BarcodeRetriever {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.qr_contact);

        BarcodeCapture barcodeCapture = (BarcodeCapture) getSupportFragmentManager().findFragmentById(R.id.barcode);
        barcodeCapture.setShowDrawRect(true);
        barcodeCapture.setRetrieval(this);
    }

    @Override
    public void onRetrieved(Barcode barcode) {

        if(barcode.valueFormat!=Barcode.CONTACT_INFO
            && barcode.valueFormat!=Barcode.EMAIL) {
            notSupportedTagAlert();
            return;
        }

        //TODO: Add contact to db here
        if(barcode.valueFormat==Barcode.CONTACT_INFO) {
            Log.d("QR", "name: "+barcode.contactInfo.name.formattedName
                    + " email: "+barcode.contactInfo.emails[0].address
                    + " phone: "+barcode.contactInfo.phones[0].number);
        } else {
            Log.d("QR", "email: "+barcode.email);
        }

        setResult(Activity.RESULT_OK, new Intent());
        finish();
    }

    private void notSupportedTagAlert()
    {
        runOnUiThread( () -> {
                new AlertDialog.Builder(this)
                        .setTitle(getText(R.string.qr_code_error))
                        .setMessage(getText(R.string.qr_code_not_supported))
                        .setIcon(R.mipmap.ic_launcher).show();
        });
    }

    @Override
    public void onRetrievedMultiple(Barcode closetToClick, List<BarcodeGraphic> barcode) {

    }

    @Override
    public void onBitmapScanned(SparseArray<Barcode> sparseArray) {

    }

    @Override
    public void onRetrievedFailed(String reason) {

    }

    @Override
    public void onPermissionRequestDenied() {

    }
}
