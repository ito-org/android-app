package org.itoapp;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.itoapp.strict.Helper;
import org.itoapp.strict.database.ItoDBHelper;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.Random;

import static org.itoapp.strict.service.TracingService.UUID_LENGTH;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals
        ;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class DatabaseInstrumentedTest {
    ItoDBHelper itoDBHelper;

    public void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }

    @Before
    public void initializeDbHelper() {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        context.deleteDatabase("ito.db");
        itoDBHelper = new ItoDBHelper(context);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidLength() {
        // invalid uuid length
        byte[] uuid = new byte[UUID_LENGTH - 4];
        itoDBHelper.insertBeacon(uuid);
    }

    @Test
    public void testBeaconInsertAndQuery() {
        byte[] beaconUUID = new byte[UUID_LENGTH];
        new Random().nextBytes(beaconUUID); // insert random values
        itoDBHelper.insertBeacon(beaconUUID);
        Date from = new Date(System.currentTimeMillis() - 1000);
        Date to = new Date(System.currentTimeMillis() + 1000);
        List<byte[]> beacons = itoDBHelper.selectBeacons(from, to); // query this beacon again
        assertArrayEquals(beaconUUID, beacons.get(0));
    }

    @Test
    public void testInfectedContactsQuery() {
        byte[] contactUUID = new byte[UUID_LENGTH];
        int proximity = 5;
        int duration = 3415;

        // when we insert the infection first and the contact second, the query should return nothing
        new Random().nextBytes(contactUUID);
        itoDBHelper.insertInfected(contactUUID);
        sleep(1000); //sleep a little so the timestamp is different
        itoDBHelper.insertContact(Helper.calculateTruncatedSHA256(contactUUID), proximity, duration);
        assertEquals(0, itoDBHelper.selectInfectedContacts().size());


        // when we insert the contact first and the infection second, the query should return the contact
        new Random().nextBytes(contactUUID);
        itoDBHelper.insertContact(Helper.calculateTruncatedSHA256(contactUUID), proximity, duration);
        sleep(1000); //sleep a little so the timestamp is different
        itoDBHelper.insertInfected(contactUUID);
        List<ItoDBHelper.ContactResult> contacts = itoDBHelper.selectInfectedContacts();
        assertEquals(1, contacts.size());
        ItoDBHelper.ContactResult contactResult = contacts.get(0);
        assertEquals(proximity, contactResult.proximity);
        assertEquals(duration, contactResult.duration);
    }
}
