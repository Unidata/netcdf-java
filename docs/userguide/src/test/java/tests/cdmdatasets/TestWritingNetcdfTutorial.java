package tests.cdmdatasets;

import static com.google.common.truth.Truth.assertThat;

import examples.cdmdatasets.WritingNetcdfTutorial;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import ucar.array.*;
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.iosp.NetcdfFileFormat;
import ucar.nc2.internal.util.CompareNetcdf2;
import ucar.nc2.write.Nc4Chunking;
import ucar.nc2.write.NcdumpArray;
import ucar.nc2.write.NetcdfFormatWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
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

    Variable.Builder t = builder.addVariable("temperature", ArrayType.DOUBLE, dims);
    t.addAttribute(new Attribute("units", "K"));
    Array data = Arrays.factory(ArrayType.INT, new int[] {3}, new int[] {1, 2, 3});
    t.addAttribute(Attribute.builder("scale").setArrayValues(data).build());

    Dimension svar_len = builder.addDimension("svar_len", 80);
    builder.addVariable("svar", ArrayType.CHAR, "svar_len");

    Dimension names = builder.addDimension("names", 3);
    builder.addVariable("names", ArrayType.CHAR, "names svar_len");

    builder.addVariable("scalar", ArrayType.DOUBLE, new ArrayList<Dimension>());

    testVarName = "test";
    builder.addVariable(testVarName, ArrayType.INT, new ArrayList<Dimension>());
    writer = builder.build();
  }

  @Test
  public void testCreateNCFileTutorial() throws IOException {
    String newFilePath = tempFolder.newFile().getAbsolutePath();
    WritingNetcdfTutorial.logger.clearLog();
    NetcdfFormatWriter w = WritingNetcdfTutorial.createNCFile(newFilePath);

    assertThat(w).isNotNull();
    assertThat(w.findDimension("lat")).isNotNull();
    assertThat(w.findVariable("temperature")).isNotNull();
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
    assertThat(v.getArrayType()).isEqualTo(ArrayType.DOUBLE);
    Array data = v.readArray();
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
    assertThat(v.getArrayType()).isEqualTo(ArrayType.CHAR);
    Array data = v.readArray();
    String dataString = NcdumpArray.printArray(data, null, null).trim();
    assertThat(dataString.substring(1, dataString.length()-1)).isEqualTo(WritingNetcdfTutorial.someStringValue);
    ncfile.close();

    assertThat(WritingNetcdfTutorial.logger.getLogSize()).isEqualTo(0);
  }

  @Test
  public void testWriteWithCompressionTutorial() throws IOException {
    WritingNetcdfTutorial.logger.clearLog();

    String datasetIn = existingFilePath;
    NetcdfFile ncIn = NetcdfDatasets.openFile(datasetIn, null);

    String datasetOut = tempFolder.newFile().getAbsolutePath();
    WritingNetcdfTutorial.writeWithCompression(ncIn, datasetOut, Nc4Chunking.Strategy.standard, 0,
        false, NetcdfFileFormat.NETCDF4);

    NetcdfFile ncOut = NetcdfDatasets.openFile(datasetOut, null);

    assertThat(ncOut).isNotNull();
    CompareNetcdf2 tc = new CompareNetcdf2(new Formatter(), false, false, true);
    assertThat(tc.compare(ncIn, ncOut, new CompareNetcdf2.Netcdf4ObjectFilter())).isTrue();

    assertThat(WritingNetcdfTutorial.logger.getLogSize()).isEqualTo(0);
  }
}
