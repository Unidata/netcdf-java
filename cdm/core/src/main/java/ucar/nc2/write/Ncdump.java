/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 *  See LICENSE for license information.
 */
package ucar.nc2.write;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import java.io.BufferedWriter;
import java.io.CharArrayWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Formatter;
import java.util.StringTokenizer;
import org.jdom2.Element;
import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.ma2.ArrayObject;
import ucar.ma2.ArraySequence;
import ucar.ma2.ArrayStructure;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataIterator;
import ucar.ma2.StructureMembers;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.ncml.NcMLWriter;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.Indent;

/**
 * Utility to implement ncdump.
 * A difference with ncdump is that the nesting of multidimensional array data is represented by nested brackets,
 * so the output is not legal CDL that can be used as input for ncgen. Also, the default is header only (-h).
 * Moved from ucar.nc2.NCdumpW
 */
public class Ncdump {

  /** Tell Ncdump if you want values printed. */
  public enum WantValues {
    none, coordsOnly, all
  }

  /**
   * ncdump that parses a command string.
   *
   * @param command command string
   * @param out send output here
   * @param ct allow task to be cancelled; may be null.
   * @return true if successful
   * @throws IOException on write error
   */
  public static boolean ncdump(String command, Writer out, CancelTask ct) throws IOException {
    // pull out the filename from the command
    String filename;
    StringTokenizer stoke = new StringTokenizer(command);
    if (stoke.hasMoreTokens())
      filename = stoke.nextToken();
    else {
      out.write(usage);
      return false;
    }

    try (NetcdfFile nc = NetcdfDatasets.openFile(filename, ct)) {
      // the rest of the command
      int pos = command.indexOf(filename);
      command = command.substring(pos + filename.length());
      return print(nc, command, out, ct);

    } catch (FileNotFoundException e) {
      out.write("file not found= ");
      out.write(filename);
      return false;

    } finally {
      out.close();
    }
  }

  /**
   * ncdump-like print of netcdf file.
   *
   * @param filename NetcdfFile to open
   * @param out print to this stream
   * @param showAll dump all variable data
   * @param showCoords only print header and coordinate variables
   * @param ncml print NcML representation (other arguments are ignored)
   * @param strict print strict CDL representation
   * @param varNames semicolon delimited list of variables whose data should be printed
   * @param ct allow task to be cancelled; may be null.
   * @return true if successful
   * @throws IOException on write error
   */
  public static boolean ncdump(String filename, Writer out, boolean showAll, boolean showCoords, boolean ncml,
      boolean strict, String varNames, CancelTask ct) throws IOException {
    try (NetcdfFile nc = NetcdfDatasets.openFile(filename, ct)) {
      return print(nc, out, showAll, showCoords, ncml, strict, varNames, ct);
    } catch (FileNotFoundException e) {
      out.write("file not found= ");
      out.write(filename);
      out.flush();
      return false;
    }
  }

  /**
   * ncdump, parsing command string, file already open.
   *
   * @param nc apply command to this file
   * @param command : command string
   * @param out send output here
   * @param ct allow task to be cancelled; may be null.
   * @return true if successful
   * @throws IOException on write error
   */
  public static boolean print(NetcdfFile nc, String command, Writer out, CancelTask ct)
      throws IOException {
    WantValues showValues = WantValues.none;
    boolean ncml = false;
    boolean strict = false;
    String varNames = null;
    String trueDataset = null;
    String fakeDataset = null;

    if (command != null) {
      StringTokenizer stoke = new StringTokenizer(command);

      while (stoke.hasMoreTokens()) {
        String toke = stoke.nextToken();
        if (toke.equalsIgnoreCase("-help")) {
          out.write(usage);
          out.write('\n');
          return true;
        }
        if (toke.equalsIgnoreCase("-vall"))
          showValues = WantValues.all;
        if (toke.equalsIgnoreCase("-c") && (showValues == WantValues.none))
          showValues = WantValues.coordsOnly;
        if (toke.equalsIgnoreCase("-ncml"))
          ncml = true;
        if (toke.equalsIgnoreCase("-cdl") || toke.equalsIgnoreCase("-strict"))
          strict = true;
        if (toke.equalsIgnoreCase("-v") && stoke.hasMoreTokens())
          varNames = stoke.nextToken();
        if (toke.equalsIgnoreCase("-datasetname") && stoke.hasMoreTokens()) {
          fakeDataset = stoke.nextToken();
          if (fakeDataset.isEmpty())
            fakeDataset = null;
          if (fakeDataset != null) {
            trueDataset = nc.getLocation();
            nc.setLocation(fakeDataset); // LOOK WTF?
          }
        }
      }
    }

    boolean ok = print(nc, out, showValues, ncml, strict, varNames, ct);
    if (trueDataset != null && fakeDataset != null)
      nc.setLocation(trueDataset);
    return ok;
  }

  /**
   * ncdump-like print of netcdf file.
   *
   * @param nc already opened NetcdfFile
   * @param out print to this stream
   * @param showAll dump all variable data
   * @param showCoords only print header and coordinate variables
   * @param ncml print NcML representation (other arguments are ignored)
   * @param strict print strict CDL representation
   * @param varNames semicolon delimited list of variables whose data should be printed. May have
   *        Fortran90 like selector: eg varName(1:2,*,2)
   * @param ct allow task to be cancelled; may be null.
   * @return true if successful
   * @throws IOException on write error
   */
  public static boolean print(NetcdfFile nc, Writer out, boolean showAll, boolean showCoords, boolean ncml,
      boolean strict, String varNames, CancelTask ct) throws IOException {

    WantValues showValues = WantValues.none;
    if (showAll)
      showValues = WantValues.all;
    else if (showCoords)
      showValues = WantValues.coordsOnly;

    return print(nc, out, showValues, ncml, strict, varNames, ct);
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////
  // heres where the work is done

  /**
   * ncdump-like print of netcdf file.
   *
   * @param nc already opened NetcdfFile
   * @param out print to this stream
   * @param showValues do you want the variable values printed?
   * @param ncml print NcML representation (other arguments are ignored)
   * @param strict print strict CDL representation
   * @param varNames semicolon delimited list of variables whose data should be printed. May have
   *        Fortran90 like selector: eg varName(1:2,*,2)
   * @param ct allow task to be cancelled; may be null.
   * @return true if successful
   * @throws IOException on write error
   */
  public static boolean print(NetcdfFile nc, Writer out, WantValues showValues, boolean ncml, boolean strict,
      String varNames, CancelTask ct) throws IOException {
    boolean headerOnly = (showValues == WantValues.none) && (varNames == null);

    try {
      if (ncml) {
        writeNcML(nc, out, showValues, null); // output schema in NcML
      } else if (headerOnly) {
        CDLWriter.writeCDL(nc, out, strict);
      } else {
        //PrintWriter ps = new PrintWriter(out);
        Formatter f = new Formatter();
        Indent indent = new Indent(2);
        CDLWriter cdlWriter = new CDLWriter(nc, f, strict);
        cdlWriter.toStringStart(indent, strict);

        indent.incr();
        ps.printf("%n%sdata:%n", indent);
        indent.incr();

        if (showValues == WantValues.all) { // dump all data
          for (Variable v : nc.getVariables()) {
            printArray(v.read(), v.getFullName(), ps, indent, ct);
            if (ct != null && ct.isCancel())
              return false;
          }
        } else if (showValues == WantValues.coordsOnly) { // dump coordVars
          for (Variable v : nc.getVariables()) {
            if (v.isCoordinateVariable())
              printArray(v.read(), v.getFullName(), ps, indent, ct);
            if (ct != null && ct.isCancel())
              return false;
          }
        }

        if ((showValues != WantValues.all) && (varNames != null)) { // dump the list of variables
          StringTokenizer stoke = new StringTokenizer(varNames, ";");
          while (stoke.hasMoreTokens()) {
            String varSubset = stoke.nextToken(); // variable name and optionally a subset

            if (varSubset.indexOf('(') >= 0) { // has a selector
              Array data = nc.readSection(varSubset);
              printArray(data, varSubset, ps, indent, ct);

            } else { // do entire variable
              Variable v = nc.findVariable(varSubset);
              if (v == null) {
                ps.print(" cant find variable: " + varSubset + "\n   " + usage);
                continue;
              }
              // dont print coord vars if they are already printed
              if ((showValues != WantValues.coordsOnly) || v.isCoordinateVariable())
                printArray(v.read(), v.getFullName(), ps, indent, ct);
            }
            if (ct != null && ct.isCancel())
              return false;
          }
        }

        indent.decr();
        indent.decr();
        cdlWriter.toStringEnd();
        PrintWriter pw = new PrintWriter(out);
        pw.write(f.toString());
      }

    } catch (Exception e) {
      e.printStackTrace();
      out.write(e.getMessage());
      out.flush();
      return false;
    }

    out.flush();
    return true;
  }


  /**
   * Print all the data of the given Variable.
   *
   * @param v variable to print
   * @param ct allow task to be cancelled; may be null.
   * @return String result
   * @throws IOException on write error
   */
  public static String printVariableData(Variable v, CancelTask ct) throws IOException {
    Array data = v.read();
    StringWriter writer = new StringWriter(10000);
    printArray(data, v.getFullName(), new PrintWriter(writer), new Indent(2), ct);
    return writer.toString();
  }

  /**
   * Print a section of the data of the given Variable.
   *
   * @param v variable to print
   * @param sectionSpec string specification
   * @param ct allow task to be cancelled; may be null.
   * @return String result formatted data ouptut
   * @throws IOException on write error
   * @throws InvalidRangeException is specified section doesnt match variable shape
   */
  private static String printVariableDataSection(Variable v, String sectionSpec, CancelTask ct)
      throws IOException, InvalidRangeException {
    Array data = v.read(sectionSpec);

    StringWriter writer = new StringWriter(20000);
    printArray(data, v.getFullName(), new PrintWriter(writer), new Indent(2), ct);
    return writer.toString();
  }

  /** Print named array to returned String. */
  public static String printArray(Array array, String name, CancelTask ct) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    printArray(array, name, null, pw, new Indent(2), ct, true);
    return sw.toString();
  }

  private static void printArray(Array array, String name, PrintWriter out, Indent indent, CancelTask ct) {
    printArray(array, name, null, out, indent, ct, true);
    out.flush();
  }

  private static void printArray(Array array, String name, String units, PrintWriter out, Indent ilev, CancelTask ct,
      boolean printSeq) { // throws IOException {
    if (ct != null && ct.isCancel())
      return;

    if (name != null)
      out.print(ilev + name + " = ");
    ilev.incr();

    if (array == null) {
      out.println("null array for " + name);
      ilev.decr();
      return;
    }

    if ((array instanceof ArrayChar) && (array.getRank() > 0)) {
      printStringArray(out, (ArrayChar) array, ilev, ct);

    } else if (array.getElementType() == String.class) {
      printStringArray(out, array, ilev, ct);

    } else if (array instanceof ArraySequence) {
      if (printSeq)
        printSequence(out, (ArraySequence) array, ilev, ct);

    } else if (array instanceof ArrayStructure) {
      printStructureDataArray(out, (ArrayStructure) array, ilev, ct);

    } else if (array.getElementType() == ByteBuffer.class) { // opaque type
      array.resetLocalIterator();
      while (array.hasNext()) {
        printByteBuffer(out, (ByteBuffer) array.next(), ilev);
        out.println(array.hasNext() ? "," : ";");// peek ahead
        if (ct != null && ct.isCancel())
          return;
      }
    } else if (array instanceof ArrayObject) {
      printVariableArray(out, (ArrayObject) array, ilev, ct);
    } else {
      printArray(array, out, ilev, ct);
    }

    if (units != null)
      out.print(" " + units);
    out.print("\n");
    ilev.decr();
    out.flush();
  }

  private static void printArray(Array ma, PrintWriter out, Indent indent, CancelTask ct) {
    if (ct != null && ct.isCancel())
      return;

    int rank = ma.getRank();
    Index ima = ma.getIndex();

    // scalar
    if (rank == 0) {
      Object value = ma.getObject(ima);

      if (ma.isUnsigned()) {
        assert value instanceof Number : "A data type being unsigned implies that it is numeric.";

        // "value" is an unsigned number, but it will be treated as signed when we print it below, because Java only
        // has signed types. If it's large enough ( >= 2^(BIT_WIDTH-1) ), its most-significant bit will be interpreted
        // as the sign bit, which will result in an invalid (negative) value being printed. To prevent that, we're
        // going to widen the number before printing it, but only if the unsigned number is being seen as negative.
        value = DataType.widenNumberIfNegative((Number) value);
      }

      out.print(value);
      return;
    }

    int[] dims = ma.getShape();
    int last = dims[0];

    out.print("\n" + indent + "{");

    if ((rank == 1) && (ma.getElementType() != StructureData.class)) {
      for (int ii = 0; ii < last; ii++) {
        Object value = ma.getObject(ima.set(ii));

        if (ma.isUnsigned()) {
          assert value instanceof Number : "A data type being unsigned implies that it is numeric.";
          value = DataType.widenNumberIfNegative((Number) value);
        }

        if (ii > 0)
          out.print(", ");
        out.print(value);
        if (ct != null && ct.isCancel())
          return;
      }
      out.print("}");
      return;
    }

    indent.incr();
    for (int ii = 0; ii < last; ii++) {
      Array slice = ma.slice(0, ii);
      if (ii > 0)
        out.print(",");
      printArray(slice, out, indent, ct);
      if (ct != null && ct.isCancel())
        return;
    }
    indent.decr();

    out.print("\n" + indent + "}");
  }

  private static void printStringArray(PrintWriter out, ArrayChar ma, Indent indent, CancelTask ct) {
    if (ct != null && ct.isCancel())
      return;

    int rank = ma.getRank();

    if (rank == 1) {
      out.print("  \"" + ma.getString() + "\"");
      return;
    }

    if (rank == 2) {
      boolean first = true;
      ArrayChar.StringIterator iter = ma.getStringIterator();
      while (iter.hasNext()) {
        if (!first)
          out.print(", ");
        out.print("\"" + iter.next() + "\"");
        first = false;
        if (ct != null && ct.isCancel())
          return;
      }
      return;
    }

    int[] dims = ma.getShape();
    int last = dims[0];

    out.print("\n" + indent + "{");
    indent.incr();
    for (int ii = 0; ii < last; ii++) {
      ArrayChar slice = (ArrayChar) ma.slice(0, ii);
      if (ii > 0)
        out.print(",");
      printStringArray(out, slice, indent, ct);
      if (ct != null && ct.isCancel())
        return;
    }
    indent.decr();

    out.print("\n" + indent + "}");
  }

  private static void printByteBuffer(PrintWriter out, ByteBuffer bb, Indent indent) {
    out.print(indent + "0x");
    int last = bb.limit() - 1;
    if (last < 0)
      out.printf("00");
    else
      for (int i = bb.position(); i <= last; i++) {
        out.printf("%02x", bb.get(i));
      }
  }

  private static void printStringArray(PrintWriter out, Array ma, Indent indent, CancelTask ct) {
    if (ct != null && ct.isCancel())
      return;

    int rank = ma.getRank();
    Index ima = ma.getIndex();

    if (rank == 0) {
      out.print("  \"" + ma.getObject(ima) + "\"");
      return;
    }

    if (rank == 1) {
      boolean first = true;
      for (int i = 0; i < ma.getSize(); i++) {
        if (!first)
          out.print(", ");
        out.print("  \"" + ma.getObject(ima.set(i)) + "\"");
        first = false;
      }
      return;
    }

    int[] dims = ma.getShape();
    int last = dims[0];

    out.print("\n" + indent + "{");
    indent.incr();
    for (int ii = 0; ii < last; ii++) {
      ArrayObject slice = (ArrayObject) ma.slice(0, ii);
      if (ii > 0)
        out.print(",");
      printStringArray(out, slice, indent, ct);
    }
    indent.decr();

    out.print("\n" + indent + "}");
  }

  private static void printStructureDataArray(PrintWriter out, ArrayStructure array, Indent indent,
      CancelTask ct) {
    try (StructureDataIterator sdataIter = array.getStructureDataIterator()) {
      int count = 0;
      while (sdataIter.hasNext()) {
        StructureData sdata = sdataIter.next();
        out.println("\n" + indent + "{");
        printStructureData(out, sdata, indent, ct);
        out.print(indent + "} " + sdata.getName() + "(" + count + ")");
        if (ct != null && ct.isCancel())
          return;
        count++;
      }
    } catch (IOException e) {
      e.printStackTrace(); // shouldnt happen ?
    }
  }

  private static void printVariableArray(PrintWriter out, ArrayObject array, Indent indent, CancelTask ct) {
    out.print("\n" + indent + "{");
    indent.incr();
    IndexIterator iter = array.getIndexIterator();
    boolean first = true;
    while (iter.hasNext()) {
      Array data = (Array) iter.next();
      if (!first) {
        out.print(", ");
      }
      printArray(data, out, indent, ct);
      first = false;
    }
    indent.decr();
    out.print("\n" + indent + "}");
  }

  private static void printSequence(PrintWriter out, ArraySequence seq, Indent indent, CancelTask ct) {
    try (StructureDataIterator iter = seq.getStructureDataIterator()) {
      while (iter.hasNext()) {
        StructureData sdata = iter.next();
        out.println("\n" + indent + "{");
        printStructureData(out, sdata, indent, ct);
        out.print(indent + "} " + sdata.getName());
        if (ct != null && ct.isCancel())
          return;
      }
    } catch (IOException e) {
      e.printStackTrace(); // shouldnt happen ??
    }
  }

  /**
   * Print contents of a StructureData.
   *
   * @param out send output here.
   * @param sdata StructureData to print.
   */
  public static void printStructureData(PrintWriter out, StructureData sdata) {
    printStructureData(out, sdata, new Indent(2), null);
    out.flush();
  }

  private static void printStructureData(PrintWriter out, StructureData sdata, Indent indent, CancelTask ct) {
    indent.incr();
    for (StructureMembers.Member m : sdata.getMembers()) {
      Array sdataArray = sdata.getArray(m);
      printArray(sdataArray, m.getName(), m.getUnitsString(), out, indent, ct, true);
      if (ct != null && ct.isCancel())
        return;
    }
    indent.decr();
  }

  /** Print StructureData to returned String. */
  public static String printStructureData(StructureData sdata) {
    CharArrayWriter carray = new CharArrayWriter(1000);
    PrintWriter pw = new PrintWriter(carray);
    for (StructureMembers.Member m : sdata.getMembers()) {
      Array memData = sdata.getArray(m);
      if (memData instanceof ArrayChar)
        pw.print(((ArrayChar) memData).getString());
      else
        printArray(memData, pw);
      pw.print(',');
    }
    return carray.toString();
  }

  /**
   * Print array as undifferentiated sequence of values.
   *
   * @param ma any Array except ArrayStructure
   * @param out print to here
   */
  public static void printArrayPlain(Array ma, PrintWriter out) {
    ma.resetLocalIterator();
    while (ma.hasNext()) {
      out.print(ma.next());
      out.print(' ');
    }
  }

  /** Print array to named PrintWriter */
  public static void printArray(Array array, PrintWriter pw) {
    printArray(array, null, null, pw, new Indent(2), null, true);
  }

  /** Print array to returned String. */
  public static String printArray(Array ma) {
    return printArray(ma, "", null);
  }

  //////////////////////////////////////////////////////////////////////////////////////
  // standard NCML writing.

  /**
   * Write the NcML representation for a file.
   * Note that ucar.nc2.dataset.NcMLWriter has a JDOM implementation, for complete NcML.
   * This method implements only the "core" NcML for plain ole netcdf files.
   *
   * @param ncfile write NcML for this file
   * @param writer write to this Writer. Must be using UTF-8 encoding (where applicable)
   * @param showValues do you want the variable values printed?
   * @param url use this for the url attribute; if null use getLocation(). // ??
   * @throws IOException on write error
   */
  private static void writeNcML(NetcdfFile ncfile, Writer writer, WantValues showValues, String url) throws IOException {
    Preconditions.checkNotNull(ncfile);
    Preconditions.checkNotNull(writer);
    Preconditions.checkNotNull(showValues);

    Predicate<Variable> writeVarsPred;
    switch (showValues) {
      case none:
        writeVarsPred = NcMLWriter.writeNoVariablesPredicate;
        break;
      case coordsOnly:
        writeVarsPred = NcMLWriter.writeCoordinateVariablesPredicate;
        break;
      case all:
        writeVarsPred = NcMLWriter.writeAllVariablesPredicate;
        break;
      default:
        String message =
            String.format("CAN'T HAPPEN: showValues (%s) != null and checked all possible enum values.", showValues);
        throw new AssertionError(message);
    }

    NcMLWriter ncmlWriter = new NcMLWriter();
    ncmlWriter.setWriteVariablesPredicate(writeVarsPred);

    Element netcdfElement = ncmlWriter.makeNetcdfElement(ncfile, url);
    ncmlWriter.writeToWriter(netcdfElement, writer);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////
  // TODO use jcommander?
  private static String usage =
      "usage: Ncdump <filename> [-cdl | -ncml] [-c | -vall] [-v varName1;varName2;..] [-v varName(0:1,:,12)]\n";

  /**
   * Main program.
   * <p>
   * <strong>ucar.nc2.NCdumpW filename [-cdl | -ncml] [-c | -vall] [-v varName1;varName2;..] [-v varName(0:1,:,12)]
   * </strong>
   * <p>
   * where:
   * <ul>
   * <li>filename : path of any CDM readable file
   * <li>cdl or ncml: output format is CDL or NcML
   * <li>-vall : dump all variable data
   * <li>-c : dump coordinate variable data
   * <li>-v varName1;varName2; : dump specified variable(s)
   * <li>-v varName(0:1,:,12) : dump specified variable section
   * </ul>
   * Default is to dump the header info only.
   *
   * @param args arguments
   */
  public static void main(String[] args) {
    if (args.length == 0) {
      System.out.println(usage);
      return;
    }

    // pull out the filename from the command
    String filename = args[0];
    try (Writer writer = new BufferedWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8));
        NetcdfFile nc = NetcdfDatasets.openFile(filename, null)) {
      // the rest of the command
      StringBuilder command = new StringBuilder();
      for (int i = 1; i < args.length; i++) {
        command.append(args[i]);
        command.append(" ");
      }
      print(nc, command.toString(), writer, null);
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }
}
