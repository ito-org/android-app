package com.example.infectiontracker.repository;

import android.app.Application;

import com.example.infectiontracker.database.AppDatabase;
import com.example.infectiontracker.database.Beacon;
import com.example.infectiontracker.database.BeaconDao;
import com.example.infectiontracker.database.OwnUUID;
import com.example.infectiontracker.database.OwnUUIDDao;

import java.util.List;

import androidx.lifecycle.LiveData;

public class BroadcastRepository {

    private OwnUUIDDao mOwnUUIDDao;
    private LiveData<List<OwnUUID>> mAllOwnUUIDs;

    public BroadcastRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        mOwnUUIDDao = db.ownUUIDDao();
        mAllOwnUUIDs = mOwnUUIDDao.getAll();
    }

    LiveData<List<OwnUUID>> getAllOwnUUIDs() {
        return mAllOwnUUIDs;
    }

    public void insert(OwnUUID ownUUID) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mOwnUUIDDao.insertAll(ownUUID);
        });
    }
}
