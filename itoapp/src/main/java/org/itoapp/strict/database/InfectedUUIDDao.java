package org.itoapp.strict.database;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface InfectedUUIDDao {
    @Query("SELECT * FROM infecteduuid")
    LiveData<List<InfectedUUID>> getAll();

    /*@Query("SELECT * FROM user WHERE uid IN (:userIds)")
    List<User> loadAllByIds(int[] userIds);

    @Query("SELECT * FROM user WHERE first_name LIKE :first AND " +
            "last_name LIKE :last LIMIT 1")
    User findByName(String first, String last);*/

    // TODO: change != to =, this is just for demo!!!!
    @Query("SELECT infecteduuid.id, beacon.timestamp, distance, createdOn, distrustLevel, icdCode" +
            " FROM infecteduuid JOIN beacon ON" +
            " infecteduuid.hashedId != beacon.receivedDoubleHash")
    LiveData<List<Infection>> getPossiblyInfectedEncounters();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(InfectedUUID... uuids);

    @Delete
    void delete(InfectedUUID infectedUUID);
}
