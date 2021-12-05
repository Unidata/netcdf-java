package ucar.nc2.grid;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.array.InvalidRangeException;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

@Category(NeedsCdmUnitTest.class)
public class TestGridFindPoint {
  @Test
  public void testFindPoint() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "transforms/Eumetsat.VerticalPerspective.grb";
    Formatter errlog = new Formatter();
    try (ucar.nc2.grid.GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      Grid grid = gds.findGrid("Pixel_scene_type").orElseThrow();
      GridHorizCoordinateSystem hcs = grid.getHorizCoordinateSystem();

      // find the x,y point for a specific lat/lon position
      double lat = 7.5973;
      double lon = 21.737;
      ProjectionPoint pp = hcs.getProjection().latLonToProj(lat, lon);

      GridHorizCoordinateSystem.CoordReturn cr = hcs.findXYindexFromCoord(pp.getX(), pp.getY()).orElseThrow();
      System.out.printf("Value at %s%n", cr);
      assertThat(cr.xindex).isEqualTo(874);
      assertThat(cr.yindex).isEqualTo(708);

      GridReferencedArray gra =
          grid.getReader().setProjectionPoint(ProjectionPoint.create(cr.xcoord, cr.ycoord)).read();
      float val = gra.data().getScalar().floatValue();
      assertThat(val).isWithin(1.e-5f).of(101.0f);
    }
  }
}
