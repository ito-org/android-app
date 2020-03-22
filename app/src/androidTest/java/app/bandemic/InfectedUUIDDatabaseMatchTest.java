package app.bandemic;

import android.content.Context;

import app.bandemic.strict.database.AppDatabase;
import app.bandemic.strict.database.Beacon;
import app.bandemic.strict.database.BeaconDao;
import app.bandemic.strict.database.InfectedUUID;
import app.bandemic.strict.database.InfectedUUIDDao;

import org.apache.commons.codec.binary.Hex;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.UUID;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class InfectedUUIDDatabaseMatchTest {
    private BeaconDao beaconDao;
    private InfectedUUIDDao infectedUUIDDao;
    private AppDatabase db;

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).build();
        beaconDao = db.beaconDao();
        infectedUUIDDao = db.infectedUUIDDao();
    }

    @After
    public void closeDb() throws IOException {
        db.close();
    }

    @Test
    public void writeUserAndReadInList() throws Exception {
        byte[] hash = Hex.decodeHex("2863284b83f4ec64223a47b62d5846533075b192ef53569525b1501371d23da7".toCharArray());

        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }


        InfectedUUID infected = new InfectedUUID(
                0,
                new Date(),
                0,
                digest.digest(hash),
                "Covid-19"
        );

        Beacon beacon = new Beacon(
                hash,
                UUID.randomUUID(),
                new Date(),
                12
        );

        infectedUUIDDao.insertAll(infected);
        beaconDao.insertAll(beacon);

        infectedUUIDDao.getPossiblyInfectedEncounters().observeForever(infectedUUIDS -> {
            // one infection should be found
            assertEquals(1, infectedUUIDS.size());
        });
    }
}
