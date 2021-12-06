/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.ncml;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Formatter;
import java.util.Iterator;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.array.Section;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.TestUtils;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.internal.util.CompareNetcdf2;
import ucar.nc2.util.Misc;

import static com.google.common.truth.Truth.assertThat;

/** Test agg union */

/*
 * <netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2">
 * <attribute name="title" type="string" value="Union cldc and lflx"/>
 * <aggregation type="union">
 * <netcdf location="file:src/test/data/ncml/nc/cldc.mean.nc"/>
 * <netcdf location="file:src/test/data/ncml/nc/lflx.mean.nc"/>
 * </aggregation>
 * </netcdf>
 * 
 * netcdf C:/dev/tds/thredds/cdm/src/test/data/ncml/nc/cldc.mean.nc {
 * dimensions:
 * time = UNLIMITED; // (456 currently)
 * lat = 21;
 * lon = 360;
 * variables:
 * float lat(lat=21);
 * :long_name = "Latitude";
 * :units = "degrees_north";
 * :actual_range = 10.0f, -10.0f; // float
 * float lon(lon=360);
 * :long_name = "Longitude";
 * :units = "degrees_east";
 * :actual_range = 0.5f, 359.5f; // float
 * double time(time=456);
 * :units = "days since 1-1-1 00:00:0.0";
 * :long_name = "Time";
 * :delta_t = "0000-01-00 00:00:00";
 * :avg_period = "0000-01-00 00:00:00";
 * :actual_range = 715511.0, 729360.0; // double
 * short cldc(time=456, lat=21, lon=360);
 * :valid_range = 0.0f, 8.0f; // float
 * :actual_range = 0.0f, 8.0f; // float
 * :units = "okta";
 * :precision = 1s; // short
 * :missing_value = 32766s; // short
 * :_FillValue = 32766s; // short
 * :long_name = "Cloudiness Monthly Mean at Surface";
 * :dataset = "COADS 1-degree Equatorial Enhanced\nAI";
 * :var_desc = "Cloudiness\nC";
 * :level_desc = "Surface\n0";
 * :statistic = "Mean\nM";
 * :parent_stat = "Individual Obs\nI";
 * :add_offset = 3276.5f; // float
 * :scale_factor = 0.1f; // float
 * 
 * :title = "COADS 1-degree Equatorial Enhanced";
 * :history = "";
 * :Conventions = "COARDS";
 * }
 * 
 * netcdf C:/dev/tds/thredds/cdm/src/test/data/ncml/nc/lflx.mean.nc {
 * dimensions:
 * time = UNLIMITED; // (456 currently)
 * lat = 21;
 * lon = 360;
 * variables:
 * float lat(lat=21);
 * :long_name = "Latitude";
 * :units = "degrees_north";
 * :actual_range = 10.0f, -10.0f; // float
 * float lon(lon=360);
 * :long_name = "Longitude";
 * :units = "degrees_east";
 * :actual_range = 0.5f, 359.5f; // float
 * double time(time=456);
 * :units = "days since 1-1-1 00:00:0.0";
 * :long_name = "Time";
 * :delta_t = "0000-01-00 00:00:00";
 * :avg_period = "0000-01-00 00:00:00";
 * :actual_range = 715511.0, 729360.0; // double
 * short lflx(time=456, lat=21, lon=360);
 * :valid_range = -1000.0f, 1000.0f; // float
 * :actual_range = -88.700005f, 236.1f; // float
 * :units = "grams/kg m/s";
 * :precision = 1s; // short
 * :missing_value = 32766s; // short
 * :_FillValue = 32766s; // short
 * :long_name = "Latent Heat Parameter Monthly Mean at Surface";
 * :dataset = "COADS 1-degree Equatorial Enhanced\nAI";
 * :var_desc = "Latent Heat Parameter\nG";
 * :level_desc = "Surface\n0";
 * :statistic = "Mean\nM";
 * :parent_stat = "Individual Obs\nI";
 * :add_offset = 2276.5f; // float
 * :scale_factor = 0.1f; // float
 * 
 * :title = "COADS 1-degree Equatorial Enhanced";
 * :history = "";
 * :Conventions = "COARDS";
 * }
 */

public class TestAggUnionSimple {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  static NetcdfFile ncfile = null;

  @BeforeClass
  public static void setUp() throws IOException {
    if (ncfile != null)
      return;
    String filename = "file:./" + TestNcmlRead.topDir + "aggUnionSimple.xml";
    ncfile = NetcdfDatasets.openDataset(filename, false, null);
  }

  @AfterClass
  public static void tearDown() throws IOException {
    if (ncfile != null)
      ncfile.close();
    ncfile = null;
  }

  @Test
  public void testDataset() {
    Variable v = ncfile.findVariable("lflx");
    assert v instanceof VariableDS;
    VariableDS vds = (VariableDS) v;
    assert vds.getOriginalArrayType() == v.getArrayType();

    Variable org = vds.getOriginalVariable();
    assert vds.getOriginalArrayType() == org.getArrayType();

    assert !(org instanceof VariableDS);

    assert v.getParentGroup().getFullName().equals(org.getParentGroup().getFullName());
  }

  @Test
  public void testRead() {
    logger.debug("ncfile = \n{}", ncfile);
    TestUtils.testReadData(ncfile, true);
  }

  @Test
  public void testStructure() {
    logger.debug("TestNested = \n{}", ncfile);

    Attribute att = ncfile.findAttribute("title");
    assert null != att;
    assert !att.isArray();
    assert att.isString();
    assert att.getArrayType() == ArrayType.STRING;
    assert att.getStringValue().equals("Union cldc and lflx");
    assert att.getNumericValue() == null;
    assert att.getNumericValue(3) == null;

    Dimension latDim = ncfile.findDimension("lat");
    assert null != latDim;
    assert latDim.getShortName().equals("lat");
    assert latDim.getLength() == 21;
    assert !latDim.isUnlimited();

    Dimension timeDim = ncfile.findDimension("time");
    assert null != timeDim;
    assert timeDim.getShortName().equals("time");
    assert timeDim.getLength() == 456;
    assert timeDim.isUnlimited();
  }

  @Test
  public void testReadCoordvar() throws IOException {
    Variable lat = ncfile.findVariable("lat");
    assert null != lat;
    assert lat.getShortName().equals("lat");
    assert lat.getRank() == 1;
    assert lat.getSize() == 21;
    assert lat.getShape()[0] == 21;
    assert lat.getArrayType() == ArrayType.FLOAT;

    assert lat.isCoordinateVariable();
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
    assert data.getSize() == 21;
    assert data.getShape()[0] == 21;
    assertThat(Misc.nearlyEquals(data.get(0).doubleValue(), 10.0)).isTrue();
    assertThat(Misc.nearlyEquals(data.get(1).doubleValue(), 9.0)).isTrue();
    assertThat(Misc.nearlyEquals(data.get(2).doubleValue(), 8.0)).isTrue();
  }

  @Test
  public void testReadData() throws IOException {
    Variable v = ncfile.findVariable("lflx");
    assert null != v;
    assert v.getShortName().equals("lflx");
    assert v.getRank() == 3;
    assert v.getSize() == 360 * 21 * 456;
    assert v.getShape()[0] == 456;
    assert v.getShape()[1] == 21;
    assert v.getShape()[2] == 360;
    assert v.getArrayType() == ArrayType.SHORT : v.getArrayType();

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
    assert att.getStringValue().equals("grams/kg m/s");
    assert att.getNumericValue() == null;
    assert att.getNumericValue(3) == null;

    Array<Short> data = (Array<Short>) v.readArray();
    assert data.getRank() == 3;
    assert data.getSize() == 360 * 21 * 456;
    assert data.getShape()[0] == 456;
    assert data.getShape()[1] == 21;
    assert data.getShape()[2] == 360;
    Iterator<Short> iter = data.iterator();

    assert 32766 == iter.next();
    assert 32766 == iter.next();
    assert 32766 == iter.next();
    assert 32766 == iter.next();
  }

  @Test
  public void testReadSlice() throws Exception {
    Variable v = ncfile.findVariable("lflx");
    int[] origin = {0, 6, 5};
    int[] shape = {1, 2, 3};

    Array<Short> data = (Array<Short>) Arrays.reduce(v.readArray(new Section(origin, shape)));
    assert data.getRank() == 2;
    assert data.getSize() == 6;
    assert data.getShape()[0] == 2;
    assert data.getShape()[1] == 3;
    Iterator<Short> iter = data.iterator();

    assert iter.next() == -22711;
    assert iter.next() == -22239;
    assert iter.next() == -22585;
    assert iter.next() == -22670;
    assert iter.next() == 32766;
    assert iter.next() == 32766;
  }

  /*
   * test that scanning gives the exact same result
   * <aggregation type="union">
   * <scan location="file:src/test/data/ncml/nc/" suffix="mean.nc"/>
   * </aggregation>
   */
  @Test
  public void testScan() throws IOException {
    String filename = "file:./" + TestNcmlRead.topDir + "aggUnionScan.xml";
    try (NetcdfDataset scanFile = NetcdfDatasets.openDataset(filename, false, null)) {
      Formatter errlog = new Formatter();
      boolean ok = CompareNetcdf2.compareFiles(ncfile, scanFile, errlog, true, false, false);
      if (!ok) {
        System.out.printf("%s%n", errlog);
      }
      assertThat(ok).isTrue();
    }
  }

  @Test
  public void testRename() throws IOException {
    String filename = "file:./" + TestNcmlRead.topDir + "aggUnionRename.xml";
    try (NetcdfDataset scanFile = NetcdfDatasets.openDataset(filename, false, null)) {
      Variable v = scanFile.findVariable("LavaFlow");
      assert v != null;
    }
  }
}
