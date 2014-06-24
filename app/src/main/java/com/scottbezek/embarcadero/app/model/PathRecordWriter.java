package com.scottbezek.embarcadero.app.model;

import android.location.Location;

import com.dropbox.sync.android.DbxRecord;
import com.scottbezek.embarcadero.app.model.data.PathRecordFields;
import com.scottbezek.embarcadero.app.util.Asserts;

/**
 */
public class PathRecordWriter {

    private final DbxRecord mPathRecord;

    PathRecordWriter(DbxRecord pathRecord) {
        mPathRecord = pathRecord;
    }

    public void addLocation(Location location) {
        Asserts.assertAllEqual(
                mPathRecord.getOrCreateList(PathRecordFields.COORD_TIME).add(location.getTime()).size(),
                mPathRecord.getOrCreateList(PathRecordFields.COORD_LATITUDE).add(location.getLatitude()).size(),
                mPathRecord.getOrCreateList(PathRecordFields.COORD_LONGITUDE).add(location.getLongitude()).size(),
                mPathRecord.getOrCreateList(PathRecordFields.COORD_ACCURACY).add(location.getAccuracy()).size(),
                mPathRecord.getOrCreateList(PathRecordFields.COORD_ALTITUDE).add(location.getAltitude()).size());
        //        mPathRecord.getOrCreateList("coord_provider").add(location.getProvider()).size()
    }

    public void setStartTime(long timeMillis) {
        mPathRecord.set(PathRecordFields.START_TIME, timeMillis);
    }

    public void setStopTime(long timeMillis) {
        mPathRecord.set(PathRecordFields.STOP_TIME, timeMillis);
    }
}
