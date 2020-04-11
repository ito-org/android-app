package org.itoapp.strict;

public class Constants {
    public static final int BLUETOOTH_COMPANY_ID = 65535; // TODO get a real company ID!
    public static final int UUID_LENGTH = 16;
    public static final int HASH_LENGTH = 26;
    public static final int BROADCAST_LENGTH = HASH_LENGTH + 1;
    public static final int UUID_VALID_INTERVAL = 1000 * 60 * 30; //ms * sec * 30 min
    public static final int CHECK_SERVER_INTERVAL = 1000 * 60 * 5; //ms * sec * 5 min
    public static final int DISTANCE_SMOOTHING_MA_LENGTH = 7;
    public static final long MIN_CONTACT_DURATION = 1000 * 60 * 3; //discard all contacts less than 3 minutes
}
