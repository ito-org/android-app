package org.itoapp.strict.repository;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;

import org.apache.commons.codec.binary.Hex;
import org.itoapp.strict.Helper;
import org.itoapp.strict.database.AppDatabase;
import org.itoapp.strict.database.InfectedUUID;
import org.itoapp.strict.database.InfectedUUIDDao;
import org.itoapp.strict.database.Infection;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;

import static org.itoapp.strict.service.TracingService.UUID_LENGTH;

public class InfectedUUIDRepository {

    private static final String LOG_TAG = "InfectedUUIDRepository";
    private static final String BASE_URL = "http://192.168.1.10:5000/";


    private InfectedUUIDDao infectedUUIDDao;

    public InfectedUUIDRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        infectedUUIDDao = db.infectedUUIDDao();
    }

    public LiveData<List<InfectedUUID>> getInfectedUUIDs() {
        return infectedUUIDDao.getAll();
    }

    public LiveData<List<Infection>> getPossiblyInfectedEncounters() {
        return infectedUUIDDao.getPossiblyInfectedEncounters();
    }

    private void insertInfectedUUID(byte[] infectedUUID) {
        byte[] hashedUUID = Helper.calculateTruncatedSHA256(infectedUUID);
        InfectedUUID infectedUuidObj = new InfectedUUID(new Date(System.currentTimeMillis()), infectedUUID, hashedUUID);
        infectedUUIDDao.insertAll(infectedUuidObj);
    }

    public void refreshInfectedUUIDs() {
        List<InfectedUUID> infectedUUIDs = getInfectedUUIDs().getValue();
        InfectedUUID lastInfectedUUID = (infectedUUIDs == null || infectedUUIDs.isEmpty()) ? null : infectedUUIDs.get(infectedUUIDs.size() - 1);
        HttpURLConnection urlConnection = null;
        try {
            //TODO use a more sophisticated library
            String appendix = lastInfectedUUID == null ? "" : "?uuid=" + Hex.encodeHexString(lastInfectedUUID.uuid);
            URL url = new URL(BASE_URL + "get_uuids" + appendix);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.addRequestProperty("Accept", "application/octet-stream");
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            byte[] uuid = new byte[UUID_LENGTH];
            while (in.read(uuid) == UUID_LENGTH) {
                insertInfectedUUID(uuid);
            }

        } catch (MalformedURLException e) {
            Log.wtf(LOG_TAG, "Malformed URL?!", e);
            throw new RuntimeException(e);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null)
                urlConnection.disconnect();
        }
    }
}
