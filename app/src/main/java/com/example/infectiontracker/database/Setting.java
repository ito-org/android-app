package com.example.infectiontracker.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(indices = {@Index(value = {"key"}, unique=true) } )
public class Setting {
    @PrimaryKey(autoGenerate = true)
    public int id = 0;

    @ColumnInfo(name = "key")
    public String key;
    @ColumnInfo(name = "value")
    public String value;

    public Setting(String key, String value) {
        this.key = key;
        this.value = value;
    }
}
