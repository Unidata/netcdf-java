/* Copyright Unidata */
package ucar.nc2.iosp.hdf5;

import static com.google.common.truth.Truth.assertThat;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import ucar.ma2.DataType;
import ucar.nc2.EnumTypedef;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.unidata.util.test.TestDir;

/** Test handling of enums in hdf5 / netcdf 4 files. */
// see Issue #126
public class TestEnumTypedef {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void problem() throws Exception {
    String s = TestDir.cdmLocalTestDataDir + "hdf5/test_atomic_types.nc";
    logger.info("TestEnumTypedef on {}%n", s);
    try (NetcdfFile ncfile = NetcdfFiles.open(s)) {
      Variable primaryCloud = ncfile.findVariable("primary_cloud");
      assertThat((Object) primaryCloud).isNotNull();
      assertThat(primaryCloud.getDataType().isEnum());
      assertThat(primaryCloud.getDataType()).isEqualTo(DataType.ENUM1);
      assertThat(primaryCloud.getEnumTypedef()).isNotNull();
      EnumTypedef typedef = primaryCloud.getEnumTypedef();
      assertThat(typedef).isNotNull();
      logger.info("TestEnumTypedef typedef name {}%n", typedef.getShortName());
      assertThat(typedef.getShortName()).isEqualTo("cloud_class_t");
    }
  }
}

