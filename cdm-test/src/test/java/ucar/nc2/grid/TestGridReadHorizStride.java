/* Copyright Unidata */
package ucar.nc2.grid;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.Arrays;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

/** Test {@link GridHorizCoordinateSystem} reading with horizontal strides. */
@Category(NeedsCdmUnitTest.class)
public class TestGridReadHorizStride {

  @Test
  public void testHorizStride() throws IOException, ucar.array.InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4";
    String gridName = "Ozone_Mixing_Ratio_isobaric";
    System.out.printf("Test Dataset %s%n", endpoint);

    Formatter errlog = new Formatter();
    try (GridDataset gds = GridDatasetFactory.openGridDataset(endpoint, errlog)) {
      assertThat(gds).isNotNull();
      Grid grid = gds.findGrid(gridName).orElseThrow();
      assertThat(grid).isNotNull();

      GridCoordinateSystem cs = grid.getCoordinateSystem();
      assertThat(cs).isNotNull();
      GridHorizCoordinateSystem hcs = cs.getHorizCoordinateSystem();
      assertThat(hcs).isNotNull();
      assertThat(hcs.getShape()).isEqualTo(ImmutableList.of(73, 144));

      GridReferencedArray geoArray = grid.getReader().setHorizStride(2).read();
      MaterializedCoordinateSystem mcs = geoArray.getMaterializedCoordinateSystem();
      assertThat(mcs).isNotNull();

      System.out.printf(" data shape=%s%n", Arrays.toString(geoArray.data().getShape()));
      assertThat(geoArray.data().getShape()).isEqualTo(new int[] {1, 93, 12, 37, 72});

      GridHorizCoordinateSystem hcs2 = mcs.getHorizCoordinateSystem();
      assertThat(hcs2).isNotNull();
      System.out.printf(" data hcs shape=%s%n", hcs2.getShape());
      assertThat(hcs2.getShape()).isEqualTo(ImmutableList.of(37, 72));
    }
  }

}
