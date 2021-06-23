package ucar.nc2.internal.grid;

import com.google.common.collect.Iterables;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.array.MinMax;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.grid.*;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.Formatter;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

/** Test {@link GridNetcdfDataset} */
public class TestGridNetcdfDataset {

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testOneProblem() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "tds_index/NCEP/NDFD/SPC/NDFD-SPC.ncx4";

    try (NetcdfDataset ds = ucar.nc2.dataset.NetcdfDatasets.openDataset(filename)) {
      Formatter infoLog = new Formatter();
      Optional<GridNetcdfDataset> result =
          GridNetcdfDataset.create(ds, infoLog).filter(gds -> !Iterables.isEmpty(gds.getGrids()));
      if (!result.isPresent()) {
        fail();
      }
      GridNetcdfDataset gridDataset = result.get();
      assertThat(gridDataset.getGridCoordinateSystems()).hasSize(4);
      assertThat(gridDataset.getGridCoordinateSystems()).hasSize(4);
      assertThat(gridDataset.getGridAxes()).hasSize(10);
      assertThat(gridDataset.getGridCoordinateSystems()).hasSize(4);
      assertThat(gridDataset.getGrids()).hasSize(4);
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testRegularIntervalCoordinate() throws IOException, InvalidRangeException {
    String filename = TestDir.cdmUnitTestDir + "tds_index/NCEP/NDFD/SPC/NDFD_SPC_CONUS_2p5km_20201116_1700.grib2.ncx4";

    try (NetcdfDataset ds = ucar.nc2.dataset.NetcdfDatasets.openDataset(filename)) {
      Formatter infoLog = new Formatter();
      Optional<GridNetcdfDataset> result =
          GridNetcdfDataset.create(ds, infoLog).filter(gds -> !Iterables.isEmpty(gds.getGrids()));
      if (!result.isPresent()) {
        fail();
      }
      GridNetcdfDataset gridDataset = result.get();
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
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testIrregularPointCoordinate() throws IOException {
    String filename =
        TestDir.cdmUnitTestDir + "gribCollections/rdavm/ds083.2/PofP/2004/200406/ds083.2-pofp-200406.ncx4";

    try (NetcdfDataset ds = ucar.nc2.dataset.NetcdfDatasets.openDataset(filename)) {
      Formatter infoLog = new Formatter();
      Optional<GridNetcdfDataset> result =
          GridNetcdfDataset.create(ds, infoLog).filter(gds -> !Iterables.isEmpty(gds.getGrids()));
      if (!result.isPresent()) {
        fail();
      }
      GridNetcdfDataset gridDataset = result.get();
      Grid grid =
          gridDataset.findGrid("Ozone_mixing_ratio_isobaric").orElseThrow(() -> new RuntimeException("Cant find grid"));
      GridCoordinateSystem csys = grid.getCoordinateSystem();
      GridAxis1D vertAxis = csys.getVerticalAxis();
      assertThat(vertAxis).isNotNull();
      assertThat(vertAxis.getSpacing()).isEqualTo(GridAxis.Spacing.irregularPoint);
      int ncoords = 6;
      assertThat(vertAxis.getNcoords()).isEqualTo(ncoords);
      double[] expected = new double[] {10.000000, 20.000000, 30.000000, 50.000000, 70.000000, 100.000000};
      for (int i = 0; i < vertAxis.getNcoords(); i++) {
        assertThat(vertAxis.getCoordMidpoint(i)).isEqualTo(expected[i]);
        if (i > 0 && i < vertAxis.getNcoords() - 1) {
          assertThat(vertAxis.getCoordEdge1(i)).isEqualTo((expected[i] + expected[i - 1]) / 2);
          assertThat(vertAxis.getCoordEdge2(i)).isEqualTo((expected[i] + expected[i + 1]) / 2);
        }
      }
      MinMax maxmin = vertAxis.getCoordEdgeMinMax();
      assertThat(maxmin.min()).isEqualTo(5.0);
      assertThat(maxmin.max()).isEqualTo(115.0);

      int count = 0;
      for (double val : vertAxis.getCoordsAsArray()) {
        assertThat(val).isEqualTo(expected[count++]);
      }
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testOffsetRegularCoordinate() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "tds_index/NCEP/NDFD/SPC/NDFD-SPC.ncx4";
    String gname = "Total_Probability_of_Severe_Thunderstorms_surface_24_Hour_Average_probability_above_0";
    System.out.printf("filename %s%n", filename);

    try (NetcdfDataset ds = ucar.nc2.dataset.NetcdfDatasets.openDataset(filename)) {
      Formatter infoLog = new Formatter();
      Optional<GridNetcdfDataset> result =
          GridNetcdfDataset.create(ds, infoLog).filter(gds -> !Iterables.isEmpty(gds.getGrids()));
      if (!result.isPresent()) {
        fail();
      }
      GridNetcdfDataset gridDataset = result.get();
      Grid grid = gridDataset.findGrid(gname).orElseThrow();
      GridCoordinateSystem csys = grid.getCoordinateSystem();
      GridAxisOffsetTimeRegular timeAxis = (GridAxisOffsetTimeRegular) csys.getTimeOffsetAxis();
      assertThat(timeAxis).isNotNull();
      assertThat(timeAxis.getSpacing()).isEqualTo(GridAxis.Spacing.discontiguousInterval);

      assertThat(timeAxis.getMinuteOffsets()).containsExactly(360, 420, 480, 540, 600, 1080, 1260);

      assertThat(timeAxis.getMinuteOffsets().size()).isEqualTo(7);
      assertThat(timeAxis.getNOffsetPerRun()).isEqualTo(2);
      double[] bounds1 = new double[] {30, 54, 29, 53, 28, 52, 27, 51, 26, 50, 18, 42, 15, 39};
      double[] bounds2 = new double[] {54, 78, 53, 77, 52, 76, 51, 75, 50, 74, 42, 66, 39, 63};

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
