/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.ncml;

import static ucar.nc2.util.Misc.nearlyEquals;
import java.io.IOException;
import java.util.Iterator;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

public class TestNcmlReadOverride {

  static NetcdfFile ncfile = null;

  @BeforeClass
  public static void setUp() {
    if (ncfile != null)
      return;
    String filename = "file:./" + TestNcmlRead.topDir + "testReadOverride.xml";

    try {
      ncfile = NcmlReader.readNcml(filename, null, null).build();
    } catch (java.net.MalformedURLException e) {
      System.out.println("bad URL error = " + e);
    } catch (IOException e) {
      System.out.println("IO error = " + e);
      e.printStackTrace();
    }
  }

  @AfterClass
  public static void tearDown() throws IOException {
    ncfile.close();
    ncfile = null;
  }

  @Test
  public void testRemoved() {
    // rh was removed
    Variable v = ncfile.findVariable("rh");
    assert null == v;
  }

  @Test
  public void testReadReplaced() {
    Variable v = ncfile.findVariable("time");
    assert null != v;
    assert v.getShortName().equals("time");
    assert v.getRank() == 1;
    assert v.getSize() == 2;
    assert v.getShape()[0] == 2;
    assert v.getArrayType() == ArrayType.DOUBLE;

    assert v.isUnlimited();
    assert v.getDimension(0) == ncfile.findDimension("time");

    Attribute att = v.findAttribute("units");
    assert null != att;
    assert !att.isArray();
    assert att.isString();
    assert att.getArrayType() == ArrayType.STRING;
    assert att.getStringValue().equals("days");
    assert att.getNumericValue() == null;
    assert att.getNumericValue(3) == null;

    try {
      Array data = v.readArray();
      assert data.getRank() == 1;
      assert data.getSize() == 2;
      assert data.getShape()[0] == 2;
      Iterator<Double> dataI = data.iterator();

      assert nearlyEquals(dataI.next(), 0.5);
      assert nearlyEquals(dataI.next(), 1.5);
      try {
        dataI.next();
        assert (false);
      } catch (Exception e) {
      }
    } catch (IOException io) {
    }
  }

  @Test
  public void testReadData() {
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
    assert att.getArrayType() == ArrayType.STRING;
    assert att.getStringValue().equals("degC");
    assert att.getNumericValue() == null;
    assert att.getNumericValue(3) == null;

    try {
      Array data = v.readArray();
      assert data.getRank() == 3;
      assert data.getSize() == 24;
      assert data.getShape()[0] == 2;
      assert data.getShape()[1] == 3;
      assert data.getShape()[2] == 4;
      Iterator<Double> dataI = data.iterator();

      assert nearlyEquals(dataI.next(), 1.0);
      assert nearlyEquals(dataI.next(), 2.0);
      assert nearlyEquals(dataI.next(), 3.0);
      assert nearlyEquals(dataI.next(), 4.0);
      assert nearlyEquals(dataI.next(), 2.0);
    } catch (IOException io) {
    }
  }

}
