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
import ucar.array.*;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.Indent;

/**
 * Utility to implement ncdump.
 * A difference with ncdump is that the nesting of multidimensional array data is represented by nested brackets,
 * so the output is not legal CDL that can be used as input for ncgen. Also, the default is header only (-h).
 * Version that works with ucar.array.Array.
 */
@Immutable
public class NcdumpArray {

  /** Tell Ncdump if you want values printed. */
  public enum WantValues {
    none, coordsOnly, all
  }

  /**
   * ncdump that parses a command string.
   *
   * @param command command string
   * @param out send output here
   * @param cancel allow task to be cancelled; may be null.
   * @throws IOException on write error
   */
  public static void ncdump(String command, Writer out, CancelTask cancel) throws IOException {
    // pull out the filename from the command
    String filename;
    StringTokenizer stoke = new StringTokenizer(command);
    if (stoke.hasMoreTokens())
      filename = stoke.nextToken();
    else {
      out.write(usage);
      return;
    }

    try (NetcdfFile nc = NetcdfDatasets.openFile(filename, cancel)) {
      // the rest of the command
      int pos = command.indexOf(filename);
      command = command.substring(pos + filename.length());
      ncdump(nc, command, out, cancel);

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
   * @param cancel allow task to be cancelled; may be null.
   */
  public static void ncdump(NetcdfFile nc, String command, Writer out, CancelTask cancel) throws IOException {
    WantValues showValues = WantValues.none;

    Builder builder = builder(nc).setCancelTask(cancel);

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
    private final NetcdfFile ncfile;
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

    public NcdumpArray build() {
      return new NcdumpArray(this);
    }
  }

  private final NetcdfFile ncfile;
  private final WantValues wantValues;
  private final boolean ncml;
  private final boolean strict;
  private final String varNames;
  private final String locationName;
  private final CancelTask cancelTask;

  private NcdumpArray(Builder builder) {
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
            printArray(out, v.readArray(), v.getFullName(), indent, cancelTask);
            if (cancelTask != null && cancelTask.isCancel())
              return out.toString();
          }
        } else if (wantValues == WantValues.coordsOnly) { // dump coordVars
          for (Variable v : ncfile.getVariables()) {
            if (v.isCoordinateVariable())
              printArray(out, v.readArray(), v.getFullName(), indent, cancelTask);
            if (cancelTask != null && cancelTask.isCancel())
              return out.toString();
          }
        }

        if ((wantValues != WantValues.all) && (varNames != null)) { // dump the list of variables
          StringTokenizer stoke = new StringTokenizer(varNames, ";");
          while (stoke.hasMoreTokens()) {
            String varSubset = stoke.nextToken(); // variable name and optionally a subset

            if (varSubset.indexOf('(') >= 0) { // has a selector
              Array<?> data = ncfile.readSectionArray(varSubset);
              printArray(out, data, varSubset, indent, cancelTask);

            } else { // do entire variable
              Variable v = ncfile.findVariable(varSubset);
              if (v == null) {
                out.format(" cant find variable: %s%n   %s", varSubset, usage);
                continue;
              }
              // dont print coord vars if they are already printed
              if ((wantValues != WantValues.coordsOnly) || v.isCoordinateVariable())
                printArray(out, v.readArray(), v.getFullName(), indent, cancelTask);
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
   * @param cancel allow task to be cancelled; may be null.
   * @return String result
   * @throws IOException on write error
   */
  public static String printVariableData(Variable v, CancelTask cancel) throws IOException {
    Array<?> data = v.readArray();
    Formatter out = new Formatter();
    printArray(out, data, v.getFullName(), new Indent(2), cancel);
    return out.toString();
  }

  /**
   * Print array as undifferentiated sequence of values.
   * 
   * @param array any Array except StructureDataArray
   */
  public static String printArrayPlain(Array<?> array) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    for (Object val : array) {
      pw.print(val);
      pw.print(' ');
    }
    return sw.toString();
  }

  /** Print array to returned String. */
  public static String printArray(Array<?> ma) {
    return printArray(ma, "", null);
  }

  /** Print named array to returned String. */
  public static String printArray(Array<?> array, String name, CancelTask cancel) {
    Formatter out = new Formatter();
    printArray(out, array, name, null, new Indent(2), cancel);
    return out.toString();
  }

  private static void printArray(Formatter out, Array<?> array, String name, Indent indent, CancelTask cancel) {
    printArray(out, array, name, null, indent, cancel);
    out.flush();
  }

  private static void printArray(Formatter out, Array<?> array, String name, String units, Indent ilev,
      CancelTask cancel) {
    if (cancel != null && cancel.isCancel()) {
      return;
    }

    if (name != null) {
      out.format("%s%s = ", ilev, name);
    }
    ilev.incr();

    if (array == null) {
      out.format("null array for %s", name);
      ilev.decr();
      return;
    }

    if ((array instanceof ArrayChar) && (array.getRank() > 0)) {
      printStringArray(out, (ArrayChar) array, ilev, cancel);

    } else if (array.getArrayType() == ArrayType.STRING) {
      printStringArray(out, (ArrayString) array, ilev, cancel);

    } else if (array instanceof StructureDataArray) {
      printStructureDataArray(out, (StructureDataArray) array, ilev, cancel);

    } else if (array.getArrayType() == ArrayType.OPAQUE) { // opaque type
      ArrayVlen<Byte> vlen = (ArrayVlen<Byte>) array;
      int count = 0;
      for (Array<Byte> inner : vlen) {
        out.format("%s%n", (count == 0) ? "," : ";"); // peek ahead
        ArrayByte barray = (ArrayByte) inner;
        printByteBuffer(out, barray.getByteBuffer(), ilev);
        if (cancel != null && cancel.isCancel())
          return;
        count++;
      }
    } else if (array instanceof ArrayVlen) {
      printVariableArray(out, (ArrayVlen<?>) array, ilev, cancel);
    } else {
      printArray(out, array, ilev, cancel);
    }

    if (units != null) {
      out.format(" %s", units);
    }
    out.format("%n");
    ilev.decr();
    out.flush();
  }

  private static void printArray(Formatter out, Array<?> ma, Indent indent, CancelTask cancel) {
    if (cancel != null && cancel.isCancel())
      return;
    int rank = ma.getRank();

    // scalar
    if (rank == 0) {
      Object value = ma.getScalar();

      if (ma.getArrayType().isUnsigned()) {
        assert value instanceof Number : "A data type being unsigned implies that it is numeric.";

        // "value" is an unsigned number, but it will be treated as signed when we print it below, because Java only
        // has signed types. If it's large enough ( >= 2^(BIT_WIDTH-1) ), its most-significant bit will be interpreted
        // as the sign bit, which will result in an invalid (negative) value being printed. To prevent that, we're
        // going to widen the number before printing it, but only if the unsigned number is being seen as negative.
        value = ArrayType.widenNumberIfNegative((Number) value);
      }

      out.format("%s", value);
      return;
    }

    int[] dims = ma.getShape();
    int last = dims[0];

    out.format("%n%s{", indent);
    Index ima = ma.getIndex();
    if ((rank == 1) && (ma.getArrayType() != ArrayType.STRUCTURE)) {
      for (int ii = 0; ii < last; ii++) {
        Object value = ma.get(ima.setElem(ii)); // LOOK

        if (ma.getArrayType().isUnsigned()) {
          assert value instanceof Number : "A data type being unsigned implies that it is numeric.";
          value = ArrayType.widenNumberIfNegative((Number) value);
        }

        if (ii > 0)
          out.format(", ");
        out.format("%s", value);
        if (cancel != null && cancel.isCancel())
          return;
      }
      out.format("}");
      return;
    }

    indent.incr();
    for (int ii = 0; ii < last; ii++) {
      Array slice = null;
      try {
        slice = Arrays.slice(ma, 0, ii);
      } catch (InvalidRangeException e) {
        e.printStackTrace();
      }
      if (ii > 0)
        out.format(",");
      printArray(out, slice, indent, cancel);
      if (cancel != null && cancel.isCancel())
        return;
    }
    indent.decr();

    out.format("%n%s}", indent);
  }

  private static void printStringArray(Formatter out, ArrayChar ma, Indent indent, CancelTask cancel) {
    if (cancel != null && cancel.isCancel())
      return;

    int rank = ma.getRank();

    if (rank == 1) {
      out.format("  \"%s\"", ma.makeStringFromChar());
      return;
    }

    if (rank == 2) {
      boolean first = true;
      Array<String> strings = ma.makeStringsFromChar();
      for (String s : strings) {
        if (!first)
          out.format(", ");
        out.format("  \"%s\"", s);
        first = false;
        if (cancel != null && cancel.isCancel())
          return;
      }
      return;
    }

    int[] dims = ma.getShape();
    int last = dims[0];

    out.format("%n%s{", indent);
    indent.incr();
    for (int ii = 0; ii < last; ii++) {
      ArrayChar slice = null;
      try {
        slice = (ArrayChar) Arrays.slice(ma, 0, ii);
      } catch (InvalidRangeException e) {
        e.printStackTrace();
      }
      if (ii > 0)
        out.format(",");
      printStringArray(out, slice, indent, cancel);
      if (cancel != null && cancel.isCancel())
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

  private static void printStringArray(Formatter out, ArrayString ma, Indent indent, CancelTask cancel) {
    if (cancel != null && cancel.isCancel())
      return;

    int rank = ma.getRank();

    if (rank == 0) {
      out.format("  \"%s\"", ma.get(0));
      return;
    }

    if (rank == 1) {
      boolean first = true;
      for (int i = 0; i < ma.length(); i++) {
        if (!first)
          out.format(", ");
        out.format("  \"%s\"", ma.get(i));
        first = false;
      }
      return;
    }

    int[] dims = ma.getShape();
    int last = dims[0];

    out.format("%n%s{", indent);
    indent.incr();
    for (int ii = 0; ii < last; ii++) {
      ArrayString slice = null;
      try {
        slice = (ArrayString) Arrays.slice(ma, 0, ii);
      } catch (InvalidRangeException e) {
        e.printStackTrace();
      }
      if (ii > 0)
        out.format(",");
      printStringArray(out, slice, indent, cancel);
    }
    indent.decr();
    out.format("%n%s}", indent);
  }

  private static void printStructureDataArray(Formatter out, StructureDataArray array, Indent indent,
      CancelTask cancel) {
    int count = 0;
    for (StructureData sdata : array) {
      out.format("%n%s{", indent);
      printStructureData(out, sdata, indent, cancel);
      out.format("%s} %s(%d)", indent, sdata.getName(), count);
      if (cancel != null && cancel.isCancel())
        return;
      count++;
    }
  }

  private static void printVariableArray(Formatter out, ArrayVlen<?> vlen, Indent indent, CancelTask cancel) {
    out.format("%n%s{", indent);
    indent.incr();
    int count = 0;
    for (Array<?> inner : vlen) {
      out.format("%s%n", (count == 0) ? "," : ";"); // peek ahead
      printArray(out, inner, indent, cancel);
      if (cancel != null && cancel.isCancel())
        return;
      count++;
    }
    indent.decr();
    out.format("%n%s}", indent);
  }

  /** Print StructureData to returned String. */
  public static String printStructureData(StructureData sdata) {
    Formatter out = new Formatter();
    for (StructureMembers.Member m : sdata.getStructureMembers()) {
      Array<?> memData = sdata.getMemberData(m);
      if (memData instanceof ArrayChar) {
        out.format("%s", ((ArrayChar) memData).makeStringFromChar());
      } else {
        printArray(out, memData, null, null, new Indent(2), null);
      }
      out.format(",");
    }
    return out.toString();
  }

  private static void printStructureData(Formatter out, StructureData sdata, Indent indent, CancelTask cancel) {
    indent.incr();
    for (StructureMembers.Member m : sdata.getStructureMembers()) {
      Array sdataArray = sdata.getMemberData(m);
      printArray(out, sdataArray, m.getName(), m.getUnitsString(), indent, cancel);
      if (cancel != null && cancel.isCancel())
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
  private static final String usage =
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
