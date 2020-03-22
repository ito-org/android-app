package com.example.infectiontracker;

import java.util.ArrayList;
import java.util.HashMap;

import androidx.collection.CircularArray;
import androidx.room.PrimaryKey;

public class BeaconInMemoryCache {

    private HashMap<byte[], CircularArray<Double>> beacons;
    private final int CAPACITY = 15;


    public BeaconInMemoryCache() {

    }

    public void addReceivedBroadcast(byte[] hash, double distance) {
        if(beacons.get(hash) == null) {
            // new unknown broadcast
            CircularArray c = new CircularArray<>(CAPACITY);
            c.addFirst(distance);
            beacons.put(hash, c);
        }
        else {
            CircularArray c = beacons.get(hash);
            if(c.size() < CAPACITY) {
                // buffer for moving average not full yet
                c.addFirst(distance);
            }
            else {
                // remove the last element and replace with new one
                if (distance < beacons.get(hash).popLast()) {
                    // new average could be below threshold

                    /*
                    if(average < smallest in database) {
                        // store in database
                    }*/
                }
            }
        }
    }
}
