/*
 * Copyright (c) 2020 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp.hdf5;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.unidata.util.test.TestDir;

public class TestEosStructMetadata {

  @Test
  public void testStructMetadataEosString() throws IOException {
    // Test file source from https://github.com/Unidata/netcdf-java/issues/455
    // but reduced in size by creating a new file with only the StructureMetadata.0 dataset using h5copy:
    // h5copy -v -i VNP10A1_A2018001_h31v11_001_2019126193423_HEGOUT.nc -o structmetadata_eos.h5 -p
    // -s "/HDFEOS INFORMATION/StructMetadata.0" -d "/HDFEOS INFORMATION/StructMetadata.0"
    // Copying file <VNP10A1_A2018001_h31v11_001_2019126193423_HEGOUT.nc> and object </HDFEOS
    // INFORMATION/StructMetadata.0>
    // to file <structmetadata_eos.h5> and object </HDFEOS INFORMATION/StructMetadata.0>
    // h5copy: Creating parent groups
    String testFile = TestDir.cdmLocalTestDataDir + "hdf5/structmetadata_eos.h5";
    try (NetcdfFile ncf = NetcdfFiles.open(testFile)) {
      Variable testVar = ncf.findVariable("HDFEOS_INFORMATION/StructMetadata\\.0");
      Array data = testVar.read();

      assertThat(data.getDataType()).isEqualTo(DataType.STRING);
      assertThat(data.getSize()).isEqualTo(1);

      Object ob = data.getObject(0);
      assertThat(ob).isInstanceOf(String.class);

      String strData = String.valueOf(ob);
      assertThat(strData).hasLength(1664);
      assertThat(strData).startsWith("GROUP=SwathStructure\nEND_GROUP=SwathStructure\nGROUP=GridStructure");
      assertThat(strData).endsWith("END_GROUP=ZaStructure\nEND\n");
    }
  }
}
