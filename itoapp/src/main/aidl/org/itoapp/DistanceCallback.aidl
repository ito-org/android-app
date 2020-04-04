// DistanceCallback.aidl
package org.itoapp;

// Declare any non-default types here with import statements

interface DistanceCallback {

    void onDistanceMeasurements(in float[] distance);
}
