/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.write;

import com.google.common.collect.Iterables;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import ucar.array.ArrayType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.EnumTypedef;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.util.Indent;
import ucar.unidata.util.StringUtil2;

/**
 * Netcdf Common Data Language (CDL) writer.
 * TODO strict mode for valid CDL
 */
public class CDLWriter {
  /**
   * Write CDL to a PrintStream (legacy)
   * 
   * @deprecated do not use
   */
  @Deprecated
  public static void writeCDL(NetcdfFile ncfile, PrintStream out, boolean strict) {
    Formatter f = new Formatter();
    CDLWriter.writeCDL(ncfile, f, strict, null);
    PrintWriter pw = new PrintWriter(out);
    pw.write(f.toString());
  }

  /**
   * Write CDL to a Writer (legacy)
   * 
   * @deprecated do not use
   */
  @Deprecated
  public static void writeCDL(NetcdfFile ncfile, Writer out, boolean strict) {
    Formatter f = new Formatter();
    CDLWriter.writeCDL(ncfile, f, strict, null);
    PrintWriter pw = new PrintWriter(out);
    pw.write(f.toString());
  }

  /** Write CDL to a Formatter */
  public static void writeCDL(NetcdfFile ncfile, Formatter out, boolean strict, @Nullable String nameOverride) {
    CDLWriter writer = new CDLWriter(ncfile, out, strict);
    writer.toStringStart(new Indent(2), strict, nameOverride);
    writer.toStringEnd();
  }

  private final NetcdfFile ncfile;
  private final Formatter out;
  private final boolean strict;

  CDLWriter(NetcdfFile ncfile, Formatter out, boolean strict) {
    this.ncfile = ncfile;
    this.out = out;
    this.strict = strict;
  }

  void toStringStart(Indent indent, boolean strict, @Nullable String nameOverride) {
    String name = (nameOverride != null) ? nameOverride : ncfile.getLocation();
    if (strict) {
      if (name.endsWith(".nc"))
        name = name.substring(0, name.length() - 3);
      if (name.endsWith(".cdl"))
        name = name.substring(0, name.length() - 4);
      name = NetcdfFiles.makeValidCDLName(name);
    }
    out.format("%snetcdf %s {%n", indent, name);
    indent.incr();
    writeCDL(ncfile.getRootGroup(), indent);
    indent.decr();
  }

  void toStringEnd() {
    out.format("}%n");
  }

  private void writeCDL(Group group, Indent indent) {
    List<EnumTypedef> enumTypedefs = group.getEnumTypedefs();
    boolean hasE = (!enumTypedefs.isEmpty());
    boolean hasD = (!group.getDimensions().isEmpty());
    boolean hasV = (!group.getVariables().isEmpty());
    // boolean hasG = (groups.size() > 0);
    boolean hasA = (!Iterables.isEmpty(group.attributes()));

    if (hasE) {
      out.format("%stypes:%n", indent);
      indent.incr();
      for (EnumTypedef e : enumTypedefs) {
        writeCDL(e, indent);
        out.format("%n");
      }
      indent.decr();
      out.format("%n");
    }

    if (hasD) {
      out.format("%sdimensions:%n", indent);
      indent.incr();
      for (Dimension myd : group.getDimensions()) {
        writeCDL(myd, indent);
        out.format("%n");
      }
      indent.decr();
    }

    if (hasV) {
      out.format("%svariables:%n", indent);
      indent.incr();
      for (Variable v : group.getVariables()) {
        if (v instanceof Structure)
          writeCDL((Structure) v, indent, false);
        else
          writeCDL(v, indent, false);
        out.format("%n");
      }
      indent.decr();
    }

    for (Group g : group.getGroups()) {
      String gname = strict ? NetcdfFiles.makeValidCDLName(g.getShortName()) : g.getShortName();
      out.format("%sgroup: %s {%n", indent, gname);
      indent.incr();
      writeCDL(g, indent);
      indent.decr();
      out.format("%s}%n%n", indent);
    }

    if (hasA) {
      if (group.isRoot())
        out.format("%s// global attributes:%n", indent);
      else
        out.format("%s// group attributes:%n", indent);

      for (Attribute att : group.attributes()) {
        // String name = strict ? NetcdfFile.escapeNameCDL(getShortName()) : getShortName();
        out.format("%s", indent);
        writeCDL(att, null);
        out.format(";");
        if (!strict && (att.getArrayType() != ArrayType.STRING))
          out.format(" // %s", att.getArrayType());
        out.format("%n");
      }
    }
  }

  private void writeCDL(Attribute att, String parentname) {
    if (strict && (att.isString() || att.getEnumType() != null)) {
      // Force type explicitly for string.
      out.format("string "); // note lower case and trailing blank
    }
    if (strict && parentname != null) {
      out.format(NetcdfFiles.makeValidCDLName(parentname));
    }
    out.format(":");
    out.format("%s", strict ? NetcdfFiles.makeValidCDLName(att.getShortName()) : att.getShortName());
    if (att.isString()) {
      out.format(" = ");
      for (int i = 0; i < att.getLength(); i++) {
        if (i != 0) {
          out.format(", ");
        }
        String val = att.getStringValue(i);
        if (val != null) {
          out.format("\"%s\"", encodeString(val));
        }
      }
    } else if (att.getEnumType() != null) {
      out.format(" = ");
      for (int i = 0; i < att.getLength(); i++) {
        if (i != 0) {
          out.format(", ");
        }
        EnumTypedef en = att.getEnumType();
        String econst = att.getStringValue(i);
        Integer ecint = en.lookupEnumInt(econst);
        if (ecint == null) {
          throw new RuntimeException("Illegal enum constant: " + econst);
        }
        out.format("\"%s\"", encodeString(econst));
      }
    } else {
      out.format(" = ");
      for (int i = 0; i < att.getLength(); i++) {
        if (i != 0)
          out.format(", ");

        ArrayType dataType = att.getArrayType();
        Number number = att.getNumericValue(i);
        if (dataType.isUnsigned()) {
          // 'number' is unsigned, but will be treated as signed when we print it below, because Java only has signed
          // types. If it is large enough ( >= 2^(BIT_WIDTH-1) ), its most-significant bit will be interpreted as the
          // sign bit, which will result in an invalid (negative) value being printed. To prevent that, we're going
          // to widen the number before printing it.
          number = ArrayType.widenNumber(number);
        }
        out.format("%s", number);

        if (dataType.isUnsigned()) {
          out.format("U");
        }

        if (dataType == ArrayType.FLOAT)
          out.format("f");
        else if (dataType == ArrayType.SHORT || dataType == ArrayType.USHORT) {
          out.format("S");
        } else if (dataType == ArrayType.BYTE || dataType == ArrayType.UBYTE) {
          out.format("B");
        } else if (dataType == ArrayType.LONG || dataType == ArrayType.ULONG) {
          out.format("L");
        }
      }
    }
  }

  private void writeCDL(Dimension dim, Indent indent) {
    String name = strict ? NetcdfFiles.makeValidCDLName(dim.getShortName()) : dim.getShortName();
    out.format("%s%s", indent, name);
    if (dim.isUnlimited())
      out.format(" = UNLIMITED;   // (%d currently)", dim.getLength());
    else if (dim.isVariableLength())
      out.format(" = UNKNOWN;");
    else
      out.format(" = %d;", dim.getLength());
  }

  private void writeCDL(EnumTypedef e, Indent indent) {
    String name = strict ? NetcdfFiles.makeValidCDLName(e.getShortName()) : e.getShortName();
    String basetype = "";
    switch (e.getBaseArrayType()) {
      case ENUM1:
        basetype = "byte ";
        break;
      case ENUM2:
        basetype = "short ";
        break;
      case ENUM4:
        basetype = "";
        break;
      default:
        assert false : "Internal error";
    }
    out.format("%s%senum %s { ", indent, basetype, name);
    int count = 0;
    Map<Integer, String> map = e.getMap();
    List<Integer> keysetList = new ArrayList<>(map.keySet());
    Collections.sort(keysetList);
    for (Integer key : keysetList) {
      String s = map.get(key);
      if (0 < count++)
        out.format(", ");
      if (strict)
        out.format("%s = %s", NetcdfFiles.makeValidCDLName(s), key);
      else
        out.format("'%s' = %s", s, key);
    }
    out.format("};");
  }

  private void writeCDL(Variable v, Indent indent, boolean useFullName) {
    out.format("%s", indent);
    ArrayType dataType = v.getArrayType();
    if (dataType.isEnum()) {
      if (v.getEnumTypedef() == null) {
        out.format("enum UNKNOWN");
      } else {
        out.format("enum %s", NetcdfFiles.makeValidCDLName(v.getEnumTypedef().getShortName()));
      }
    } else {
      out.format("%s", dataType.toCdl());
    }

    // if (isVariableLength) out.append("(*)"); // TODO
    out.format(" ");
    v.getNameAndDimensions(out, useFullName, strict);
    out.format(";");
    out.format("%n");

    indent.incr();
    for (Attribute att : v.attributes()) {
      out.format("%s", indent);
      writeCDL(att, v.getShortName());
      out.format(";");
      if (!strict && (att.getArrayType() != ArrayType.STRING)) {
        out.format(" // %s", att.getArrayType());
      }
      out.format("%n");
    }
    indent.decr();
  }

  private static final char[] org = {'\b', '\f', '\n', '\r', '\t', '\\', '\'', '\"'};
  private static final String[] replace = {"\\b", "\\f", "\\n", "\\r", "\\t", "\\\\", "\\\'", "\\\""};

  /**
   * Replace special characters '\t', '\n', '\f', '\r', for CDL
   *
   * @param s string to quote
   * @return equivilent string replacing special chars
   */
  private static String encodeString(String s) {
    return StringUtil2.replace(s, org, replace);
  }

  private void writeCDL(Structure s, Indent indent, boolean useFullName) {
    out.format("%s%s {%n", indent, s.getArrayType());

    indent.incr();
    for (Variable v : s.getVariables()) {
      // nested structures
      if (v instanceof Structure) {
        writeCDL((Structure) v, indent, useFullName);
      } else {
        writeCDL(v, indent, useFullName);
      }
    }
    indent.decr();

    out.format("%s} ", indent);
    s.getNameAndDimensions(out, useFullName, strict);
    out.format(";%n");

    for (Attribute att : s.attributes()) {
      out.format("%s", indent);
      writeCDL(att, s.getShortName());
      out.format(";");
      if (!strict && (att.getArrayType() != ArrayType.STRING))
        out.format(" // %s", att.getArrayType());
      out.format("%n");
    }
  }

}
