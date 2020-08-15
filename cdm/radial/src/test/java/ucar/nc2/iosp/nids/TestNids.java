/*
 * Copyright (c) 1998-2020 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp.nids;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.ArrayStructure;
import ucar.ma2.MAMath;
import ucar.ma2.MAMath.MinMax;
import ucar.ma2.StructureData;
import ucar.nc2.*;
import ucar.unidata.util.test.TestDir;
import java.io.*;
import java.lang.invoke.MethodHandles;

public class TestNids {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static String basereflectFile = TestDir.localTestDataDir + "nids/N0R_20041119_2147";
  private static String basereflect1File = TestDir.localTestDataDir + "nids/N1R_20050119_1548";
  private static String basereflect2File = TestDir.localTestDataDir + "nids/N2R_20050119_1528";
  private static String basereflect3File = TestDir.localTestDataDir + "nids/N3R_20050119_1548";
  private static String basereflectCFile = TestDir.localTestDataDir + "nids/NCR_20050119_1548";
  private static String basereflect248File = TestDir.localTestDataDir + "nids/N0Z_20050119_1538";
  private static String radialVelocityFile = TestDir.localTestDataDir + "nids/N0V_20041117_1646";
  private static String radialVelocity1File = TestDir.localTestDataDir + "nids/N1V_20050119_1548";
  private static String echotopFile = TestDir.localTestDataDir + "nids/NET_20041123_1648";
  private static String oneHourPrecipFile = TestDir.localTestDataDir + "nids/N1P_20041122_1837";
  private static String StormRelMeanVel0File = TestDir.localTestDataDir + "nids/N0S_20050119_1548";
  private static String StormRelMeanVel1File = TestDir.localTestDataDir + "nids/N1S_20041117_1640";
  private static String StormRelMeanVel2File = TestDir.localTestDataDir + "nids/N2S_20050120_1806";
  private static String StormRelMeanVel3File = TestDir.localTestDataDir + "nids/N3S_20050120_1806";
  private static String totalPrecipFile = TestDir.localTestDataDir + "nids/NTP_20050119_1528";
  private static String digitPrecipArrayFile = TestDir.localTestDataDir + "nids/DPA_20041123_1709";
  private static String vertIntegLiquidFile = TestDir.localTestDataDir + "nids/NVL_20041130_1946";
  private static String vadWindProfileFile = TestDir.localTestDataDir + "nids/NVW_20041117_1657";

  @Test
  public void testNidsReadRadial() throws IOException {
    NetcdfFile ncfile = null;
    try {
      System.out.println("**** Open " + basereflectFile);
      ncfile = NetcdfFiles.open(basereflectFile);

    } catch (java.io.IOException e) {
      System.out.println(" fail = " + e);
      e.printStackTrace();
      assert (false);
    }

    Variable v = null;
    v = ncfile.findVariable("BaseReflectivity");
    testReadData(v);
    assert (null != v.getDimension(0));
    assert (null != v.getDimension(1));

    ncfile.close();
    try {
      System.out.println("**** Open " + basereflect1File);
      ncfile = NetcdfFiles.open(basereflect1File);

    } catch (java.io.IOException e) {
      System.out.println(" fail = " + e);
      e.printStackTrace();
      assert (false);
    }
    v = ncfile.findVariable("BaseReflectivity");
    testReadData(v);
    assert (null != v.getDimension(0));
    assert (null != v.getDimension(1));

    ncfile.close();
    try {
      System.out.println("**** Open " + basereflect2File);
      ncfile = NetcdfFiles.open(basereflect2File);

    } catch (java.io.IOException e) {
      System.out.println(" fail = " + e);
      e.printStackTrace();
      assert (false);
    }
    v = ncfile.findVariable("BaseReflectivity");
    testReadData(v);
    assert (null != v.getDimension(0));
    assert (null != v.getDimension(1));

    ncfile.close();
    try {
      System.out.println("**** Open " + basereflect3File);
      ncfile = NetcdfFiles.open(basereflect3File);

    } catch (java.io.IOException e) {
      System.out.println(" fail = " + e);
      e.printStackTrace();
      assert (false);
    }
    v = ncfile.findVariable("BaseReflectivity");
    testReadData(v);
    assert (null != v.getDimension(0));
    assert (null != v.getDimension(1));

    ncfile.close();
    try {
      System.out.println("**** Open " + basereflect248File);
      ncfile = NetcdfFiles.open(basereflect248File);

    } catch (java.io.IOException e) {
      System.out.println(" fail = " + e);
      e.printStackTrace();
      assert (false);
    }

    v = ncfile.findVariable("BaseReflectivity248");
    testReadData(v);
    assert (null != v.getDimension(0));
    assert (null != v.getDimension(1));

    ncfile.close();
    try {
      System.out.println("**** Open " + StormRelMeanVel0File);
      ncfile = NetcdfFiles.open(StormRelMeanVel0File);

    } catch (java.io.IOException e) {
      System.out.println(" fail = " + e);
      e.printStackTrace();
      assert (false);
    }

    v = ncfile.findVariable("StormMeanVelocity");
    testReadData(v);
    assert (null != v.getDimension(0));
    assert (null != v.getDimension(1));

    ncfile.close();
    try {
      System.out.println("**** Open " + StormRelMeanVel1File);
      ncfile = NetcdfFiles.open(StormRelMeanVel1File);

    } catch (java.io.IOException e) {
      System.out.println(" fail = " + e);
      e.printStackTrace();
      assert (false);
    }
    v = ncfile.findVariable("StormMeanVelocity");
    testReadData(v);
    assert (null != v.getDimension(0));
    assert (null != v.getDimension(1));

    ncfile.close();
    try {
      System.out.println("**** Open " + StormRelMeanVel2File);
      ncfile = NetcdfFiles.open(StormRelMeanVel2File);

    } catch (java.io.IOException e) {
      System.out.println(" fail = " + e);
      e.printStackTrace();
      assert (false);
    }
    v = ncfile.findVariable("StormMeanVelocity");
    testReadData(v);
    assert (null != v.getDimension(0));
    assert (null != v.getDimension(1));

    ncfile.close();
    try {
      System.out.println("**** Open " + StormRelMeanVel3File);
      ncfile = NetcdfFiles.open(StormRelMeanVel3File);

    } catch (java.io.IOException e) {
      System.out.println(" fail = " + e);
      e.printStackTrace();
      assert (false);
    }
    v = ncfile.findVariable("StormMeanVelocity");
    testReadData(v);
    assert (null != v.getDimension(0));
    assert (null != v.getDimension(1));

    ncfile.close();
    try {
      System.out.println("**** Open " + radialVelocityFile);
      ncfile = NetcdfFiles.open(radialVelocityFile);

    } catch (java.io.IOException e) {
      System.out.println(" fail = " + e);
      e.printStackTrace();
      assert (false);
    }

    v = ncfile.findVariable("RadialVelocity");
    testReadData(v);
    assert (null != v.getDimension(0));
    assert (null != v.getDimension(1));

    ncfile.close();
    try {
      System.out.println("**** Open " + radialVelocity1File);
      ncfile = NetcdfFiles.open(radialVelocity1File);

    } catch (java.io.IOException e) {
      System.out.println(" fail = " + e);
      e.printStackTrace();
      assert (false);
    }
    v = ncfile.findVariable("RadialVelocity");
    testReadData(v);
    assert (null != v.getDimension(0));
    assert (null != v.getDimension(1));
    ncfile.close();
  }

  @Test
  public void testNidsReadRadialN1P() throws IOException {
    NetcdfFile ncfile = null;
    try {
      System.out.println("**** Open " + oneHourPrecipFile);
      ncfile = NetcdfFiles.open(oneHourPrecipFile);

    } catch (java.io.IOException e) {
      System.out.println(" fail = " + e);
      e.printStackTrace();
      assert (false);
    }

    Variable v = ncfile.findVariable("Precip1hr_RAW");
    testReadData(v);
    assert (null != v.getDimension(0));
    assert (null != v.getDimension(1));

    ncfile.close();
    try {
      System.out.println("**** Open " + totalPrecipFile);
      ncfile = NetcdfFiles.open(totalPrecipFile);

    } catch (java.io.IOException e) {
      System.out.println(" fail = " + e);
      e.printStackTrace();
      assert (false);
    }

    v = ncfile.findVariable("PrecipAccum_RAW");
    testReadData(v);
    assert (null != v.getDimension(0));
    assert (null != v.getDimension(1));
    ncfile.close();
  }

  @Test
  public void testNidsReadRaster() throws IOException {
    NetcdfFile ncfile = null;
    try {
      System.out.println("**** Open " + echotopFile);
      ncfile = NetcdfFiles.open(echotopFile);
    } catch (java.io.IOException e) {
      System.out.println(" fail = " + e);
      e.printStackTrace();
      assert (false);
    }
    Variable v = ncfile.findVariable("EchoTop");
    testReadData(v);
    assert (null != v.getDimension(0));
    assert (null != v.getDimension(1));

    ncfile.close();
    try {
      System.out.println("**** Open " + vertIntegLiquidFile);
      ncfile = NetcdfFiles.open(vertIntegLiquidFile);
    } catch (java.io.IOException e) {
      System.out.println(" fail = " + e);
      e.printStackTrace();
      assert (false);
    }

    v = ncfile.findVariable("VertLiquid");
    testReadData(v);
    assert (null != v.getDimension(0));
    assert (null != v.getDimension(1));

    v = ncfile.findVariable("VertLiquid_RAW");
    testReadData(v);
    assert (null != v.getDimension(0));
    assert (null != v.getDimension(1));
    ncfile.close();
    try {
      System.out.println("**** Open " + basereflectCFile);
      ncfile = NetcdfFiles.open(basereflectCFile);

    } catch (java.io.IOException e) {
      System.out.println(" fail = " + e);
      e.printStackTrace();
      assert (false);
    }

    v = ncfile.findVariable("BaseReflectivityComp");
    testReadData(v);
    assert (null != v.getDimension(0));
    assert (null != v.getDimension(1));
    ncfile.close();
  }

  @Test
  public void testNidsReadNVW() throws IOException {
    NetcdfFile ncfile = null;
    Variable v = null;
    Array a = null;
    try {
      System.out.println("**** Open " + vadWindProfileFile);
      ncfile = NetcdfFiles.open(vadWindProfileFile);
    } catch (java.io.IOException e) {
      System.out.println(" fail = " + e);
      e.printStackTrace();
      assert (false);
    }

    assert (null != ncfile.findVariable("textStruct_code8"));
    assert (null != ncfile.findVariable("textStruct_code8").getDimension(0));

    v = ncfile.findVariable("unlinkedVectorStruct");
    testReadDataAsShort((Structure) v, "iValue");

    v = ncfile.findVariable("VADWindSpeed");
    testReadData(v);

    v = ncfile.findVariable("TabMessagePage");
    testReadData(v);
    ncfile.close();
  }

  @Test
  public void testNidsReadDPA() throws IOException {
    NetcdfFile ncfile = null;
    Variable v = null;
    Array a = null;
    try {
      System.out.println("**** Open " + digitPrecipArrayFile);
      ncfile = NetcdfFiles.open(digitPrecipArrayFile);
    } catch (java.io.IOException e) {
      System.out.println(" fail = " + e);
      e.printStackTrace();
      assert (false);
    }

    v = ncfile.findVariable("PrecipArray_0");
    testReadData(v);
    /*
     * v = ncfile.findVariable("PrecipArray_1");
     * testReadData(v);
     * 
     * v = ncfile.findVariable("PrecipArray_2");
     * testReadData(v);
     * 
     * v = ncfile.findVariable("PrecipArray_3");
     * testReadData(v);
     * 
     * v = ncfile.findVariable("PrecipArray_4");
     * testReadData(v);
     * 
     * v = ncfile.findVariable("PrecipArray_5");
     * testReadData(v);
     * 
     * v = ncfile.findVariable("PrecipArray_6");
     * testReadData(v);
     * 
     * v = ncfile.findVariable("PrecipArray_7");
     * testReadData(v);
     * 
     * v = ncfile.findVariable("PrecipArray_8");
     * testReadData(v);
     * 
     * v = ncfile.findVariable("PrecipArray_9");
     * testReadData(v);
     * 
     * v = ncfile.findVariable("PrecipArray_10");
     * testReadData(v);
     * 
     * v = ncfile.findVariable("PrecipArray_11");
     * testReadData(v);
     * 
     * v = ncfile.findVariable("PrecipArray_12");
     * testReadData(v);
     * 
     * v = ncfile.findVariable("PrecipArray_13");
     * testReadData(v);
     */


    assert (null != ncfile.findVariable("textStruct_code1").getDimension(0));

    ncfile.close();
  }

  @Test
  public void testRadialImageMessagePcode180() throws IOException {
    // Radial Image message, product code 180 (TDWR)
    double comparisonTolerance = 0.1;
    String basereflect180TdwrFile = TestDir.localTestDataDir + "nids/Level3_TUL_TZ0_20200811_1804.nids";
    try (NetcdfFile ncf = NetcdfFiles.open(basereflect180TdwrFile)) {
      Variable bref = ncf.findVariable("BaseReflectivity");
      Array data = bref.read();
      double max = MAMath.getMaximum(data);
      // max reflectivity value as shown by NWS web display at the time
      // not a *great* check, but not the worst either.
      assertThat(max).isWithin(comparisonTolerance).of(56.5);
      // test that range of the radial axis variable is good
      // expect 0 to 48 nautical miles (according to the ICD)
      // which is roughly 0 to 88650 meters
      Variable gate = ncf.findVariable("gate");
      Array gateValues = gate.read();
      MinMax minMax = MAMath.getMinMax(gateValues);
      assertThat(minMax.min).isWithin(comparisonTolerance).of(0);
      assertThat(minMax.max).isWithin(comparisonTolerance).of(88650);
    }
  }

  @Test
  public void testRadialImageMessagePcode153() throws IOException {
    // Radial Image message, product code 153 (super res reflectivity).
    double comparisonTolerance = 0.1;
    String datafile = TestDir.localTestDataDir + "nids/H0Z_20200812_1318";
    try (NetcdfFile ncf = NetcdfFiles.open(datafile)) {
      Variable bref = ncf.findVariable("BaseReflectivityDR");
      Array data = bref.read();
      double max = MAMath.getMaximum(data);
      // expected max reflectivity value obtained from metpy decoder.
      assertThat(max).isWithin(comparisonTolerance).of(59.0);
      // test that range of the radial axis variable is good.
      Variable gate = ncf.findVariable("gate");
      Array gateValues = gate.read();
      MinMax minMax = MAMath.getMinMax(gateValues);
      assertThat(minMax.min).isWithin(comparisonTolerance).of(0);
      // within 1 km of 460 km.
      assertThat(minMax.max).isWithin(1000).of(460000);
    }
  }

  @Test
  public void testRadialImageMessagePcode154() throws IOException {
    // Radial Image message, product code 154 (super res velocity).
    double comparisonTolerance = 0.1;
    String datafile = TestDir.localTestDataDir + "nids/H0V_20200812_1309";
    try (NetcdfFile ncf = NetcdfFiles.open(datafile)) {
      Variable bref = ncf.findVariable("BaseVelocityDV");
      Array data = bref.read();
      double max = MAMath.getMaximum(data);
      // expected max velocity value obtained from metpy decoder.
      assertThat(max).isWithin(comparisonTolerance).of(44.5);
      // test that range of the radial axis variable is good.
      Variable gate = ncf.findVariable("gate");
      Array gateValues = gate.read();
      MinMax minMax = MAMath.getMinMax(gateValues);
      assertThat(minMax.min).isWithin(comparisonTolerance).of(0);
      // within 1 km of 300 km.
      assertThat(minMax.max).isWithin(1000).of(300000);
    }
  }

  @Test
  public void testRadialImageMessagePcode155() throws IOException {
    // Radial Image message, product code 155 (super res spectrum width).
    double comparisonTolerance = 0.1;
    String datafile = TestDir.localTestDataDir + "nids/H0W_20200812_1305";
    try (NetcdfFile ncf = NetcdfFiles.open(datafile)) {
      Variable bref = ncf.findVariable("SpectrumWidth");
      Array data = bref.read();
      double max = MAMath.getMaximum(data);
      // expected max spectrum width value obtained from metpy decoder.
      assertThat(max).isWithin(comparisonTolerance).of(15.0);
      // test that range of the radial axis variable is good.
      Variable gate = ncf.findVariable("gate");
      Array gateValues = gate.read();
      MinMax minMax = MAMath.getMinMax(gateValues);
      assertThat(minMax.min).isWithin(comparisonTolerance).of(0);
      // within 1 km of 300 km.
      assertThat(minMax.max).isWithin(1000).of(300000);
    }
  }

  @Test
  public void testRadialImageMessagePcode167() throws IOException {
    // Radial Image message, product code 167 (super res digital correlation coefficient).
    double comparisonTolerance = 0.1;
    String datafile = TestDir.localTestDataDir + "nids/H0C_20200814_0417";
    try (NetcdfFile ncf = NetcdfFiles.open(datafile)) {
      Variable bref = ncf.findVariable("CorrelationCoefficient");
      Array data = bref.read();
      double max = MAMath.getMaximum(data);
      // expected max correlation coefficient value obtained from metpy decoder.
      // can be greater than 1 due to the way it is measured, but should not be much greater than one.
      assertThat(max).isWithin(comparisonTolerance).of(1.05167);
      // test that range of the radial axis variable is good.
      Variable gate = ncf.findVariable("gate");
      Array gateValues = gate.read();
      MinMax minMax = MAMath.getMinMax(gateValues);
      assertThat(minMax.min).isWithin(comparisonTolerance).of(0);
      // within 1 km of 300 km.
      assertThat(minMax.max).isWithin(1000).of(300000);
    }
  }

  private void testReadData(Variable v) {
    Array a = null;
    assert (null != v);
    assert (null != v.getDimension(0));
    try {
      a = v.read();
      assert (null != a);
    } catch (java.io.IOException e) {
      e.printStackTrace();
      assert (false);
    }
    assert (v.getSize() == a.getSize());
  }

  private void testReadDataAsShort(Structure v, String memberName) {
    Array a = null;
    assert (null != v);
    assert (null != v.getDimension(0));
    try {
      a = v.read();
      assert (null != a);
    } catch (java.io.IOException e) {
      e.printStackTrace();
      assert (false);
    }
    assert (v.getSize() == a.getSize());
    assert (a instanceof ArrayStructure);

    int sum = 0;
    ArrayStructure as = (ArrayStructure) a;
    int n = (int) as.getSize();
    for (int i = 0; i < n; i++) {
      StructureData sdata = as.getStructureData(i);
      sum += sdata.getScalarShort(memberName);
    }

    System.out.println("test short sum = " + sum);
  }


}
