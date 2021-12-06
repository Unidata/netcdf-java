/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.ncml;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import static com.google.common.truth.Truth.assertThat;

/** Test ncml value element in the JUnit framework. */

public class TestNcmlValues {

  NetcdfFile ncfile = null;
  String ncml = null;
  int expectedIntLength;
  int[] expectedIntShape = null;
  Array<Number> expectedIntValues = null;
  String[] intVarNames = null;

  @Before
  public void setUp() {
    ncml = "<?xml version='1.0' encoding='UTF-8'?>\n" // leavit
        + "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" // leavit
        + "   <dimension name='intDim' length='3' />\n" // leavit
        + "   <variable name='singleWs' type='int' shape='intDim'>\n" // leavit
        + "     <attribute name='description' value='Test Single White Space With Default Separator' />\n" // leavit
        + "     <values>0 1 2</values>\n" // leavit
        + "   </variable>\n" // leavit
        + "   <variable name='multiWs' type='int' shape='intDim'>\n" // leavit
        + "     <attribute name='description' value='Test Multi-length White Spaces With Default Separator' />\n" // leavit
        + "     <values>0    1  2</values>\n" // leavit
        + "   </variable>\n" // leavit
        + "   <variable name='tabs' type='int' shape='intDim'>\n" // leavit
        + "     <attribute name='description' value='Test Tab Spaces With Default Separator' />\n" // leavit
        + "     <values>0\t1\t2</values>\n" // leavit
        + "   </variable>\n" // leavit
        + "   <variable name='mixedTabSpace' type='int' shape='intDim'>\n" // leavit
        + "     <attribute name='description' value='Test Mixed Tab/Single-Space Spaces With Default Separator' />\n" // leavit
        + "     <values>0\t1 2</values>\n" // leavit
        + "   </variable>\n" // leavit
        + "   <variable name='mixedTabSpaces' type='int' shape='intDim'>\n" // leavit
        + "     <attribute name='description' value='Test Mixed Tab/Multi-Space Spaces With Default Separator' />\n" // leavit
        + "     <values>0\t1    2</values>\n" // leavit
        + "   </variable>\n" // leavit
        + "   <variable name='mixedSpace' type='int' shape='intDim'>\n" // leavit
        + "     <attribute name='description' value='Test Mixed Spaces With Default Separator' />\n" // leavit
        + "     <values>0\t  1\t    2</values>\n" // leavit
        + "   </variable>\n" // leavit
        + "   <variable name='customSep' type='int' shape='intDim'>\n" // leavit
        + "     <attribute name='description' value='Test Custom Separator' />\n" // leavit
        + "     <values separator='-'>0-1-2</values>\n" // leavit
        + "   </variable>\n" // leavit
        + "</netcdf>"; // leavit

    expectedIntLength = 3;
    expectedIntShape = new int[] {expectedIntLength};
    intVarNames =
        new String[] {"singleWs", "multiWs", "tabs", "mixedTabSpace", "mixedTabSpaces", "mixedSpace", "customSep"};
    int[] parray = new int[expectedIntLength];
    for (int i = 0; i < expectedIntLength; i++) {
      parray[i] = i;
    }
    expectedIntValues = Arrays.factory(ArrayType.INT, expectedIntShape, parray);

    try {
      ncfile = NcmlReader.readNcml(new StringReader(ncml), null, null).build();
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
  public void testIntVals() throws IOException {
    // build list of variables to test
    List<Variable> varList = new ArrayList<>();

    for (String varName : intVarNames) {
      varList.add(ncfile.findVariable(varName));
    }

    for (Variable var : varList) {
      System.out.println("  " + var.getDescription());
      Array<Number> values = (Array<Number>) var.readArray();

      assertThat(expectedIntShape).isEqualTo(values.getShape());
      assertThat(expectedIntLength).isEqualTo(values.getSize());
      assertThat(Arrays.equalNumbers(values, expectedIntValues)).isTrue();
    }
  }
}
