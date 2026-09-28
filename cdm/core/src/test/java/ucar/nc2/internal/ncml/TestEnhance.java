/*
 * Copyright (c) 1998-2026 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.ncml;

import static com.google.common.truth.Truth.assertThat;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.MAMath;
import ucar.ma2.MAMath.MinMax;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.unidata.util.test.TestDir;

/** Test NcmlNew enhancement */
public class TestEnhance {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static String dataDir = TestDir.cdmLocalTestDataDir + "ncml/enhance/";

  // aggregation of two members whose lat/lon are stored in radians (scale_factor 57.29578) and whose
  // ir_brightness_temperature is a packed ushort (scale_factor 0.01)
  private static final String aggNoMemberEnhancedLocation = dataDir + "aggNoMemberEnhanced.ncml";
  private static final String aggMemberEnhanceLocation = dataDir + "aggMemberEnhanced.ncml";
  private static final double LAT_MIN = 28.0;
  private static final double LAT_MAX = 48.0;
  private static final double LON_MIN = -100.0;
  private static final double LON_MAX = -79.0;
  private static final double DATA_MIN = 194.66;
  private static final double DATA_MAX = 319.50;

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
      assertThat(scaledvar.attributes().hasAttribute("scale_factor")).isFalse();
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
      assertThat(scaledvar.attributes().hasAttribute("scale_factor")).isFalse();
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
      assertThat(scaledvar.readScalarFloat()).isEqualTo(12.0f);
    }
  }

  @Test
  public void testEnhancedAgg() throws IOException {
    try (NetcdfDataset ncd = NetcdfDatasets.openDataset(aggNoMemberEnhancedLocation)) {
      testAgg(ncd);
    }

    // nested datasets with enhance="true"
    try (NetcdfDataset ncd = NetcdfDatasets.openDataset(aggMemberEnhanceLocation)) {
      testAgg(ncd);
    }
  }

  @Test
  public void testEnhancedAggOldApi() throws IOException {
    try (NetcdfDataset ncd = NetcdfDataset.openDataset(aggNoMemberEnhancedLocation)) {
      testAgg(ncd);
    }

    // nested datasets with enhance="true"
    try (NetcdfDataset ncd = NetcdfDataset.openDataset(aggMemberEnhanceLocation)) {
      testAgg(ncd);
    }
  }

  @Test
  public void testUnenhancedAggOfEnhancedDatasets() throws IOException {
    // an aggregation that is not itself enhanced must still see the values enhanced by its members
    try (NetcdfFile ncfile = NetcdfDatasets.openFile(aggMemberEnhanceLocation, null)) {
      checkMemberEnhancedValues(ncfile);
    }
    try (NetcdfFile ncfile = NetcdfDatasets.openDataset(aggMemberEnhanceLocation, false, null)) {
      checkMemberEnhancedValues(ncfile);
    }
  }

  private void checkMemberEnhancedValues(NetcdfFile ncfile) throws IOException {
    Variable lat = ncfile.findVariable("latitude");
    assertThat((Object) lat).isNotNull();
    MinMax latMinMax = MAMath.getMinMax(lat.read());
    assertThat(latMinMax.min).isWithin(1.0e-3).of(LAT_MIN);
    assertThat(latMinMax.max).isWithin(1.0e-3).of(LAT_MAX);
  }

  private void testAgg(NetcdfDataset ncd) throws IOException {
    assertThat(ncd.findDimension("time").getLength()).isEqualTo(2);

    Variable var = ncd.findVariable("ir_brightness_temperature");
    assertThat((Object) var).isNotNull();
    MinMax minMax = MAMath.getMinMax(var.read());
    assertThat(minMax.min).isWithin(0.1).of(DATA_MIN);
    assertThat(minMax.max).isWithin(0.1).of(DATA_MAX);

    checkCoord(ncd, "latitude", LAT_MIN, LAT_MAX);
    checkCoord(ncd, "longitude", LON_MIN, LON_MAX);
  }

  private void checkCoord(NetcdfDataset ncd, String name, double expectedMin, double expectedMax) throws IOException {
    Variable coord = ncd.findVariable(name);
    assertThat((Object) coord).isNotNull();

    Array data = coord.read();
    MinMax minMax = MAMath.getMinMax(data);
    assertThat(minMax.min).isWithin(1.0e-3).of(expectedMin);
    assertThat(minMax.max).isWithin(1.0e-3).of(expectedMax);

    // reading a second time (data may now be cached) must give the same answer
    MinMax again = MAMath.getMinMax(coord.read());
    assertThat(again.min).isWithin(1.0e-6).of(minMax.min);
    assertThat(again.max).isWithin(1.0e-6).of(minMax.max);

    // a section must agree with the corresponding part of the full read
    try {
      Array section = coord.read(new int[] {0}, new int[] {2});
      MinMax sectionMinMax = MAMath.getMinMax(section);
      assertThat(sectionMinMax.min).isWithin(1.0e-6)
          .of(MAMath.getMinMax(data.section(new int[] {0}, new int[] {2})).min);
      assertThat(sectionMinMax.max).isWithin(1.0e-6)
          .of(MAMath.getMinMax(data.section(new int[] {0}, new int[] {2})).max);
    } catch (InvalidRangeException e) {
      throw new RuntimeException(e);
    }
  }
}
