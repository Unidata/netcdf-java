package ucar.nc2.ncml;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.array.Array;
import ucar.array.InvalidRangeException;
import ucar.nc2.grid.Grid;
import ucar.nc2.grid.GridDataset;
import ucar.nc2.grid.GridDatasetFactory;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Formatter;

/**
 * JoinNew has bug when groups are present because non-agg vars are not getting proxied.
 * The example unfortunately does not satisfy agg homogeneity: one is 44 x 60, the other 46 x 60.
 */
@Category(NeedsCdmUnitTest.class)
@Ignore("Fails")
public class TestJoinNewWithGroups {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  // test case from joleenf@ssec.wisc.edu 03/22/2012

  @Test
  public void testJoinNewWithGroups() throws IOException, InvalidRangeException {
    String location = TestDir.cdmUnitTestDir + "agg/groups/groupsJoinNew.ncml";
    Formatter errlog = new Formatter();
    try (GridDataset ncd = GridDatasetFactory.openGridDataset(location, errlog)) {
      assertThat(ncd).isNotNull();
      Grid va = ncd.findGrid("All_Data/Lifted_Index").orElseThrow(); // the only agg var
      assertThat(va).isNotNull();
      assertThat(va.getCoordinateSystem().getNominalShape()).hasSize(3);
      assertThat(va.getCoordinateSystem().getNominalShape()).isEqualTo(ImmutableList.of(2, 44, 60));

      Grid v = ncd.findGrid("All_Data/CAPE").orElseThrow(); // random non-agg var
      assertThat(va.getCoordinateSystem().getNominalShape()).hasSize(2);
      Array<?> data = v.getReader().read().data();
      assertThat(va.getCoordinateSystem().getNominalShape()).isEqualTo(data.getShape());
    }
  }
}
