package org.itoapp.strict.database;

import java.util.Date;

import androidx.room.ColumnInfo;

public class Infection {
    @ColumnInfo(name = "id")
    public int infectionId;
    @ColumnInfo(name = "timestamp")
    public Date encounterDate;
    public double distance;
    public Date createdOn;
    public int distrustLevel;
    public String icdCode;
}
