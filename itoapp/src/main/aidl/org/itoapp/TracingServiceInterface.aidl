// TracingServiceInterface.aidl
package org.itoapp;

import org.itoapp.DistanceCallback;

// Declare any non-default types here with import statements

interface TracingServiceInterface {

    void setDistanceCallback(DistanceCallback distanceCallback);
}
