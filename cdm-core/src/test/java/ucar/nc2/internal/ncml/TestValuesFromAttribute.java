/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.ncml;

import java.io.IOException;
import java.io.StringReader;
import org.junit.Test;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;

/** Test reading and processing NcML attributes */
public class TestValuesFromAttribute {

  private String ncml = "<?xml version='1.0' encoding='UTF-8'?>\n"
      + "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2' location='file:src/test/data/ncml/nc/lflx.mean.nc'>\n" // leaavit
      + "   <variable name='titleAsVariable' type='String' shape=''>\n" // leaavit
      + "     <values fromAttribute='title'/>\n" // leaavit
      + "   </variable>\n" // leaavit
      + "   <variable name='titleAsVariable2' type='String' shape=''>\n" // leaavit
      + "     <values fromAttribute='@title'/>\n" // leaavit
      + "   </variable>\n" // leaavit
      + "   <variable name='VariableAttribute' type='double' shape='2'>\n" // leaavit
      + "     <values fromAttribute='time@actual_range'/>\n" // leaavit
      + "   </variable>\n" + "</netcdf>"; // leaavit

  @Test
  public void testValuesFromAttribute() throws IOException {
    String filename = "file:./" + TestNcmlRead.topDir + "TestValuesFromAttribute.xml";

    try (NetcdfDataset ncfile = NetcdfDatasets.openNcmlDataset(new StringReader(ncml), filename, null)) {
      System.out.println(" TestNcmlAggExisting.open " + filename + "\n" + ncfile);

      Variable newVar = ncfile.findVariable("titleAsVariable");
      assert null != newVar;

      assert newVar.getShortName().equals("titleAsVariable");
      assert newVar.getRank() == 0;
      assert newVar.getSize() == 1;
      assert newVar.getArrayType() == ArrayType.STRING;

      Array<String> data = (Array<String>) newVar.readArray();
      assert data.get(0).equals("COADS 1-degree Equatorial Enhanced");

      ////////////////
      newVar = ncfile.findVariable("titleAsVariable2");
      assert null != newVar;

      assert newVar.getShortName().equals("titleAsVariable2");
      assert newVar.getRank() == 0;
      assert newVar.getSize() == 1;
      assert newVar.getArrayType() == ArrayType.STRING;

      data = (Array<String>) newVar.readArray();
      assert data.get(0).equals("COADS 1-degree Equatorial Enhanced");

      ///////////////
      newVar = ncfile.findVariable("VariableAttribute");
      assert null != newVar;

      assert newVar.getShortName().equals("VariableAttribute");
      assert newVar.getRank() == 1;
      assert newVar.getSize() == 2;
      assert newVar.getArrayType() == ArrayType.DOUBLE;

      Array<Double> ddata = (Array<Double>) newVar.readArray();
      assert ddata.getRank() == 1;
      assert ddata.getSize() == 2;

      double[] result = new double[] {715511.0, 729360.0};
      for (int i = 0; i < result.length; i++) {
        assert result[i] == ddata.get(i);
      }

    }
  }

}
