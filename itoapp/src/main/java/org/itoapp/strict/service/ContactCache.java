package org.itoapp.strict.service;

import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;

import androidx.collection.CircularArray;

import org.itoapp.DistanceCallback;
import org.itoapp.strict.Constants;
import org.itoapp.strict.database.ItoDBHelper;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ContactCache {
    private static final String LOG_TAG = "ContactCache";

    private ItoDBHelper dbHelper;
    private Handler serviceHandler;
    private HashMap<ByteBuffer, CacheEntry> cache = new HashMap<>();
    private DistanceCallback distanceCallback;

    public ContactCache(ItoDBHelper dbHelper, Handler serviceHandler) {
        this.dbHelper = dbHelper;
        this.serviceHandler = serviceHandler;
    }

    private void flush(ByteBuffer hash) {
        Log.d(LOG_TAG, "Flushing distance to DB");
        CacheEntry entry = cache.get(hash);
        entry.lowestDistance = Math.min(calculateDistance(entry), entry.lowestDistance);
        int contactDuration = (int) (entry.lastReceived - entry.firstReceived);
        if (contactDuration > Constants.MIN_CONTACT_DURATION)
            dbHelper.insertContact(entry.hash, (int) entry.lowestDistance, contactDuration);
        cache.remove(hash);
    }

    public void flush() {
        for (ByteBuffer hash : cache.keySet()) {
            flush(hash);
        }
    }

    public void addReceivedBroadcast(byte[] hash, float distance) {
        ByteBuffer hashString = ByteBuffer.wrap(hash);
        CacheEntry entry = cache.get(hashString);

        if (entry == null) {
            // new unknown broadcast
            entry = new CacheEntry();
            cache.put(hashString, entry);
            entry.hash = hash;
            entry.firstReceived = System.currentTimeMillis();
        }

        entry.lastReceived = System.currentTimeMillis();

        // postpone flushing
        serviceHandler.removeCallbacks(entry.flushRunnable);
        serviceHandler.postDelayed(entry.flushRunnable, Constants.UUID_VALID_INTERVAL);

        CircularArray<Float> distances = entry.distances;
        distances.addFirst(distance);
        if (distances.size() == Constants.DISTANCE_SMOOTHING_MA_LENGTH) {
            entry.lowestDistance = Math.min(calculateDistance(entry), entry.lowestDistance);
            distances.popLast();
        }
        if (distanceCallback != null) {
            try {
                distanceCallback.onDistanceMeasurements(calculateDistances());
            } catch (RemoteException e) {
                distanceCallback = null;
            }
        }
    }

    private float calculateDistance(CacheEntry cacheEntry) {
        CircularArray<Float> measuredDistances = cacheEntry.distances;
        float distance = 0;

        for (int j = 0; j < measuredDistances.size(); j++) {
            distance += measuredDistances.get(j) / measuredDistances.size();
        }
        return distance;
    }

    private float[] calculateDistances() {
        float[] distances = new float[cache.size()];
        List<CacheEntry> cacheEntries = new ArrayList<>(cache.values());
        for (int i = 0; i < distances.length; i++) {
            CacheEntry cacheEntry = cacheEntries.get(i);
            distances[i] = calculateDistance(cacheEntry);
        }

        return distances;
    }

    public void setDistanceCallback(DistanceCallback distanceCallback) {
        this.distanceCallback = distanceCallback;
    }

    private class CacheEntry {
        long firstReceived;
        long lastReceived;
        byte[] hash;
        CircularArray<Float> distances = new CircularArray<>(Constants.DISTANCE_SMOOTHING_MA_LENGTH);
        float lowestDistance = Float.MAX_VALUE;
        Runnable flushRunnable = () -> flush(ByteBuffer.wrap(hash));
    }
}
