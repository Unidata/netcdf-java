package tests.cdmdatasets;

import static com.google.common.truth.Truth.assertThat;

import examples.cdmdatasets.WritingNetcdfTutorial;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import ucar.ma2.Array;
import ucar.ma2.ArrayDouble;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.util.CompareNetcdf2;
import ucar.nc2.write.Nc4Chunking;
import ucar.nc2.write.NetcdfFileFormat;
import ucar.nc2.write.NetcdfFormatWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestWritingNetcdfTutorial {

  @ClassRule
  public static final TemporaryFolder tempFolder = new TemporaryFolder();

  public static NetcdfFormatWriter.Builder builder;
  public static NetcdfFormatWriter writer;
  public static String existingFilePath;
  public static String testVarName;

  @Before
  public void setUpTests() throws IOException {
    existingFilePath = tempFolder.newFile().getAbsolutePath();
    builder = NetcdfFormatWriter.createNewNetcdf3(existingFilePath);

    // 2) Create two Dimensions, named lat and lon, of lengths 64 and 129 respectively, and add them to the root group
    Dimension latDim = builder.addDimension("lat", 64);
    Dimension lonDim = builder.addDimension("lon", 128);

    List<Dimension> dims = new ArrayList<Dimension>();
    dims.add(latDim);
    dims.add(lonDim);

    Variable.Builder t = builder.addVariable("temperature", DataType.DOUBLE, dims);
    t.addAttribute(new Attribute("units", "K"));
    Array data = Array.factory(DataType.INT, new int[] {3}, new int[] {1, 2, 3});
    t.addAttribute(Attribute.builder("scale").setValues(data).build());

    Dimension svar_len = builder.addDimension("svar_len", 80);
    builder.addVariable("svar", DataType.CHAR, "svar_len");

    Dimension names = builder.addDimension("names", 3);
    builder.addVariable("names", DataType.CHAR, "names svar_len");

    builder.addVariable("scalar", DataType.DOUBLE, new ArrayList<Dimension>());

    testVarName = "test";
    builder.addVariable(testVarName, DataType.INT, new ArrayList<Dimension>());
    writer = builder.build();
  }

  @Test
  public void testCreateNCFileTutorial() throws IOException {
    String newFilePath = tempFolder.newFile().getAbsolutePath();
    WritingNetcdfTutorial.logger.clearLog();
    NetcdfFormatWriter w = WritingNetcdfTutorial.createNCFile(newFilePath);

    assertThat(w).isNotNull();
    assertThat(w.findDimension("lat")).isNotNull();
    assertThat((Iterable<?>) w.findVariable("temperature")).isNotNull();
    assertThat(w.findGlobalAttribute("versionStr")).isNotNull();
    assertThat(WritingNetcdfTutorial.logger.getLogSize()).isEqualTo(0);
  }

  @Test
  public void testSetFillOptionTutorial() {
    // just test for errors and deprecation warnings
    WritingNetcdfTutorial.setFillOption(builder);
  }

  @Test
  public void testOpenNCFileForWriteTutorial() throws IOException, InvalidRangeException {
    // open writer to existing file
    NetcdfFormatWriter returnedWriter = WritingNetcdfTutorial.openNCFileForWrite(existingFilePath);
    assertThat(returnedWriter).isNotNull();

    // write with writer
    double expectedValue = 0.0;
    ArrayDouble.D0 writeData = new ArrayDouble.D0();
    writeData.set(expectedValue);
    returnedWriter.write(testVarName, writeData);
    returnedWriter.close();

    // verify data is in file
    NetcdfFile ncfile = NetcdfFiles.open(existingFilePath);
    Variable v = ncfile.findVariable(testVarName);
    Array data = v.read();
    assertThat(data.getDouble(0)).isEqualTo(expectedValue);
    ncfile.close();
  }

  @Test
  public void testWriteDoubleDataTutorial() throws IOException {
    WritingNetcdfTutorial.logger.clearLog();
    // write data
    String varName = "temperature";
    WritingNetcdfTutorial.writeDoubleData(writer, varName);
    writer.close();

    // verify data
    NetcdfFile ncfile = NetcdfFiles.open(existingFilePath);
    Variable v = ncfile.findVariable(varName);
    assertThat(v.getDataType()).isEqualTo(DataType.DOUBLE);
    Array data = v.read();
    assertThat(data).isNotNull();
    assertThat(data.getShape()).isEqualTo(v.getShape());
    ncfile.close();

    assertThat(WritingNetcdfTutorial.logger.getLogSize()).isEqualTo(0);
  }

  @Test
  public void testWriteCharDataTutorial() throws IOException {
    WritingNetcdfTutorial.logger.clearLog();
    // write data
    String varName = "svar";
    WritingNetcdfTutorial.writeCharData(writer, varName);
    writer.close();

    // verify data
    NetcdfFile ncfile = NetcdfFiles.open(existingFilePath);
    Variable v = ncfile.findVariable(varName);
    assertThat(v.getDataType()).isEqualTo(DataType.CHAR);
    Array data = v.read();
    assertThat(data.toString()).isEqualTo(WritingNetcdfTutorial.someStringValue);
    ncfile.close();

    assertThat(WritingNetcdfTutorial.logger.getLogSize()).isEqualTo(0);
  }

  @Test
  public void testWriteStringArrayTutorial() throws IOException, InvalidRangeException {
    WritingNetcdfTutorial.logger.clearLog();
    // write data
    String varName = "names";
    WritingNetcdfTutorial.writeStringArray(writer, varName);
    writer.close();

    // verify data
    NetcdfFile ncfile = NetcdfFiles.open(existingFilePath);
    Variable v = ncfile.findVariable(varName);
    assertThat(v.getDataType()).isEqualTo(DataType.CHAR);
    Array data1 = v.read("0,:");
    Array data2 = v.read("1,:");
    Array data3 = v.read("2,:");
    assertThat(data1.toString()).isEqualTo(WritingNetcdfTutorial.someStringValue);
    assertThat(data2.toString()).isEqualTo(WritingNetcdfTutorial.anotherStringValue);
    assertThat(data3.toString()).isEqualTo(WritingNetcdfTutorial.aThirdStringValue);
    ncfile.close();

    assertThat(WritingNetcdfTutorial.logger.getLogSize()).isEqualTo(0);
  }

  @Test
  public void testWriteScalarTutorial() throws IOException {
    WritingNetcdfTutorial.logger.clearLog();
    // write data
    String varName = "scalar";
    double val = 222.333;

    WritingNetcdfTutorial.writeScalarData(writer, varName, val);
    writer.close();

    // verify data
    NetcdfFile ncfile = NetcdfFiles.open(existingFilePath);
    Variable v = ncfile.findVariable(varName);
    assertThat(v.getDataType()).isEqualTo(DataType.DOUBLE);
    Array data = v.read();
    assertThat(data.getDouble(0)).isEqualTo(val);
    ncfile.close();

    assertThat(WritingNetcdfTutorial.logger.getLogSize()).isEqualTo(0);
  }

  @Test
  public void testWriteRecordTutorial() throws IOException, InvalidRangeException {
    WritingNetcdfTutorial.logger.clearLog();

    String newFilePath = tempFolder.newFile().getAbsolutePath();
    NetcdfFormatWriter returnedWriter = WritingNetcdfTutorial.writeRecordOneAtATime(newFilePath);
    assertThat(returnedWriter).isNotNull();
    returnedWriter.close();

    NetcdfFile ncfile = NetcdfFiles.open(newFilePath);
    String[] varNames = new String[] {"time", "rh", "T"};
    int numRecords = 10;
    for (String name : varNames) {
      Variable v = ncfile.findVariable(name);
      assertThat((Iterable<?>) v).isNotNull();
      assertThat(v.getShape(0)).isEqualTo(numRecords);
    }

    assertThat(WritingNetcdfTutorial.logger.getLogSize()).isEqualTo(0);
  }

  @Test
  public void testWriteWithCompressionTutorial() throws IOException {
    WritingNetcdfTutorial.logger.clearLog();

    String datasetIn = existingFilePath;
    NetcdfFile ncIn = NetcdfDatasets.openFile(datasetIn, null);

    String datasetOut = tempFolder.newFile().getAbsolutePath();
    NetcdfFile ncOut = WritingNetcdfTutorial.writeWithCompression(ncIn, datasetOut,
        Nc4Chunking.Strategy.standard, 0, false, NetcdfFileFormat.NETCDF4);

    assertThat(ncOut).isNotNull();
    assertThat(new CompareNetcdf2().compare(ncIn, ncOut)).isTrue();

    assertThat(WritingNetcdfTutorial.logger.getLogSize()).isEqualTo(0);
  }
}
