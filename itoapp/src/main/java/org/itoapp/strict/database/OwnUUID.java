package org.itoapp.strict.database;

import java.util.Date;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class OwnUUID {
    @NonNull
    @PrimaryKey
    public byte[] ownUUID;
    public Date timestamp;

    public OwnUUID(@NonNull byte[] ownUUID, Date timestamp) {
        this.ownUUID = ownUUID;
        this.timestamp = timestamp;
    }
}
