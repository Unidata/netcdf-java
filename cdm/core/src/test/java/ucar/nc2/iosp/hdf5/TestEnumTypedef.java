/* Copyright Unidata */
package ucar.nc2.iosp.hdf5;

import static com.google.common.truth.Truth.assertThat;
import org.junit.Test;
import ucar.ma2.DataType;
import ucar.nc2.EnumTypedef;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.unidata.util.test.TestDir;
import java.io.File;

import java.io.File;

/** Test handling of enums in hdf5 / netcdf 4 files. */
public class TestEnumTypedef {

  @Test
  public void problem() throws Exception {
    StringBuilder s = new StringBuilder();
    s.append(System.getProperty("user.dir"));
    s.append("/");
    s.append(TestDir.cdmLocalTestDataDir);
    s.append("hdf5/test_atomic_types.nc");
    translate(s);
    File f = new File(s.toString());
    assert (f.canRead());
    try (NetcdfFile ncfile = NetcdfFiles.open(s.toString())) {
      Variable primaryCloud = ncfile.findVariable("primary_cloud");
      assertThat((Object) primaryCloud).isNotNull();
      assertThat(primaryCloud.getDataType().isEnum());
      assertThat(primaryCloud.getDataType()).isEqualTo(DataType.ENUM1);
      assertThat(primaryCloud.getEnumTypedef()).isNotNull();
      EnumTypedef typedef = primaryCloud.getEnumTypedef();
      assertThat(typedef).isNotNull();
      // see Issue #126
      assertThat(typedef.getShortName()).isEqualTo("cloud_class_t");
    }
  }

  private void translate(StringBuilder sb) {
    char sep = File.separator.charAt(0);
    for (int i = 0; i < sb.length(); i++) {
      char c = sb.charAt(i);
      sb.setCharAt(i, (c == '/' || c == '\\') ? sep : c);
    }
  }

}
