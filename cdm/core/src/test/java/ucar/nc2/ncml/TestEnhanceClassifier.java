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

public class TestEnhanceClassifier {

  private static String dataDir = TestDir.cdmLocalTestDataDir + "ncml/enhance/";

  public static final int[] all_ones = {1, 1, 1, 1, 1};
  public static final Array DATA_all_ones = Array.makeFromJavaArray(all_ones);
  public static final int[] all_zeroes = {0, 0, 0, 0, 0};
  public static final Array DATA_all_zeroes = Array.makeFromJavaArray(all_zeroes);
  public static final int[] mixNumbers = {1, 0, 1, 1, 0};
  public static final Array DATA_mixNumbers = Array.makeFromJavaArray(mixNumbers);


  /** test on doubles, all positives, all negatives and a mixed array */
  @Test
  public void testEnhanceClassifier_doubles() throws IOException {
    try (NetcdfFile ncfile = NetcdfDatasets.openDataset(dataDir + "testClassifier.ncml", true, null)) {
      Variable doublePositives = ncfile.findVariable("doublePositives");
      assertThat((Object) doublePositives).isNotNull();
      assertThat(doublePositives.getDataType()).isEqualTo(DataType.DOUBLE);
      assertThat(doublePositives.attributes().hasAttribute("classify")).isTrue();
      Array dataDoubles = doublePositives.read();
      assertThat(nearlyEquals(dataDoubles, DATA_all_ones)).isTrue();

      Variable doubleNegatives = ncfile.findVariable("doubleNegatives");
      assertThat((Object) doubleNegatives).isNotNull();
      assertThat(doubleNegatives.getDataType()).isEqualTo(DataType.DOUBLE);
      assertThat(doubleNegatives.attributes().hasAttribute("classify")).isTrue();
      Array datadoubleNegatives = doubleNegatives.read();
      assertThat(nearlyEquals(datadoubleNegatives, DATA_all_zeroes)).isTrue();

      Variable doubleMix = ncfile.findVariable("doubleMix");
      assertThat((Object) doubleMix).isNotNull();
      assertThat(doubleMix.getDataType()).isEqualTo(DataType.DOUBLE);
      assertThat(doubleMix.attributes().hasAttribute("classify")).isTrue();
      Array datadoubleMix = doubleMix.read();
      assertThat(nearlyEquals(datadoubleMix, DATA_mixNumbers)).isTrue();

    }


  }

  /** test on floats, all positives, all negatives and a mixed array */
  @Test
  public void testEnhanceClassifier_floats() throws IOException {
    try (NetcdfFile ncfile = NetcdfDatasets.openDataset(dataDir + "testClassifier.ncml", true, null)) {

      Variable floatPositives = ncfile.findVariable("floatPositives");
      assertThat((Object) floatPositives).isNotNull();
      assertThat(floatPositives.getDataType()).isEqualTo(DataType.FLOAT);
      assertThat(floatPositives.attributes().hasAttribute("classify")).isTrue();
      Array datafloats = floatPositives.read();
      assertThat(nearlyEquals(datafloats, DATA_all_ones)).isTrue();

      Variable floatNegatives = ncfile.findVariable("floatNegatives");
      assertThat((Object) floatNegatives).isNotNull();
      assertThat(floatNegatives.getDataType()).isEqualTo(DataType.FLOAT);
      assertThat(floatNegatives.attributes().hasAttribute("classify")).isTrue();
      Array datafloatNegatives = floatNegatives.read();
      assertThat(nearlyEquals(datafloatNegatives, DATA_all_zeroes)).isTrue();

      Variable floatMix = ncfile.findVariable("floatMix");
      assertThat((Object) floatMix).isNotNull();
      assertThat(floatMix.getDataType()).isEqualTo(DataType.FLOAT);
      assertThat(floatMix.attributes().hasAttribute("classify")).isTrue();
      Array datafloatsMix = floatMix.read();
      assertThat(nearlyEquals(datafloatsMix, DATA_mixNumbers)).isTrue();

    }

  }

  /** enhance is not applied to Integers, so we expect the same values after application */
  @Test
  public void testEnhanceClassifier_integers() throws IOException {

    try (NetcdfFile ncfile = NetcdfDatasets.openDataset(dataDir + "testClassifier.ncml", true, null)) {
      Variable IntegerPositives = ncfile.findVariable("intPositives");
      assertThat((Object) IntegerPositives).isNotNull();
      assertThat(IntegerPositives.getDataType()).isEqualTo(DataType.INT);
      assertThat(IntegerPositives.attributes().hasAttribute("classify")).isTrue();
      Array dataIntegers = IntegerPositives.read();
      assertThat(nearlyEquals(dataIntegers, DATA_all_ones)).isTrue();

      Variable intNegatives = ncfile.findVariable("intNegatives");
      assertThat((Object) intNegatives).isNotNull();
      assertThat(intNegatives.getDataType()).isEqualTo(DataType.INT);
      assertThat(intNegatives.attributes().hasAttribute("classify")).isTrue();
      Array dataintNegatives = intNegatives.read();
      assertThat(nearlyEquals(dataintNegatives, DATA_all_zeroes)).isTrue();

      Variable intMix = ncfile.findVariable("intMix");
      assertThat((Object) intMix).isNotNull();
      assertThat(intMix.getDataType()).isEqualTo(DataType.INT);
      assertThat(intMix.attributes().hasAttribute("classify")).isTrue();
      Array dataintMix = intMix.read();
      assertThat(nearlyEquals(dataintMix, DATA_mixNumbers)).isTrue();
    }

  }
}
