// TracingServiceInterface.aidl
package org.itoapp;

import org.itoapp.DistanceCallback;
import org.itoapp.PublishUUIDsCallback;

interface TracingServiceInterface {
    void setDistanceCallback(DistanceCallback distanceCallback);

    void publishBeaconUUIDs(long from, long to, PublishUUIDsCallback callback);

    boolean isPossiblyInfected();
}
