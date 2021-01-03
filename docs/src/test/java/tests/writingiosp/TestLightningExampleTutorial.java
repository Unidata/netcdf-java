package tests.writingiosp;

import examples.writingiosp.LightningExampleTutorial;
import org.junit.BeforeClass;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayInt;
import ucar.ma2.DataType;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.text.ParseException;

import static com.google.common.truth.Truth.assertThat;

public class TestLightningExampleTutorial {

  private final static String testFilePath =
      "src/site/files/netcdfJava_tutorial/writingiosp/lightningData.txt";

  private static RandomAccessFile raf;

  private static int nRecords = 190;

  @BeforeClass
  public static void setUpTest() throws IOException {
    raf = RandomAccessFile.acquire(testFilePath);
  }

  @Test
  public void testImplementIospTutorial() {
    assertThat(LightningExampleTutorial.getIOSP()).isNotNull();
  }

  @Test
  public void testImplementIsValidFileTutorial() throws IOException {
    assertThat(LightningExampleTutorial.implementIsValidFile(raf)).isTrue();
  }

  @Test
  public void testReadAllDataTutorial() throws IOException, NumberFormatException, ParseException {
    assertThat(LightningExampleTutorial.readALlData(raf)).isEqualTo(nRecords);
  }

  @Test
  public void testImplementBuildTutorial() throws IOException {
    Group.Builder builder = Group.builder();
    LightningExampleTutorial.implementBuild(raf, builder, null);
    Group group = builder.build();

    assertThat(group.findDimension("record")).isNotNull();
    assertThat((Iterable<?>) group.findVariableLocal("date")).isNotNull();
    assertThat(group.hasAttribute("title")).isTrue();
  }

  @Test
  public void testDataStructures() {
    // test Array types not deprecated
    LightningExampleTutorial.createDataArrays();
  }

  @Test
  public void testImplementReadMethodsTutorial()
      throws IOException, NumberFormatException, ParseException {
    assertThat(LightningExampleTutorial.implementReadMethods(raf)).isEqualTo(nRecords);
    assertThat(LightningExampleTutorial.dateArray.getSize()).isEqualTo(nRecords);
    assertThat(LightningExampleTutorial.latArray.getSize()).isEqualTo(nRecords);
    assertThat(LightningExampleTutorial.lonArray.getSize()).isEqualTo(nRecords);
    assertThat(LightningExampleTutorial.ampArray.getSize()).isEqualTo(nRecords);
    assertThat(LightningExampleTutorial.nstrokesArray.getSize()).isEqualTo(nRecords);
  }

  @Test
  public void testSetCachedDataTutorial() {
    Group.Builder builder = Group.builder();
    Dimension dim = Dimension.builder("record", nRecords).build();
    builder.addDimension(dim);
    ArrayInt.D1 dateArray = (ArrayInt.D1) Array.factory(DataType.INT, new int[] {nRecords});
    ArrayDouble.D1 latArray = (ArrayDouble.D1) Array.factory(DataType.DOUBLE, new int[] {nRecords});
    LightningExampleTutorial.setCachedData(builder, dim, dateArray, latArray);
    Group group = builder.build();

    Variable var1 = group.findVariableLocal("date");
    Variable var2 = group.findVariableLocal("lat");
    assertThat(var1.hasCachedData()).isTrue();
    assertThat(var2.hasCachedData()).isTrue();
  }

  @Test
  public void testAddCoordSystemsTutorial() {
    Group.Builder builder = Group.builder();
    Dimension dim = Dimension.builder("record", nRecords).build();
    builder.addDimension(dim);
    ArrayInt.D1 dateArray = (ArrayInt.D1) Array.factory(DataType.INT, new int[] {nRecords});
    ArrayDouble.D1 latArray = (ArrayDouble.D1) Array.factory(DataType.DOUBLE, new int[] {nRecords});
    ArrayDouble.D1 lonArray = (ArrayDouble.D1) Array.factory(DataType.DOUBLE, new int[] {nRecords});
    LightningExampleTutorial.addCoordSystemsAndTypedDatasets(builder, dim, dateArray, latArray,
        lonArray);
  }

  @Test(expected = ClassNotFoundException.class)
  public void testRegisterIospTutorial()
      throws InstantiationException, IllegalAccessException, ClassNotFoundException {
    LightningExampleTutorial.registerIOSP();
  }
}
