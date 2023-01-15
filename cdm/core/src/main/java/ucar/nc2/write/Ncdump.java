/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.write;

import com.google.common.base.Preconditions;
import java.io.BufferedWriter;
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
import java.util.function.Predicate;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
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
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.Indent;

/**
 * Utility to implement ncdump.
 * A difference with ncdump is that the nesting of multidimensional array data is represented by nested brackets,
 * so the output is not legal CDL that can be used as input for ncgen. Also, the default is header only (-h).
 * Moved from ucar.nc2.NCdumpW
 */
@Immutable
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
   * @throws IOException on write error
   */
  public static void ncdump(String command, Writer out, CancelTask ct) throws IOException {
    // pull out the filename from the command
    String filename;
    StringTokenizer stoke = new StringTokenizer(command);
    if (stoke.hasMoreTokens())
      filename = stoke.nextToken();
    else {
      out.write(usage);
      return;
    }

    try (NetcdfFile nc = NetcdfDatasets.openFile(filename, ct)) {
      // the rest of the command
      int pos = command.indexOf(filename);
      command = command.substring(pos + filename.length());
      ncdump(nc, command, out, ct);

    } catch (FileNotFoundException e) {
      out.write("file not found= ");
      out.write(filename);

    } finally {
      out.close();
    }
  }

  /**
   * ncdump, parsing command string, file already open.
   *
   * @param nc apply command to this file
   * @param command : command string
   * @param out send output here
   * @param ct allow task to be cancelled; may be null.
   */
  public static void ncdump(NetcdfFile nc, String command, Writer out, CancelTask ct) throws IOException {
    WantValues showValues = WantValues.none;

    Builder builder = builder(nc).setCancelTask(ct);

    if (command != null) {
      StringTokenizer stoke = new StringTokenizer(command);

      while (stoke.hasMoreTokens()) {
        String toke = stoke.nextToken();
        if (toke.equalsIgnoreCase("-help")) {
          out.write(usage);
          out.write('\n');
          return;
        }
        if (toke.equalsIgnoreCase("-vall")) {
          showValues = WantValues.all;
        }
        if (toke.equalsIgnoreCase("-c") && (showValues == WantValues.none)) {
          showValues = WantValues.coordsOnly;
        }
        if (toke.equalsIgnoreCase("-ncml")) {
          builder.setNcml(true);
        }
        if (toke.equalsIgnoreCase("-cdl") || toke.equalsIgnoreCase("-strict")) {
          builder.setStrict(true);
        }
        if (toke.equalsIgnoreCase("-v") && stoke.hasMoreTokens()) {
          builder.setVarNames(stoke.nextToken());
        }
        if (toke.equalsIgnoreCase("-datasetname") && stoke.hasMoreTokens()) {
          builder.setLocationName(stoke.nextToken());
        }
      }
    }
    builder.setWantValues(showValues);

    out.write(builder.build().print());
    out.flush();
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////

  public static Builder builder(NetcdfFile ncfile) {
    return new Builder(ncfile);
  }

  public static class Builder {
    private NetcdfFile ncfile;
    private WantValues wantValues = WantValues.none;
    private boolean ncml;
    private boolean strict;
    private String varNames;
    @Nullable
    private String locationName; // NcML location attribute
    private CancelTask cancelTask;

    private Builder(NetcdfFile ncfile) {
      this.ncfile = ncfile;
    }

    /** show all Variable's values */
    public Builder setShowAllValues() {
      this.wantValues = WantValues.all;
      return this;
    }

    /** show Coordinate Variable's values only */
    public Builder setShowCoordValues() {
      this.wantValues = WantValues.coordsOnly;
      return this;
    }

    public Builder setLocationName(String locationName) {
      if (locationName != null && !locationName.isEmpty()) {
        this.locationName = locationName;
      }
      return this;
    }

    /** set what Variables' values you want output. */
    public Builder setWantValues(WantValues wantValues) {
      this.wantValues = wantValues;
      return this;
    }

    /** set true if outout should be ncml, otherwise CDL. */
    public Builder setNcml(boolean ncml) {
      this.ncml = ncml;
      return this;
    }

    /** strict CDL representation, default false */
    public Builder setStrict(boolean strict) {
      this.strict = strict;
      return this;
    }

    /**
     * @param varNames semicolon delimited list of variables whose data should be printed. May have
     *        Fortran90 like selector: eg varName(1:2,*,2)
     */
    public Builder setVarNames(String varNames) {
      this.varNames = varNames;
      return this;
    }

    /** allow task to be cancelled */
    public Builder setCancelTask(CancelTask cancelTask) {
      this.cancelTask = cancelTask;
      return this;
    }

    public Ncdump build() {
      return new Ncdump(this);
    }
  }

  private final NetcdfFile ncfile;
  private final WantValues wantValues;
  private final boolean ncml;
  private final boolean strict;
  private final String varNames;
  private final String locationName;
  private final CancelTask cancelTask;

  private Ncdump(Builder builder) {
    this.ncfile = builder.ncfile;
    this.wantValues = builder.wantValues;
    this.ncml = builder.ncml;
    this.strict = builder.strict;
    this.varNames = builder.varNames;
    this.locationName = builder.locationName;
    this.cancelTask = builder.cancelTask;
  }

  public String print() {
    boolean headerOnly = (wantValues == WantValues.none) && (varNames == null);
    Formatter out = new Formatter();

    try {
      if (ncml) {
        return writeNcml(ncfile, wantValues, locationName); // output schema in NcML
      } else if (headerOnly) {
        CDLWriter.writeCDL(ncfile, out, strict, locationName);
      } else {
        Indent indent = new Indent(2);
        CDLWriter cdlWriter = new CDLWriter(ncfile, out, strict);
        cdlWriter.toStringStart(indent, strict, locationName);

        indent.incr();
        out.format("%n%sdata:%n", indent);
        indent.incr();

        if (wantValues == WantValues.all) { // dump all data
          for (Variable v : ncfile.getVariables()) {
            printArray(out, v.read(), v.getFullName(), indent, cancelTask);
            if (cancelTask != null && cancelTask.isCancel())
              return out.toString();
          }
        } else if (wantValues == WantValues.coordsOnly) { // dump coordVars
          for (Variable v : ncfile.getVariables()) {
            if (v.isCoordinateVariable())
              printArray(out, v.read(), v.getFullName(), indent, cancelTask);
            if (cancelTask != null && cancelTask.isCancel())
              return out.toString();
          }
        }

        if ((wantValues != WantValues.all) && (varNames != null)) { // dump the list of variables
          StringTokenizer stoke = new StringTokenizer(varNames, ";");
          while (stoke.hasMoreTokens()) {
            String varSubset = stoke.nextToken(); // variable name and optionally a subset

            if (varSubset.indexOf('(') >= 0) { // has a selector
              Array data = ncfile.readSection(varSubset);
              printArray(out, data, varSubset, indent, cancelTask);

            } else { // do entire variable
              Variable v = ncfile.findVariable(varSubset);
              if (v == null) {
                out.format(" cant find variable: %s%n   %s", varSubset, usage);
                continue;
              }
              // dont print coord vars if they are already printed
              if ((wantValues != WantValues.coordsOnly) || v.isCoordinateVariable())
                printArray(out, v.read(), v.getFullName(), indent, cancelTask);
            }
            if (cancelTask != null && cancelTask.isCancel())
              return out.toString();
          }
        }

        indent.decr();
        indent.decr();
        cdlWriter.toStringEnd();
      }

    } catch (Exception e) {
      out.format("%n%s%n", e.getMessage());
    }

    return out.toString();
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
    Formatter out = new Formatter();
    printArray(out, data, v.getFullName(), new Indent(2), ct);
    return out.toString();
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

    Formatter out = new Formatter();
    printArray(out, data, v.getFullName(), new Indent(2), ct);
    return out.toString();
  }

  /**
   * Print array as undifferentiated sequence of values.
   *
   * @param ma any Array except ArrayStructure
   */
  public static String printArrayPlain(Array ma) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    ma.resetLocalIterator();
    while (ma.hasNext()) {
      pw.print(ma.next());
      pw.print(' ');
    }
    return sw.toString();
  }

  /** Print array to returned String. */
  public static String printArray(Array ma) {
    return printArray(ma, "", null);
  }

  /** Print named array to returned String. */
  public static String printArray(Array array, String name, CancelTask ct) {
    Formatter out = new Formatter();
    printArray(out, array, name, null, new Indent(2), ct, true);
    return out.toString();
  }

  private static void printArray(Formatter out, Array array, String name, Indent indent, CancelTask ct) {
    printArray(out, array, name, null, indent, ct, true);
    out.flush();
  }

  private static void printArray(Formatter out, Array array, String name, String units, Indent ilev, CancelTask ct,
      boolean printSeq) {
    if (ct != null && ct.isCancel())
      return;

    if (name != null)
      out.format("%s%s = ", ilev, name);
    ilev.incr();

    if (array == null) {
      out.format("null array for %s", name);
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
        out.format("%s%n", array.hasNext() ? "," : ";"); // peek ahead
        if (ct != null && ct.isCancel())
          return;
      }
    } else if (array instanceof ArrayObject) {
      printVariableArray(out, (ArrayObject) array, ilev, ct);
    } else {
      printArray(out, array, ilev, ct);
    }

    if (units != null)
      out.format(" %s", units);
    out.format("%n");
    ilev.decr();
    out.flush();
  }

  private static void printArray(Formatter out, Array ma, Indent indent, CancelTask ct) {
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

      out.format("%s", value);
      return;
    }

    int[] dims = ma.getShape();
    int last = dims[0];

    out.format("%n%s{", indent);

    if ((rank == 1) && (ma.getElementType() != StructureData.class)) {
      for (int ii = 0; ii < last; ii++) {
        Object value = ma.getObject(ima.set(ii));

        if (ma.isUnsigned()) {
          assert value instanceof Number : "A data type being unsigned implies that it is numeric.";
          value = DataType.widenNumberIfNegative((Number) value);
        }

        if (ii > 0)
          out.format(", ");
        out.format("%s", value);
        if (ct != null && ct.isCancel())
          return;
      }
      out.format("}");
      return;
    }

    indent.incr();
    for (int ii = 0; ii < last; ii++) {
      Array slice = ma.slice(0, ii);
      if (ii > 0)
        out.format(",");
      printArray(out, slice, indent, ct);
      if (ct != null && ct.isCancel())
        return;
    }
    indent.decr();

    out.format("%n%s}", indent);
  }

  private static void printStringArray(Formatter out, ArrayChar ma, Indent indent, CancelTask ct) {
    if (ct != null && ct.isCancel())
      return;

    int rank = ma.getRank();

    if (rank == 1) {
      out.format("  \"%s\"", ma.getString());
      return;
    }

    if (rank == 2) {
      boolean first = true;
      ArrayChar.StringIterator iter = ma.getStringIterator();
      while (iter.hasNext()) {
        if (!first)
          out.format(", ");
        out.format("  \"%s\"", iter.next());
        first = false;
        if (ct != null && ct.isCancel())
          return;
      }
      return;
    }

    int[] dims = ma.getShape();
    int last = dims[0];

    out.format("%n%s{", indent);
    indent.incr();
    for (int ii = 0; ii < last; ii++) {
      ArrayChar slice = (ArrayChar) ma.slice(0, ii);
      if (ii > 0)
        out.format(",");
      printStringArray(out, slice, indent, ct);
      if (ct != null && ct.isCancel())
        return;
    }
    indent.decr();

    out.format("%n%s}", indent);
  }

  private static void printByteBuffer(Formatter out, ByteBuffer bb, Indent indent) {
    out.format("%s0x", indent);
    int last = bb.limit() - 1;
    if (last < 0)
      out.format("00");
    else
      for (int i = bb.position(); i <= last; i++) {
        out.format("%02x", bb.get(i));
      }
  }

  private static void printStringArray(Formatter out, Array ma, Indent indent, CancelTask ct) {
    if (ct != null && ct.isCancel())
      return;

    int rank = ma.getRank();
    Index ima = ma.getIndex();

    if (rank == 0) {
      out.format("  \"%s\"", ma.getObject(ima));
      return;
    }

    if (rank == 1) {
      boolean first = true;
      for (int i = 0; i < ma.getSize(); i++) {
        if (!first)
          out.format(", ");
        out.format("  \"%s\"", ma.getObject(ima.set(i)));
        first = false;
      }
      return;
    }

    int[] dims = ma.getShape();
    int last = dims[0];

    out.format("%n%s{", indent);
    indent.incr();
    for (int ii = 0; ii < last; ii++) {
      Array slice = (Array) ma.slice(0, ii); // replaces ArrayObject slice = (ArrayObject)o; because ArrayObject is
                                             // over-casting since printStringArray takes Array
      if (ii > 0)
        out.format(",");
      printStringArray(out, slice, indent, ct);
    }
    indent.decr();
    out.format("%n%s}", indent);
  }

  private static void printStructureDataArray(Formatter out, ArrayStructure array, Indent indent, CancelTask ct) {
    try (StructureDataIterator sdataIter = array.getStructureDataIterator()) {
      int count = 0;
      while (sdataIter.hasNext()) {
        StructureData sdata = sdataIter.next();
        out.format("%n%s{", indent);
        printStructureData(out, sdata, indent, ct);
        out.format("%s} %s(%d)", indent, sdata.getName(), count);
        if (ct != null && ct.isCancel())
          return;
        count++;
      }
    } catch (IOException ioe) {
      out.format("%n%s%n", ioe.getMessage());
    }
  }

  private static void printVariableArray(Formatter out, ArrayObject array, Indent indent, CancelTask ct) {
    out.format("%n%s{", indent);
    indent.incr();
    IndexIterator iter = array.getIndexIterator();
    boolean first = true;
    while (iter.hasNext()) {
      Array data = (Array) iter.next();
      if (!first) {
        out.format(", ");
      }
      printArray(out, data, indent, ct);
      first = false;
    }
    indent.decr();
    out.format("%n%s}", indent);
  }

  private static void printSequence(Formatter out, ArraySequence seq, Indent indent, CancelTask ct) {
    try (StructureDataIterator iter = seq.getStructureDataIterator()) {
      while (iter.hasNext()) {
        StructureData sdata = iter.next();
        out.format("%n%s{", indent);
        printStructureData(out, sdata, indent, ct);
        out.format("%s} %s", indent, sdata.getName());
        if (ct != null && ct.isCancel())
          return;
      }
    } catch (IOException ioe) {
      out.format("%n%s%n", ioe.getMessage());
    }
  }

  /** Print StructureData to returned String. */
  public static String printStructureData(StructureData sdata) {
    Formatter out = new Formatter();
    for (StructureMembers.Member m : sdata.getMembers()) {
      Array memData = sdata.getArray(m);
      if (memData instanceof ArrayChar) {
        out.format("%s", ((ArrayChar) memData).getString());
      } else {
        printArray(out, memData, null, null, new Indent(2), null, true);
      }
      out.format(",");
    }
    return out.toString();
  }

  private static void printStructureData(Formatter out, StructureData sdata, Indent indent, CancelTask ct) {
    indent.incr();
    for (StructureMembers.Member m : sdata.getMembers()) {
      Array sdataArray = sdata.getArray(m);
      printArray(out, sdataArray, m.getName(), m.getUnitsString(), indent, ct, true);
      if (ct != null && ct.isCancel())
        return;
    }
    indent.decr();
  }

  //////////////////////////////////////////////////////////////////////////////////////
  // standard NCML writing.

  /**
   * Write the NcML representation for a file.
   * Note that ucar.nc2.dataset.NcMLWriter has a JDOM implementation, for complete NcML.
   * This method implements only the "core" NcML for plain ole netcdf files.
   *
   * @param ncfile write NcML for this file
   * @param showValues do you want the variable values printed?
   * @param url use this for the url attribute; if null use getLocation(). // ??
   */
  private static String writeNcml(NetcdfFile ncfile, WantValues showValues, @Nullable String url) {
    Preconditions.checkNotNull(ncfile);
    Preconditions.checkNotNull(showValues);

    Predicate<? super Variable> writeVarsPred;
    switch (showValues) {
      case none:
        writeVarsPred = NcmlWriter.writeNoVariablesPredicate;
        break;
      case coordsOnly:
        writeVarsPred = NcmlWriter.writeCoordinateVariablesPredicate;
        break;
      case all:
        writeVarsPred = NcmlWriter.writeAllVariablesPredicate;
        break;
      default:
        String message =
            String.format("CAN'T HAPPEN: showValues (%s) != null and checked all possible enum values.", showValues);
        throw new AssertionError(message);
    }

    NcmlWriter ncmlWriter = new NcmlWriter(null, null, writeVarsPred);
    Element netcdfElement = ncmlWriter.makeNetcdfElement(ncfile, url);
    return ncmlWriter.writeToString(netcdfElement);
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
      ncdump(nc, command.toString(), writer, null);
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }
}
