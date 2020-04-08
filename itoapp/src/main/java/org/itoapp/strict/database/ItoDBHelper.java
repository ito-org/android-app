package org.itoapp.strict.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import androidx.annotation.Nullable;

import org.itoapp.strict.Helper;
import org.itoapp.strict.service.TracingService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.itoapp.strict.service.TracingService.HASH_LENGTH;

public class ItoDBHelper extends SQLiteOpenHelper {

    private static final String LOG_TAG = "ItoDBHelper";
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "ito.db";

    private static final String CREATE_BEACON_TABLE =
            "CREATE TABLE beacons (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "uuid BLOB NOT NULL UNIQUE ON CONFLICT IGNORE" +
                    ");";

    private static final String CREATE_BEACON_TIMESTAMP_INDEX =
            "CREATE UNIQUE INDEX beacon_timestamp ON beacons (timestamp);";

    private static final String CREATE_INFECTED_TABLE =
            "CREATE TABLE infected (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "uuid BLOB NOT NULL UNIQUE ON CONFLICT IGNORE," +
                    "hashed_uuid BLOB NOT NULL UNIQUE ON CONFLICT IGNORE" +
                    ");";

    private static final String CREATE_INFECTED_HASHED_UUID_INDEX =
            "CREATE UNIQUE INDEX infected_hashed_uuid ON infected (hashed_uuid);";

    private static final String CREATE_INFECTED_UUID_INDEX =
            "CREATE UNIQUE INDEX infected_uuid ON infected (uuid);";

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

    private static final String INSERT_BEACON =
            "INSERT OR IGNORE INTO beacons(uuid) VALUES (?);";

    private static final String INSERT_CONTACT =
            "INSERT OR IGNORE INTO contacts(hashed_uuid, proximity, duration) VALUES (?, ?, ?);";

    private static final String INSERT_INFECTED =
            "INSERT OR IGNORE INTO infected(uuid, hashed_uuid) VALUES (?, ?);";

    private static final String SELECT_INFECTED_CONTACTS =
            "SELECT contact.proximity, contact.duration " +
            "FROM contacts contact, infected infected " +
            "WHERE contact.timestamp < infected.timestamp AND " +
            "contact.hashed_uuid = infected.hashed_uuid;";

    private static final String SELECT_BEACONS =
            "SELECT uuid FROM beacons WHERE " +
                    "? < timestamp AND timestamp < ?;";

    private static final String SELECT_RANDOM_LAST_INFECTED =
            "SELECT uuid FROM infected WHERE " +
            "id = MAX(" +
                    "(SELECT MAX(id) from infected) - ABS(RANDOM() % 10)," +
                    "0);";

    private static final String DELETE_WHERE_CLAUSE =
            "julianday('now') - julianday(timestamp) > 14;";


    private final SQLiteStatement INSERT_BEACON_STATEMENT;
    private final SQLiteStatement INSERT_CONTACT_STATEMENT;
    private final SQLiteStatement INSERT_INFECTED_STATEMENT;

    public ItoDBHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        SQLiteDatabase database = getWritableDatabase();
        //insert statements
        INSERT_BEACON_STATEMENT = database.compileStatement(INSERT_BEACON);
        INSERT_CONTACT_STATEMENT = database.compileStatement(INSERT_CONTACT);
        INSERT_INFECTED_STATEMENT = database.compileStatement(INSERT_INFECTED);
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
        database.insertOrThrow("beacons", null, contentValues);

        //INSERT_BEACON_STATEMENT.bindBlob(1, uuid);
        //INSERT_BEACON_STATEMENT.executeInsert();
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
        /*
        INSERT_CONTACT_STATEMENT.bindBlob(1, hashed_uuid);
        INSERT_CONTACT_STATEMENT.bindLong(2, proximity);
        INSERT_CONTACT_STATEMENT.bindLong(3, duration);
        INSERT_CONTACT_STATEMENT.executeInsert();*/
    }

    public synchronized void insertInfected(byte[] uuid) {
        Log.d(LOG_TAG, "Inserting infected");
        checkUUID(uuid);

        SQLiteDatabase database = getWritableDatabase();
        ContentValues contentValues = new ContentValues(2);
        contentValues.put("uuid", uuid);
        contentValues.put("hashed_uuid", Helper.calculateTruncatedSHA256(uuid));
        database.insertWithOnConflict("infected", null, contentValues, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public synchronized List<byte[]> selectBeacons(Date from, Date to) {
        Log.d(LOG_TAG, "Querying beacons");
        /*SELECT_BEACONS_STATEMENT.bindLong(1, from.getTime());
        SELECT_BEACONS_STATEMENT.bindLong(2, to.getTime());*/

        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = database.query("beacons", //table
                new String[]{"uuid"}, //columns
                "datetime(?, 'unixepoch') < timestamp AND timestamp < datetime(?, 'unixepoch')", //selection
                new String[]{from.getTime() / 1000 + "", to.getTime() / 1000 + ""}, //selectionArgs
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

    public static class ContactResult {
        public int proximity;
        public int duration;
    }

    public synchronized List<ContactResult> selectInfectedContacts() {
        Log.d(LOG_TAG, "Selecting infected contacts");
        /*SELECT_BEACONS_STATEMENT.bindLong(1, from.getTime());
        SELECT_BEACONS_STATEMENT.bindLong(2, to.getTime());*/

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
        if(cursor.moveToNext())
            result = cursor.getBlob(uuidColumnIndex);
        cursor.close();
        return result;
    }

    public synchronized void cleanupDB() {
        Log.d(LOG_TAG, "Cleaning DB");

        SQLiteDatabase database = getWritableDatabase();
        database.delete("beacons", DELETE_WHERE_CLAUSE, null);
        database.delete("infected", DELETE_WHERE_CLAUSE, null);
        database.delete("contacts", DELETE_WHERE_CLAUSE, null);
    }
}
