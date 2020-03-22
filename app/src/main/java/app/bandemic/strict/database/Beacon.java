package app.bandemic.strict.database;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Beacon {

    @PrimaryKey
    public int id = 0;
    public byte[] receivedHash;
    public byte[] receivedDoubleHash;
    public Date timestamp;
    public double distance;

    public Beacon(byte[] receivedHash,
                  Date timestamp,
                  double distance) {
        this.receivedHash = receivedHash;
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        this.receivedDoubleHash = digest.digest(receivedHash);
        this.timestamp = timestamp;
        this.distance = distance;
    }
}
