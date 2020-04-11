package org.itoapp.strict.service;

import android.os.AsyncTask;
import android.os.RemoteException;
import android.util.Log;

import org.itoapp.PublishUUIDsCallback;
import org.itoapp.strict.database.ItoDBHelper;
import org.itoapp.strict.network.NetworkHelper;

import java.io.IOException;

class PublishBeaconsTask extends AsyncTask<Void, Void, Void> {
    private static final String LOG_TAG = "PublishBeaconsTask";
    private ItoDBHelper dbHelper;
    private long from;
    private long to;
    private PublishUUIDsCallback callback;

    public PublishBeaconsTask(ItoDBHelper dbHelper, long from, long to, PublishUUIDsCallback callback) {
        this.dbHelper = dbHelper;
        this.from = from;
        this.to = to;
        this.callback = callback;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        try {
            NetworkHelper.publishUUIDs(dbHelper.selectBeacons(from, to));
            try {
                callback.onSuccess();
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "._.", e);
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Could not publish UUIDs!", e);
            try {
                callback.onFailure();
            } catch (RemoteException ex) {
                Log.e(LOG_TAG, "._.", e);
            }
        }
        return null;
    }
}
