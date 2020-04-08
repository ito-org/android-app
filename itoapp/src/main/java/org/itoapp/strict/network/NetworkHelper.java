package org.itoapp.strict.network;

import android.util.Log;

import org.itoapp.strict.database.ItoDBHelper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import static org.itoapp.strict.service.TracingService.UUID_LENGTH;

public class NetworkHelper {

    private static final String LOG_TAG = "InfectedUUIDRepository";
    private static final String BASE_URL = "http://192.168.1.10:5000/";

    private static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    private static String encodeHexString(byte[] data) {
        StringBuilder result = new StringBuilder();
        // two characters form the hex value.
        for (byte b : data) {
            result.append(HEX_DIGITS[(0xF0 & b) >>> 4]);
            result.append(HEX_DIGITS[0x0F & b]);
        }
        return result.toString();

    }

    public static void refreshInfectedUUIDs(ItoDBHelper dbHelper) {
        byte[] lastInfectedUUID = dbHelper.selectRandomLastUUID();
        HttpURLConnection urlConnection = null;
        try {
            //TODO use a more sophisticated library
            String appendix = lastInfectedUUID == null ? "" : "?uuid=" + encodeHexString(lastInfectedUUID);
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

    public static void publishUUIDs(List<byte[]> selectBeacons) throws IOException {
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(BASE_URL + "post_uuids");
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.addRequestProperty("Content-Type", "application/octet-stream");
            OutputStream os = new BufferedOutputStream(urlConnection.getOutputStream());
            for (byte[] uuid : selectBeacons) {
                os.write(uuid);
            }
            os.close();
        } catch (MalformedURLException e) {
            Log.wtf(LOG_TAG, "Malformed URL?!", e);
            throw new RuntimeException(e);
        } finally {
            if (urlConnection != null)
                urlConnection.disconnect();
        }
    }
}
