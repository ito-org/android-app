package org.itoapp.strict.service;

import android.os.AsyncTask;
import android.util.Log;

import org.itoapp.strict.database.ItoDBHelper;
import org.itoapp.strict.network.NetworkHelper;

import java.util.List;

public class CheckServerTask extends AsyncTask<Void, Void, Void> {
    private static final String LOG_TAG = "CheckServerTask";
    private ItoDBHelper dbHelper;

    public CheckServerTask(ItoDBHelper itoDBHelper) {
        this.dbHelper = itoDBHelper;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        NetworkHelper.refreshInfectedUUIDs(dbHelper);
        List<ItoDBHelper.ContactResult> contactResults = dbHelper.selectInfectedContacts();
        if (!contactResults.isEmpty()) {
            Log.w(LOG_TAG, "Possibly encountered UUIDs: " + contactResults.size());
        }
        return null;
    }
}
