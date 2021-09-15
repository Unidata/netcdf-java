/*
 * Copyright (c) 2020 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.array.Array;
import ucar.array.Index;
import ucar.array.InvalidRangeException;
import ucar.array.Section;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.calendar.CalendarDateRange;
import ucar.nc2.calendar.CalendarDateUnit;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.internal.util.CompareArrayToArray;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.Arrays;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

/**
 * Test point and grid subsetting of a curvilinear coverage.
 */
@Category(NeedsCdmUnitTest.class)
public class TestGridSubsetCurvilinear {
  private static final String curvilinearGrid = TestDir.cdmUnitTestDir + "transforms/UTM/artabro_20120425.nc";
  private static GridDataset covDs;
  private static NetcdfFile ncf;
  private static final double TOL = 1.0e-5;

  private final String latVarName = "lat";
  private final String lonVarName = "lon";
  private final String timeVarName = "time";
  private final String covVarName = "hs";

  ///////////////////////////////////////
  // well known values, verified manually
  //
  // These are coordinate values used to make the test subsets
  private final double subsetLat1 = 43.42208;
  private final double subsetLon1 = -8.24151;
  private final LatLonPoint subsetLatLon1 = LatLonPoint.create(subsetLat1, subsetLon1);
  private final double subsetLat2 = 43.46804;
  private final double subsetLon2 = -8.21323;
  private final LatLonPoint subsetLatLon2 = LatLonPoint.create(subsetLat2, subsetLon2);
  private final String subsetTimeIsoStart = "2012-04-26T09:23:01";
  private final String subsetTimeIsoEnd = "2012-04-26T19:56:10Z";
  private final CalendarDate subsetCalendarDateStart =
      CalendarDate.fromUdunitIsoDate(null, subsetTimeIsoStart).orElseThrow();
  private final CalendarDate subsetCalendarDateEnd =
      CalendarDate.fromUdunitIsoDate(null, subsetTimeIsoEnd).orElseThrow();

  // closest time, lat, and lon values to the subset time, lat, and lon from the the dataset
  private final int closestIndexTimeStart = 21;
  private final int closestIndexTimeEnd = 32;
  private final int closestTimeStart = 75600;
  private final int closestTimeEnd = 115200;
  private final CalendarDate closestCalendarDateStart =
      CalendarDate.fromUdunitIsoDate(null, "2012-04-26T09:00:00").orElseThrow();
  private final CalendarDate closestCalendarDateEnd =
      CalendarDate.fromUdunitIsoDate(null, "2012-04-26T20:00:00").orElseThrow();

  private final int closestIndexX1 = 109;
  private final int closestIndexY1 = 107;
  private final double closestLat1 = 43.42203903198242;
  private final double closestLon1 = -8.24138069152832;
  private final LatLonPoint closestLatLon1 = LatLonPoint.create(closestLat1, closestLon1);

  private final int closestIndexX2 = 129;
  private final int closestIndexY2 = 97;
  private final double closestLat2 = 43.468040466308594;
  private final double closestLon2 = -8.213236808776855;
  private final LatLonPoint closestLatLon2 = LatLonPoint.create(closestLat2, closestLon2);

  @BeforeClass
  public static void readDataset() throws IOException {
    Formatter errlog = new Formatter();
    covDs = GridDatasetFactory.openGridDataset(curvilinearGrid, errlog);
    ncf = NetcdfDatasets.openFile(curvilinearGrid, null);
    System.out.printf("curvilinearGrid = %s errs = %s%n", curvilinearGrid, errlog);
  }

  /////////////////////////////////////////////////
  // test helper methods to reduce code duplication

  private void checkNeighborNumerical(Index neighborIndex, Array<Number> values, double subset, double actual) {
    double neighbor = values.get(neighborIndex).doubleValue();
    double neighborLatDiff = Math.abs(subset - neighbor);
    assertThat(Math.abs(subset - actual)).isLessThan(neighborLatDiff);
  }

  private void checkNeighborLatLon(Index neighborIndex, Array<Number> latValues, Array<Number> lonValues,
      LatLonPoint subset, LatLonPoint actual) {
    checkNeighborNumerical(neighborIndex, latValues, subset.getLatitude(), actual.getLatitude());
    checkNeighborNumerical(neighborIndex, lonValues, subset.getLongitude(), actual.getLongitude());
  }

  private void checkWellKnownLatLon(Variable lon, Variable lat, int closestIndexY, int closestIndexX,
      LatLonPoint closestLatLonPoint, LatLonPoint subsetLatLonPoint) throws IOException {
    // test well known spatial indices
    Array<Number> lonValues = (Array<Number>) lon.readArray();
    Array<Number> latValues = (Array<Number>) lat.readArray();

    Index testIndex = lonValues.getIndex();
    testIndex.set(closestIndexY, closestIndexX);
    double actualLon = lonValues.get(testIndex).doubleValue();
    double actualLat = latValues.get(testIndex).doubleValue();
    LatLonPoint actualLatLon = LatLonPoint.create(actualLat, actualLon);

    assertThat(actualLat).isWithin(1e-6).of(closestLatLonPoint.getLatitude());
    assertThat(actualLon).isWithin(1e-6).of(closestLatLonPoint.getLongitude());

    // check that surrounding grid points are not closer to the subset lat/lon than the one
    // manually identified as the closest
    Index neighborIndex = lonValues.getIndex();
    for (int j = closestIndexY - 1; j <= closestIndexY + 1; j = j + 2) {
      neighborIndex.set(j, closestIndexX);
      checkNeighborLatLon(neighborIndex, latValues, lonValues, subsetLatLonPoint, actualLatLon);
      for (int i = closestIndexX - 1; i <= closestIndexX + 1; i = i + 2) {
        neighborIndex.set(closestIndexY, i);
        checkNeighborLatLon(neighborIndex, latValues, lonValues, subsetLatLonPoint, actualLatLon);

        neighborIndex.set(j, i);
        checkNeighborLatLon(neighborIndex, latValues, lonValues, subsetLatLonPoint, actualLatLon);
      }
    }
  }

  private void checkWellKnownTime(Variable time, int closestIndex, int closestTime, CalendarDate closestCalendarDate,
      String subsetIsoString) throws IOException {
    // test well known time indices
    Array<Number> timeValues = (Array<Number>) time.readArray();
    Index testTimeIndex = timeValues.getIndex();
    testTimeIndex.set(closestIndex);
    int actualTime = timeValues.get(testTimeIndex).intValue();
    CalendarDateUnit cdu = CalendarDateUnit.fromUdunitString(null, time.getUnitsString()).orElseThrow();
    CalendarDate actualCalendarDate = cdu.makeCalendarDate(actualTime);
    assertThat(actualTime).isEqualTo(closestTime);
    assertThat(actualCalendarDate).isEqualTo(closestCalendarDate);

    // Check that we have the correct "closest" date / time
    // Should not be impacted by being a curvilinear grid, but why not
    // go ahead and test it out (not pretending to be a unit test here).
    CalendarDate subsetCalendarDate = CalendarDate.fromUdunitIsoDate(null, subsetIsoString).orElseThrow();
    double subsetTime = cdu.makeOffsetFromRefDate(subsetCalendarDate);
    for (int i = closestIndex - 1; i <= closestIndex + 1; i = i + 2) {
      testTimeIndex.set(i);
      checkNeighborCalendarDate(testTimeIndex, timeValues, cdu, subsetTime, actualTime);
    }
  }

  private void checkNeighborCalendarDate(Index neighborIndex, Array<Number> offsetTimeValues, CalendarDateUnit cdu,
      double subsetOffsetTime, int actualOffsetTime) {
    checkNeighborNumerical(neighborIndex, offsetTimeValues, subsetOffsetTime, actualOffsetTime);

    CalendarDate actualCalendarDate = cdu.makeCalendarDate(actualOffsetTime);
    CalendarDate neighborCalendarDate = cdu.makeCalendarDate(offsetTimeValues.get(neighborIndex).intValue());
    CalendarDate subsetCalendarDate = cdu.makeCalendarDate((long) subsetOffsetTime);
    double diffActualCalendar = Math.abs(subsetCalendarDate.getDifferenceInMsecs(actualCalendarDate));
    double diffNeighbor = Math.abs(subsetCalendarDate.getDifferenceInMsecs(neighborCalendarDate));
    assertThat(diffActualCalendar).isLessThan(diffNeighbor);
  }

  private Array<Number> getExpectedData(Variable var, int startIndX, int endIndX, int startIndY, int endIndY)
      throws IOException, InvalidRangeException {
    return getExpectedData(var, startIndX, endIndX, startIndY, endIndY, -1, -1);
  }

  private Array<Number> getExpectedData(Variable var, int startIndX, int endIndX, int startIndY, int endIndY,
      int startIndTime, int endIndTime) throws InvalidRangeException, IOException {
    // Build a section string for the data read
    Section.Builder sectionBuilder = Section.builder();
    ImmutableList<Dimension> dims = var.getDimensions();
    for (Dimension dim : dims) {
      String dimName = dim.getShortName();
      switch (dim.getShortName()) {
        case "x":
          if (startIndX != -1) {
            sectionBuilder.appendRange(startIndX, endIndX);
          } else {
            if (endIndX >= startIndX) {
              sectionBuilder.appendRange(startIndX, endIndX);
            } else {
              sectionBuilder.appendRange(endIndX, startIndX);
            }
          }
          break;
        case "y":
          if (startIndY != -1) {
            if (endIndY >= startIndY) {
              sectionBuilder.appendRange(startIndY, endIndY);
            } else {
              sectionBuilder.appendRange(endIndY, startIndY);
            }
          } else {
            sectionBuilder.appendRange(null);
          }
          break;
        case "time":
          if (startIndTime != -1) {
            sectionBuilder.appendRange(startIndTime, endIndTime);
          } else {
            sectionBuilder.appendRange(null);
          }
          break;
        default:
          throw new IllegalStateException(String.format("Dim name \"%s\" unexpected. Fail.", dimName));
      }
    }
    // read directly from the 3D array
    return (Array<Number>) var.readArray(sectionBuilder.build().fill(var.getShape()));
  }

  ///////////////
  // test methods
  @Test
  public void testWellKnownValues() throws IOException {
    Variable time = ncf.findVariable(timeVarName);
    Variable lon = ncf.findVariable(lonVarName);
    Variable lat = ncf.findVariable(latVarName);

    assert time != null;
    assert lat != null;
    assert lon != null;

    checkWellKnownTime(time, closestIndexTimeStart, closestTimeStart, closestCalendarDateStart, subsetTimeIsoStart);
    checkWellKnownTime(time, closestIndexTimeEnd, closestTimeEnd, closestCalendarDateEnd, subsetTimeIsoEnd);

    // test well known spatial indices
    checkWellKnownLatLon(lon, lat, closestIndexY1, closestIndexX1, closestLatLon1, subsetLatLon1);
    checkWellKnownLatLon(lon, lat, closestIndexY2, closestIndexX2, closestLatLon2, subsetLatLon2);
  }

  @Test
  public void testCoverageHcs() throws IOException {
    Variable lon = ncf.findVariable(lonVarName);
    Variable lat = ncf.findVariable(latVarName);
    assertThat(lat).isNotNull();
    assertThat(lon).isNotNull();

    Array<Number> lonValues = (Array<Number>) lon.readArray();
    Array<Number> latValues = (Array<Number>) lat.readArray();

    Index testIndex = lonValues.getIndex();
    testIndex.set(closestIndexY1, closestIndexX1);
    double actualLon = lonValues.get(testIndex).doubleValue();
    double actualLat = latValues.get(testIndex).doubleValue();
    LatLonPoint actualLatLon = LatLonPoint.create(actualLat, actualLon);

    Grid coverage = covDs.findGrid(covVarName).orElseThrow();
    GridHorizCoordinateSystem hcs = coverage.getHorizCoordinateSystem();

    // make sure the coverage coordinate system is able to get the expected lat/lon point
    // using the expected x and y indices
    LatLonPoint covLatLonPoint = hcs.getLatLon(closestIndexX1, closestIndexY1);
    assertThat(covLatLonPoint.nearlyEquals(actualLatLon, TOL)).isTrue();
  }

  @Test
  public void testCoverageTime() throws IOException {
    Variable time = ncf.findVariable(timeVarName);
    assertThat(time).isNotNull();

    Array<Number> timeValues = (Array<Number>) time.readArray();

    Index testIndex = timeValues.getIndex();
    testIndex.set(closestIndexTimeStart);
    int actualTime = timeValues.get(testIndex).intValue();

    Grid coverage = covDs.findGrid(covVarName).orElseThrow();
    GridTimeCoordinateSystem tcs = coverage.getTimeCoordinateSystem();
    GridAxis<?> timeAxis = tcs.getTimeOffsetAxis(0);
    double timeAxisValue = timeAxis.getCoordDouble(closestIndexTimeStart);

    // make sure the coverage coordinate system is able to get the same lat/lon point at
    // using the expected x and y indices
    assertThat((double) actualTime).isWithin(1e-6).of(timeAxisValue);
    assertThat((double) closestTimeStart).isWithin(1e-6).of(timeAxisValue);
  }

  @Test
  public void testSetLatLonPoint() throws IOException, ucar.array.InvalidRangeException {
    Variable var = ncf.findVariable(covVarName);
    assertThat(var).isNotNull();
    String varName = var.getFullName();
    assertThat(varName).isNotNull();

    // Now, subset the coverage and compare with the time series read from the array object
    Grid coverage = covDs.findGrid(covVarName).orElseThrow();
    GridReferencedArray geoArray = coverage.getReader().setLatLonPoint(subsetLatLon1).setTimeLatest().read();
    MaterializedCoordinateSystem mcs = geoArray.getMaterializedCoordinateSystem();
    assertThat(mcs).isNotNull();
    System.out.printf(" data cs shape=%s%n", mcs.getMaterializedShape());
    System.out.printf(" data shape=%s%n", Arrays.toString(geoArray.data().getShape()));

    assertThat(Ints.asList(geoArray.data().getShape())).isEqualTo(mcs.getMaterializedShape());

    // compare the values of the data arrays
    double val = geoArray.data().getScalar().doubleValue();
    assertThat(val).isWithin(TOL).of(0.08546);
  }

  @Test
  public void testSetLatLonPointWithTimeRange() throws IOException, InvalidRangeException {
    Variable var = ncf.findVariable(covVarName);
    assertThat(var).isNotNull();

    // testWellKnownValues() and testCoverageHcs() shows that we we have correctly identified the x and y indices for
    // the closest grid point to the subset lat/lon.
    // Now, let's read the expected "time series" from the grid point and test that against the time series
    // from the subset coverage
    Array<Number> expectedTimeSeries = getExpectedData(var, closestIndexX1, closestIndexX1, closestIndexY1,
        closestIndexY1, closestIndexTimeStart, closestIndexTimeEnd);

    // Now, subset the coverage and compare with the time series read from the array object
    Grid coverage = covDs.findGrid(covVarName).orElseThrow();
    GridReferencedArray geoArray = coverage.getReader().setLatLonPoint(subsetLatLon1)
        .setDateRange(CalendarDateRange.of(subsetCalendarDateStart, subsetCalendarDateEnd)).read();
    MaterializedCoordinateSystem mcs = geoArray.getMaterializedCoordinateSystem();
    assertThat(mcs).isNotNull();

    // expected time series array and the coverage subset array should have the same shape
    assertThat(geoArray.data().getShape()).isEqualTo(expectedTimeSeries.getShape());
    // compare the values of the data arrays
    assertThat(CompareArrayToArray.compareData(var.getFullName(), expectedTimeSeries, geoArray.data())).isTrue();
  }

  @Test
  public void testSetLatLonBB() throws IOException, InvalidRangeException {
    Variable var = ncf.findVariable(covVarName);
    assertThat(var).isNotNull();
    String varName = var.getFullName();
    assertThat(varName).isNotNull();

    double deltaLat = subsetLatLon2.getLatitude() - subsetLatLon1.getLatitude();
    double deltaLon = subsetLatLon2.getLongitude() - subsetLatLon1.getLongitude();
    LatLonRect subsetBoundingBox = LatLonRect.builder(subsetLatLon1, deltaLat, deltaLon).build();

    // subset coverage using well known location
    Grid coverage = covDs.findGrid(covVarName).orElseThrow();
    GridReferencedArray geoArray = coverage.getReader().setLatLonBoundingBox(subsetBoundingBox).read();
    MaterializedCoordinateSystem mcs = geoArray.getMaterializedCoordinateSystem();
    assertThat(mcs).isNotNull();

    // the returned area is generally bigger thaan the request, esp for curvilinear
    LatLonRect covSubsetBoundingBox = mcs.getHorizCoordinateSystem().getLatLonBoundingBox();
    System.out.printf(" request llbb= %s%n", subsetBoundingBox);
    System.out.printf(" subset llbb= %s%n", covSubsetBoundingBox);
    assertThat(covSubsetBoundingBox.nearlyEquals(LatLonRect.fromSpec("43.404860, -8.274957, 0.080894, 0.110731"), TOL))
        .isTrue();
  }

  @Test
  public void testSetLatLonBBWithTimeRange() throws IOException, InvalidRangeException {
    Variable var = ncf.findVariable(covVarName);
    assertThat(var).isNotNull();
    String varName = var.getFullName();
    assertThat(varName).isNotNull();

    Variable timeVar = ncf.findVariable(timeVarName);
    assertThat(timeVar).isNotNull();
    Array<Number> timeVarVals = (Array<Number>) timeVar.readArray();

    double deltaLat = subsetLatLon2.getLatitude() - subsetLatLon1.getLatitude();
    double deltaLon = subsetLatLon2.getLongitude() - subsetLatLon1.getLongitude();
    LatLonRect subsetBoundingBox = LatLonRect.builder(subsetLatLon1, deltaLat, deltaLon).build();

    // subset coverage using well known location
    Grid coverage = covDs.findGrid(covVarName).orElseThrow();
    GridReferencedArray geoArray = coverage.getReader().setLatLonBoundingBox(subsetBoundingBox)
        .setDateRange(CalendarDateRange.of(subsetCalendarDateStart, subsetCalendarDateEnd)).read();
    MaterializedCoordinateSystem mcs = geoArray.getMaterializedCoordinateSystem();
    assertThat(mcs).isNotNull();

    // compare subset time values
    GridAxisPoint timeAxis = (GridAxisPoint) mcs.getTimeCoordSystem().getTimeOffsetAxis(0);
    int nPointsTime = timeAxis.getNominalSize();
    assertThat(nPointsTime).isEqualTo(closestIndexTimeEnd - closestIndexTimeStart + 1);
    int idx = 0;
    for (Number coord : timeAxis) {
      double timeVarVal = timeVarVals.get(closestIndexTimeStart + idx).doubleValue();
      assertThat(timeVarVal).isWithin(1e-6).of(coord.doubleValue());
      idx++;
    }

    // the returned area is generally bigger thaan the request, esp for curvilinear
    LatLonRect covSubsetBoundingBox = mcs.getHorizCoordinateSystem().getLatLonBoundingBox();
    System.out.printf(" request llbb= %s%n", subsetBoundingBox);
    System.out.printf(" subset llbb= %s%n", covSubsetBoundingBox);
    assertThat(covSubsetBoundingBox.nearlyEquals(LatLonRect.fromSpec("43.404860, -8.274957, 0.080894, 0.110731"), TOL))
        .isTrue();
  }

  @AfterClass
  public static void closeResources() throws IOException {
    covDs.close();
    ncf.close();
  }
}
