/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ncml;

import static com.google.common.truth.Truth.assertThat;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.DataType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.unidata.util.test.TestDir;

/** Test NcmlNew enhancement */
public class TestEnhance {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static String dataDir = TestDir.cdmLocalTestDataDir + "ncml/enhance/";

  @Test
  public void testStandaloneNoEnhance() throws IOException {
    try (NetcdfFile ncfile = NetcdfDatasets.openFile(dataDir + "testStandaloneNoEnhance.ncml", null)) {
      Variable unvar = ncfile.findVariable("unvar");
      assertThat((Object) unvar).isNotNull();
      assertThat(unvar.getDataType()).isEqualTo(DataType.SHORT);
      assertThat(unvar.attributes().hasAttribute("_Unsigned")).isTrue();
      assertThat(unvar.attributes().findAttributeString("_Unsigned", "")).isEqualTo("true");
      assertThat(unvar.readScalarShort()).isEqualTo(-9981);

      Variable scaledvar = ncfile.findVariable("scaledvar");
      assertThat((Object) scaledvar).isNotNull();
      assertThat(scaledvar.getDataType()).isEqualTo(DataType.SHORT);
      assertThat(scaledvar.attributes().hasAttribute("scale_factor")).isTrue();
      assertThat(scaledvar.attributes().findAttributeDouble("scale_factor", 1.0)).isEqualTo(2.0);
      assertThat(scaledvar.readScalarShort()).isEqualTo(1);
    }
  }

  @Test
  public void testStandaloneNoEnhanceDataset() throws IOException {
    try (NetcdfFile ncfile = NetcdfDatasets.openDataset(dataDir + "testStandaloneNoEnhance.ncml", false, null)) {
      Variable unvar = ncfile.findVariable("unvar");
      assertThat((Object) unvar).isNotNull();
      assertThat(unvar.getDataType()).isEqualTo(DataType.SHORT);
      assertThat(unvar.attributes().hasAttribute("_Unsigned")).isTrue();
      assertThat(unvar.attributes().findAttributeString("_Unsigned", "")).isEqualTo("true");
      assertThat(unvar.readScalarShort()).isEqualTo(-9981);

      Variable scaledvar = ncfile.findVariable("scaledvar");
      assertThat((Object) scaledvar).isNotNull();
      assertThat(scaledvar.getDataType()).isEqualTo(DataType.SHORT);
      assertThat(scaledvar.attributes().hasAttribute("scale_factor")).isTrue();
      assertThat(scaledvar.attributes().findAttributeDouble("scale_factor", 1.0)).isEqualTo(2.0);
      assertThat(scaledvar.readScalarShort()).isEqualTo(1);
    }
  }

  @Test
  public void testStandaloneEnhance() throws IOException {
    try (NetcdfFile ncfile = NetcdfDatasets.openFile(dataDir + "testStandaloneEnhance.ncml", null)) {
      Variable unvar = ncfile.findVariable("unvar");
      assertThat((Object) unvar).isNotNull();
      assertThat(unvar.getDataType()).isEqualTo(DataType.UINT);
      assertThat(unvar.attributes().hasAttribute("_Unsigned")).isTrue();
      assertThat(unvar.attributes().findAttributeString("_Unsigned", "")).isEqualTo("true");
      assertThat(unvar.readScalarInt()).isEqualTo(55555);

      Variable scaledvar = ncfile.findVariable("scaledvar");
      assertThat((Object) scaledvar).isNotNull();
      assertThat(scaledvar.getDataType()).isEqualTo(DataType.FLOAT);
      assertThat(scaledvar.attributes().hasAttribute("scale_factor")).isTrue();
      assertThat(scaledvar.attributes().findAttributeDouble("scale_factor", 1.0)).isEqualTo(2.0);
      assertThat(scaledvar.readScalarFloat()).isEqualTo(12.0f);
    }
  }

  @Test
  public void testStandaloneEnhanceDataset() throws IOException {
    try (NetcdfFile ncfile = NetcdfDatasets.openDataset(dataDir + "testStandaloneNoEnhance.ncml", true, null)) {
      Variable unvar = ncfile.findVariable("unvar");
      assertThat((Object) unvar).isNotNull();
      assertThat(unvar.getDataType()).isEqualTo(DataType.UINT);
      assertThat(unvar.attributes().hasAttribute("_Unsigned")).isTrue();
      assertThat(unvar.attributes().findAttributeString("_Unsigned", "")).isEqualTo("true");
      assertThat(unvar.readScalarInt()).isEqualTo(55555);

      Variable scaledvar = ncfile.findVariable("scaledvar");
      assertThat((Object) scaledvar).isNotNull();
      assertThat(scaledvar.getDataType()).isEqualTo(DataType.FLOAT);
      assertThat(scaledvar.attributes().hasAttribute("scale_factor")).isTrue();
      assertThat(scaledvar.attributes().findAttributeDouble("scale_factor", 1.0)).isEqualTo(2.0);
      assertThat(scaledvar.readScalarFloat()).isEqualTo(12.0f);
    }
  }

  @Test
  public void testStandaloneDoubleEnhanceDataset() throws IOException {
    try (NetcdfFile ncfile = NetcdfDatasets.openDataset(dataDir + "testStandaloneEnhance.ncml", true, null)) {
      Variable unvar = ncfile.findVariable("unvar");
      assertThat((Object) unvar).isNotNull();
      assertThat(unvar.getDataType()).isEqualTo(DataType.UINT);
      assertThat(unvar.attributes().hasAttribute("_Unsigned")).isTrue();
      assertThat(unvar.attributes().findAttributeString("_Unsigned", "")).isEqualTo("true");
      assertThat(unvar.readScalarInt()).isEqualTo(55555);

      Variable scaledvar = ncfile.findVariable("scaledvar");
      assertThat((Object) scaledvar).isNotNull();
      assertThat(scaledvar.getDataType()).isEqualTo(DataType.FLOAT);
      assertThat(scaledvar.attributes().hasAttribute("scale_factor")).isTrue();
      assertThat(scaledvar.attributes().findAttributeDouble("scale_factor", 1.0)).isEqualTo(2.0);
      assertThat(scaledvar.readScalarFloat()).isEqualTo(12.0f);
    }
  }

}
