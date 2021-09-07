package ucar.nc2.grid;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.array.Array;
import ucar.array.InvalidRangeException;
import ucar.array.Section;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.constants.FeatureType;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

/**
 * Test index to coordinate space mapping for curvilinear grids, e.g., lat(i,j), lon(i,j).
 */
@Category(NeedsCdmUnitTest.class)
public class TestCurvilinearGridPointMapping {

  private final String datasetLocation = TestDir.cdmUnitTestDir + "transforms/UTM/artabro_20120425.nc";
  private final String covName = "hs";

  private final int lonIndex = 170;
  private final int latIndex = 62;

  @Test
  public void checkGridCoordSystem_getLatLon() throws IOException, InvalidRangeException {
    NetcdfFile ncf = NetcdfFiles.open(datasetLocation);
    Variable latVar = ncf.findVariable("lat");
    assertThat(latVar).isNotNull();
    Section section = Section.builder().appendRange(latIndex, latIndex).appendRange(lonIndex, lonIndex).build();
    Array<Number> latArray = (Array<Number>) latVar.readArray(section);
    Variable lonVar = ncf.findVariable("lon");
    assertThat(lonVar).isNotNull();
    Array<Number> lonArray = (Array<Number>) lonVar.readArray(section);

    float latVal = latArray.getScalar().floatValue();
    float lonVal = lonArray.getScalar().floatValue();
    System.out.printf("latVal = %f lonVal = %f%n", latVal, lonVal);

    Formatter errlog = new Formatter();
    try (GridDataset gds = GridDatasetFactory.openGridDataset(datasetLocation, errlog)) {
      assertThat(gds).isNotNull();
      assertThat(gds.getGrids()).hasSize(10);
      Grid grid = gds.findGrid(covName).orElseThrow();
      assertThat(grid).isNotNull();

      GridCoordinateSystem cs = grid.getCoordinateSystem();
      assertThat(cs).isNotNull();
      assertThat(cs.getFeatureType()).isEqualTo(FeatureType.CURVILINEAR);
      GridHorizCoordinateSystem hcs = cs.getHorizCoordinateSystem();
      assertThat(hcs).isNotNull();
      assertThat(hcs.isCurvilinear()).isTrue();

      float TOL = 1.0e-4f; // just the average of the edges, not exact.
      LatLonPoint llPnt = hcs.getLatLon(lonIndex, latIndex);
      assertThat(latVal).isWithin(TOL).of((float) llPnt.getLatitude());
      assertThat(lonVal).isWithin(TOL).of((float) llPnt.getLongitude());
    }
  }
}
