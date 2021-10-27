/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import org.junit.Test;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

/** Test {@link NetcdfDataset} */
public class TestNetcdfDataset {

  @Test
  public void testNcmlFileId() throws IOException {
    String filename = TestDir.cdmLocalTestDataDir + "testNested.ncml";
    try (NetcdfDataset ds = NetcdfDatasets.openDataset(filename)) {
      assertThat(ds.getFileTypeId()).isEqualTo("NcML/NetCDF-3");
    }
  }

  @Test
  public void testN3FileId() throws IOException {
    String filename = TestDir.cdmLocalTestDataDir + "example1.nc";
    try (NetcdfDataset ds = NetcdfDatasets.openDataset(filename)) {
      assertThat(ds.getFileTypeId()).isEqualTo("NetCDF-3");
      assertThat(ds.getFileTypeDescription()).isEqualTo("NetCDF-3/CDM");
    }
  }

}
