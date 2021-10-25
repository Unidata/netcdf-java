/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.ncml;

import java.io.IOException;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.unidata.util.test.Assert2;

/** Test NcML AggExisting ways to define coordinate variable values */
public class TestAggExistingCoordVars {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testType1() throws IOException {
    String filename = "file:./" + TestNcmlRead.topDir + "aggExisting1.xml";

    NetcdfDataset ncfile = NetcdfDatasets.openDataset(filename, false, null);
    logger.debug(" TestNcmlAggExisting.open {}", filename);

    Variable time = ncfile.findVariable("time");
    assert null != time;

    assert time.getShortName().equals("time");
    assert time.getRank() == 1;
    assert time.getSize() == 59;
    assert time.getShape()[0] == 59;
    assert time.getDataType() == DataType.INT;

    assert time.getDimension(0) == ncfile.findDimension("time");

    Array data = time.read();
    assert data.getRank() == 1;
    assert data.getSize() == 59;
    assert data.getShape()[0] == 59;
    assert data.getElementType() == int.class;

    int count = 0;
    IndexIterator dataI = data.getIndexIterator();
    while (dataI.hasNext()) {
      assert dataI.getIntNext() == 7 + 2 * count;
      count++;
    }

    ncfile.close();
  }


  String aggExisting2 = "<?xml version='1.0' encoding='UTF-8'?>\n"
      + "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" // leavit
      + "   <variable name='time'>\n" // leavit
      + "     <attribute name='units' value='hours since 2006-06-16 00:00'/>\n" // leavit
      + "     <attribute name='_CoordinateAxisType' value='Time' />\n" // leavit
      + "   </variable>\n" // leavit
      + "  <aggregation type='joinExisting' dimName='time' >\n" // leavit
      + "    <netcdf location='nc/cg/CG2006158_120000h_usfc.nc' ncoords='1' coordValue='12' />\n" // leavit
      + "    <netcdf location='nc/cg/CG2006158_130000h_usfc.nc' ncoords='1' coordValue='13' />\n" // leavit
      + "    <netcdf location='nc/cg/CG2006158_140000h_usfc.nc' ncoords='1' coordValue='14' />\n" // leavit
      + "  </aggregation>\n" // leavit
      + "</netcdf>";

  @Test
  public void testType2() throws IOException {
    String filename = "file:./" + TestNcmlRead.topDir + "aggExisting2.xml";

    NetcdfDataset ncfile = NetcdfDatasets.openNcmlDataset(new StringReader(aggExisting2), filename, null);
    logger.debug(" TestNcmlAggExisting.open {}\n{}", filename, ncfile);

    Variable time = ncfile.findVariable("time");
    assert null != time;

    assert time.getShortName().equals("time");
    assert time.getRank() == 1;
    assert time.getSize() == 3;
    assert time.getShape()[0] == 3;
    assert time.getDataType() == DataType.DOUBLE;

    assert time.getDimension(0) == ncfile.findDimension("time");

    double[] result = new double[] {12, 13, 14};

    Array data = time.read();
    assert data.getRank() == 1;
    assert data.getSize() == 3;
    assert data.getShape()[0] == 3;
    assert data.getElementType() == double.class;

    int count = 0;
    IndexIterator dataI = data.getIndexIterator();
    while (dataI.hasNext()) {
      double val = dataI.getDoubleNext();
      Assert2.assertNearlyEquals(val, result[count]);
      count++;
    }

    ncfile.close();
  }

  @Test
  public void testType3() throws IOException {
    String filename = "file:./" + TestNcmlRead.topDir + "aggExisting.xml";

    NetcdfDataset ncfile = NetcdfDatasets.openDataset(filename, false, null);
    logger.debug(" TestNcmlAggExisting.open {}\n{}", filename, ncfile);

    Variable time = ncfile.findVariable("time");
    assert null != time;

    String testAtt = time.findAttributeString("ncmlAdded", null);
    assert testAtt != null;
    assert testAtt.equals("timeAtt");

    assert time.getShortName().equals("time");
    assert time.getRank() == 1;
    assert time.getSize() == 59;
    assert time.getShape()[0] == 59;
    assert time.getDataType() == DataType.INT;

    assert time.getDimension(0) == ncfile.findDimension("time");

    Array data = time.read();
    assert data.getRank() == 1;
    assert data.getSize() == 59;
    assert data.getShape()[0] == 59;
    assert data.getElementType() == int.class;

    int count = 0;
    IndexIterator dataI = data.getIndexIterator();
    while (dataI.hasNext())
      assert dataI.getIntNext() == count++;

    ncfile.close();
  }

  @Test
  public void testType4() throws IOException {
    String filename = "file:" + TestNcmlRead.topDir + "aggExisting4.ncml";

    NetcdfDataset ncfile = NetcdfDatasets.openDataset(filename, false, null);

    Variable time = ncfile.findVariable("time");
    assert null != time;

    assert time.getShortName().equals("time");
    assert time.getRank() == 1;
    assert time.getSize() == 3;
    assert time.getShape()[0] == 3;
    assert time.getDataType() == DataType.DOUBLE;

    assert time.getDimension(0) == ncfile.findDimension("time");

    double[] result = new double[] {1.1496816E9, 1.1496852E9, 1.1496888E9};

    Array data = time.read();
    assert data.getRank() == 1;
    assert data.getSize() == 3;
    assert data.getShape()[0] == 3;
    assert data.getElementType() == double.class;

    int count = 0;
    IndexIterator dataI = data.getIndexIterator();
    while (dataI.hasNext()) {
      Assert2.assertNearlyEquals(dataI.getDoubleNext(), result[count]);
      count++;
    }

    ncfile.close();
  }

  @Test
  public void testWithDateFormatMark() throws Exception {
    String filename = "file:" + TestNcmlRead.topDir + "aggExistingOne.xml";
    NetcdfDataset ncfile = NetcdfDatasets.openDataset(filename, false, null);

    Variable time = ncfile.findVariable("time");
    assert null != time;

    assert time.getShortName().equals("time");
    assert time.getRank() == 1;
    assert time.getSize() == 3;
    assert time.getShape()[0] == 3;
    assert time.getDataType() == DataType.STRING;

    assert time.getDimension(0) == ncfile.findDimension("time");

    String[] result = new String[] {"2006-06-07T12:00Z", "2006-06-07T13:00Z", "2006-06-07T14:00Z"};

    Array data = time.read();
    assert data.getRank() == 1;
    assert data.getSize() == 3;
    assert data.getShape()[0] == 3;
    assert data.getElementType() == String.class;

    int count = 0;
    IndexIterator dataI = data.getIndexIterator();
    while (dataI.hasNext()) {
      String val = (String) dataI.getObjectNext();
      assert val.equals(result[count]) : val + " != " + result[count];
      count++;
    }

    ncfile.close();
  }

  @Test
  public void testClimatologicalDate() throws IOException {
    String filename = "file:" + TestNcmlRead.topDir + "aggExisting5.ncml";
    NetcdfDataset ncfile = NetcdfDatasets.openDataset(filename, false, null);

    Variable time = ncfile.findVariable("time");
    assert null != time;

    assert time.getShortName().equals("time");
    assert time.getRank() == 1;
    assert time.getSize() == 59;
    assert time.getShape()[0] == 59;
    assert time.getDataType() == DataType.INT;

    assert time.getDimension(0) == ncfile.findDimension("time");

    Array data = time.read();
    assert data.getRank() == 1;
    assert data.getSize() == 59;
    assert data.getShape()[0] == 59;
    assert data.getElementType() == int.class;

    int count = 0;
    while (data.hasNext()) {
      assert data.nextInt() == count : data.nextInt() + "!=" + count;
      count++;
    }

    ncfile.close();
  }
}
