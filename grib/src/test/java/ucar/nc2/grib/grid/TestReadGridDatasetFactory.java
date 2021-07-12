/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grid;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.array.MinMax;
import ucar.nc2.grid.CoordInterval;
import ucar.nc2.grid2.Grid;
import ucar.nc2.grid2.GridAxis;
import ucar.nc2.grid2.GridAxisInterval;
import ucar.nc2.grid2.GridAxisSpacing;
import ucar.nc2.grid2.GridCoordinateSystem;
import ucar.nc2.grid2.GridDataset;
import ucar.nc2.grid2.GridDatasetFactory;
import ucar.nc2.grid2.GridTimeCoordinateSystem;
import ucar.nc2.grid2.Grids;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

/** Test reading Grib through {@link GridDatasetFactory} */
public class TestReadGridDatasetFactory {

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testOneProblem() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "tds_index/NCEP/NDFD/SPC/NDFD_SPC_CONUS_CONDUIT.ncx4";
    System.out.printf("filename %s%n", filename);

    Formatter errlog = new Formatter();
    try (GridDataset gridDataset = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gridDataset).isNotNull();
      assertThat(gridDataset.getGridCoordinateSystems()).hasSize(4);
      assertThat(gridDataset.getGridCoordinateSystems()).hasSize(4);
      assertThat(gridDataset.getGridAxes()).hasSize(10);
      assertThat(gridDataset.getGridCoordinateSystems()).hasSize(4);
      assertThat(gridDataset.getGrids()).hasSize(4);
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testRegularIntervalCoordinate() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "tds_index/NCEP/NDFD/SPC/NDFD_SPC_CONUS_CONDUIT.ncx4";
    System.out.printf("filename %s%n", filename);

    Formatter errlog = new Formatter();
    try (GridDataset gridDataset = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gridDataset).isNotNull();
      Grid grid = gridDataset.findGrid("Convective_Hazard_Outlook_surface_24_Hour_Average")
          .orElseThrow(() -> new RuntimeException("Cant find grid"));
      GridCoordinateSystem csys = grid.getCoordinateSystem();
      GridTimeCoordinateSystem tcsys = csys.getTimeCoordinateSystem();
      assertThat(tcsys).isNotNull();
      GridAxis<?> timeAxis = tcsys.getTimeOffsetAxis(0);
      assertThat((Object) timeAxis).isNotNull();
      assertThat((Object) timeAxis).isInstanceOf(GridAxisInterval.class);
      GridAxisInterval timeAxis1D = (GridAxisInterval) timeAxis;

      assertThat(timeAxis1D.getSpacing()).isEqualTo(GridAxisSpacing.regularInterval);
      assertThat(timeAxis1D.getNominalSize()).isEqualTo(3);
      double[] expected = new double[] {-13, 11, 35, 59};
      for (int i = 0; i < timeAxis1D.getNominalSize(); i++) {
        CoordInterval intv = timeAxis1D.getCoordInterval(i);
        assertThat(intv.start()).isEqualTo(expected[i]);
        assertThat(intv.end()).isEqualTo(expected[i + 1]);
        assertThat(timeAxis1D.getCoordMidpoint(i)).isEqualTo((expected[i] + expected[i + 1]) / 2);
      }
      MinMax maxmin = Grids.getCoordEdgeMinMax(timeAxis1D);
      assertThat(maxmin.min()).isEqualTo(expected[0]);
      assertThat(maxmin.max()).isEqualTo(expected[3]);
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testIrregularPointCoordinate() throws IOException {
    String filename =
        TestDir.cdmUnitTestDir + "gribCollections/rdavm/ds083.2/PofP/2004/200406/ds083.2-pofp-200406.ncx4";
    System.out.printf("filename %s%n", filename);

    Formatter errlog = new Formatter();
    try (GridDataset gridDataset = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gridDataset).isNotNull();
      Grid grid =
          gridDataset.findGrid("Ozone_mixing_ratio_isobaric").orElseThrow(() -> new RuntimeException("Cant find grid"));
      GridCoordinateSystem csys = grid.getCoordinateSystem();
      GridAxis<?> vertAxis = csys.getVerticalAxis();
      assertThat((Object) vertAxis).isNotNull();
      assertThat(vertAxis.getSpacing()).isEqualTo(GridAxisSpacing.irregularPoint);
      int ncoords = 6;
      assertThat(vertAxis.getNominalSize()).isEqualTo(ncoords);
      double[] expected = new double[] {10.000000, 20.000000, 30.000000, 50.000000, 70.000000, 100.000000};
      double[] bounds = new double[] {5, 15.000000, 25.000000, 40.000000, 60.000000, 85.000000, 115.000000};
      for (int i = 0; i < vertAxis.getNominalSize(); i++) {
        assertThat(vertAxis.getCoordMidpoint(i)).isEqualTo(expected[i]);
        CoordInterval intv = vertAxis.getCoordInterval(i);
        assertThat(intv.start()).isEqualTo(bounds[i]);
        assertThat(intv.end()).isEqualTo(bounds[i + 1]);
      }
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testDiscontiguousIntervalCoordinate() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "tds_index/NCEP/NDFD/NWS/NDFD_NWS_CONUS_CONDUIT.ncx4";
    String gname = "Maximum_temperature_height_above_ground_Mixed_intervals_Maximum";
    System.out.printf("filename %s%n", filename);

    Formatter errlog = new Formatter();
    try (GridDataset gridDataset = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gridDataset).isNotNull();
      Grid grid = gridDataset.findGrid(gname).orElseThrow();
      GridCoordinateSystem csys = grid.getCoordinateSystem();
      GridTimeCoordinateSystem tcsys = csys.getTimeCoordinateSystem();
      assertThat(tcsys).isNotNull();

      GridAxis<?> timeAxis = tcsys.getTimeOffsetAxis(0);
      assertThat((Object) timeAxis).isNotNull();
      assertThat((Object) timeAxis).isInstanceOf(GridAxisInterval.class);
      assertThat(timeAxis.getSpacing()).isEqualTo(GridAxisSpacing.discontiguousInterval);
      GridAxisInterval timeAxisIntv = (GridAxisInterval) timeAxis;

      double[] bounds1 = new double[] {12, 36, 60, 84, 108, 132, 156};
      double[] bounds2 = new double[] {24, 48, 72, 96, 120, 144, 168};

      for (int i = 0; i < timeAxisIntv.getNominalSize(); i++) {
        CoordInterval intv = timeAxisIntv.getCoordInterval(i);
        assertThat(intv.start()).isEqualTo(bounds1[i]);
        assertThat(intv.end()).isEqualTo(bounds2[i]);
        assertThat(timeAxisIntv.getCoordMidpoint(i)).isEqualTo((bounds1[i] + bounds2[i]) / 2);
      }

      int count = 0;
      for (CoordInterval val : timeAxisIntv) {
        assertThat(val).isEqualTo(CoordInterval.create(bounds1[count], bounds2[count]));
        count++;
      }
    }
  }

}
