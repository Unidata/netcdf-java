/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 *  See LICENSE for license information.
 */
package ucar.nc2.internal.ncml;

import java.io.IOException;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.ncml.TestNcMLRead;

/** Test reading and processing NcML attributes */
public class TestValuesFromAttribute extends TestCase {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public TestValuesFromAttribute(String name) {
    super(name);
  }

  String ncml = "<?xml version='1.0' encoding='UTF-8'?>\n"
      + "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2' location='file:src/test/data/ncml/nc/lflx.mean.nc'>\n"
      + "   <variable name='titleAsVariable' type='String' shape=''>\n" // dont mess
      + "     <values fromAttribute='title'/>\n"                        // dont mess
      + "   </variable>\n"                                              // dont mess
      + "   <variable name='titleAsVariable2' type='String' shape=''>\n"// dont mess
      + "     <values fromAttribute='@title'/>\n"                        // dont mess
      + "   </variable>\n"                                              // dont mess
      + "   <variable name='VariableAttribute' type='double' shape='2'>\n"// dont mess
      + "     <values fromAttribute='time@actual_range'/>\n"             // dont mess
      + "   </variable>\n" + "</netcdf>";                                // dont mess

  public void testValuesFromAttribute() throws IOException, InvalidRangeException {
    String filename = "file:./" + TestNcMLRead.topDir + "TestValuesFromAttribute.xml";

    NetcdfFile ncfile = NcMLReaderNew.readNcML(new StringReader(ncml), filename, null).build();
    System.out.println(" TestNcmlAggExisting.open " + filename + "\n" + ncfile);

    Variable newVar = ncfile.findVariable("titleAsVariable");
    assert null != newVar;

    assert newVar.getShortName().equals("titleAsVariable");
    assert newVar.getRank() == 0;
    assert newVar.getSize() == 1;
    assert newVar.getDataType() == DataType.STRING;

    Array data = newVar.read();
    assert data.getElementType() == String.class;

    Object val = data.getObject(0);
    assert val instanceof String;
    assert val.equals("COADS 1-degree Equatorial Enhanced");

    ////////////////
    newVar = ncfile.findVariable("titleAsVariable2");
    assert null != newVar;

    assert newVar.getShortName().equals("titleAsVariable2");
    assert newVar.getRank() == 0;
    assert newVar.getSize() == 1;
    assert newVar.getDataType() == DataType.STRING;

    data = newVar.read();
    assert data.getElementType() == String.class;

    val = data.getObject(0);
    assert val instanceof String;
    assert val.equals("COADS 1-degree Equatorial Enhanced");

    ///////////////
    newVar = ncfile.findVariable("VariableAttribute");
    assert null != newVar;

    assert newVar.getShortName().equals("VariableAttribute");
    assert newVar.getRank() == 1;
    assert newVar.getSize() == 2;
    assert newVar.getDataType() == DataType.DOUBLE;

    data = newVar.read();
    assert data.getRank() == 1;
    assert data.getSize() == 2;
    assert data.getElementType() == double.class;

    double[] result = new double[] {715511.0, 729360.0};
    for (int i = 0; i < result.length; i++) {
      assert result[i] == data.getDouble(i);
    }

    ncfile.close();
  }

}
