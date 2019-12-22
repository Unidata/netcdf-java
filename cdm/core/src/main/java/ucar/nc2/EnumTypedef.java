/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import ucar.ma2.DataType;
import ucar.nc2.util.Indent;
import javax.annotation.concurrent.Immutable;
import java.util.*;

/**
 * Enumeration Typedef is a map from integers to Strings.
 * For ENUM1, ENUM2, ENUM4 enumeration types.
 * Immutable.
 *
 * @author caron
 */
@Immutable
public class EnumTypedef extends CDMNode {
  // Constants for the unsigned max values for enum(1,2,4)
  private static final int UBYTE_MAX = 255;
  private static final int USHORT_MAX = 65535;

  private final ImmutableMap<Integer, String> map;
  private final ImmutableList<String> enumStrings;
  private final DataType basetype;

  public EnumTypedef(String name, Map<Integer, String> map) {
    this(name, map, DataType.ENUM4); // default basetype
  }

  public EnumTypedef(String name, Map<Integer, String> map, DataType basetype) {
    super(name);
    Preconditions.checkArgument(validateMap(map, basetype));
    this.map = ImmutableMap.copyOf(map);

    enumStrings = ImmutableList.sortedCopyOf(map.values());

    assert basetype == DataType.ENUM1 || basetype == DataType.ENUM2 || basetype == DataType.ENUM4;
    this.basetype = basetype;
  }

  @Deprecated
  public ImmutableList<String> getEnumStrings() {
    return enumStrings;
  }

  /** Will return ImmutableMap in version 6. */
  public Map<Integer, String> getMap() {
    return map;
  }

  /** One of DataType.ENUM1, DataType.ENUM2, or DataType.ENUM4. */
  public DataType getBaseType() {
    return this.basetype;
  }

  private boolean validateMap(Map<Integer, String> map, DataType basetype) {
    if (map == null || basetype == null)
      return false;
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

  /** Get the name corresponding to the enum value. */
  public String lookupEnumString(int e) {
    String result = map.get(e);
    return (result == null) ? "Unknown enum value=" + e : result;
  }

  /** Get the enum value corresponding to the name. */
  public Integer lookupEnumInt(String name) {
    for (Map.Entry<Integer, String> entry : map.entrySet()) {
      if (entry.getValue().equalsIgnoreCase(name))
        return entry.getKey();
    }
    return null;
  }

  /**
   * CDL string representation.
   *
   * @param strict if true, write in strict adherence to CDL definition.
   * @return CDL representation.
   */
  public String writeCDL(boolean strict) {
    Formatter out = new Formatter();
    writeCDL(out, new Indent(2), strict);
    return out.toString();
  }

  protected void writeCDL(Formatter out, Indent indent, boolean strict) {
    String name = strict ? NetcdfFile.makeValidCDLName(getShortName()) : getShortName();
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
        out.format("%s = %s", NetcdfFile.makeValidCDLName(s), key);
      else
        out.format("'%s' = %s", s, key);
    }
    out.format("};");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    EnumTypedef that = (EnumTypedef) o;

    if (map == that.map)
      return true;
    if (map == null)
      return false;
    if (!map.equals(that.map))
      return false;
    String name = getShortName();
    String thatname = that.getShortName();
    return Objects.equals(name, thatname);

  }

  @Override
  public int hashCode() {
    String name = getShortName();
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (map != null ? map.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    Formatter f = new Formatter();
    f.format("EnumTypedef %s: ", getShortName());
    for (int key : map.keySet()) {
      f.format("%d=%s,", key, map.get(key));
    }
    return f.toString();
  }
}
