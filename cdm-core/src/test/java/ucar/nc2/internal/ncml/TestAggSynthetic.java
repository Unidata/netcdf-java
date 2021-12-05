/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.ncml;

import java.io.IOException;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Index;
import ucar.array.InvalidRangeException;
import ucar.array.Section;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.util.Misc;

import static com.google.common.truth.Truth.assertThat;

/** Test netcdf dataset in the JUnit framework. */
public class TestAggSynthetic {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void test1() throws Exception {
    String filename = "file:./" + TestNcmlRead.topDir + "aggSynthetic.xml";
    NetcdfDataset ncfile = NetcdfDatasets.openDataset(filename, false, null);

    Variable v = ncfile.findVariable("time");
    assert v != null;
    String testAtt = v.findAttributeString("units", null);
    assert testAtt != null;
    assert testAtt.equals("months since 2000-6-16 6:00");

    testDimensions(ncfile);
    testCoordVar(ncfile);
    testAggCoordVar(ncfile);
    testReadData(ncfile, "T");
    testReadSlice(ncfile, "T");

    ncfile.close();
  }

  @Test
  public void test2() throws Exception {
    String filename = "file:./" + TestNcmlRead.topDir + "aggSynthetic2.xml";
    NetcdfDataset ncfile = NetcdfDatasets.openDataset(filename, false, null);

    testDimensions(ncfile);
    testCoordVar(ncfile);
    testAggCoordVar2(ncfile);
    testReadData(ncfile, "T");
    testReadSlice(ncfile, "T");

    ncfile.close();
  }

  @Test
  public void test3() throws Exception {
    String filename = "file:./" + TestNcmlRead.topDir + "aggSynthetic3.xml";
    NetcdfDataset ncfile = NetcdfDatasets.openDataset(filename, false, null);

    testDimensions(ncfile);
    testCoordVar(ncfile);
    testAggCoordVar3(ncfile);
    testReadData(ncfile, "T");
    testReadSlice(ncfile, "T");

    ncfile.close();
  }

  @Test
  public void testNoCoord() throws Exception {
    String filename = "file:./" + TestNcmlRead.topDir + "aggSynNoCoord.xml";
    NetcdfDataset ncfile = NetcdfDatasets.openDataset(filename, false, null);

    testDimensions(ncfile);
    testCoordVar(ncfile);
    testAggCoordVarNoCoord(ncfile);
    testReadData(ncfile, "T");
    testReadSlice(ncfile, "T");

    ncfile.close();
  }

  @Test
  public void testNoCoordDir() throws Exception {
    String filename = "file:./" + TestNcmlRead.topDir + "aggSynNoCoordsDir.xml";
    NetcdfDataset ncfile = NetcdfDatasets.openDataset(filename, false, null);

    testDimensions(ncfile);
    testCoordVar(ncfile);
    testAggCoordVarNoCoordsDir(ncfile);
    testReadData(ncfile, "T");
    testReadSlice(ncfile, "T");

    ncfile.close();
  }

  @Test
  public void testJoinNewScalarCoord() throws Exception {
    String filename = "file:./" + TestNcmlRead.topDir + "aggJoinNewScalarCoord.xml";
    NetcdfDataset ncfile = NetcdfDatasets.openDataset(filename, false, null);

    Variable v = ncfile.findVariable("time");
    assert v != null;
    String testAtt = v.findAttributeString("units", null);
    assert testAtt != null;
    assert testAtt.equals("seconds since 2017-01-01");

    testDimensions(ncfile);
    testCoordVar(ncfile);
    testAggCoordVarJoinedScalar(ncfile);
    testReadData(ncfile, "T");
    testReadSlice(ncfile, "T");

    ncfile.close();
  }

  @Test
  public void testRename() throws Exception {
    String xml = "<?xml version='1.0' encoding='UTF-8'?>\n" // leavit
        + "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" // leavit
        + "  <variable name='Temperature' orgName='T' />\n" // leavit
        + "  <aggregation  dimName='time' type='joinNew'>\n" // leavit
        + "    <variableAgg name='T'/>\n" // leavit
        + "    <scan location='src/test/data/ncml/nc/' suffix='Dir.nc' subdirs='false'/>\n" // leavit
        + "  </aggregation>\n" // leavit
        + "</netcdf>"; // leavit

    String filename = "file:./" + TestNcmlRead.topDir + "exclude/aggSynRename.xml";
    NetcdfDataset ncfile = NetcdfDatasets.openNcmlDataset(new StringReader(xml), null, null);

    testDimensions(ncfile);
    testCoordVar(ncfile);
    testAggCoordVarNoCoordsDir(ncfile);
    testReadData(ncfile, "Temperature");
    testReadSlice(ncfile, "Temperature");
    ncfile.close();
  }

  @Test
  public void testScan() throws Exception {
    String xml = "<?xml version='1.0' encoding='UTF-8'?>\n"
        + "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" //
        + "  <variable name='time' type='int' shape='time'>\n" //
        + "    <attribute name='long_name' type='string' value='time coordinate' />\n" //
        + "    <attribute name='units' type='string' value='days since 2001-8-31 00:00:00 UTC' />\n" //
        + "    <values start='0' increment='10' />\n" //
        + "  </variable>\n" //
        + "  <aggregation dimName='time' type='joinNew'>\n" //
        + "    <variableAgg name='T'/>\n" //
        + "    <scan location='src/test/data/ncml/nc/' suffix='Dir.nc' subdirs='false'/>\n" //
        + "  </aggregation>\n" //
        + "</netcdf>";

    NetcdfFile ncfile = NcmlReader.readNcml(new StringReader(xml), null, null).build();

    testDimensions(ncfile);
    testCoordVar(ncfile);
    testAggCoordVarScan(ncfile);
    testReadData(ncfile, "T");
    testReadSlice(ncfile, "T");
    ncfile.close();
  }

  private void testDimensions(NetcdfFile ncfile) {
    logger.debug("ncfile = {}", ncfile);

    Dimension latDim = ncfile.findDimension("lat");
    assert null != latDim;
    assert latDim.getShortName().equals("lat");
    assert latDim.getLength() == 3;
    assert !latDim.isUnlimited();

    Dimension lonDim = ncfile.findDimension("lon");
    assert null != lonDim;
    assert lonDim.getShortName().equals("lon");
    assert lonDim.getLength() == 4;
    assert !lonDim.isUnlimited();

    Dimension timeDim = ncfile.findDimension("time");
    assert null != timeDim;
    assert timeDim.getShortName().equals("time");
    assert timeDim.getLength() == 3 : timeDim.getLength();
  }

  private void testCoordVar(NetcdfFile ncfile) throws IOException {
    Variable lat = ncfile.findVariable("lat");
    assert null != lat;
    assert lat.getShortName().equals("lat");
    assert lat.getRank() == 1;
    assert lat.getSize() == 3;
    assert lat.getShape()[0] == 3;
    assert lat.getArrayType() == ArrayType.FLOAT;

    assert !lat.isUnlimited();
    assert lat.getDimension(0).equals(ncfile.findDimension("lat"));

    Attribute att = lat.findAttribute("units");
    assert null != att;
    assert !att.isArray();
    assert att.isString();
    assert att.getArrayType() == ArrayType.STRING;
    assert att.getStringValue().equals("degrees_north");
    assert att.getNumericValue() == null;
    assert att.getNumericValue(3) == null;

    Array<Number> data = (Array<Number>) lat.readArray();
    assert data.getRank() == 1;
    assert data.getSize() == 3;
    assert data.getShape()[0] == 3;

    assertThat(Misc.nearlyEquals(data.get(0).doubleValue(), 41.0)).isTrue();
    assertThat(Misc.nearlyEquals(data.get(1).doubleValue(), 40.0)).isTrue();
    assertThat(Misc.nearlyEquals(data.get(2).doubleValue(), 39.0)).isTrue();
  }

  private void testAggCoordVar(NetcdfFile ncfile) throws IOException {
    Variable time = ncfile.findVariable("time");
    assert null != time;
    assert time.getShortName().equals("time");
    assert time.getRank() == 1 : time.getRank();
    assert time.getShape()[0] == 3;
    assert time.getArrayType() == ArrayType.INT;

    assert time.getDimension(0) == ncfile.findDimension("time");

    Array<Number> data = (Array<Number>) time.readArray();
    assert data.get(0).intValue() == 0;
    assert data.get(1).intValue() == 10;
    assert data.get(2).intValue() == 99;
  }

  private void testAggCoordVar2(NetcdfFile ncfile) throws IOException {

    Variable time = ncfile.findVariable("time");
    assert null != time;
    assert time.getShortName().equals("time");
    assert time.getRank() == 1 : time.getRank();
    assert time.getShape()[0] == 3;
    assert time.getArrayType() == ArrayType.INT;

    assert time.getDimension(0) == ncfile.findDimension("time");

    Array<Number> data = (Array<Number>) time.readArray();
    assert data.get(0).intValue() == 0;
    assert data.get(1).intValue() == 1;
    assert data.get(2).intValue() == 2;
  }

  private void testAggCoordVar3(NetcdfFile ncfile) throws IOException {
    Variable time = ncfile.findVariable("time");
    assert null != time;
    assert time.getShortName().equals("time");
    assert time.getRank() == 1 : time.getRank();
    assert time.getShape()[0] == 3;
    assert time.getArrayType() == ArrayType.DOUBLE : time.getArrayType();

    assert time.getDimension(0) == ncfile.findDimension("time");

    Array<Number> data = (Array<Number>) time.readArray();
    assertThat(Misc.nearlyEquals(data.get(0).doubleValue(), 0.0)).isTrue();
    assertThat(Misc.nearlyEquals(data.get(1).doubleValue(), 10.0)).isTrue();
    assertThat(Misc.nearlyEquals(data.get(2).doubleValue(), 99.0)).isTrue();
  }

  private void testAggCoordVarScan(NetcdfFile ncfile) throws IOException {
    Variable time = ncfile.findVariable("time");
    assert null != time;
    assert time.getShortName().equals("time");
    assert time.getRank() == 1 : time.getRank();
    assert time.getShape()[0] == 3;
    assert time.getArrayType() == ArrayType.INT : time.getArrayType();

    assert time.getDimension(0) == ncfile.findDimension("time");

    int count = 0;
    Array<Number> data = (Array<Number>) time.readArray();
    for (Number val : data) {
      int vali = val.intValue();
      assert vali == count * 10;
      count++;
    }
  }

  private void testAggCoordVarJoinedScalar(NetcdfFile ncfile) throws IOException {

    Variable time = ncfile.findVariable("time");
    assert null != time;
    assert time.getShortName().equals("time");
    assert time.getRank() == 1 : time.getRank();
    assert time.getShape()[0] == 3;
    assert time.getArrayType() == ArrayType.INT;

    assert time.getDimension(0) == ncfile.findDimension("time");

    Array<Number> data = (Array<Number>) time.readArray();
    assert data.get(0).intValue() == 82932;
    assert data.get(1).intValue() == 83232;
    assert data.get(2).intValue() == 83532;
  }

  private void testAggCoordVarNoCoord(NetcdfFile ncfile) throws IOException {
    Variable time = ncfile.findVariable("time");
    assert null != time;
    assert time.getShortName().equals("time");
    assert time.getRank() == 1 : time.getRank();
    assert time.getShape()[0] == 3;
    assert time.getArrayType() == ArrayType.STRING : time.getArrayType();

    assert time.getDimension(0) == ncfile.findDimension("time");

    Array<String> data = (Array<String>) time.readArray();

    String coordName = data.get(0);
    assert coordName.equals("time0.nc") : coordName;
    coordName = data.get(1);
    assert coordName.equals("time1.nc") : coordName;
    coordName = data.get(2);
    assert coordName.equals("time2.nc") : coordName;
  }

  private void testAggCoordVarNoCoordsDir(NetcdfFile ncfile) throws IOException {
    Variable time = ncfile.findVariable("time");
    assert null != time;
    assert time.getShortName().equals("time");
    assert time.getRank() == 1 : time.getRank();
    assert time.getShape()[0] == 3;
    assert time.getArrayType() == ArrayType.STRING : time.getArrayType();

    assert time.getDimension(0) == ncfile.findDimension("time");

    Array<String> data = (Array<String>) time.readArray();
    String coordName = data.get(0);
    assert coordName.equals("time0Dir.nc") : coordName;
    coordName = data.get(1);
    assert coordName.equals("time1Dir.nc") : coordName;
    coordName = data.get(2);
    assert coordName.equals("time2Dir.nc") : coordName;
  }

  private void testReadData(NetcdfFile ncfile, String name) throws IOException {

    Variable v = ncfile.findVariable(name);
    assert null != v;
    assert v.getShortName().equals(name);
    assert v.getRank() == 3;
    assert v.getSize() == 36 : v.getSize();
    assert v.getShape()[0] == 3;
    assert v.getShape()[1] == 3;
    assert v.getShape()[2] == 4;
    assert v.getArrayType() == ArrayType.DOUBLE;

    assert !v.isCoordinateVariable();

    assert v.getDimension(0) == ncfile.findDimension("time");
    assert v.getDimension(1) == ncfile.findDimension("lat");
    assert v.getDimension(2) == ncfile.findDimension("lon");

    Array<Number> data = (Array<Number>) v.readArray();
    assert data.getRank() == 3;
    assert data.getSize() == 36;
    assert data.getShape()[0] == 3;
    assert data.getShape()[1] == 3;
    assert data.getShape()[2] == 4;

    int[] shape = data.getShape();
    Index tIndex = data.getIndex();
    for (int i = 0; i < shape[0]; i++)
      for (int j = 0; j < shape[1]; j++)
        for (int k = 0; k < shape[2]; k++) {
          double val = data.get(tIndex.set(i, j, k)).doubleValue();
          assertThat(Misc.nearlyEquals(val, 100 * i + 10 * j + k)).isTrue();
        }

  }

  private void readSlice(NetcdfFile ncfile, int[] origin, int[] shape, String name)
      throws IOException, InvalidRangeException {

    Variable v = ncfile.findVariable(name);

    Array<Number> data = (Array<Number>) v.readArray(new Section(origin, shape));
    assertThat(data.getRank()).isEqualTo(3);
    assert data.getSize() == shape[0] * shape[1] * shape[2];
    assert data.getShape()[0] == shape[0] : data.getShape()[0] + " " + shape[0];
    assert data.getShape()[1] == shape[1];
    assert data.getShape()[2] == shape[2];

    Index tIndex = data.getIndex();
    for (int i = 0; i < shape[0]; i++)
      for (int j = 0; j < shape[1]; j++)
        for (int k = 0; k < shape[2]; k++) {
          double val = data.get(tIndex.set(i, j, k)).doubleValue();
          assertThat(Misc.nearlyEquals(val, 100 * (i + origin[0]) + 10 * j + k)).isTrue();
        }
  }

  private void testReadSlice(NetcdfFile ncfile, String name) throws IOException, InvalidRangeException {
    readSlice(ncfile, new int[] {0, 0, 0}, new int[] {3, 3, 4}, name);
    readSlice(ncfile, new int[] {0, 0, 0}, new int[] {2, 3, 2}, name);
    readSlice(ncfile, new int[] {2, 0, 0}, new int[] {1, 3, 4}, name);
    readSlice(ncfile, new int[] {1, 0, 0}, new int[] {2, 2, 3}, name);
  }
}
