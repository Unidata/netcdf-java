package ucar.unidata.io.s3;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import org.junit.Test;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;

public class TestS3NetcdfDataset {

  @Test
  public void shouldGetLastModifiedForS3File() throws IOException {
    final String location = "cdms3:thredds-test-data?testData.nc";
    try (NetcdfDataset netcdfDataset = NetcdfDatasets.openDataset(location)) {
      assertThat(netcdfDataset.getLastModified()).isGreaterThan(0);
    }
  }
}
