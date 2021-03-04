/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2;

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;
import javax.annotation.concurrent.Immutable;

import ucar.array.ArrayType;
import ucar.ma2.DataType;
import ucar.nc2.constants.CDM;

/** Builder for VariableSimpleIF, makes true immutable objects. */
@Immutable
public class VariableSimpleBuilder {

  /** @deprecated use fromMember(ucar.array.StructureMembers.Member m) */
  public static VariableSimpleBuilder fromMember(ucar.ma2.StructureMembers.Member m) {
    return new VariableSimpleBuilder(m.getName(), m.getDescription(), m.getUnitsString(), m.getDataType(),
        Dimensions.makeDimensionsAnon(m.getShape()));
  }

  public static VariableSimpleBuilder fromMember(ucar.array.StructureMembers.Member m) {
    return new VariableSimpleBuilder(m.getName(), m.getDescription(), m.getUnitsString(), m.getArrayType(),
        Dimensions.makeDimensionsAnon(m.getShape()));
  }

  /** @deprecated use makeScalar(String name, String desc, String units, ArrayType dt) */
  @Deprecated
  public static VariableSimpleBuilder makeScalar(String name, String desc, String units, DataType dt) {
    return new VariableSimpleBuilder(name, desc, units, dt, null);
  }

  public static VariableSimpleBuilder makeScalar(String name, String desc, String units, ArrayType dt) {
    return new VariableSimpleBuilder(name, desc, units, dt, null);
  }

  public static VariableSimpleBuilder makeString(String name, String desc, String units, int str_len) {
    Dimension d = Dimension.builder(name + "_strlen", str_len).setIsShared(false).build();
    return new VariableSimpleBuilder(name, desc, units, ArrayType.CHAR, Collections.singletonList(d));
  }

  private final String name, desc, units;
  private final ArrayType dt;
  private final AttributeContainerMutable atts;
  private final ImmutableList<Dimension> dims;

  /**
   * @deprecated use VariableSimpleBuilder(String name, String desc, String units, ArrayType dt, List<Dimension> dims)
   */
  @Deprecated
  public VariableSimpleBuilder(String name, String desc, String units, DataType dt, List<Dimension> dims) {
    this.name = name;
    this.desc = desc;
    this.units = units;
    this.dt = dt.getArrayType();
    this.dims = (dims == null || dims.size() == 0) ? ImmutableList.of() : ImmutableList.copyOf(dims);
    this.atts = new AttributeContainerMutable(name);

    if (units != null) {
      atts.addAttribute(new Attribute(CDM.UNITS, units));
    }
    if (desc != null) {
      atts.addAttribute(new Attribute(CDM.LONG_NAME, desc));
    }
  }

  public VariableSimpleBuilder(String name, String desc, String units, ArrayType dt, List<Dimension> dims) {
    this.name = name;
    this.desc = desc;
    this.units = units;
    this.dt = dt;
    this.dims = (dims == null || dims.size() == 0) ? ImmutableList.of() : ImmutableList.copyOf(dims);
    this.atts = new AttributeContainerMutable(name);

    if (units != null) {
      atts.addAttribute(new Attribute(CDM.UNITS, units));
    }
    if (desc != null) {
      atts.addAttribute(new Attribute(CDM.LONG_NAME, desc));
    }
  }

  public VariableSimpleBuilder addAttribute(Attribute att) {
    atts.addAttribute(att);
    return this;
  }

  public VariableSimpleBuilder addAttribute(String name, String value) {
    atts.addAttribute(name, value);
    return this;
  }

  public VariableSimpleIF build() {
    return new VariableSimple(this);
  }

  @Immutable
  private static class VariableSimple implements VariableSimpleIF {
    private final String name, desc, units;
    private final ArrayType dt;
    private final AttributeContainer atts;
    private final ImmutableList<Dimension> dims;

    private VariableSimple(VariableSimpleBuilder builder) {
      this.name = builder.name;
      this.desc = builder.desc;
      this.units = builder.units;
      this.dt = builder.dt;
      this.atts = builder.atts.toImmutable();
      this.dims = builder.dims;
    }

    @Override
    public String getFullName() {
      return name;
    }

    @Override
    public String getShortName() {
      return name;
    }

    @Override
    public String getDescription() {
      return desc;
    }

    @Override
    public String getUnitsString() {
      return units;
    }

    @Override
    public int getRank() {
      return this.dims.size();
    }

    @Override
    public int[] getShape() {
      return Dimensions.makeShape(this.dims);
    }

    @Override
    public ImmutableList<Dimension> getDimensions() {
      return dims;
    }

    @Override
    public DataType getDataType() {
      return dt.getDataType();
    }

    @Override
    public ArrayType getArrayType() {
      return dt;
    }

    @Override
    public AttributeContainer attributes() {
      return new AttributeContainerMutable(name, atts);
    }

    @Override
    public int compareTo(VariableSimpleIF o) {
      return name.compareTo(o.getShortName()); // ??
    }
  }
}
