package com.scottbezek.embarcadero.app.model;

import com.dropbox.sync.android.DbxRecord;

import android.location.Location;

/**
 */
public class PathRecordWriter {

    private final DbxRecord mPathRecord;

    PathRecordWriter(DbxRecord pathRecord) {
        mPathRecord = pathRecord;
    }

    public void addLocation(Location location) {
        assertAllEqual(
                mPathRecord.getOrCreateList("coord_time").add(location.getTime()).size(),
                mPathRecord.getOrCreateList("coord_latitude").add(location.getLatitude()).size(),
                mPathRecord.getOrCreateList("coord_longitude").add(location.getLongitude()).size(),
                mPathRecord.getOrCreateList("coord_accuracy").add(location.getAccuracy()).size(),
                mPathRecord.getOrCreateList("coord_altitude").add(location.getAltitude()).size());
        //        mPathRecord.getOrCreateList("coord_provider").add(location.getProvider()).size()
    }

    private static void assertAllEqual(int firstValue, int... values) {
        boolean fail = false;
        for (int i = 0; i < values.length; i++) {
            if (values[i] != firstValue) {
                fail = true;
                break;
            }
        }

        if (fail) {
            StringBuilder sb = new StringBuilder("Expected all the same value: [");
            sb.append(firstValue);
            for (int i = 0; i < values.length; i++) {
                sb.append(", ");
                sb.append(values[i]);
            }
            sb.append("]");
            throw new AssertionError(sb.toString());
        }
    }

    public void setStartTime(long timeMillis) {
        mPathRecord.set("start_time", timeMillis);
    }

    public void setStopTime(long timeMillis) {
        mPathRecord.set("stop_time", timeMillis);
    }
}
