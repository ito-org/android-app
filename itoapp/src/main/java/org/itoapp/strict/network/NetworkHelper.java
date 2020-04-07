package org.itoapp.strict.network;

import android.util.Log;

import org.apache.commons.codec.binary.Hex;
import org.itoapp.strict.database.ItoDBHelper;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static org.itoapp.strict.service.TracingService.UUID_LENGTH;

public class NetworkHelper {

    private static final String LOG_TAG = "InfectedUUIDRepository";
    private static final String BASE_URL = "http://192.168.1.10:5000/";

    public static void refreshInfectedUUIDs(ItoDBHelper dbHelper) {
        byte[] lastInfectedUUID = dbHelper.selectRandomLastUUID();
        HttpURLConnection urlConnection = null;
        try {
            //TODO use a more sophisticated library
            String appendix = lastInfectedUUID == null ? "" : "?uuid=" + Hex.encodeHexString(lastInfectedUUID);
            URL url = new URL(BASE_URL + "get_uuids" + appendix);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.addRequestProperty("Accept", "application/octet-stream");
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            byte[] uuid = new byte[UUID_LENGTH];
            while (in.read(uuid) == UUID_LENGTH) {
                dbHelper.insertInfected(uuid);
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
