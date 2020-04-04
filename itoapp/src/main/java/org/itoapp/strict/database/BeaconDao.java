package org.itoapp.strict.database;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface BeaconDao {
    @Query("SELECT * FROM beacon")
    LiveData<List<Beacon>> getAll();

    @Query("SELECT * FROM beacon")
    LiveData<List<Beacon>> getAllDistinctBroadcast();

    /*@Query("SELECT * FROM user WHERE uid IN (:userIds)")
    List<User> loadAllByIds(int[] userIds);

    @Query("SELECT * FROM user WHERE first_name LIKE :first AND " +
            "last_name LIKE :last LIMIT 1")
    User findByName(String first, String last);*/

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(Beacon... beacons);

    @Delete
    void delete(Beacon beacon);
}
