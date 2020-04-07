package org.itoapp.strict;

import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static org.itoapp.strict.service.TracingService.HASH_LENGTH;

public class Helper {

    private static final String LOG_TAG = "Helper";
    private static MessageDigest sha256MessageDigest;

    private Helper() {
    }

    public static byte[] calculateTruncatedSHA256(byte[] uuid) {
        if(sha256MessageDigest == null) {
            try {
                sha256MessageDigest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                Log.wtf(LOG_TAG, "Algorithm not found", e);
                throw new RuntimeException(e);
            }
        }

        byte[] sha256Hash = sha256MessageDigest.digest(uuid);
        return Arrays.copyOf(sha256Hash, HASH_LENGTH);
    }
}
