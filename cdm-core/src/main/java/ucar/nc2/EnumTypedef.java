/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import javax.annotation.Nullable;

import ucar.array.ArrayType;
import ucar.ma2.DataType;
import ucar.nc2.util.Indent;
import javax.annotation.concurrent.Immutable;
import java.util.*;

/**
 * A named map from integers to Strings; a user-defined Enum used as a Variable's data type.
 * For ENUM1, ENUM2, ENUM4 enumeration types.
 */
@Immutable
public class EnumTypedef {
  // Constants for the unsigned max values for enum(1,2,4)
  private static final int UBYTE_MAX = 255;
  private static final int USHORT_MAX = 65535;

  private final String name;
  private final ImmutableMap<Integer, String> map;
  private final ArrayType basetype;

  /** Make an EnumTypedef with base type ENUM4. */
  public EnumTypedef(String name, Map<Integer, String> map) {
    this(name, map, ArrayType.ENUM4); // default basetype
  }

  /** @deprecated use EnumTypedef(String name, Map<Integer, String> map, ArrayType basetype) */
  @Deprecated
  public EnumTypedef(String name, Map<Integer, String> map, DataType basetype) {
    this.name = name;
    Preconditions.checkArgument(basetype == DataType.ENUM1 || basetype == DataType.ENUM2 || basetype == DataType.ENUM4);
    this.basetype = basetype.getArrayType();

    Preconditions.checkNotNull(map);
    Preconditions.checkArgument(validateMap(map, this.basetype));
    this.map = ImmutableMap.copyOf(map);
  }

  /** Make an EnumTypedef setting the base type (must be ENUM1, ENUM2, ENUM4). */
  public EnumTypedef(String name, Map<Integer, String> map, ArrayType basetype) {
    this.name = name;
    Preconditions
        .checkArgument(basetype == ArrayType.ENUM1 || basetype == ArrayType.ENUM2 || basetype == ArrayType.ENUM4);
    this.basetype = basetype;

    Preconditions.checkNotNull(map);
    Preconditions.checkArgument(validateMap(map, basetype));
    this.map = ImmutableMap.copyOf(map);
  }

  /** @deprecated use getBaseArrayType() */
  @Deprecated
  public DataType getBaseType() {
    return this.basetype.getDataType();
  }

  /** One of ArrayType.ENUM1, ArrayType.ENUM2, or ArrayType.ENUM4. */
  public ArrayType getBaseArrayType() {
    return this.basetype;
  }

  public String getShortName() {
    return name;
  }

  public ImmutableMap<Integer, String> getMap() {
    return map;
  }

  private boolean validateMap(Map<Integer, String> map, ArrayType basetype) {
    for (Integer i : map.keySet()) {
      // WARNING, we do not have signed/unsigned info available
      switch (basetype) {
        case ENUM1:
          if (i < Byte.MIN_VALUE || i > UBYTE_MAX)
            return false;
          break;
        case ENUM2:
          if (i < Short.MIN_VALUE || i > USHORT_MAX)
            return false;
          break;
        case ENUM4:
          break; // enum4 is always ok
        default:
          return false;
      }
    }
    return true;
  }


  /** Get the enum value corresponding to the name. */
  @Nullable
  public Integer lookupEnumInt(String name) {
    for (Map.Entry<Integer, String> entry : map.entrySet()) {
      if (entry.getValue().equalsIgnoreCase(name))
        return entry.getKey();
    }
    return null;
  }

  /** Get the name corresponding to the enum value. */
  @Nullable
  public String lookupEnumString(int e) {
    return map.get(e);
  }

  void writeCDL(Formatter out, Indent indent, boolean strict) {
    String name = strict ? NetcdfFiles.makeValidCDLName(this.name) : this.name;
    String basetype = "";
    switch (this.basetype) {
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
    // List<Object> keyset = Arrays.asList(map.keySet().toArray());
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EnumTypedef that = (EnumTypedef) o;
    return com.google.common.base.Objects.equal(name, that.name) && com.google.common.base.Objects.equal(map, that.map)
        && basetype == that.basetype;
  }

  // Needed for netCDF4 wierdness
  public boolean equalsMapOnly(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EnumTypedef that = (EnumTypedef) o;
    return com.google.common.base.Objects.equal(map, that.map);
  }

  @Override
  public int hashCode() {
    return com.google.common.base.Objects.hashCode(name, map, basetype);
  }

  @Override
  public String toString() {
    Formatter f = new Formatter();
    writeCDL(f, new Indent(0), false);
    return f.toString();
  }
}
