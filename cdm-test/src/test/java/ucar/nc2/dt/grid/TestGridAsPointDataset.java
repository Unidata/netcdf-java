package ucar.nc2.dt.grid;

import java.io.IOException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.Array;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

@Category(NeedsCdmUnitTest.class)
public class TestGridAsPointDataset {
  @Test
  public void testStuff() throws IOException {
    String fileIn = TestDir.cdmUnitTestDir + "transforms/Eumetsat.VerticalPerspective.grb";
    GridDataset gds = ucar.nc2.dt.grid.GridDataset.open(fileIn);
    GridDatatype grid = gds.findGridDatatype( "Pixel_scene_type");
    GridCoordSystem gcs = grid.getCoordinateSystem();

    double lat = 8.0;
    double lon = 21.0;

    // find the x,y point for a specific lat/lon position
    int[] xy = gcs.findXYindexFromLatLon(lat, lon, null); // xy[0] = x, xy[1] = y

    // read the data at that lat, lon a specific t and z value
    Array data  = grid.readDataSlice(0, 0, xy[1], xy[0]); // note t, z, y, x
    double val = data.getDouble(0);
    System.out.printf("Value at %f %f == %f%n", lat, lon, val);
  }
}
