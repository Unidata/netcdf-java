package ucar.nc2.dataset;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import org.junit.Test;

public class TestNetcdfDataset {

  @Test
  public void shouldGetLastModified() throws IOException {
    final String location = "src/test/data/ncml/nc/jan.nc";
    try (NetcdfDataset netcdfDataset = NetcdfDatasets.openDataset(location)) {
      assertThat(netcdfDataset.getLastModified()).isGreaterThan(0);
    }
  }
}
