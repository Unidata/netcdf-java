/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grib.collection;

import java.util.List;

import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.nc2.Variable;
import ucar.nc2.grib.collection.GribIosp.Time2Dinfo;
import ucar.nc2.grib.collection.GribIosp.Time2DinfoType;
import ucar.nc2.grib.coord.CoordinateRuntime;
import ucar.nc2.grib.coord.CoordinateTime;
import ucar.nc2.grib.coord.CoordinateTime2D;
import ucar.nc2.grib.coord.CoordinateTimeAbstract;
import ucar.nc2.grib.coord.CoordinateTimeIntv;
import ucar.nc2.grib.coord.TimeCoordIntvValue;
import ucar.nc2.calendar.CalendarPeriod;

/** Make Time2D coordinates when requested. */
class Time2DLazyCoordinate {

  static ucar.array.Array<?> makeLazyCoordinateArray(Variable v2, Time2Dinfo info,
      GribCollectionImmutable gribCollection) {
    double[] data;
    if (info.time2D != null) {
      data = makeLazyTime2Darray(info);
    } else {
      data = makeLazyTime1Darray(info, gribCollection);
    }
    return Arrays.factory(ArrayType.DOUBLE, v2.getShape(), data);
  }

  // only for the 2d times
  private static double[] makeLazyTime1Darray(Time2Dinfo info, GribCollectionImmutable gribCollection) {
    int length = info.time1D.getSize();
    double[] data = new double[length];
    for (int i = 0; i < length; i++) {
      data[i] = Double.NaN;
    }

    // coordinate values
    switch (info.which) {
      case reftime:
        CoordinateRuntime rtc = (CoordinateRuntime) info.time1D;
        int count = 0;
        for (double val : rtc.getRuntimeOffsetsInTimeUnits()) {
          data[count++] = val;
        }
        return data;

      case timeAuxRef:
        CoordinateTimeAbstract time = (CoordinateTimeAbstract) info.time1D;
        count = 0;
        List<Double> masterOffsets = gribCollection.getMasterRuntime().getOffsetsInTimeUnits();
        for (int masterIdx : time.getTime2runtime()) {
          data[count++] = masterOffsets.get(masterIdx - 1);
        }
        return data;

      default:
        throw new IllegalStateException("makeLazyTime1Darray must be reftime or timeAuxRef");
    }
  }

  // only for the 2d times
  private static double[] makeLazyTime2Darray(Time2Dinfo info) {
    CoordinateTime2D time2D = info.time2D;
    CalendarPeriod timeUnit = time2D.getTimeUnit();

    int nruns = time2D.getNruns();
    int ntimes = time2D.getNtimes();
    int length = nruns * ntimes;
    if (info.which == Time2DinfoType.bounds) {
      length *= 2;
    }

    double[] data = new double[length];
    for (int i = 0; i < length; i++) {
      data[i] = Double.NaN;
    }
    int count;

    // coordinate values
    switch (info.which) {
      case off:
        for (int runIdx = 0; runIdx < nruns; runIdx++) {
          CoordinateTime coordTime = (CoordinateTime) time2D.getTimeCoordinate(runIdx);
          int timeIdx = 0;
          for (long val : coordTime.getOffsetSorted()) {
            data[runIdx * ntimes + timeIdx] = timeUnit.getValue() * val + time2D.getOffset(runIdx);
            timeIdx++;
          }
        }
        break;

      case offU:
        count = 0;
        for (int runIdx = 0; runIdx < nruns; runIdx++) {
          CoordinateTime coordTime = (CoordinateTime) time2D.getTimeCoordinate(runIdx);
          for (long val : coordTime.getOffsetSorted()) {
            data[count++] = timeUnit.getValue() * val + time2D.getOffset(runIdx);
          }
        }
        break;

      case intv:
        for (int runIdx = 0; runIdx < nruns; runIdx++) {
          CoordinateTimeIntv timeIntv = (CoordinateTimeIntv) time2D.getTimeCoordinate(runIdx);
          int timeIdx = 0;
          for (TimeCoordIntvValue tinv : timeIntv.getTimeIntervals()) {
            // use upper bounds for coord value
            data[runIdx * ntimes + timeIdx] = timeUnit.getValue() * tinv.getBounds2() + time2D.getOffset(runIdx);
            timeIdx++;
          }
        }
        break;

      case intvU:
        count = 0;
        for (int runIdx = 0; runIdx < nruns; runIdx++) {
          CoordinateTimeIntv timeIntv = (CoordinateTimeIntv) time2D.getTimeCoordinate(runIdx);
          for (TimeCoordIntvValue tinv : timeIntv.getTimeIntervals()) {
            // use upper bounds for coord value
            data[count++] = timeUnit.getValue() * tinv.getBounds2() + time2D.getOffset(runIdx);
          }
        }
        break;

      case is1Dtime:
        CoordinateRuntime runtime = time2D.getRuntimeCoordinate();
        count = 0;
        for (double val : runtime.getOffsetsInTimeUnits()) { // convert to udunits
          data[count++] = val;
        }
        break;

      case isUniqueRuntime: // the aux runtime coordinate
        CoordinateRuntime runtimeU = time2D.getRuntimeCoordinate();
        List<Double> runOffsets = runtimeU.getOffsetsInTimeUnits();
        count = 0;
        for (int run = 0; run < time2D.getNruns(); run++) {
          CoordinateTimeAbstract timeCoord = time2D.getTimeCoordinate(run);
          for (int time = 0; time < timeCoord.getNCoords(); time++) {
            data[count++] = runOffsets.get(run);
          }
        }
        break;

      case bounds:
        for (int runIdx = 0; runIdx < nruns; runIdx++) {
          CoordinateTimeIntv timeIntv = (CoordinateTimeIntv) time2D.getTimeCoordinate(runIdx);
          int timeIdx = 0;
          for (TimeCoordIntvValue tinv : timeIntv.getTimeIntervals()) {
            data[runIdx * ntimes * 2 + timeIdx] = timeUnit.getValue() * tinv.getBounds1() + time2D.getOffset(runIdx);
            data[runIdx * ntimes * 2 + timeIdx + 1] =
                timeUnit.getValue() * tinv.getBounds2() + time2D.getOffset(runIdx);
            timeIdx += 2;
          }
        }
        break;

      case boundsU:
        count = 0;
        for (int runIdx = 0; runIdx < nruns; runIdx++) {
          CoordinateTimeIntv timeIntv = (CoordinateTimeIntv) time2D.getTimeCoordinate(runIdx);
          for (TimeCoordIntvValue tinv : timeIntv.getTimeIntervals()) {
            data[count++] = timeUnit.getValue() * tinv.getBounds1() + time2D.getOffset(runIdx);
            data[count++] = timeUnit.getValue() * tinv.getBounds2() + time2D.getOffset(runIdx);
          }
        }
        break;

      default:
        throw new IllegalStateException();
    }

    return data;
  }
}
