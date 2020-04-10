package org.itoapp;

import org.itoapp.strict.database.ItoDBHelper;
import org.itoapp.strict.network.NetworkHelper;
import org.itoapp.strict.service.TracingService;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class NetworkTest {

    @Test
    public void testNetwork() throws IOException {

        byte[] uuid = new byte[TracingService.UUID_LENGTH];
        new Random().nextBytes(uuid);

        AtomicBoolean uuidReceived = new AtomicBoolean(false);

        ItoDBHelper mockDB = Mockito.mock(ItoDBHelper.class);
        Mockito.when(mockDB.selectRandomLastUUID()).thenReturn(null);
        Mockito.doAnswer(invocation -> {
            byte[] bytes = invocation.getArgumentAt(0, byte[].class);
            uuidReceived.set(true);
            return null;
        }).when(mockDB).insertInfected(Mockito.any(byte[].class));

        NetworkHelper.publishUUIDs(Collections.singletonList(uuid));
        NetworkHelper.refreshInfectedUUIDs(mockDB);

        assertTrue(uuidReceived.get());
    }
}
