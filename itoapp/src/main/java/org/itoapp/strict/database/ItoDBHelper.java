package org.itoapp.strict.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import org.itoapp.strict.Helper;
import org.itoapp.strict.service.TracingService;

import java.util.ArrayList;
import java.util.List;

import static org.itoapp.strict.service.TracingService.HASH_LENGTH;

public class ItoDBHelper extends SQLiteOpenHelper {

    private static final String LOG_TAG = "ItoDBHelper";
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "ito.db";

    private static final String BEACON_TABLE_NAME = "beacons";
    private static final String INFECTED_TABLE_NAME = "infected";

    private static final String CREATE_BEACON_TABLE =
            "CREATE TABLE "+BEACON_TABLE_NAME+" (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "uuid BLOB NOT NULL UNIQUE ON CONFLICT IGNORE" +
                    ");";

    private static final String CREATE_BEACON_TIMESTAMP_INDEX =
            "CREATE UNIQUE INDEX beacon_timestamp ON "+BEACON_TABLE_NAME+" (timestamp);";

    private static final String CREATE_INFECTED_TABLE =
            "CREATE TABLE " + INFECTED_TABLE_NAME + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "uuid BLOB NOT NULL UNIQUE ON CONFLICT IGNORE," +
                    "hashed_uuid BLOB NOT NULL UNIQUE ON CONFLICT IGNORE" +
                    ");";

    private static final String CREATE_INFECTED_HASHED_UUID_INDEX =
            "CREATE UNIQUE INDEX " + INFECTED_TABLE_NAME + "_hashed_uuid ON infected (hashed_uuid);";

    private static final String CREATE_INFECTED_UUID_INDEX =
            "CREATE UNIQUE INDEX " + INFECTED_TABLE_NAME + "_uuid ON infected (uuid);";

    private static final String CREATE_CONTACT_TABLE =
            "CREATE TABLE contacts (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "hashed_uuid BLOB NOT NULL UNIQUE ON CONFLICT IGNORE," +
                    "proximity INTEGER," +
                    "duration INTEGER" +
                    ");";

    private static final String CREATE_CONTACT_HASHED_UUID_INDEX =
            "CREATE UNIQUE INDEX contact_hashed_uuid ON contacts (hashed_uuid);";

    private static final String SELECT_INFECTED_CONTACTS =
            "SELECT contact.proximity, contact.duration " +
                    "FROM contacts contact, " + INFECTED_TABLE_NAME + " infected " +
                    "WHERE contact.timestamp < infected.timestamp AND " +
                    "contact.hashed_uuid = infected.hashed_uuid;";

    private static final String SELECT_RANDOM_LAST_INFECTED =
            "SELECT uuid FROM " + INFECTED_TABLE_NAME + " WHERE " +
                    "id = MAX(" +
                    "(SELECT MAX(id) from " + INFECTED_TABLE_NAME + ") - ABS(RANDOM() % 10)," +
                    "0);";

    private static final String DELETE_WHERE_CLAUSE =
            "julianday('now') - julianday(timestamp) > 14;";


    public ItoDBHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_BEACON_TABLE);
        db.execSQL(CREATE_BEACON_TIMESTAMP_INDEX);
        db.execSQL(CREATE_INFECTED_TABLE);
        db.execSQL(CREATE_INFECTED_HASHED_UUID_INDEX);
        db.execSQL(CREATE_INFECTED_UUID_INDEX);
        db.execSQL(CREATE_CONTACT_TABLE);
        db.execSQL(CREATE_CONTACT_HASHED_UUID_INDEX);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // not yet applicable
    }

    private void checkUUID(byte[] uuid) {
        if (uuid == null || uuid.length != TracingService.UUID_LENGTH)
            throw new IllegalArgumentException();
    }

    private void checkHashedUUID(byte[] hashedUUID) {

        if (hashedUUID == null || hashedUUID.length != HASH_LENGTH)
            throw new IllegalArgumentException();
    }

    public synchronized void insertBeacon(byte[] uuid) {
        Log.d(LOG_TAG, "Inserting beacon");
        checkUUID(uuid);

        SQLiteDatabase database = getWritableDatabase();
        ContentValues contentValues = new ContentValues(1);
        contentValues.put("uuid", uuid);
        database.insertOrThrow(BEACON_TABLE_NAME, null, contentValues);
    }

    public synchronized void insertContact(byte[] hashed_uuid, int proximity, int duration) {
        Log.d(LOG_TAG, "Inserting contact");
        checkHashedUUID(hashed_uuid);

        SQLiteDatabase database = getWritableDatabase();
        ContentValues contentValues = new ContentValues(3);
        contentValues.put("hashed_uuid", hashed_uuid);
        contentValues.put("proximity", proximity);
        contentValues.put("duration", duration);
        database.insertOrThrow("contacts", null, contentValues);
    }

    public synchronized void insertInfected(byte[] uuid) {
        Log.d(LOG_TAG, "Inserting infected");
        checkUUID(uuid);

        SQLiteDatabase database = getWritableDatabase();
        ContentValues contentValues = new ContentValues(2);
        contentValues.put("uuid", uuid);
        contentValues.put("hashed_uuid", Helper.calculateTruncatedSHA256(uuid));
        database.insertWithOnConflict(INFECTED_TABLE_NAME, null, contentValues, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public synchronized List<byte[]> selectBeacons(long from, long to) {
        Log.d(LOG_TAG, "Querying beacons");

        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = database.query(BEACON_TABLE_NAME, //table
                new String[]{"uuid"}, //columns
                "datetime(?, 'unixepoch') < timestamp AND timestamp < datetime(?, 'unixepoch')", //selection
                new String[]{from / 1000 + "", to / 1000 + ""}, //selectionArgs
                null, //groupBy
                null, //having
                null, //orderBy
                null //limit
        );
        List<byte[]> result = new ArrayList<>();
        int columnIndex = cursor.getColumnIndexOrThrow("uuid");
        while (cursor.moveToNext()) {
            byte[] uuid = cursor.getBlob(columnIndex);
            result.add(uuid);
        }
        cursor.close();
        return result;
    }

    public synchronized List<ContactResult> selectInfectedContacts() {
        Log.d(LOG_TAG, "Selecting infected contacts");

        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = database.rawQuery(SELECT_INFECTED_CONTACTS, null);
        List<ContactResult> result = new ArrayList<>();
        int proximityColumnIndex = cursor.getColumnIndexOrThrow("proximity");
        int durationColumnIndex = cursor.getColumnIndexOrThrow("duration");
        while (cursor.moveToNext()) {
            ContactResult contactResult = new ContactResult();
            contactResult.proximity = cursor.getInt(proximityColumnIndex);
            contactResult.duration = cursor.getInt(durationColumnIndex);
            result.add(contactResult);
        }
        cursor.close();
        return result;
    }

    public synchronized byte[] selectRandomLastUUID() {
        Log.d(LOG_TAG, "Querying random uuid");
        SQLiteDatabase database = getReadableDatabase();

        Cursor cursor = database.rawQuery(SELECT_RANDOM_LAST_INFECTED, null);
        byte[] result = null;
        int uuidColumnIndex = cursor.getColumnIndexOrThrow("uuid");
        if (cursor.moveToNext())
            result = cursor.getBlob(uuidColumnIndex);
        cursor.close();
        return result;
    }

    public synchronized void cleanupDB() {
        Log.d(LOG_TAG, "Cleaning DB");

        SQLiteDatabase database = getWritableDatabase();
        database.delete(BEACON_TABLE_NAME, DELETE_WHERE_CLAUSE, null);
        database.delete(INFECTED_TABLE_NAME, DELETE_WHERE_CLAUSE, null);
        database.delete("contacts", DELETE_WHERE_CLAUSE, null);
    }

    public static class ContactResult {
        public int proximity;
        public int duration;
    }
}
