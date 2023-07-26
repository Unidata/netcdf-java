package ucar.nc2.ncml;

import static com.google.common.truth.Truth.assertThat;
import static ucar.ma2.MAMath.nearlyEquals;

import java.io.IOException;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.unidata.util.test.TestDir;

public class TestEnhanceNormalizer {

  private static String dataDir = TestDir.cdmLocalTestDataDir + "ncml/enhance/";
  public static final double[] DOUBLES = {0.0, 0.25, 0.50, 0.75, 1.0};
  public static final Array DATA_DOUBLES = Array.makeFromJavaArray(DOUBLES);
  public static final double[] SAMEDOUBLES = {5.0, 5.0, 5.0, 5.0, 5.0};
  public static final Array DATA_SAMEDOUBLES = Array.makeFromJavaArray(SAMEDOUBLES);
  public static final float[] FLOATS = {0.0F, 0.25F, 0.50F, 0.75F, 1.0F};
  public static final Array DATA_FLOATS = Array.makeFromJavaArray(FLOATS);
  public static final int[] INTS = {1, 2, 3, 4, 5};
  public static final Array DATA_INTS = Array.makeFromJavaArray(INTS);

  @Test
  public void testEnhanceNormalizer() throws IOException {
    try (NetcdfFile ncfile = NetcdfDatasets.openDataset(dataDir + "testNormalizer.ncml", true, null)) {
      Variable doubleVar = ncfile.findVariable("doublevar");
      assertThat((Object) doubleVar).isNotNull();
      assertThat(doubleVar.getDataType()).isEqualTo(DataType.DOUBLE);
      assertThat(doubleVar.attributes().hasAttribute("normalize")).isTrue();
      Array dataDoubles = doubleVar.read();
      assertThat(nearlyEquals(dataDoubles, DATA_DOUBLES)).isTrue();

      Variable sameDoubleVar = ncfile.findVariable("samedoublevar");
      assertThat((Object) sameDoubleVar).isNotNull();
      assertThat(sameDoubleVar.getDataType()).isEqualTo(DataType.DOUBLE);
      assertThat(sameDoubleVar.attributes().hasAttribute("normalize")).isTrue();
      Array dataSameDoubles = sameDoubleVar.read();
      assertThat(nearlyEquals(dataSameDoubles, DATA_SAMEDOUBLES)).isTrue(); // The enhancement doesn't apply if all the
                                                                            // values are the equal, so it returns the
                                                                            // same data

      Variable floatVar = ncfile.findVariable("floatvar");
      assertThat((Object) floatVar).isNotNull();
      assertThat(floatVar.getDataType()).isEqualTo(DataType.FLOAT);
      assertThat(doubleVar.attributes().hasAttribute("normalize")).isTrue();
      Array dataFloats = doubleVar.read();
      assertThat(nearlyEquals(dataFloats, DATA_FLOATS)).isTrue();

      Variable intVar = ncfile.findVariable("intvar");
      assertThat((Object) intVar).isNotNull();
      assertThat(intVar.getDataType()).isEqualTo(DataType.INT);
      assertThat(intVar.attributes().hasAttribute("normalize")).isTrue();
      Array data = intVar.read();
      assertThat(nearlyEquals(data, DATA_INTS)).isTrue(); // The enhancement doesn't apply to ints, so the data should
      // be equal to the input array
    }
  }

}
