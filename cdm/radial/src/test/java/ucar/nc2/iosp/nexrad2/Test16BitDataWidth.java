package ucar.nc2.iosp.nexrad2;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.MAMath;
import ucar.ma2.MAMath.MinMax;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

@Category(NeedsCdmUnitTest.class)
public class Test16BitDataWidth {

  private static final String filename =
      TestDir.cdmUnitTestDir + "formats/nexrad/newLevel2/testfiles/Level2_KDDC_20201007_1914.ar2v";

  @Test
  public void testNonPhiVar() throws IOException, InvalidRangeException {
    try (NetcdfFile ncf = NetcdfFiles.open(filename)) {
      // verified against metpy
      int expectedMin = 0;
      int expectedMax = 1058;
      Variable var = ncf.findVariable("DifferentialReflectivity_HI");
      Array data = var.read("0,:,:");
      MinMax minMax = MAMath.getMinMax(data);
      assertThat(minMax.min).isWithin(1e-6).of(expectedMin);
      assertThat(minMax.max).isWithin(1e-6).of(expectedMax);
    }
  }

  @Test
  public void testNonPhiVarEnhanced() throws IOException, InvalidRangeException {
    try (NetcdfDataset ncf = NetcdfDatasets.openDataset(filename)) {
      // verified against metpy
      int expectedMin = -13;
      int expectedMax = 20;
      Variable var = ncf.findVariable("DifferentialReflectivity_HI");
      Array data = var.read("0,:,:");
      MinMax minMax = MAMath.getMinMax(data);
      assertThat(minMax.min).isWithin(1e-6).of(expectedMin);
      assertThat(minMax.max).isWithin(1e-6).of(expectedMax);
    }
  }

}
