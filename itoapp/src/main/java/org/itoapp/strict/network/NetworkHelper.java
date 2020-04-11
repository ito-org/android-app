package org.itoapp.strict.network;

import android.util.Log;

import org.itoapp.strict.Helper;
import org.itoapp.strict.database.ItoDBHelper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static org.itoapp.strict.Helper.bytesToUUID;
import static org.itoapp.strict.Helper.uuidToBytes;
import static org.itoapp.strict.Constants.UUID_LENGTH;

public class NetworkHelper {

    private static final String LOG_TAG = "InfectedUUIDRepository";
    private static final String BASE_URL = "https://api.ito-app.org";

    public static void refreshInfectedUUIDs(ItoDBHelper dbHelper) {
        byte[] lastInfectedUUID = dbHelper.selectRandomLastUUID();
        UUID lastInfectedUUID2 = lastInfectedUUID == null ? UUID.randomUUID() : Helper.bytesToUUID(lastInfectedUUID);
        HttpURLConnection urlConnection = null;
        try {
            //TODO use a more sophisticated library
            String appendix = "?uuid=" + lastInfectedUUID2.toString();
            URL url = new URL(BASE_URL + "/pull/v0/cases" + appendix);
            urlConnection = (HttpURLConnection) url.openConnection();
            InputStreamReader inputStreamReader = new InputStreamReader(new BufferedInputStream(urlConnection.getInputStream()));
            byte[] uuidBytes = new byte[UUID_LENGTH];
            char[] tempBuffer = new char[37];
            while (inputStreamReader.read(tempBuffer) == tempBuffer.length) {
                UUID uuid = UUID.fromString(new String(tempBuffer, 0, 36));
                uuidToBytes(uuid, uuidBytes);
                dbHelper.insertInfected(uuidBytes);
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

    private static void writeUUIDJson(byte[] uuid, Writer outputStreamWriter) throws IOException {
        outputStreamWriter.write("{\"uuid\":\"" + bytesToUUID(uuid).toString() + "\"}");
    }

    public static void publishUUIDs(List<byte[]> beacons) throws IOException {
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(BASE_URL + "/push/v0/cases/report");
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.addRequestProperty("Content-Type", "application/json");
            Writer outputStreamWriter = new OutputStreamWriter(new BufferedOutputStream(urlConnection.getOutputStream()));
            outputStreamWriter.write("[");

            Iterator<byte[]> iterator = beacons.iterator();
            if (iterator.hasNext())
                writeUUIDJson(iterator.next(), outputStreamWriter);

            while (iterator.hasNext()) {
                outputStreamWriter.write(",");
                writeUUIDJson(iterator.next(), outputStreamWriter);
            }
            outputStreamWriter.write("]");
            outputStreamWriter.close();

            InputStreamReader inputStreamReader = new InputStreamReader(new BufferedInputStream(urlConnection.getInputStream()));
            inputStreamReader.read();
            inputStreamReader.close();
        } catch (MalformedURLException e) {
            Log.wtf(LOG_TAG, "Malformed URL?!", e);
            throw new RuntimeException(e);
        } finally {
            if (urlConnection != null)
                urlConnection.disconnect();
        }
    }
}
