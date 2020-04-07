package org.itoapp.strict.database;

import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Beacon {
    private static final String LOG_TAG = "Beacon";

    @PrimaryKey(autoGenerate = true)
    public int id = 0;
    public byte[] receivedHash;
    public Date timestamp;
    public double distance;
    public long duration;

    public Beacon(byte[] receivedHash, Date timestamp, long duration, double distance) {
        this.receivedHash = receivedHash;
        this.timestamp = timestamp;
        this.duration = duration;
        this.distance = distance;
    }
}
