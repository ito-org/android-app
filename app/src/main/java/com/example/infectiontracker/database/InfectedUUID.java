package com.example.infectiontracker.database;

import java.util.Date;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class InfectedUUID {
    @PrimaryKey
    public int id;
    public Date createdOn;
    public int distrustLevel;
    public byte[] hashedId; // this is actually double hashed
    public String icdCode;
}
