/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.iosp.hdf5;

import static com.google.common.truth.Truth.assertThat;
import org.junit.Test;
import ucar.array.ArrayType;
import ucar.nc2.EnumTypedef;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.unidata.util.test.TestDir;

/** Test handling of enums in hdf5 / netcdf 4 files. */
public class TestEnumTypedef {

  @Test
  public void problem() throws Exception {
    try (NetcdfFile ncfile = NetcdfFiles.open(TestDir.cdmLocalTestDataDir + "hdf5/test_atomic_types.nc")) {
      Variable primaryCloud = ncfile.findVariable("primary_cloud");
      assertThat((Object) primaryCloud).isNotNull();
      assertThat(primaryCloud.getArrayType().isEnum());
      assertThat(primaryCloud.getArrayType()).isEqualTo(ArrayType.ENUM1);
      assertThat(primaryCloud.getEnumTypedef()).isNotNull();
      EnumTypedef typedef = primaryCloud.getEnumTypedef();
      assertThat(typedef).isNotNull();
      // TODO disable this until we have a fix see Issue #126
      // assertThat(typedef.getShortName()).isEqualTo("cloud_class_t");
    }
  }

}
