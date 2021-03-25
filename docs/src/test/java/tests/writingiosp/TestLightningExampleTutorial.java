package tests.writingiosp;

import examples.writingiosp.LightningExampleTutorial;
import org.junit.BeforeClass;
import org.junit.Test;
import ucar.array.*;
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
    assertThat(group.findVariableLocal("date")).isNotNull();
    assertThat(group.findAttribute("title")).isNotNull();
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
    assertThat(Arrays.computeSize(LightningExampleTutorial.dateArray.getShape())).isEqualTo(nRecords);
    assertThat(Arrays.computeSize(LightningExampleTutorial.latArray.getShape())).isEqualTo(nRecords);
    assertThat(Arrays.computeSize(LightningExampleTutorial.lonArray.getShape())).isEqualTo(nRecords);
    assertThat(Arrays.computeSize(LightningExampleTutorial.ampArray.getShape())).isEqualTo(nRecords);
    assertThat(Arrays.computeSize(LightningExampleTutorial.nstrokesArray.getShape())).isEqualTo(nRecords);
  }

  @Test
  public void testSetSourceDataTutorial() {
    Group.Builder builder = Group.builder();
    Dimension dim = Dimension.builder("record", nRecords).build();
    builder.addDimension(dim);

    Array<Integer> dateArray = Arrays.factory(ArrayType.INT, new int[] {nRecords});
    Array<Double> latArray = Arrays.factory(ArrayType.DOUBLE, new int[] {nRecords});
    LightningExampleTutorial.setSourceData(builder, dim, (ArrayInteger)dateArray, (ArrayDouble)latArray);
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
    Array<Integer> dateArray = Arrays.factory(ArrayType.INT, new int[] {nRecords});
    Array<Double> latArray = Arrays.factory(ArrayType.DOUBLE, new int[] {nRecords});
    Array<Double> lonArray = Arrays.factory(ArrayType.DOUBLE, new int[] {nRecords});
    LightningExampleTutorial.addCoordSystemsAndTypedDatasets(builder, dim, (ArrayInteger)dateArray, (ArrayDouble)latArray,
            (ArrayDouble)lonArray);
  }

  @Test(expected = ClassNotFoundException.class)
  public void testRegisterIospTutorial()
      throws InstantiationException, IllegalAccessException, ClassNotFoundException {
    LightningExampleTutorial.registerIOSP();
  }
}
