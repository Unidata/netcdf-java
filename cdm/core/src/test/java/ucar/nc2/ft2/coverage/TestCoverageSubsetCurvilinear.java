/*
 * Copyright (c) 2020 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft2.coverage;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.Collections;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.ma2.Section.Builder;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.util.CompareNetcdf2;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/**
 * Test point and grid subsetting of a curvilinear coverage.
 *
 * todo: merge with cdm-test/src/test/java/ucar/nc2/ft/coverage/TestCoverageCurvilinear.java
 */
@Category(NeedsCdmUnitTest.class)
public class TestCoverageSubsetCurvilinear {
  private static final String curvilinearGrid = TestDir.cdmUnitTestDir + "transforms/UTM/artabro_20120425.nc";
  private static FeatureDatasetCoverage covDs;
  private static NetcdfFile ncf;

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
  private final CalendarDate subsetCalendarDateStart = CalendarDate.parseISOformat(null, subsetTimeIsoStart);
  private final CalendarDate subsetCalendarDateEnd = CalendarDate.parseISOformat(null, subsetTimeIsoEnd);

  // closest time, lat, and lon values to the subset time, lat, and lon from the the dataset
  private final int closestTimeStart = 75600;
  private final int closestTimeEnd = 115200;
  private final CalendarDate closestCalendarDateStart = CalendarDate.parseISOformat(null, "2012-04-26T09:00:00");
  private final CalendarDate closestCalendarDateEnd = CalendarDate.parseISOformat(null, "2012-04-26T20:00:00");
  private final double closestLat1 = 43.42203903198242;
  private final double closestLon1 = -8.24138069152832;
  private final LatLonPoint closestLatLon1 = LatLonPoint.create(closestLat1, closestLon1);
  private final double closestLat2 = 43.468040466308594;
  private final double closestLon2 = -8.213236808776855;
  private final LatLonPoint closestLatLon2 = LatLonPoint.create(closestLat2, closestLon2);

  // index values associated with the closest coordinate values
  private final int closestIndexTimeStart = 21;
  private final int closestIndexTimeEnd = 32;
  private final int closestIndexX1 = 109;
  private final int closestIndexY1 = 107;
  private final int closestIndexX2 = 129;
  private final int closestIndexY2 = 97;

  @BeforeClass
  public static void readDataset() throws IOException {
    covDs = CoverageDatasetFactory.open(curvilinearGrid);
    ncf = NetcdfDatasets.openFile(curvilinearGrid, null);
  }

  /////////////////////////////////////////////////
  // test helper methods to reduce code duplication

  private void checkNeighborNumerical(Index neighborIndex, Array values, double subset, double actual) {
    double neighbor = values.getDouble(neighborIndex);
    double neighborLatDiff = Math.abs(subset - neighbor);
    assertThat(Math.abs(subset - actual)).isLessThan(neighborLatDiff);
  }

  private void checkNeighborLatLon(Index neighborIndex, Array latValues, Array lonValues, LatLonPoint subset,
      LatLonPoint actual) {
    checkNeighborNumerical(neighborIndex, latValues, subset.getLatitude(), actual.getLatitude());
    checkNeighborNumerical(neighborIndex, lonValues, subset.getLongitude(), actual.getLongitude());
  }

  private void checkWellKnownLatLon(Variable lon, Variable lat, int closestIndexY, int closestIndexX,
      LatLonPoint closestLatLonPoint, LatLonPoint subsetLatLonPoint) throws IOException {
    // test well known spatial indices
    Index testIndex = Index.factory(lon.getShape());
    testIndex.set(new int[] {closestIndexY, closestIndexX});
    Array lonValues = lon.read();
    Array latValues = lat.read();
    double actualLon = lonValues.getDouble(testIndex);
    double actualLat = latValues.getDouble(testIndex);
    LatLonPoint actualLatLon = LatLonPoint.create(actualLat, actualLon);

    assertThat(actualLat).isWithin(1e-6).of(closestLatLonPoint.getLatitude());
    assertThat(actualLon).isWithin(1e-6).of(closestLatLonPoint.getLongitude());

    // check that surrounding grid points are not closer to the subset lat/lon than the one
    // manually identified as the closest
    Index neighborIndex = Index.factory(lon.getShape());
    for (int j = closestIndexY - 1; j <= closestIndexY + 1; j = j + 2) {
      neighborIndex.set(new int[] {j, closestIndexX});
      checkNeighborLatLon(neighborIndex, latValues, lonValues, subsetLatLonPoint, actualLatLon);
      for (int i = closestIndexX - 1; i <= closestIndexX + 1; i = i + 2) {
        neighborIndex.set(new int[] {closestIndexY, i});
        checkNeighborLatLon(neighborIndex, latValues, lonValues, subsetLatLonPoint, actualLatLon);

        neighborIndex.set(new int[] {j, i});
        checkNeighborLatLon(neighborIndex, latValues, lonValues, subsetLatLonPoint, actualLatLon);
      }
    }
  }

  private void checkWellKnownTime(Variable time, int closestIndex, int closestTime, CalendarDate closestCalendarDate,
      String subsetIsoString) throws IOException {
    // test well known time indices
    Index testTimeIndex = Index.factory(time.getShape());
    Array timeValues = time.read();
    testTimeIndex.set(new int[] {closestIndex});
    int actualTime = timeValues.getInt(testTimeIndex);
    CalendarDateUnit cdu = CalendarDateUnit.of(null, time.getUnitsString());
    CalendarDate actualCalendarDate = cdu.makeCalendarDate(actualTime);
    assertThat(actualTime).isEqualTo(closestTime);
    assertThat(actualCalendarDate).isEqualTo(closestCalendarDate);

    // Check that we have the correct "closest" date / time
    // Should not be impacted by being a curvilinear grid, but why not
    // go ahead and test it out (not pretending to be a unit test here).
    CalendarDate subsetCalendarDate = CalendarDate.parseISOformat(null, subsetIsoString);
    double subsetTime = cdu.makeOffsetFromRefDate(subsetCalendarDate);
    for (int i = closestIndex - 1; i <= closestIndex + 1; i = i + 2) {
      testTimeIndex.set(new int[] {i});
      checkNeighborCalendarDate(testTimeIndex, timeValues, cdu, subsetTime, actualTime);
    }
  }

  private void checkNeighborCalendarDate(Index neighborIndex, Array offsetTimeValues, CalendarDateUnit cdu,
      double subsetOffsetTime, int actualOffsetTime) {
    checkNeighborNumerical(neighborIndex, offsetTimeValues, subsetOffsetTime, actualOffsetTime);

    CalendarDate actualCalendarDate = cdu.makeCalendarDate(actualOffsetTime);
    CalendarDate neighborCalendarDate = cdu.makeCalendarDate(offsetTimeValues.getInt(neighborIndex));
    CalendarDate subsetCalendarDate = cdu.makeCalendarDate(subsetOffsetTime);
    double diffActualCalendar = Math.abs(subsetCalendarDate.getDifferenceInMsecs(actualCalendarDate));
    double diffNeighbor = Math.abs(subsetCalendarDate.getDifferenceInMsecs(neighborCalendarDate));
    assertThat(diffActualCalendar).isLessThan(diffNeighbor);
  }

  private Array getExpectedData(Variable var, int startIndX, int endIndX, int startIndY, int endIndY)
      throws IOException, InvalidRangeException {
    return getExpectedData(var, startIndX, endIndX, startIndY, endIndY, -1, -1);
  }

  private Array getExpectedData(Variable var, int startIndX, int endIndX, int startIndY, int endIndY, int startIndTime,
      int endIndTime) throws InvalidRangeException, IOException {
    // Build a section string for the data read
    Builder sectionBuilder = Section.builder();
    ImmutableList<Dimension> dims = var.getDimensions();
    for (Dimension dim : dims) {
      String dimName = dim.getName();
      switch (dim.getName()) {
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
            sectionBuilder.appendRangeAll();
          }
          break;
        case "time":
          if (startIndTime != -1) {
            sectionBuilder.appendRange(startIndTime, endIndTime);
          } else {
            sectionBuilder.appendRangeAll();
          }
          break;
        default:
          throw new IllegalStateException(String.format("Dim name \"%s\" unexpected. Fail.", dimName));
      }
    }
    // read directly from the 3D array
    return var.read(sectionBuilder.build());
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

    assert lon != null;
    assert lat != null;

    Array lonValues = lon.read();
    Array latValues = lat.read();

    Index testIndex = Index.factory(lon.getShape());
    testIndex.set(new int[] {closestIndexY1, closestIndexX1});
    double actualLon = lonValues.getDouble(testIndex);
    double actualLat = latValues.getDouble(testIndex);
    LatLonPoint actualLatLon = LatLonPoint.create(actualLat, actualLon);

    CoverageCollection gcc = covDs.findCoverageDataset(FeatureType.CURVILINEAR);
    Coverage coverage = gcc.findCoverage(covVarName);
    CoverageCoordSys coordSys = coverage.getCoordSys();
    HorizCoordSys hcs = coordSys.getHorizCoordSys();

    // make sure the coverage coordinate system is able to get the same lat/lon point at
    // using the expected x and y indices
    LatLonPoint covLatLonPoint = hcs.getLatLon(closestIndexY1, closestIndexX1);
    assertThat(covLatLonPoint).isEqualTo(actualLatLon);
  }

  @Test
  public void testCoverageTime() throws IOException {
    Variable time = ncf.findVariable(timeVarName);

    assert time != null;

    Array timeValues = time.read();

    Index testIndex = Index.factory(time.getShape());
    testIndex.set(new int[] {closestIndexTimeStart});
    int actualTime = timeValues.getInt(testIndex);

    CoverageCollection gcc = covDs.findCoverageDataset(FeatureType.CURVILINEAR);
    Coverage coverage = gcc.findCoverage(covVarName);
    CoverageCoordSys coordSys = coverage.getCoordSys();
    CoverageCoordAxis covAxis = coordSys.getTimeAxis();
    assertThat(covAxis).isInstanceOf(CoverageCoordAxis1D.class);
    CoverageCoordAxis1D timeAxis = (CoverageCoordAxis1D) covAxis;
    double timeAxisValue = timeAxis.getCoordMidpoint(closestIndexTimeStart);

    // make sure the coverage coordinate system is able to get the same lat/lon point at
    // using the expected x and y indices
    assertThat((double) actualTime).isWithin(1e-6).of(timeAxisValue);
    assertThat((double) closestTimeStart).isWithin(1e-6).of(timeAxisValue);
  }

  @Test
  public void coverageCurvilinearPointSubset() throws IOException, InvalidRangeException {
    Variable var = ncf.findVariable(covVarName);
    assert var != null;
    String varName = var.getFullName();
    assert varName != null;

    // testWellKnownValues() and testCoverageHcs() shows that we we have correctly identified the x and y indices for
    // the closest grid point to the subset lat/lon.
    // Now, let's read the expected "time series" from the grid point and test that against the time series
    // from the subset coverage
    Array expectedTimeSeries = getExpectedData(var, closestIndexX1, closestIndexX1, closestIndexY1, closestIndexY1);

    // Now, subset the coverage and compare with the time series read from the array object
    CoverageCollection gcc = covDs.findCoverageDataset(FeatureType.CURVILINEAR);
    Coverage coverage = gcc.findCoverage(covVarName);

    // subset coverage using well known location
    SubsetParams subsetParams = new SubsetParams();
    subsetParams.setVariables(Collections.singletonList(covVarName));
    subsetParams.setLatLonPoint(subsetLatLon1);
    GeoReferencedArray covGeoRefArray = coverage.readData(subsetParams);
    Array subsetData = covGeoRefArray.getData();

    // expected time series array and the coverage subset array should have the same shape
    assertThat(expectedTimeSeries.getShape()).isEqualTo(subsetData.getShape());
    // compare the values of the data arrays
    assertThat(var.getFullName()).isNotNull();
    assertThat(CompareNetcdf2.compareData(var.getFullName(), expectedTimeSeries, subsetData)).isTrue();
  }

  @Test
  public void coverageCurvilinearPointSubsetWithTime() throws IOException, InvalidRangeException {
    Variable var = ncf.findVariable(covVarName);
    assert var != null;

    // testWellKnownValues() and testCoverageHcs() shows that we we have correctly identified the x and y indices for
    // the closest grid point to the subset lat/lon.
    // Now, let's read the expected "time series" from the grid point and test that against the time series
    // from the subset coverage
    Array expectedTimeSeries = getExpectedData(var, closestIndexX1, closestIndexX1, closestIndexY1, closestIndexY1,
        closestIndexTimeStart, closestIndexTimeEnd);

    // Now, subset the coverage and compare with the time series read from the array object
    CoverageCollection gcc = covDs.findCoverageDataset(FeatureType.CURVILINEAR);
    Coverage coverage = gcc.findCoverage(covVarName);

    // subset coverage using well known location
    SubsetParams subsetParams = new SubsetParams();
    subsetParams.setVariables(Collections.singletonList(covVarName));
    subsetParams.setLatLonPoint(subsetLatLon1);
    subsetParams.setTimeRange(CalendarDateRange.of(subsetCalendarDateStart, subsetCalendarDateEnd));
    GeoReferencedArray covGeoRefArray = coverage.readData(subsetParams);
    Array subsetData = covGeoRefArray.getData();

    // expected time series array and the coverage subset array should have the same shape
    assertThat(expectedTimeSeries.getShape()).isEqualTo(subsetData.getShape());
    // compare the values of the data arrays
    assertThat(CompareNetcdf2.compareData(var.getFullName(), expectedTimeSeries, subsetData)).isTrue();
  }

  @Test
  public void coverageCurvilinearGridSubset() throws IOException, InvalidRangeException {
    Variable var = ncf.findVariable(covVarName);
    assert var != null;
    String varName = var.getFullName();
    assert varName != null;

    // subset coverage using well known location
    CoverageCollection gcc = covDs.findCoverageDataset(FeatureType.CURVILINEAR);
    Coverage coverage = gcc.findCoverage(covVarName);
    SubsetParams subsetParams = new SubsetParams();
    subsetParams.setVariables(Collections.singletonList(covVarName));
    double deltaLat = subsetLatLon2.getLatitude() - subsetLatLon1.getLatitude();
    double deltaLon = subsetLatLon2.getLongitude() - subsetLatLon1.getLongitude();
    LatLonRect subsetBoundingBox = new LatLonRect(subsetLatLon1, deltaLat, deltaLon);
    subsetParams.setLatLonBoundingBox(subsetBoundingBox);
    GeoReferencedArray covGeoRefArray = coverage.readData(subsetParams);
    LatLonRect covSubsetBoundingBox = covGeoRefArray.getCoordSysForData().getHorizCoordSys().calcLatLonBoundingBox();

    // make sure that the bounding box of the subsetted data from the coverage is contained within the
    // bounding box of the area used to subset
    assertThat(subsetBoundingBox.containedIn(covSubsetBoundingBox)).isTrue();
  }

  @Test
  public void coverageCurvilinearGridSubsetWithTime() throws IOException, InvalidRangeException {
    Variable var = ncf.findVariable(covVarName);
    assert var != null;
    String varName = var.getFullName();
    assert varName != null;

    Variable timeVar = ncf.findVariable(timeVarName);
    assert timeVar != null;

    // subset coverage using well known location
    CoverageCollection gcc = covDs.findCoverageDataset(FeatureType.CURVILINEAR);
    Coverage coverage = gcc.findCoverage(covVarName);
    SubsetParams subsetParams = new SubsetParams();
    subsetParams.setVariables(Collections.singletonList(covVarName));
    double deltaLat = subsetLatLon2.getLatitude() - subsetLatLon1.getLatitude();
    double deltaLon = subsetLatLon2.getLongitude() - subsetLatLon1.getLongitude();
    LatLonRect subsetBoundingBox = new LatLonRect(subsetLatLon1, deltaLat, deltaLon);
    subsetParams.setLatLonBoundingBox(subsetBoundingBox);
    subsetParams.setTimeRange(CalendarDateRange.of(subsetCalendarDateStart, subsetCalendarDateEnd));
    GeoReferencedArray covGeoRefArray = coverage.readData(subsetParams);

    // compare subset time values
    int nPointsTime = covGeoRefArray.getCoordSysForData().getTimeAxis().getNcoords();
    assertThat(nPointsTime).isEqualTo(closestIndexTimeEnd - closestIndexTimeStart + 1);
    Array covAxisVals = covGeoRefArray.getCoordSysForData().getTimeAxis().getCoordsAsArray();
    Array timeVarVals = timeVar.read();
    for (int i = 0; i < nPointsTime; i++) {
      double timeVarVal = timeVarVals.getDouble(closestIndexTimeStart + i);
      assertThat(timeVarVal).isWithin(1e-6).of(covAxisVals.getDouble(i));
    }

    // make sure that the bounding box of the subsetted data from the coverage is contained within the
    // bounding box of the area used to subset
    LatLonRect covSubsetBoundingBox = covGeoRefArray.getCoordSysForData().getHorizCoordSys().calcLatLonBoundingBox();
    assertThat(subsetBoundingBox.containedIn(covSubsetBoundingBox)).isTrue();
  }

  @AfterClass
  public static void closeResources() throws IOException {
    covDs.close();
    ncf.close();
  }
}
