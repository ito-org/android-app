package com.example.infectiontracker.repository;

import android.app.Application;
import android.util.Log;

import com.example.infectiontracker.database.AppDatabase;
import com.example.infectiontracker.database.Beacon;
import com.example.infectiontracker.database.BeaconDao;
import com.example.infectiontracker.database.OwnUUID;
import com.example.infectiontracker.database.OwnUUIDDao;

import java.util.List;

import androidx.lifecycle.LiveData;

public class BroadcastRepository {

    private OwnUUIDDao mOwnUUIDDao;
    private BeaconDao mBeaconDao;
    private LiveData<List<OwnUUID>> mAllOwnUUIDs;
    private LiveData<List<Beacon>> mAllBeacons;

    public BroadcastRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        mOwnUUIDDao = db.ownUUIDDao();
        mBeaconDao = db.beaconDao();
        mAllOwnUUIDs = mOwnUUIDDao.getAll();
        mAllOwnUUIDs = mOwnUUIDDao.getAll();
    }

    public LiveData<List<OwnUUID>> getAllOwnUUIDs() {
        return mAllOwnUUIDs;
    }

    public LiveData<List<Beacon>> getAllBeacons() {
        return mAllBeacons;
    }

    public void insertOwnUUID(OwnUUID ownUUID) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mOwnUUIDDao.insertAll(ownUUID);
        });
    }

    public void insertBeacon(Beacon beacon) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mBeaconDao.insertAll(beacon);
        });
    }
}
