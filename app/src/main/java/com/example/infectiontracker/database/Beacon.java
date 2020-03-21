package com.example.infectiontracker.database;

import java.util.Date;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Beacon {

    @PrimaryKey
    @NonNull
    public byte[] receivedHash;
    public UUID ownUUID;
    public Date timestamp;
    public int risk;

    public Beacon(byte[] receivedHash,
                  UUID ownUUID,
                  Date timestamp,
                  int risk) {
        this.receivedHash = receivedHash;
        this.ownUUID = ownUUID;
        this.timestamp = timestamp;
        this.risk = risk;
    }
}
