/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dods;

import java.util.ArrayList;
import opendap.dap.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.ArrayStructure;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.StructureMembers;
import ucar.nc2.Variable;
import ucar.nc2.write.Ncdump;
import ucar.unidata.io.http.ReadFromUrl;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.UtilsMa2Test;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.util.List;

/** Test ConvertD2N */
@RunWith(Parameterized.class)
public class TestConvertD2N {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  static public final String server = "http://" + TestDir.dap2TestServer + "/dts/";

  @Parameterized.Parameters(name = "{0}")
  static public List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();
    result.add(new Object[] {"test.01", ""}); // scalars
    result.add(new Object[] {"test.02", ""}); // 1D arrays
    result.add(new Object[] {"test.03", ""}); // 3D arrays
    result.add(new Object[] {"test.04", ""}); // Structure with scalars
    result.add(new Object[] {"test.05", ""}); // nested Structures with scalars
    result.add(new Object[] {"test.07a", ""}); // Structure
    result.add(new Object[] {"test.21", ""}); // Structure with multidim fields
    result.add(new Object[] {"test.50", ""}); // array of structures
    result.add(new Object[] {"test.53", ""}); // array of structures with nested scalar structure
    result.add(new Object[] {"test.06", ""}); // Grids
    result.add(new Object[] {"test.06a", ""}); // Grids

    result.add(new Object[] {"b31", ""}); // top Sequence
    result.add(new Object[] {"test.07", ""}); // top Sequence
    result.add(new Object[] {"test.56", ""}); // top Sequence with multidim field
    result.add(new Object[] {"test.31", ""}); // top Sequence with nested Structure, Grid //

    result.add(new Object[] {"NestedSeq", ""});
    result.add(new Object[] {"NestedSeq2", ""});
    result.add(new Object[] {"NestedSeq2", "person1.age,person1.stuff&person1.age=3"}); // with CE

    result.add(new Object[] {"test.22", ""}); // Structure with nested Structure, Grid
    // result.add(new Object[] {"test.23", ""}); // Structure with nested Sequence TODO fails
    result.add(new Object[] {"test.31", ""}); // Structure with nested Structure, Grid

    return result;
  }

  @Parameterized.Parameter(0)
  public String url;

  @Parameterized.Parameter(1)
  public String CE;

  @Test
  public void test() throws IOException, DAP2Exception, InvalidRangeException {
    testDataDDSfromServer(server + url, CE);
    testArray(server + url);
  }

  static DataDDS testDataDDSfromServer(String urlName, String CE) throws IOException, opendap.dap.DAP2Exception {

    System.out.println("--DConnect =" + urlName);
    DConnect2 dodsConnection = new DConnect2(urlName, true);

    // get the DDS
    DDS dds = dodsConnection.getDDS();
    dds.print(System.out);
    // DodsV root = DodsV.parseDDS( dds);

    // get the DAS
    DAS das = dodsConnection.getDAS();
    das.print(System.out);
    System.out.println();

    // root.parseDAS(das);

    // get the DataDDS
    System.out.println("--DConnect.getData CE= " + CE);
    DataDDS dataDDS = dodsConnection.getData("?" + CE, null);
    dataDDS.print(System.out);
    System.out.println();

    System.out.println("--show DataDDS");
    PrintWriter pw = new PrintWriter(System.out);
    showDDS(dataDDS, pw);
    pw.flush();
    System.out.println();

    System.out.println("--parseDataDDS DodsV.show");
    DodsV dataRoot = DodsV.parseDataDDS(dataDDS);
    dataRoot.show(System.out, "");
    System.out.println();

    // try to parse with ConvertD2N
    System.out.println("--testConvertDDS");
    testConvertDDS(urlName, dataDDS, System.out);
    System.out.println();

    // show the original contents
    System.out.println("--" + urlName + ".asc?" + CE);
    System.out.println(ReadFromUrl.readURLcontents(urlName + ".asc?" + CE));

    System.out.println("============");

    return dataDDS;
  }

  static void testArray(String urlName) throws IOException, opendap.dap.DAP2Exception {
    System.out.println("checkArray =" + urlName);
    DConnect2 dodsConnection = new DConnect2(urlName, true);

    // get the DataDDS
    DataDDS dataDDS = dodsConnection.getData("?", null);
    dataDDS.print(System.out);
    System.out.println();
    DodsV root = DodsV.parseDataDDS(dataDDS);

    ConvertD2N converter = new ConvertD2N();
    try (DodsNetcdfFile dodsfile = DodsNetcdfFile.builder().build(urlName, null)) {
      for (Variable v : dodsfile.getVariables()) {
        String name = DodsNetcdfFiles.getDODSConstraintName(v);
        DodsV dodsV = root.findByDodsShortName(name);
        if (dodsV == null) {
          System.out.println("Cant find " + name);
          continue;
        }
        Array data = converter.convertTopVariable(v, null, dodsV);
        showArray(v.getFullName(), data, System.out, "");
      }

      /*
       * for (int i = 0; i < root.children.size(); i++) {
       * DodsV dodsV = (DodsV) root.children.get(i);
       * Variable v = dodsfile.findVariable( dodsV.getNetcdfShortName());
       * Array data = converter.convertTopVariable(v, null, dodsV);
       * showArray( data, System.out, "");
       * }
       */
      System.out.println("============");
    }
  }

  static void showDDS(DataDDS dds, PrintWriter out) {
    out.println("DDS=" + dds.getEncodedName());
    for (BaseType bt : dds.getVariables()) {
      showBT(bt, out, " ");
    }
  }

  static boolean showData = false;
  static boolean useNC = false;

  static void testConvertDDS(String urlName, DataDDS dataDDS, PrintStream out) throws IOException, DAP2Exception {
    try (DodsNetcdfFile dodsfile = DodsNetcdfFile.builder().build(urlName, null)) {
      System.out.println(dodsfile.toString());

      if (useNC) {
        List vars = dodsfile.getVariables();
        for (int i = 0; i < vars.size(); i++) {
          Variable v = (Variable) vars.get(i);
          Array data = v.read();
          if (showData)
            logger.debug(Ncdump.printArray(data, v.getFullName() + data.shapeToString(), null));
        }
      }

      ConvertD2N converter = new ConvertD2N();
      DodsV root = DodsV.parseDataDDS(dataDDS);
      for (int i = 0; i < root.children.size(); i++) {
        DodsV dodsV = root.children.get(i);
        Variable v = dodsfile.findVariable(dodsV.getFullName());
        Array data = converter.convertTopVariable(v, null, dodsV);
        showArray(v.getFullName(), data, out, "");

        if (useNC) {
          Array data2 = v.read();
          UtilsMa2Test.testEquals(data, data2);
        }

        if (showData)
          logger.debug(Ncdump.printArray(data, v.getFullName() + data.shapeToString(), null));
      }
    }

  }

  static void showBT(BaseType bt, PrintWriter out, String space) {

    if (bt instanceof DSequence) {
      showSequence((DSequence) bt, out, space);
      return;
    }

    if (bt instanceof DArray) {
      showArray((DArray) bt, out, space);
      return;
    }

    out.println(space + bt.getEncodedName() + " (" + bt.getClass().getName() + ")");

    if (bt instanceof DConstructor) {
      String nspace = space + " ";
      for (BaseType nbt : ((DConstructor) bt).getVariables()) {
        showBT(nbt, out, nspace);
      }
      out.println(space + "-----" + bt.getEncodedName());
    }

  }

  static void showSequence(DSequence seq, PrintWriter out, String space) {
    int nrows = seq.getRowCount();
    out.println(space + seq.getEncodedName() + " (" + seq.getClass().getName() + ")");

    String nspace = space + " ";

    // for sequences, gotta look at the _rows_ (!)
    if (nrows > 0) {
      out.println(nspace + "Vector[" + nrows + "] allvalues; show first:");
      for (BaseType bt : seq.getRow(0)) {
        showBT(bt, out, nspace + " ");
      }
    }
  }

  static void showArray(DArray a, PrintWriter out, String space) {
    int nrows = a.getLength();
    out.print(space + a.getEncodedName() + " (" + a.getClass().getName() + ") ");

    out.print(" (");
    int count = 0;
    for (DArrayDimension dim : a.getDimensions()) {
      String name = dim.getEncodedName() == null ? "" : dim.getEncodedName() + "=";
      if (count > 0)
        out.print(",");
      out.print(name + dim.getSize());
      count++;
    }
    out.println(")");

    String nspace = space + " ";
    PrimitiveVector pv = a.getPrimitiveVector();
    BaseType template = pv.getTemplate();
    out.println(nspace + pv.getClass().getName() + "[" + nrows + "] template=" + template.getClass().getName());

    if ((pv instanceof BaseTypePrimitiveVector) && !(template instanceof DString)) {
      if (nrows > 0) {
        BaseType vbt = ((BaseTypePrimitiveVector) pv).getValue(0);
        showBT(vbt, out, nspace + " ");
      }
    }

  }

  static void showArray(String name, Array a, PrintStream out, String space) {
    out.print(space + "Array " + name + " (" + a.getClass().getName() + ") ");
    showShape(a.getShape(), out);
    out.println();

    if (a instanceof ArrayStructure) {
      ArrayStructure sa = (ArrayStructure) a;
      StructureMembers sm = sa.getStructureMembers();
      for (StructureMembers.Member member : sm.getMembers()) {
        out.print(space + " " + member.getDataType() + " " + member.getName());
        showShape(member.getShape(), out);
        out.println();
        Object data = member.getDataArray();
        if (data != null) {
          Array array = (Array) data;
          showArray(member.getName(), array, out, space + "  ");
        }
      }
      out.println();
    }

  }

  static void showShape(int[] shape, PrintStream out) {
    out.print(" (");
    for (int i = 0; i < shape.length; i++) {
      if (i > 0)
        out.print(",");
      out.print(shape[i]);
    }
    out.print(")");
  }

}
