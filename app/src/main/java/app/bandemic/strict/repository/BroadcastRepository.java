package app.bandemic.strict.repository;

import android.app.Application;

import app.bandemic.strict.database.AppDatabase;
import app.bandemic.strict.database.Beacon;
import app.bandemic.strict.database.BeaconDao;
import app.bandemic.strict.database.OwnUUID;
import app.bandemic.strict.database.OwnUUIDDao;

import java.util.List;

import androidx.lifecycle.LiveData;

public class BroadcastRepository {

    private OwnUUIDDao mOwnUUIDDao;
    private BeaconDao mBeaconDao;
    private LiveData<List<OwnUUID>> mAllOwnUUIDs;
    private LiveData<List<Beacon>> mAllBeacons;
    private LiveData<List<Beacon>> mDistinctBeacons;

    public BroadcastRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        mOwnUUIDDao = db.ownUUIDDao();
        mBeaconDao = db.beaconDao();
        mAllOwnUUIDs = mOwnUUIDDao.getAll();
        mAllBeacons = mBeaconDao.getAll();
        mDistinctBeacons = mBeaconDao.getAllDistinctBroadcast();
    }

    public LiveData<List<OwnUUID>> getAllOwnUUIDs() {
        return mAllOwnUUIDs;
    }

    public LiveData<List<Beacon>> getAllBeacons() {
        return mAllBeacons;
    }

    public LiveData<List<Beacon>> getDistinctBeacons() {
        return mDistinctBeacons;
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
