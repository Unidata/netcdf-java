package ucar.nc2.internal.grid;

import com.google.common.collect.Iterables;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.grid.*;
import ucar.nc2.util.MinMax;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.Formatter;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

/** Test {@link GridDatasetImpl} */
public class TestGridDatasetImpl {

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testOneProblem() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "gribCollections/ndfd_spc/NDFD-SPC.ncx4";

    try (NetcdfDataset ds = ucar.nc2.dataset.NetcdfDatasets.openDataset(filename)) {
      Formatter infoLog = new Formatter();
      Optional<GridDatasetImpl> result =
          GridDatasetImpl.create(ds, infoLog).filter(gds -> !Iterables.isEmpty(gds.getGrids()));
      if (!result.isPresent()) {
        fail();
      }
      GridDatasetImpl gridDataset = result.get();
      assertThat(gridDataset.getCoordAxes()).hasSize(10);
      assertThat(gridDataset.getCoordSystems()).hasSize(4);
      assertThat(gridDataset.getGrids()).hasSize(4);
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testRegularIntervalCoordinate() throws IOException, InvalidRangeException {
    String filename = TestDir.cdmUnitTestDir + "gribCollections/ndfd_spc/NDFD_SPC_CONUS_2p5km_20201116_1700.grib2.ncx4";

    try (NetcdfDataset ds = ucar.nc2.dataset.NetcdfDatasets.openDataset(filename)) {
      Formatter infoLog = new Formatter();
      Optional<GridDatasetImpl> result =
          GridDatasetImpl.create(ds, infoLog).filter(gds -> !Iterables.isEmpty(gds.getGrids()));
      if (!result.isPresent()) {
        fail();
      }
      GridDatasetImpl gridDataset = result.get();
      Grid grid = gridDataset.findGrid("Convective_Hazard_Outlook_surface_24_Hour_Average")
          .orElseThrow(() -> new RuntimeException("Cant find grid"));
      GridCoordinateSystem csys = grid.getCoordinateSystem();
      GridAxis1DTime timeAxis = csys.getTimeAxis();
      assertThat(timeAxis).isNotNull();
      assertThat(timeAxis.getSpacing()).isEqualTo(GridAxis.Spacing.regularInterval);
      assertThat(timeAxis.getNcoords()).isEqualTo(3);
      double[] expected = new double[] {-5, 19, 43, 67};
      for (int i = 0; i < timeAxis.getNcoords(); i++) {
        assertThat(timeAxis.getCoordEdge1(i)).isEqualTo(expected[i]);
        assertThat(timeAxis.getCoordEdge2(i)).isEqualTo(expected[i + 1]);
        assertThat(timeAxis.getCoordMidpoint(i)).isEqualTo((expected[i] + expected[i + 1]) / 2);
      }
      MinMax maxmin = timeAxis.getCoordEdgeMinMax();
      assertThat(maxmin.min()).isEqualTo(expected[0]);
      assertThat(maxmin.max()).isEqualTo(expected[3]);

      double[] expectedBounds = new double[] {-5, 19, 19, 43, 43, 67};
      int count = 0;
      for (double val : timeAxis.getCoordBoundsAsArray()) {
        assertThat(val).isEqualTo(expectedBounds[count++]);
      }

      count = 0;
      for (double val : timeAxis.getCoordsAsArray()) {
        assertThat(val).isEqualTo((expectedBounds[count] + expectedBounds[count + 1]) / 2);
        count += 2;
      }

      GridSubset subset = new GridSubset().setRunTimeLatest();
      GridReferencedArray geoArray = grid.readData(subset);
      assertThat(geoArray.data().getRank()).isEqualTo(3);
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testIrregularPointCoordinate() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "gribCollections/ndfd_spc/NDFD-SPC.ncx4";

    try (NetcdfDataset ds = ucar.nc2.dataset.NetcdfDatasets.openDataset(filename)) {
      Formatter infoLog = new Formatter();
      Optional<GridDatasetImpl> result =
          GridDatasetImpl.create(ds, infoLog).filter(gds -> !Iterables.isEmpty(gds.getGrids()));
      if (!result.isPresent()) {
        fail();
      }
      GridDatasetImpl gridDataset = result.get();
      Grid grid = gridDataset.findGrid("Convective_Hazard_Outlook_surface_24_Hour_Average")
          .orElseThrow(() -> new RuntimeException("Cant find grid"));
      GridCoordinateSystem csys = grid.getCoordinateSystem();
      GridAxis1DTime timeAxis = csys.getRunTimeAxis();
      assertThat(timeAxis).isNotNull();
      assertThat(timeAxis.getSpacing()).isEqualTo(GridAxis.Spacing.irregularPoint);
      int ncoords = 17;
      assertThat(timeAxis.getNcoords()).isEqualTo(ncoords);
      double[] expected = new double[] {0, 3, 4, 8, 12, 13, 15, 20, 23, 24, 27, 28, 32, 36, 39, 44, 47};
      for (int i = 0; i < timeAxis.getNcoords(); i++) {
        assertThat(timeAxis.getCoordMidpoint(i)).isEqualTo(expected[i]);
        if (i > 0 && i < timeAxis.getNcoords() - 1) {
          assertThat(timeAxis.getCoordEdge1(i)).isEqualTo((expected[i] + expected[i - 1]) / 2);
          assertThat(timeAxis.getCoordEdge2(i)).isEqualTo((expected[i] + expected[i + 1]) / 2);
        }
      }
      MinMax maxmin = timeAxis.getCoordEdgeMinMax();
      assertThat(maxmin.min()).isEqualTo(-1.5);
      assertThat(maxmin.max()).isEqualTo(48.5);

      int count = 0;
      for (double val : timeAxis.getCoordsAsArray()) {
        assertThat(val).isEqualTo(expected[count++]);
      }
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testOffsetRegularCoordinate() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "gribCollections/ndfd_spc/NDFD-SPC.ncx4";

    try (NetcdfDataset ds = ucar.nc2.dataset.NetcdfDatasets.openDataset(filename)) {
      Formatter infoLog = new Formatter();
      Optional<GridDatasetImpl> result =
          GridDatasetImpl.create(ds, infoLog).filter(gds -> !Iterables.isEmpty(gds.getGrids()));
      if (!result.isPresent()) {
        fail();
      }
      GridDatasetImpl gridDataset = result.get();
      Grid grid =
          gridDataset.findGrid("Total_Probability_of_Severe_Thunderstorms_surface_24_Hour_Average_probability_above_0")
              .orElseThrow(() -> new RuntimeException("Cant find grid"));
      GridCoordinateSystem csys = grid.getCoordinateSystem();
      GridAxisOffsetTimeRegular timeAxis = (GridAxisOffsetTimeRegular) csys.getTimeOffsetAxis();
      assertThat(timeAxis).isNotNull();
      assertThat(timeAxis.getSpacing()).isEqualTo(GridAxis.Spacing.discontiguousInterval);

      /*
       * CdmIndex
       * hour 8: timeIntv: (28,52), (52,76), (2)
       * hour 9: timeIntv: (27,51), (51,75), (2)
       * hour 10: timeIntv: (26,50), (50,74), (2)
       * hour 21: timeIntv: (15,39), (39,63), (2)
       */
      assertThat(timeAxis.getHourOffsets()).containsExactly(8, 9, 10, 21);

      /*
       * NewGrid
       * npts: 2 [0.000000,0.000000] spacing=discontiguousInterval [4, 2]
       * validtime1Offset values =
       * {
       * {40.0, 64.0},
       * {39.0, 63.0},
       * {38.0, 62.0},
       * {27.0, 51.0}
       * }
       * validtime1Offset bounds =
       * {
       * {
       * {28.0, 52.0},
       * {52.0, 76.0}
       * },
       * {
       * {27.0, 51.0},
       * {51.0, 75.0}
       * },
       * {
       * {26.0, 50.0},
       * {50.0, 74.0}
       * },
       * {
       * {15.0, 39.0},
       * {39.0, 63.0}
       * }
       * }
       */

      assertThat(timeAxis.getHourOffsets().size()).isEqualTo(4);
      assertThat(timeAxis.getNOffsetPerRun()).isEqualTo(2);
      double[] bounds1 = new double[] {28, 52, 27, 51, 26, 50, 15, 39};
      double[] bounds2 = new double[] {52, 76, 51, 75, 50, 74, 39, 63};

      int count = 0;
      for (double val : timeAxis.getCoordBoundsAsArray()) {
        if (count % 2 == 0) {
          assertThat(val).isEqualTo(bounds1[count / 2]);
        } else {
          assertThat(val).isEqualTo(bounds2[count / 2]);
        }
        count++;
      }

      count = 0;
      for (double val : timeAxis.getCoordsAsArray()) {
        assertThat(val).isEqualTo((bounds1[count] + bounds2[count]) / 2);
        count++;
      }
    }
  }

}
