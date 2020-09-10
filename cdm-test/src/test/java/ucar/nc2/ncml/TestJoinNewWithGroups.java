package ucar.nc2.ncml;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * JoinNew has bug when groups are present because non-agg vars are not getting proxied.
 * The example unfortunately does not satisfy homogeneity = one is 44 x 60, the other 46 x 60.
 */
@Category(NeedsCdmUnitTest.class)
@Ignore("Fails")
public class TestJoinNewWithGroups {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  // test case from joleenf@ssec.wisc.edu 03/22/2012

  @Test
  public void testJoinNewWithGroups() throws IOException {
    String location = TestDir.cdmUnitTestDir + "agg/groups/groupsJoinNew.ncml";
    try (GridDataset ncd = GridDataset.open(location)) {
      GridDatatype v = ncd.findGridDatatype("All_Data/Lifted_Index"); // the only agg var
      assertThat(v).isNotNull();
      assertThat(v.getRank()).isEqualTo(3);
      Section s = new Section(v.getShape());
      // assert s.equals(new Section(new int[] {2, 44, 60})) : s ;

      v = ncd.findGridDatatype("All_Data/CAPE"); // random non-agg var
      assertThat(v).isNotNull();
      assertThat(v.getRank()).isEqualTo(2);
      Array a = v.readVolumeData(0);
      System.out.printf("array section for %s = %s%n", v, new Section(a.getShape()));
    }
  }
}
