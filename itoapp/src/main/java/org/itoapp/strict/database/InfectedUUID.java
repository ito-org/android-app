package org.itoapp.strict.database;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

@Entity(indices = {@Index(value = {"hashedUUID"}, unique = true)})
public class InfectedUUID {
    @PrimaryKey(autoGenerate = true)
    @SerializedName("id")
    public int id = 0;
    @SerializedName("created_on")
    public Date createdOn;
    @SerializedName("uuid")
    public byte[] uuid;
    @SerializedName("hashedUUID")
    public byte[] hashedUUID;

    public InfectedUUID(
            Date createdOn,
            byte[] uuid,
            byte[] hashedUUID
    ) {
        this.createdOn = createdOn;
        this.uuid = uuid;
        this.hashedUUID = hashedUUID;
    }
}
