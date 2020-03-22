package app.bandemic.strict.database;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface SettingsDao {
    @Query("SELECT * FROM Setting")
    List<Setting> getAll();

    @Query("SELECT value FROM Setting WHERE key=:key")
    String getSetting(String key);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertSettting(Setting setting);
}
