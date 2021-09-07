/* Copyright Unidata */
package ucar.nc2.grid;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.grib.collection.Grib;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

/** Test {@link GridHorizCoordinateSystem} reading with horizontal subsets. */
public class TestGridReadHorizSubset {

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testMSG() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "transforms/Eumetsat.VerticalPerspective.grb";
    System.out.printf("open %s%n", filename);

    Formatter errlog = new Formatter();
    try (GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      String gribId = "VAR_3-0-8";
      Grid coverage = gds.findGridByAttribute(Grib.VARIABLE_ID_ATTNAME, gribId).orElseThrow(); // "Pixel_scene_type");
      assertThat(coverage).isNotNull();

      GridCoordinateSystem cs = coverage.getCoordinateSystem();
      assertThat(cs).isNotNull();
      assertThat(cs.getNominalShape()).isEqualTo(ImmutableList.of(1, 1, 1237, 1237));
      GridHorizCoordinateSystem hcs = cs.getHorizCoordinateSystem();
      assertThat(hcs).isNotNull();

      // bbox = ll: 16.79S 20.5W+ ur: 14.1N 20.09E
      LatLonRect bbox = new LatLonRect(-16.79, -20.5, 14.1, 20.9);

      Projection p = hcs.getProjection();
      ProjectionRect prect = p.latLonToProjBB(bbox); // must override default implementation
      System.out.printf("%s -> %s %n", bbox, prect);

      ProjectionRect expected =
          new ProjectionRect(ProjectionPoint.create(-2129.568880, -1793.004131), 4297.845286, 3308.388526);
      // assert prect.nearlyEquals(expected);
      assertThat(prect.nearlyEquals(expected)).isTrue();

      LatLonRect bb2 = p.projToLatLonBB(prect);
      System.out.printf("%s -> %s %n", prect, bb2);
      GridReferencedArray geo = coverage.getReader().setLatLonBoundingBox(bbox).read();
      assertThat(geo).isNotNull();
      assertThat(geo.getMaterializedCoordinateSystem()).isNotNull();
      assertThat(geo.getMaterializedCoordinateSystem().getHorizCoordinateSystem()).isNotNull();

      assertThat(geo.getMaterializedCoordinateSystem().getHorizCoordinateSystem().getShape())
          .isEqualTo(ImmutableList.of(363, 479));
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testLatLonSubset() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "conventions/problem/SUPER-NATIONAL_latlon_IR_20070222_1600.nc";
    System.out.printf("open %s%n", filename);

    Formatter errlog = new Formatter();
    try (GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      String gribId = "micron11";
      Grid coverage = gds.findGrid(gribId).orElseThrow();
      assertThat(coverage).isNotNull();

      GridCoordinateSystem cs = coverage.getCoordinateSystem();
      assertThat(cs).isNotNull();
      GridHorizCoordinateSystem hcs = cs.getHorizCoordinateSystem();
      assertThat(hcs).isNotNull();

      LatLonRect bbox = LatLonRect.builder(LatLonPoint.create(40.0, -100.0), 10.0, 20.0).build();
      checkLatLonSubset(hcs, coverage, bbox, new int[] {141, 281});

      bbox = LatLonRect.builder(LatLonPoint.create(-40.0, -180.0), 120.0, 300.0).build();
      checkLatLonSubset(hcs, coverage, bbox, new int[] {800, 1300});
    }
  }

  static GridReferencedArray checkLatLonSubset(GridHorizCoordinateSystem hcs, Grid coverage, LatLonRect bbox,
      int[] expectedShape) throws Exception {
    System.out.printf(" coverage llbb = %s width=%f%n", hcs.getLatLonBoundingBox().toString2(),
        hcs.getLatLonBoundingBox().getWidth());
    System.out.printf(" constrain bbox= %s width=%f%n", bbox.toString2(), bbox.getWidth());

    GridReferencedArray geoArray = coverage.getReader().setLatLonBoundingBox(bbox).setTimePresent().read();
    assertThat(geoArray).isNotNull();
    System.out.printf(" data shape=%s%n", java.util.Arrays.toString(geoArray.data().getShape()));
    assertThat(geoArray.data().getShape()).isEqualTo(expectedShape);

    MaterializedCoordinateSystem mcs = geoArray.getMaterializedCoordinateSystem();
    assertThat(mcs).isNotNull();
    GridHorizCoordinateSystem hcs2 = mcs.getHorizCoordinateSystem();
    assertThat(hcs2).isNotNull();
    System.out.printf(" data hcs shape=%s%n", hcs2.getShape());
    int rank = expectedShape.length;
    ImmutableList<Integer> expectedHcs = ImmutableList.of(expectedShape[rank - 2], expectedShape[rank - 1]);
    assertThat(hcs2.getShape()).isEqualTo(expectedHcs);

    return geoArray;
  }

}
