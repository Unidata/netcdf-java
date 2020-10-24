/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grid;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.internal.util.CompareArrayToMa2;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/** Compare reading netcdf with Array */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestReadGridCompare {

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    FileFilter ff = TestDir.FileFilterSkipSuffix(".cdl .ncml perverse.nc");
    List<Object[]> result = new ArrayList<>(500);
    try {
      result.add(new Object[] {TestDir.cdmLocalTestDataDir + "ncml/nc/cldc.mean.nc"});
      result.add(new Object[] {TestDir.cdmLocalTestDataDir + "ncml/fmrc/GFS_Puerto_Rico_191km_20090729_0000.nc"});
      result.add(new Object[] {TestDir.cdmLocalTestDataDir + "ncml/nc/ubyte_1.nc4"});
    } catch (Exception e) {
      e.printStackTrace();
    }

    return result;
  }

  /////////////////////////////////////////////////////////////

  public TestReadGridCompare(String filename) {
    this.filename = filename;
  }

  private final String filename;

  @Test
  public void compareGrid() throws IOException, InvalidRangeException {
    // these are failing in old code.
    if (filename.endsWith("cdm_sea_soundings.nc4"))
      return;
    if (filename.endsWith("IntTimSciSamp.nc"))
      return;

    compareGridDataset(filename);
    compareGridCoverage(filename);
  }

  public static void compareGridDataset(String filename) throws IOException, InvalidRangeException {
    Formatter errlog = new Formatter();
    try (GridDataset newDataset = GridDatasetFactory.openGridDataset(filename, errlog);
        ucar.nc2.dt.grid.GridDataset dataset = ucar.nc2.dt.grid.GridDataset.open(filename)) {
      System.out.println("compareGridDataset: " + newDataset.getLocation());

      boolean ok = true;
      for (Grid grid : newDataset.getGrids()) {
        ucar.nc2.dt.grid.GeoGrid geogrid = dataset.findGridByName(grid.getName());
        assertThat(geogrid).isNotNull();

        GridCoordinateSystem newGcs = grid.getCoordinateSystem();
        assertThat(newGcs.getTimeAxis()).isNotNull();
        GridAxis1DTime newTimeAxis = newGcs.getTimeAxis();

        ucar.nc2.dt.GridCoordSystem gcs = geogrid.getCoordinateSystem();
        assertThat(gcs.getTimeAxis()).isNotNull();
        CoordinateAxis axis = gcs.getTimeAxis();
        assertThat(axis).isInstanceOf(CoordinateAxis1DTime.class);
        CoordinateAxis1DTime timeAxis = (CoordinateAxis1DTime) axis;

        assertThat(newTimeAxis.getNcoords()).isEqualTo(timeAxis.getSize());
        for (int timeIdx = 0; timeIdx < newTimeAxis.getNcoords(); timeIdx++) {
          CalendarDate date = newTimeAxis.getCalendarDate(timeIdx);
          assertThat(date).isEqualTo(timeAxis.getCalendarDate(timeIdx));

          if (newGcs.getVerticalAxis() != null) {
            GridAxis1D newVertAxis = newGcs.getVerticalAxis();
            CoordinateAxis1D vertAxis = (CoordinateAxis1D) gcs.getVerticalAxis();
            assertThat(newVertAxis.getNcoords()).isEqualTo(vertAxis.getSize());
            for (int vertIdx = 0; vertIdx < newVertAxis.getNcoords(); vertIdx++) {
              GridSubset subset = new GridSubset();
              subset.setTime(date);
              subset.setVertCoord(newVertAxis.getCoordMidpoint(vertIdx));
              GridReferencedArray geoArray = grid.readData(subset);

              ucar.ma2.Array org = geogrid.readDataSlice(timeIdx, vertIdx, -1, -1);
              Formatter f = new Formatter();
              boolean ok1 = CompareArrayToMa2.compareData(f, grid.getName(), org, geoArray.data(), false, true);
              if (!ok1) {
                System.out.printf("timeIdx %d vertIdx %d %s%n", timeIdx, vertIdx, f);
              }
              ok &= ok1;
            }

          } else {
            GridSubset subset = new GridSubset();
            subset.setTime(date);
            GridReferencedArray geoArray = grid.readData(subset);

            ucar.ma2.Array org = geogrid.readVolumeData(timeIdx);
            Formatter f = new Formatter();
            boolean ok1 = CompareArrayToMa2.compareData(f, grid.getName(), org, geoArray.data(), false, true);
            if (!ok1) {
              System.out.printf("%s%n", f);
            }
            ok &= ok1;
          }
        }
      }
      assertThat(ok).isTrue();
    }
  }

  public static void compareGridCoverage(String filename) throws IOException, InvalidRangeException {
    Formatter errlog = new Formatter();
    try (GridDataset newDataset = GridDatasetFactory.openGridDataset(filename, errlog);
        FeatureDatasetCoverage covDataset = CoverageDatasetFactory.open(filename)) {
      System.out.println("compareGridCoverage: " + newDataset.getLocation());

      CoverageCollection covCollection = covDataset.getCoverageCollections().get(0);

      boolean ok = true;
      for (Grid grid : newDataset.getGrids()) {
        Coverage coverage = covCollection.findCoverage(grid.getName());
        assertThat(coverage).isNotNull();

        GridCoordinateSystem newGcs = grid.getCoordinateSystem();
        assertThat(newGcs.getTimeAxis()).isNotNull();
        GridAxis1DTime newTimeAxis = newGcs.getTimeAxis();

        CoverageCoordSys gcs = coverage.getCoordSys();
        assertThat(gcs.getTimeAxis()).isNotNull();
        CoverageCoordAxis axis = gcs.getTimeAxis();
        assertThat(axis).isInstanceOf(CoverageCoordAxis1D.class);
        CoverageCoordAxis1D timeAxis = (CoverageCoordAxis1D) axis;

        assertThat(newTimeAxis.getNcoords()).isEqualTo(timeAxis.getNcoords());
        for (int timeIdx = 0; timeIdx < newTimeAxis.getNcoords(); timeIdx++) {
          CalendarDate date = newTimeAxis.getCalendarDate(timeIdx);
          assertThat(date).isEqualTo(timeAxis.makeDate(timeAxis.getCoordMidpoint(timeIdx)));

          if (newGcs.getVerticalAxis() != null) {
            GridAxis1D newVertAxis = newGcs.getVerticalAxis();
            CoverageCoordAxis zAxis = gcs.getZAxis();
            assertThat(zAxis).isInstanceOf(CoverageCoordAxis1D.class);
            CoverageCoordAxis1D vertAxis = (CoverageCoordAxis1D) zAxis;

            assertThat(newVertAxis.getNcoords()).isEqualTo(vertAxis.getNcoords());
            for (int vertIdx = 0; vertIdx < newVertAxis.getNcoords(); vertIdx++) {
              GridSubset subset = new GridSubset();
              subset.setTime(date);
              subset.setVertCoord(newVertAxis.getCoordMidpoint(vertIdx));
              GridReferencedArray gridArray = grid.readData(subset);

              SubsetParams subsetp = new SubsetParams();
              subsetp.setTime(date);
              subsetp.setVertCoord(vertAxis.getCoordMidpoint(vertIdx));
              GeoReferencedArray geoArray = coverage.readData(subsetp);

              Formatter f = new Formatter();
              boolean ok1 =
                  CompareArrayToMa2.compareData(f, grid.getName(), geoArray.getData(), gridArray.data(), false, true);
              if (!ok1) {
                System.out.printf("timeIdx %d vertIdx %d %s%n", timeIdx, vertIdx, f);
              }
              ok &= ok1;
            }

          } else {
            GridSubset subset = new GridSubset();
            subset.setTime(date);
            GridReferencedArray gridArray = grid.readData(subset);

            SubsetParams subsetp = new SubsetParams();
            subsetp.setTime(date);
            GeoReferencedArray geoArray = coverage.readData(subsetp);

            Formatter f = new Formatter();
            boolean ok1 =
                CompareArrayToMa2.compareData(f, grid.getName(), geoArray.getData(), gridArray.data(), false, true);
            if (!ok1) {
              System.out.printf("%s%n", f);
            }
            ok &= ok1;
          }
        }
      }
      assertThat(ok).isTrue();
    }
  }

}

