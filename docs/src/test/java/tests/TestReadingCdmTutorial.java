package tests;

import static com.google.common.truth.Truth.assertThat;

import examples.ReadingCdmTutorial;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.write.Ncdump;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestReadingCdmTutorial {

  private static String scalarDataPathStr = TestDir.cdmTestDataDir + "thredds/public/testdata/testData.nc";
  private static String exampleDataPathStr = TestDir.cdmLocalFromTestDataDir +  "jan.nc";

  private static NetcdfFile scalarNcfile;
  private static NetcdfFile exampleNcfile;
  private static String var3DName = "T";
  private static String scalarVarName = "Nx";
  private static String stringVarName = "datetime";
  private static Variable var3d;
  private static Variable varScalar;
  private static Variable varString;

  @BeforeClass
  public static void setUpTests() throws Exception {
    exampleNcfile = NetcdfFiles.open(exampleDataPathStr);
    var3d = exampleNcfile.findVariable(var3DName);
    if (var3d == null) {
      throw new Exception(var3DName + " does not exist in file: " + exampleDataPathStr);
    }

    scalarNcfile = NetcdfFiles.open(scalarDataPathStr);
    varScalar = scalarNcfile.findVariable(scalarVarName);
    if (varScalar == null) {
      throw new Exception(scalarVarName + " does not exist in file: " + scalarNcfile);
    }
    varString = scalarNcfile.findVariable(stringVarName);
    if (varString == null) {
      throw new Exception(stringVarName + " does not exist in file: " + scalarNcfile);
    }
  }

  @AfterClass
  public static void cleanUp() throws IOException {
    scalarNcfile.close();
    exampleNcfile.close();
  }

  @Test
  public void testOpenNCFileTutorial() {
    // test open success
    ReadingCdmTutorial.logger.clearLog();
    ReadingCdmTutorial.openNCFile(exampleDataPathStr);
    assertThat(ReadingCdmTutorial.logger.getLogSize()).isEqualTo(0);

    // test open fail
    ReadingCdmTutorial.openNCFile("");
    assertThat(ReadingCdmTutorial.logger.getLastLogMsg()).isEqualTo(ReadingCdmTutorial.yourOpenNetCdfFileErrorMsgTxt);
  }

  @Test
  public void testToolsUIDataDumpTutorial() throws IOException {
    // inputs
    String failVarName = "INVALID";
    String successSectionSpec = "0:30:10,1,0:3";
    String failSectionSpec = ":, :, 0:4";

    // test invalid var name
    ReadingCdmTutorial.logger.clearLog();
    ReadingCdmTutorial.toolsUIDataDump(exampleNcfile, failVarName, successSectionSpec);
    assertThat(ReadingCdmTutorial.logger.getLogSize()).isEqualTo(0);

    // test read success
    ReadingCdmTutorial.toolsUIDataDump(exampleNcfile, var3DName, successSectionSpec);
    assertThat(ReadingCdmTutorial.logger.getLogSize()).isGreaterThan(0);
    String data = ReadingCdmTutorial.logger.getLastLogMsg();
    assertThat(data).isNotEmpty();
    assertThat(data).isNotEqualTo(ReadingCdmTutorial.yourReadVarErrorMsgTxt);

    // test read range exception
    ReadingCdmTutorial.toolsUIDataDump(exampleNcfile, var3DName, failSectionSpec);
    System.out.print(ReadingCdmTutorial.logger.getLastLogMsg());
    assertThat(ReadingCdmTutorial.logger.getLastLogMsg()).isEqualTo(ReadingCdmTutorial.yourReadVarErrorMsgTxt);
  }

  @Test
  public void testReadAllVarDataTutorial() throws IOException {
    Array data = ReadingCdmTutorial.readAllVarData(var3d);
    assertThat(data).isNotNull();
    assertThat(data.getDataType()).isEqualTo(DataType.DOUBLE);
  }

  @Test
  public void testReadByOriginAndSizeTutorial() throws IOException, InvalidRangeException {
    Array data = ReadingCdmTutorial.readByOriginAndSize(var3d);
    assertThat(data).isNotNull();
    assertThat(data.getDataType()).isEqualTo(DataType.DOUBLE);
    assertThat(data.getRank()).isEqualTo(2);
  }

  @Test
  public void testReadInLoopTutorial() throws IOException, InvalidRangeException {
    ReadingCdmTutorial.logger.clearLog();
    ReadingCdmTutorial.readInLoop(var3d);
    assertThat(ReadingCdmTutorial.logger.getLogSize()).isEqualTo(var3d.getShape()[0]);
  }

  @Test
  public void  testReadSubset() throws IOException, InvalidRangeException {
    Array data = ReadingCdmTutorial.readSubset(var3d);
    assertThat(data).isNotNull();
    assertThat(data.getDataType()).isEqualTo(DataType.DOUBLE);
    assertThat(data.getRank()).isEqualTo(var3d.getRank());
    assertThat(data.getShape()).isNotEqualTo(var3d.getShape());
  }

  @Test
  public void testReadByStrideAndRange() throws IOException, InvalidRangeException {
    // test read with String range
    Array data1 = ReadingCdmTutorial.readByStride(var3d);
    assertThat(data1).isNotNull();
    assertThat(data1.getDataType()).isEqualTo(DataType.DOUBLE);
    assertThat(data1.getRank()).isEqualTo(var3d.getRank());

    // test read with Range object
    Array data2 = ReadingCdmTutorial.readByRange(var3d);
    assertThat(data2).isNotNull();
    assertThat(data2.getDataType()).isEqualTo(DataType.DOUBLE);
    assertThat(data2.getRank()).isEqualTo(var3d.getRank());

    // convert to strings to check equality
    String dataStr1 = Ncdump.printArray(data1, "data", null);
    String dataStr2 = Ncdump.printArray(data2, "data", null);
    assertThat(dataStr1).isEqualTo(dataStr2);
  }

  @Test
  public void testReadInLoopRangesTutorial() throws IOException, InvalidRangeException {
    ReadingCdmTutorial.logger.clearLog();
    ReadingCdmTutorial.readInLoopRanges(var3d);
    assertThat(ReadingCdmTutorial.logger.getLogSize()).isEqualTo(var3d.getShape()[0]);
  }

  @Test
  public void testSectionsTutorial() throws IOException, InvalidRangeException {
    int[] varShape = var3d.getShape();
    List ranges = new ArrayList();
    ranges.add(new Range(0, 0));
    ranges.add(new Range(0, varShape[1]-1, 2));
    ranges.add(new Range(0, varShape[2]-1, 2));

    List<int[]> args = ReadingCdmTutorial.convertRangesToSection(var3d, ranges);
    int ndims = var3d.getRank();
    assertThat(args.get(0)).hasLength(ndims);
    assertThat(args.get(1)).hasLength(ndims);
  }

  @Test
  public void testReadScalarTutorial() throws IOException {
    // test double, float, and int scalars
    List data = ReadingCdmTutorial.readScalars(varScalar);
    assertThat(data.get(0)).isInstanceOf(Double.class);
    assertThat(data.get(1)).isInstanceOf(Float.class);
    assertThat(data.get(2)).isInstanceOf(Integer.class);

    // test string scalar
    String sval = ReadingCdmTutorial.readStringScalar(varString);
    assertThat(sval).isNotEmpty();
    assertThat(sval).isInstanceOf(String.class);
  }

  @Test
  public void testIterateKnownRankTutorial() throws IOException {
    List list = ReadingCdmTutorial.iterateForLoop(var3d);
    assertThat(list.size()).isEqualTo(var3d.getSize());
  }

  @Test
  public void testIndexIteratorTutorial() throws IOException, InvalidRangeException {
    double sum = ReadingCdmTutorial.indexIterator(var3d);
    assertThat(sum).isNotNaN();
  }

  @Test
  public void testRangeIteratorTutorial() throws IOException, InvalidRangeException {
    double sum = ReadingCdmTutorial.rangeIterator(var3d);
    assertThat(sum).isNotNaN();
  }

  @Test
  public void testCastDataArrayTutorial() throws IOException {
    List list = ReadingCdmTutorial.castDataArray(var3d);
    assertThat(list.size()).isEqualTo(var3d.getSize());
  }

  @Test
  public void testIndexManipulationTutorial() throws IOException, InvalidRangeException {
    // just test it runs without errors or deprecation warnings
    Array data = var3d.read();
    int[] origin = new int[]{0, 0, 0};
    int[] shape = new int[]{31, 1, 1};
    int[] stride = new int[]{1, 1, 1};
    ReadingCdmTutorial.indexManipulation(data, origin, shape, stride);
  }

  @Test
  public void testGetFlatArrayTutorial() throws IOException {
    Array data = var3d.read();
    double[] data1D = ReadingCdmTutorial.get1DArray(data);
    assertThat(data1D).hasLength((int)data.getSize());
  }

  @Test
  public void testScourCacheTutorial() {
    // just test it runs without errors or deprecation warnings
    ReadingCdmTutorial.scourCache();
  }
}
