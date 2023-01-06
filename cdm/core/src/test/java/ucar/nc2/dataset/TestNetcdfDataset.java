package ucar.nc2.dataset;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.io.StringReader;
import org.junit.Test;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.unidata.util.test.TestDir;

public class TestNetcdfDataset {
  private static final String TEST_FILE = TestDir.cdmLocalTestDataDir + "ncml/nc/jan.nc";

  @Test
  public void shouldGetLastModified() throws IOException {
    try (NetcdfDataset netcdfDataset = NetcdfDatasets.openDataset(TEST_FILE)) {
      assertThat(netcdfDataset.getLastModified()).isGreaterThan(0);
    }
  }

  @Test
  public void shouldContainVariableDsWhenOpeningDataset() throws IOException {
    try (NetcdfDataset netcdfDataset = NetcdfDatasets.openDataset(TEST_FILE)) {
      final Variable datasetVariable = netcdfDataset.findVariable("lat");
      assertThat((Object) datasetVariable).isInstanceOf(VariableDS.class);
    }
  }

  @Test
  public void shouldContainVariableDsWhenBuildingFromNetcdfFile() throws IOException {
    try (NetcdfFile netcdfFile = NetcdfDatasets.openFile(TEST_FILE, null)) {
      final Variable fileVariable = netcdfFile.findVariable("lat");
      assertThat((Object) fileVariable).isNotInstanceOf(VariableDS.class);

      final NetcdfDataset netcdfDataset = NetcdfDataset.builder(netcdfFile).build();
      final Variable datasetVariable = netcdfDataset.findVariable("lat");
      assertThat((Object) datasetVariable).isInstanceOf(VariableDS.class);
    }
  }

  @Test
  public void shouldContainVariableDsWhenEnhancingNetcdfFile() throws IOException {
    try (NetcdfFile netcdfFile = NetcdfDatasets.openFile(TEST_FILE, null)) {
      final NetcdfDataset netcdfDataset =
          NetcdfDatasets.enhance(netcdfFile, NetcdfDataset.getDefaultEnhanceMode(), null);
      final Variable datasetVariable = netcdfDataset.findVariable("lat");
      assertThat((Object) datasetVariable).isInstanceOf(VariableDS.class);
    }
  }

  @Test
  public void shouldContainVariableDsWhenEnhancingNetcdfDataset() throws IOException {
    try (NetcdfFile netcdfFile = NetcdfDatasets.openFile(TEST_FILE, null)) {
      final NetcdfDataset netcdfDataset = NetcdfDataset.builder(netcdfFile).build();
      final NetcdfDataset enhancedDataset =
          NetcdfDatasets.enhance(netcdfDataset, NetcdfDataset.getDefaultEnhanceMode(), null);
      final Variable datasetVariable = enhancedDataset.findVariable("lat");
      assertThat((Object) datasetVariable).isInstanceOf(VariableDS.class);
    }
  }

  @Test
  public void shouldContainVariableDsWhenOpenNcmlDataset() throws IOException {
    String ncml = "<?xml version='1.0' encoding='UTF-8'?>\n" //
        + "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2' enhance='All'>\n" //
        + "  <dimension name='lat' length='3' />\n" //
        + "  <variable name='var' shape='lat' type='int'/>\n" //
        + "</netcdf>"; //

    try (NetcdfDataset netcdfDataset = NetcdfDatasets.openNcmlDataset(new StringReader(ncml), null, null)) {
      final Variable datasetVariable = netcdfDataset.findVariable("var");
      assertThat((Object) datasetVariable).isInstanceOf(VariableDS.class);
    }
  }
}
