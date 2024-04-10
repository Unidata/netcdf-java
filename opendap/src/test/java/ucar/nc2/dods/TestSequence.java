package ucar.nc2.dods;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import java.lang.invoke.MethodHandles;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.unidata.util.test.DapTestContainer;

public class TestSequence {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testSequence() {
    // The old url: "http://tsds.net/tsds/test/Scalar" is no longer valid.
    // So replaced with an equivalent.
    // Also had to replace the struct "TimeSeries" and the field "time"
    String url = "http://" + DapTestContainer.DTS_PATH + "/whoi";
    try {
      NetcdfDataset ds = NetcdfDatasets.openDataset(url);
      assertThat(ds).isNotNull();
      System.out.println(ds);
      Structure struct = (Structure) ds.findVariable("emolt_sensor");
      Variable var = struct.findVariable("TEMP");
      assertThat((Object) var).isNotNull();
      Array arr = var.read();
      int n = (int) arr.getSize();
      int i;
      for (i = 0; arr.hasNext() && i < n; i++) {
        assertThat(arr.nextDouble()).isNotNull();
      }
      assertThat(i).isEqualTo(n);
    } catch (Exception e) {
      Assert.fail("Exception thrown in testSequence: " + e.getMessage());
    }
  }
}
