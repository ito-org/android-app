package org.itoapp.strict.database;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface OwnUUIDDao {
    @Query("SELECT * FROM ownuuid")
    LiveData<List<OwnUUID>> getAll();

    /*@Query("SELECT * FROM user WHERE uid IN (:userIds)")
    List<User> loadAllByIds(int[] userIds);

    @Query("SELECT * FROM user WHERE first_name LIKE :first AND " +
            "last_name LIKE :last LIMIT 1")
    User findByName(String first, String last);*/

    @Insert
    void insertAll(OwnUUID... uuids);

    @Delete
    void delete(OwnUUID ownUUID);
}
