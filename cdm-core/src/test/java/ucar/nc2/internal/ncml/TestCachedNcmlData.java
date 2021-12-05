/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.ncml;

import java.io.IOException;

import com.google.common.truth.Truth;
import org.junit.Ignore;
import org.junit.Test;
import ucar.array.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.unidata.util.test.TestDir;

import static com.google.common.truth.Truth.assertThat;

public class TestCachedNcmlData {

  @Test
  public void testCachedData() throws IOException {

    try (NetcdfFile ncd = NetcdfDatasets.openFile(TestDir.cdmLocalTestDataDir + "point/profileMultidim.ncml", null)) {
      Variable v = ncd.findVariable("data");
      assertThat(v).isNotNull();
      Array<?> data = v.readArray();
      assertThat(data.getSize()).isEqualTo(50);
    }
  }

  @Test
  @Ignore("doesnt work because NetcdfFileProvider cant pass in IospMessage")
  public void testCachedDataWithStructure() throws IOException {
    DatasetUrl durl = DatasetUrl.findDatasetUrl(TestDir.cdmLocalTestDataDir + "point/profileMultidim.ncml");

    try (NetcdfFile ncd = NetcdfDatasets.openFile(durl, -1, null, NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE)) {
      Variable s = ncd.findVariable("record");
      assertThat(s).isNotNull();
      assertThat(s).isInstanceOf(Structure.class);
      assertThat(s.getSize()).isEqualTo(5);

      Array<?> data = s.readArray();
      assertThat(data.getSize()).isEqualTo(5);
    }
  }


}
