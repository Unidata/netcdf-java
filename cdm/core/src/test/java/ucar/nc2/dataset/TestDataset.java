package ucar.nc2.dataset;

import org.junit.Test;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

public class TestDataset {

  @Test
  public void testNcmlFileId() throws IOException {
    String filename = TestDir.cdmLocalTestDataDir + "testNested.ncml";
    try (NetcdfDataset ds = NetcdfDatasets.openDataset(filename)) {
      assertThat(ds.getFileTypeId()).isEqualTo("NcML/NetCDF-3");
    }
  }

}
