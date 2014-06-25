package com.scottbezek.embarcadero.app.model.data;

import android.location.Location;

import com.dropbox.sync.android.DbxList;
import com.dropbox.sync.android.DbxRecord;
import com.scottbezek.embarcadero.app.util.Asserts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

@Immutable
public class PathCoord {

    private final long mTime;
    private final double mLatitude;
    private final double mLongitude;
    private final double mAccuracy;
    private final double mAltitude;

    public PathCoord(long time, double latitude, double longitude, double accuracy, double altitude) {
        mTime = time;
        mLatitude = latitude;
        mLongitude = longitude;
        mAccuracy = accuracy;
        mAltitude = altitude;
    }

    public double getLongitude() {
        return mLongitude;
    }

    public double getLatitude() {
        return mLatitude;
    }

    public static List<PathCoord> listFrom(@Nonnull DbxRecord pathRecord) {
        // TODO(sbezek): probably want to pool PathCoords and make PathCoord mutable/reusable -- otherwise we're going to be creating a ton of garbage on every datastore change
        Asserts.assertAllEqual(pathRecord.hasField(PathRecordFields.COORD_TIME),
                pathRecord.hasField(PathRecordFields.COORD_LATITUDE),
                pathRecord.hasField(PathRecordFields.COORD_LONGITUDE),
                pathRecord.hasField(PathRecordFields.COORD_ACCURACY),
                pathRecord.hasField(PathRecordFields.COORD_ALTITUDE));

        if (!pathRecord.hasField(PathRecordFields.COORD_TIME)) {
            return Collections.emptyList();
        }

        final DbxList times = pathRecord.getList(PathRecordFields.COORD_TIME);
        final DbxList latitudes = pathRecord.getList(PathRecordFields.COORD_LATITUDE);
        final DbxList longitudes = pathRecord.getList(PathRecordFields.COORD_LONGITUDE);
        final DbxList accuracies = pathRecord.getList(PathRecordFields.COORD_ACCURACY);
        final DbxList altitudes = pathRecord.getList(PathRecordFields.COORD_ALTITUDE);
        Asserts.assertAllEqual(
                times.size(),
                latitudes.size(),
                longitudes.size(),
                accuracies.size(),
                altitudes.size());

        List<PathCoord> coords = new ArrayList<>(times.size());
        for (int i = 0; i < times.size(); i++) {
            coords.add(new PathCoord(
                    times.getLong(i),
                    latitudes.getDouble(i),
                    longitudes.getDouble(i),
                    accuracies.getDouble(i),
                    altitudes.getDouble(i)));
        }
        return coords;
    }

    public static PathCoord from(Location location) {
        return new PathCoord(location.getTime(), location.getLatitude(), location.getLongitude(),
                location.getAccuracy(), location.getAltitude());
    }
}
