package org.itoapp;

import org.itoapp.strict.database.ItoDBHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class DatabaseTest {
    @Test
    public void dbOpen() {
        new ItoDBHelper(null);
    }
}
