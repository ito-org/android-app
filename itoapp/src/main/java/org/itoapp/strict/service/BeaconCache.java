package org.itoapp.strict.service;

import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;

import androidx.collection.CircularArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.itoapp.DistanceCallback;
import org.itoapp.strict.database.Beacon;
import org.itoapp.strict.repository.BroadcastRepository;

import okhttp3.Cache;
import okio.ByteString;

public class BeaconCache {
    private static final String LOG_TAG = "BeaconCache";
    private final int MOVING_AVERAGE_LENGTH = 7;
    private final long FLUSH_AFTER_MILLIS = 1000 * 60 * 3; // flush after three minutes

    private BroadcastRepository broadcastRepository;
    private Handler serviceHandler;
    private HashMap<ByteString, CacheEntry> cache = new HashMap<>();
    private DistanceCallback distanceCallback;

    public BeaconCache(BroadcastRepository broadcastRepository, Handler serviceHandler) {
        this.broadcastRepository = broadcastRepository;
        this.serviceHandler = serviceHandler;
    }

    private void flush(ByteString hash) {
        Log.d(LOG_TAG, "Flushing distance to DB");
        CacheEntry entry = cache.get(hash);
        CircularArray<Double> distances = entry.distances;
        double avg = 0;
        for (int i = 0; i < distances.size(); i++) {
            avg += distances.get(i) / distances.size();
        }
        if (avg < entry.lowestDistance) {
            entry.lowestDistance = avg;
        }
        insertIntoDB(entry.hash, entry.lowestDistance, entry.firstReceived, entry.lastReceived - entry.firstReceived);
        cache.remove(hash);
    }

    public void flush() {
        for (ByteString hash : cache.keySet()) {
            flush(hash);
        }
    }

    public void addReceivedBroadcast(byte[] hash, double distance) {
        ByteString hashString = ByteString.of(hash);
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
        serviceHandler.postDelayed(entry.flushRunnable, FLUSH_AFTER_MILLIS);

        CircularArray<Double> distances = entry.distances;
        distances.addFirst(distance);
        if (distances.size() == MOVING_AVERAGE_LENGTH) {

            //calculate moving average
            double avg = 0;
            for (int i = 0; i < MOVING_AVERAGE_LENGTH; i++) {
                avg += distances.get(i) / MOVING_AVERAGE_LENGTH;
            }
            if (avg < entry.lowestDistance) {
                //insert new lowest value to DB
                entry.lowestDistance = avg;
                //insertIntoDB(hash, avg);
            }
            distances.popLast();
        }
        if(distanceCallback != null) {
            try {
                distanceCallback.onDistanceMeasurements(calculateDistances());
            } catch (RemoteException e) {
                distanceCallback = null;
            }
        }
    }

    private float[] calculateDistances() {
        float[] distances = new float[cache.size()];
        List<CacheEntry> cacheEntries = new ArrayList<>(cache.values());
        for(int i = 0; i < distances.length; i++){
            CacheEntry cacheEntry = cacheEntries.get(i);
            CircularArray<Double> individualDistances = cacheEntry.distances;

            for (int j = 0; j < individualDistances.size(); j++) {
                distances[i] += individualDistances.get(j) / individualDistances.size();
            }
        }

        return distances;
    }

    private void insertIntoDB(byte[] hash, double distance, long startTime, long duration) {
        broadcastRepository.insertBeacon(new Beacon(
                hash,
                new Date(startTime),
                duration,
                distance
        ));
    }

    public void setDistanceCallback(DistanceCallback distanceCallback) {
        this.distanceCallback = distanceCallback;
    }

    private class CacheEntry {
        long firstReceived;
        long lastReceived;
        byte[] hash;
        CircularArray<Double> distances = new CircularArray<>(MOVING_AVERAGE_LENGTH);
        double lowestDistance = Double.MAX_VALUE;
        Runnable flushRunnable = () -> flush(ByteString.of(hash));
    }
}
