/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.ncml;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.array.InvalidRangeException;
import ucar.array.Section;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.write.NcdumpArray;
import ucar.unidata.util.test.Assert2;

import static com.google.common.truth.Truth.assertThat;

/**
 * Test agg union.
 * the 2 files are copies, use explicit to mask one and rename the other.
 */

/*
 * <?xml version="1.0" encoding="UTF-8"?>
 * <netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2">
 * 
 * <attribute name="title" type="string" value="Example Data"/>
 * 
 * <aggregation type="union">
 * <netcdf location="file:src/test/data/ncml/nc/example2.nc">
 * <explicit/>
 * 
 * <dimension name="time" length="2" isUnlimited="true"/>
 * <dimension name="lat" length="3"/>
 * <dimension name="lon" length="4"/>
 * 
 * <variable name="lat" type="float" shape="lat">
 * <attribute name="units" type="string" value="degrees_north"/>
 * </variable>
 * <variable name="lon" type="float" shape="lon">
 * <attribute name="units" type="string" value="degrees_east"/>
 * </variable>
 * <variable name="time" type="int" shape="time">
 * <attribute name="units" type="string" value="hours"/>
 * </variable>
 * 
 * <variable name="ReletiveHumidity" type="int" shape="time lat lon" orgName="rh">
 * <attribute name=CDM.LONG_NAME type="string" value="relative humidity"/>
 * <attribute name="units" type="string" value="percent"/>
 * </variable>
 * </netcdf>
 * 
 * <netcdf location="file:src/test/data/ncml/nc/example1.nc">
 * <explicit/>
 * 
 * <dimension name="time" length="2" isUnlimited="true"/>
 * <dimension name="lat" length="3"/>
 * <dimension name="lon" length="4"/>
 * 
 * <variable name="Temperature" type="double" shape="time lat lon" orgName="T">
 * <attribute name=CDM.LONG_NAME type="string" value="surface temperature"/>
 * <attribute name="units" type="string" value="degC"/>
 * </variable>
 * </netcdf>
 * 
 * </aggregation>
 * </netcdf>
 * 
 * netcdf C:/dev/tds/thredds/cdm/src/test/data/ncml/nc/example1.nc {
 * dimensions:
 * time = UNLIMITED; // (2 currently)
 * lat = 3;
 * lon = 4;
 * variables:
 * int rh(time=2, lat=3, lon=4);
 * :long_name = "relative humidity";
 * :units = "percent";
 * double T(time=2, lat=3, lon=4);
 * :long_name = "surface temperature";
 * :units = "degC";
 * float lat(lat=3);
 * :units = "degrees_north";
 * float lon(lon=4);
 * :units = "degrees_east";
 * int time(time=2);
 * :units = "hours";
 * 
 * :title = "Example Data";
 * }
 * 
 * netcdf C:/dev/tds/thredds/cdm/src/test/data/ncml/nc/example2.nc {
 * dimensions:
 * time = UNLIMITED; // (2 currently)
 * lat = 3;
 * lon = 4;
 * variables:
 * int rh(time=2, lat=3, lon=4);
 * :long_name = "relative humidity";
 * :units = "percent";
 * double T(time=2, lat=3, lon=4);
 * :long_name = "surface temperature";
 * :units = "degC";
 * float lat(lat=3);
 * :units = "degrees_north";
 * float lon(lon=4);
 * :units = "degrees_east";
 * int time(time=2);
 * :units = "hours";
 * 
 * :title = "Example Data";
 * }
 */

public class TestAggUnion {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  static NetcdfFile ncfile = null;

  @BeforeClass
  public static void setUp() throws IOException {
    if (ncfile != null)
      return;
    String filename = "file:./" + TestNcmlRead.topDir + "aggUnion.xml";
    ncfile = NcmlReader.readNcml(filename, null, null).build();
  }

  @AfterClass
  public static void tearDown() throws IOException {
    if (ncfile != null)
      ncfile.close();
    ncfile = null;
  }

  @Test
  public void testDataset() {
    Variable v = ncfile.findVariable("ReletiveHumidity");
    assert v instanceof VariableDS;
    VariableDS vds = (VariableDS) v;
    assert vds.getOriginalArrayType() == v.getArrayType();

    Variable org = vds.getOriginalVariable();
    assert org.getShortName().equals("rh");
    assert vds.getOriginalArrayType() == org.getArrayType();

    assert v.getParentGroup().getFullName().equals(org.getParentGroup().getFullName());
  }

  @Test
  public void testMetadata() {
    logger.debug("TestNested = \n{}", ncfile);

    Attribute att = ncfile.findAttribute("title");
    assert null != att;
    assert !att.isArray();
    assert att.isString();
    assert att.getArrayType() == ArrayType.STRING;
    assert att.getStringValue().equals("Example Data");
    assert att.getNumericValue() == null;
    assert att.getNumericValue(3) == null;

    Dimension latDim = ncfile.findDimension("lat");
    assert null != latDim;
    assert latDim.getShortName().equals("lat");
    assert latDim.getLength() == 3;
    assert !latDim.isUnlimited();

    Dimension timeDim = ncfile.findDimension("time");
    assert null != timeDim;
    assert timeDim.getShortName().equals("time");
    assert timeDim.getLength() == 2;
    assert timeDim.isUnlimited();
  }

  @Test
  public void testCoordvar() throws IOException {
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
    Assert2.assertNearlyEquals(data.get(0).doubleValue(), 41.0);
    Assert2.assertNearlyEquals(data.get(1).doubleValue(), 40.0);
    Assert2.assertNearlyEquals(data.get(2).doubleValue(), 39.0);
  }

  @Test
  public void testReadData() throws IOException {
    Variable v = ncfile.findVariable("ReletiveHumidity");
    assert null != v;
    assert v.getShortName().equals("ReletiveHumidity");
    assert v.getRank() == 3;
    assert v.getSize() == 24;
    assert v.getShape()[0] == 2;
    assert v.getShape()[1] == 3;
    assert v.getShape()[2] == 4;
    assert v.getArrayType() == ArrayType.INT;

    assert !v.isCoordinateVariable();
    assert v.isUnlimited();

    assert v.getDimension(0).equals(ncfile.findDimension("time"));
    assert v.getDimension(1).equals(ncfile.findDimension("lat"));
    assert v.getDimension(2).equals(ncfile.findDimension("lon"));

    Attribute att = v.findAttribute("units");
    assert null != att;
    assert !att.isArray();
    assert att.isString();
    assert att.getArrayType() == ArrayType.STRING;
    assert att.getStringValue().equals("percent");
    assert att.getNumericValue() == null;
    assert att.getNumericValue(3) == null;

    Array<Number> data = (Array<Number>) v.readArray();
    assert data.getRank() == 3;
    assert data.getSize() == 24 : data.getSize();
    assert data.getShape()[0] == 2;
    assert data.getShape()[1] == 3;
    assert data.getShape()[2] == 4;
    System.out.printf("testReadData = %s%n", NcdumpArray.printArray(data));

    int count = 0;
    for (Number val : data) {
      int expected = (count < 12) ? count + 1 : count + 9;
      assertThat(val).isEqualTo(expected);
      count++;
    }
  }

  @Test
  public void testReadSlice() throws IOException, InvalidRangeException {
    Variable v = ncfile.findVariable("ReletiveHumidity");
    int[] origin = new int[3];
    int[] shape = {2, 3, 1};

    Array<Number> data = (Array<Number>) v.readArray(new Section(origin, shape));
    assert data.getRank() == 3;
    assert data.getSize() == 6;
    assert data.getShape()[0] == 2;
    assert data.getShape()[1] == 3;
    assert data.getShape()[2] == 1;

    int[] expected = new int[] {1, 5, 9, 21, 25, 29};
    int count = 0;
    for (Number val : data) {
      assertThat(val).isEqualTo(expected[count++]);
    }
  }

  @Test
  public void testReadSlice2() throws IOException, InvalidRangeException {
    Variable v = ncfile.findVariable("ReletiveHumidity");
    int[] origin = new int[3];
    int[] shape = {2, 1, 3};

    Array<Number> data = (Array<Number>) v.readArray(new Section(origin, shape));
    data = Arrays.reduce(data);
    assert data.getRank() == 2;
    assert data.getSize() == 6;
    assert data.getShape()[0] == 2;
    assert data.getShape()[1] == 3;

    int[] expected = new int[] {1, 2, 3, 21, 22, 23};
    int count = 0;
    for (Number val : data) {
      assertThat(val).isEqualTo(expected[count++]);
    }
  }

  @Test
  public void testReadDataAlias() throws IOException {

    Variable v = ncfile.findVariable("T");
    assert null == v;

    v = ncfile.findVariable("Temperature");
    assert null != v;
    assert v.getShortName().equals("Temperature");
    assert v.getRank() == 3;
    assert v.getSize() == 24;
    assert v.getShape()[0] == 2;
    assert v.getShape()[1] == 3;
    assert v.getShape()[2] == 4;
    assert v.getArrayType() == ArrayType.DOUBLE;

    assert !v.isCoordinateVariable();
    assert v.isUnlimited();

    assert v.getDimension(0).equals(ncfile.findDimension("time"));
    assert v.getDimension(1).equals(ncfile.findDimension("lat"));
    assert v.getDimension(2).equals(ncfile.findDimension("lon"));

    Attribute att = v.findAttribute("units");
    assert null != att;
    assert !att.isArray();
    assert att.isString();
    assert att.getArrayType() == ArrayType.STRING;
    assert att.getStringValue().equals("degC");
    assert att.getNumericValue() == null;
    assert att.getNumericValue(3) == null;

    Array<Number> data = (Array<Number>) v.readArray();
    assert data.getRank() == 3;
    assert data.getSize() == 24;
    assert data.getShape()[0] == 2;
    assert data.getShape()[1] == 3;
    assert data.getShape()[2] == 4;
    System.out.printf("testReadDataAlias = %s%n", NcdumpArray.printArray(data));

    double[] expected =
        new double[] {1, 2, 3, 4, 2, 4, 6, 8, 3, 6, 9, 12, 2.5, 5.0, 7.5, 10.0, 5, 10, 15, 20, 7.5, 15, 22.5, 30};
    int count = 0;
    for (Number val : data) {
      assertThat(val).isEqualTo(expected[count++]);
    }
  }
}
