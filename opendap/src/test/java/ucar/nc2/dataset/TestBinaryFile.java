package ucar.nc2.dataset;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.unidata.util.test.TestDir;

public class TestBinaryFile {

  private static final String filename = TestDir.localTestDataDir + "cdm_read/GFS_Global_0p5deg_20201006_0600.grib2";

  @Test
  public void testFileOpenOldApi() throws IOException {
    try (NetcdfFile ncf = NetcdfDataset.openFile("file:" + filename + ".dods", null)) {
      assertThat(ncf).isNotNull();
    }
  }

  @Test
  public void testFileOpen() throws IOException {
    try (NetcdfFile ncf = NetcdfDatasets.openFile("file:" + filename + ".dods", null)) {
      assertThat(ncf).isNotNull();
    }
  }

  @Test
  public void testFileOpenDds() throws IOException {
    try (NetcdfFile ncf = NetcdfDatasets.openFile("file:" + filename + ".dds", null)) {
      assertThat(ncf).isNotNull();
    }
  }

  @Test
  public void testFileOpenDas() throws IOException {
    try (NetcdfFile ncf = NetcdfDatasets.openFile("file:" + filename + ".das", null)) {
      assertThat(ncf).isNotNull();
    }
  }

  @Test
  public void testDatasetOpenOldApi() throws IOException {
    try (NetcdfDataset ncd = NetcdfDataset.openDataset("file:" + filename + ".dods")) {
      assertThat(ncd).isNotNull();
    }
  }

  @Test
  public void testDatasetOpen() throws IOException {
    try (NetcdfDataset ncd = NetcdfDatasets.openDataset("file:" + filename + ".dods")) {
      assertThat(ncd).isNotNull();
    }
  }

  @Test
  public void testGlobalMetadata() throws IOException {
    try (NetcdfFile ncf = NetcdfDatasets.openFile("file:" + filename + ".dods", null)) {
      assertThat(ncf.findGlobalAttributeIgnoreCase("conventions")).isNotNull();
    }
  }

  @Test
  public void testVar() throws IOException {
    try (NetcdfFile ncf = NetcdfDatasets.openFile("file:" + filename + ".dods", null)) {
      assert ncf.findVariable("Temperature_surface") != null;
    }
  }

  @Test
  public void testVarMetadata() throws IOException {
    try (NetcdfFile ncf = NetcdfDatasets.openFile("file:" + filename + ".dods", null)) {
      Variable temperature = ncf.findVariable("Temperature_surface");
      assertThat(temperature.findAttribute("units")).isNotNull();
    }
  }

  @Test
  public void testVarReadFull() throws IOException {
    try (NetcdfFile ncf = NetcdfDatasets.openFile("file:" + filename + ".dods", null)) {
      Variable temperature = ncf.findVariable("Temperature_surface");
      Array temperatureData = temperature.read();
      assertThat(temperatureData.getShape()).isEqualTo(new int[] {3, 8, 15});
    }
  }

  @Test
  public void testVarReadSection() throws IOException, InvalidRangeException {
    try (NetcdfFile ncf = NetcdfDatasets.openFile("file:" + filename + ".dods", null)) {
      Variable temperature = ncf.findVariable("Temperature_surface");
      Array temperatureData = temperature.read("0,0,0:4");
      assertThat(temperatureData.getShape()).isEqualTo(new int[] {1, 1, 5});
    }
  }
}
