/* Copyright Unidata */
package ucar.nc2.iosp.hdf5;

import static com.google.common.truth.Truth.assertThat;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import ucar.ma2.DataType;
import ucar.nc2.*;
import ucar.unidata.util.test.TestDir;

/** Test handling of enums in hdf5 / netcdf 4 files. */
// see Issue #126
public class TestEnumTypedef {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  // Test case where enum type is in same group to the variable using it.
  @Test
  public void test1() throws Exception {
    String s = TestDir.cdmLocalTestDataDir + "hdf5/test_atomic_types.nc";
    logger.info("TestEnumTypedef on {}%n", s);
    try (NetcdfFile ncfile = NetcdfFiles.open(s)) {
      Variable primaryCloud = ncfile.findVariable("/primary_cloud");
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

  // Test case where enum type is in a parent group to the variable using it.
  @Test
  public void test2() throws Exception {
    String s = TestDir.cdmLocalTestDataDir + "hdf5/test_enum_2.nc4";
    logger.info("TestEnumTypedef on {}%n", s);
    try (NetcdfFile ncfile = NetcdfFiles.open(s)) {
      Group h = ncfile.findGroup("/h");
      Variable primaryCloud = h.findVariableLocal("primary_cloud");
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

