package app.bandemic.strict.database;

import java.util.Date;
import java.util.UUID;

import androidx.room.TypeConverter;

public class Converters {

    @TypeConverter
    public static UUID fromStringUUID(String value) {
        return UUID.fromString(value);
    }

    @TypeConverter
    public static String toStringUUID(UUID uuid) {
        return uuid.toString();
    }

    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }

}
