package app.bandemic.strict.database;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class InfectedUUID {
    @PrimaryKey
    @SerializedName("id")
    public int id;
    @SerializedName("created_on")
    public Date createdOn;
    @SerializedName("distrust_level")
    public int distrustLevel;
    @SerializedName("hashed_id")
    public byte[] hashedId; // this is actually double hashed
    @SerializedName("icd_code")
    public String icdCode;

    public InfectedUUID(
            int id,
            Date createdOn,
            int distrustLevel,
            byte[] hashedId,
            String icdCode
    ) {
        this.id = id;
        this.createdOn = createdOn;
        this.distrustLevel = distrustLevel;
        this.hashedId = hashedId;
        this.icdCode = icdCode;
    }
}
