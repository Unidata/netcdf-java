/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.ncml;

import static ucar.nc2.util.Misc.nearlyEquals;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.array.ArrayType;
import ucar.ma2.IndexIterator;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.unidata.util.test.TestDir;

@RunWith(Parameterized.class)
public class TestNcmlRead {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static String topDir = TestDir.cdmLocalTestDataDir + "ncml/";

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> getTestParameters() {
    Collection<Object[]> filenames = new ArrayList<>();
    filenames.add(new Object[] {"testRead.xml"});
    filenames.add(new Object[] {"readMetadata.xml"});
    filenames.add(new Object[] {"testReadHttps.xml"});
    return filenames;
  }

  private String ncmlLocation;
  private NetcdfFile ncfile;

  public TestNcmlRead(String filename) {
    this.ncmlLocation = "file:" + topDir + filename;
    try {
      ncfile = NcmlReader.readNcml(ncmlLocation, null, null).build();
    } catch (java.net.MalformedURLException e) {
      System.out.println("bad URL error = " + e);
    } catch (IOException e) {
      System.out.println("IO error = " + e);
      e.printStackTrace();
    }
  }

  @After
  public void tearDown() throws IOException {
    ncfile.close();
  }

  @Test
  public void testStructure() {
    System.out.println("ncfile opened = " + ncmlLocation + "\n" + ncfile);

    Attribute att = ncfile.findAttribute("title");
    assert null != att;
    assert !att.isArray();
    assert att.isString();
    assert att.getArrayType() == ArrayType.STRING;
    assert att.getStringValue().equals("Example Data");
    assert att.getNumericValue() == null;
    assert att.getNumericValue(3) == null;

    att = ncfile.findAttribute("testFloat");
    assert null != att;
    assert att.isArray();
    assert !att.isString();
    assert att.getArrayType() == ArrayType.FLOAT;
    assert att.getStringValue() == null;
    assert att.getNumericValue().equals(1.0f);
    assert att.getNumericValue(3).equals(4.0f);

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
  public void testReadCoordvar() {
    Variable lat = ncfile.findVariable("lat");
    assert null != lat;
    assert lat.getShortName().equals("lat");
    assert lat.getRank() == 1;
    assert lat.getSize() == 3;
    assert lat.getShape()[0] == 3;
    assert lat.getArrayType() == ArrayType.FLOAT;

    assert !lat.isUnlimited();

    assert lat.getDimension(0) == ncfile.findDimension("lat");

    Attribute att = lat.findAttribute("units");
    assert null != att;
    assert !att.isArray();
    assert att.isString();
    assert att.getArrayType() == ArrayType.STRING;
    assert att.getStringValue().equals("degrees_north");
    assert att.getNumericValue() == null;
    assert att.getNumericValue(3) == null;

    try {
      Array data = lat.read();
      assert data.getRank() == 1;
      assert data.getSize() == 3;
      assert data.getShape()[0] == 3;
      assert data.getElementType() == float.class;

      IndexIterator dataI = data.getIndexIterator();
      assert nearlyEquals(dataI.getDoubleNext(), 41.0);
      assert nearlyEquals(dataI.getDoubleNext(), 40.0);
      assert nearlyEquals(dataI.getDoubleNext(), 39.0);
    } catch (IOException io) {
    }

  }

  @Test
  public void testReadData() throws Exception {
    Variable v = ncfile.findVariable("rh");
    assert null != v;
    assert v.getShortName().equals("rh");
    assert v.getRank() == 3;
    assert v.getSize() == 24;
    assert v.getShape()[0] == 2;
    assert v.getShape()[1] == 3;
    assert v.getShape()[2] == 4;
    assert v.getArrayType() == ArrayType.INT;

    assert !v.isCoordinateVariable();
    assert v.isUnlimited();

    assert v.getDimension(0) == ncfile.findDimension("time");
    assert v.getDimension(1) == ncfile.findDimension("lat");
    assert v.getDimension(2) == ncfile.findDimension("lon");

    Attribute att = v.findAttribute("units");
    assert null != att;
    assert !att.isArray();
    assert att.isString();
    assert att.getArrayType() == ArrayType.STRING;
    assert att.getStringValue().equals("percent");
    assert att.getNumericValue() == null;
    assert att.getNumericValue(3) == null;

    Array data = v.read();
    assert data.getRank() == 3;
    assert data.getSize() == 24;
    assert data.getShape()[0] == 2;
    assert data.getShape()[1] == 3;
    assert data.getShape()[2] == 4;
    assert data.getElementType() == int.class;

    IndexIterator dataI = data.getIndexIterator();
    assert dataI.getIntNext() == 1;
    assert dataI.getIntNext() == 2;
    assert dataI.getIntNext() == 3;
    assert dataI.getIntNext() == 4;
    assert dataI.getIntNext() == 5;
  }

  @Test
  public void testReadSlice() throws Exception {
    Variable v = ncfile.findVariable("rh");
    int[] origin = new int[3];
    int[] shape = {2, 3, 1};

    Array data = v.read(origin, shape);
    assert data.getRank() == 3;
    assert data.getSize() == 6;
    assert data.getShape()[0] == 2;
    assert data.getShape()[1] == 3;
    assert data.getShape()[2] == 1;
    assert data.getElementType() == int.class;

    IndexIterator dataI = data.getIndexIterator();
    assert dataI.getIntNext() == 1;
    assert dataI.getIntNext() == 5;
    assert dataI.getIntNext() == 9;
    assert dataI.getIntNext() == 21;
    assert dataI.getIntNext() == 25;
    assert dataI.getIntNext() == 29;
  }

  @Test
  public void testReadSlice2() throws Exception {
    Variable v = ncfile.findVariable("rh");
    int[] origin = new int[3];
    int[] shape = {2, 1, 3};

    Array data = v.read(origin, shape).reduce();
    assert data.getRank() == 2;
    assert data.getSize() == 6;
    assert data.getShape()[0] == 2;
    assert data.getShape()[1] == 3;
    assert data.getElementType() == int.class;

    IndexIterator dataI = data.getIndexIterator();
    assert dataI.getIntNext() == 1;
    assert dataI.getIntNext() == 2;
    assert dataI.getIntNext() == 3;
    assert dataI.getIntNext() == 21;
    assert dataI.getIntNext() == 22;
    assert dataI.getIntNext() == 23;

  }

  @Test
  public void testReadDataAlias() throws Exception {
    Variable v = ncfile.findVariable("T");
    assert null != v;
    assert v.getShortName().equals("T");
    assert v.getRank() == 3;
    assert v.getSize() == 24;
    assert v.getShape()[0] == 2;
    assert v.getShape()[1] == 3;
    assert v.getShape()[2] == 4;
    assert v.getArrayType() == ArrayType.DOUBLE;

    assert !v.isCoordinateVariable();
    assert v.isUnlimited();

    assert v.getDimension(0) == ncfile.findDimension("time");
    assert v.getDimension(1) == ncfile.findDimension("lat");
    assert v.getDimension(2) == ncfile.findDimension("lon");

    Attribute att = v.findAttribute("units");
    assert null != att;
    assert !att.isArray();
    assert att.isString();
    assert att.getArrayType() == ArrayType.STRING : att.getArrayType();
    assert att.getStringValue().equals("degC");
    assert att.getNumericValue() == null;
    assert att.getNumericValue(3) == null;

    Array data = v.read();
    assert data.getRank() == 3;
    assert data.getSize() == 24;
    assert data.getShape()[0] == 2;
    assert data.getShape()[1] == 3;
    assert data.getShape()[2] == 4;
    assert data.getElementType() == double.class;

    IndexIterator dataI = data.getIndexIterator();
    assert nearlyEquals(dataI.getDoubleNext(), 1.0);
    assert nearlyEquals(dataI.getDoubleNext(), 2.0);
    assert nearlyEquals(dataI.getDoubleNext(), 3.0);
    assert nearlyEquals(dataI.getDoubleNext(), 4.0);
    assert nearlyEquals(dataI.getDoubleNext(), 2.0);
  }

}
