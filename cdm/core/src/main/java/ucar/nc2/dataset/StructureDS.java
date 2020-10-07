/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset;

import com.google.common.collect.ImmutableList;
import javax.annotation.Nullable;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.internal.dataset.StructureDataArrayEnhancer;
import ucar.nc2.util.CancelTask;
import ucar.ma2.*;
import java.io.IOException;

/** An "enhanced" Structure. */
public class StructureDS extends ucar.nc2.Structure implements StructureEnhanced {

  /** A StructureDS may wrap another Structure. */
  @Nullable
  public Variable getOriginalVariable() {
    return orgVar;
  }

  /** When this wraps another Variable, get the original Variable's DataType. */
  public DataType getOriginalDataType() {
    return DataType.STRUCTURE;
  }

  /** When this wraps another Variable, get the original Variable's name. */
  public String getOriginalName() {
    return orgName;
  }

  @Override
  @Deprecated
  public Array reallyRead(Variable client, CancelTask cancelTask) throws IOException {
    Array result;

    if (hasCachedData())
      result = super.reallyRead(client, cancelTask);
    else if (orgVar != null)
      result = orgVar.read();
    else {
      throw new IllegalStateException("StructureDS has no way to get data");
      // Object data = smProxy.getFillValue(getDataType());
      // return Array.factoryConstant(dataType.getPrimitiveClassType(), getShape(), data);
    }

    StructureDataEnhancer enhancer = new StructureDataEnhancer(this);
    return enhancer.enhance((ArrayStructure) result, null);
  }

  @Override
  public ucar.array.Array<?> proxyReadArray(Variable client, CancelTask cancelTask) throws IOException {
    ucar.array.Array<?> result;

    if (hasCachedData()) {
      result = super.proxyReadArray(client, cancelTask);
    } else if (orgVar != null) {
      result = orgVar.readArray();
    } else {
      throw new IllegalStateException("StructureDS has no way to get data");
      // Object data = smProxy.getFillValue(getDataType());
      // return Array.factoryConstant(dataType.getPrimitiveClassType(), getShape(), data);
    }

    StructureDataArrayEnhancer enhancer = new StructureDataArrayEnhancer(this, (ucar.array.StructureDataArray) result);
    return enhancer.enhance();
  }

  @Override
  @Deprecated
  public Array reallyRead(Variable client, Section section, CancelTask cancelTask)
      throws IOException, InvalidRangeException {
    if (section.computeSize() == getSize()) {
      return _read();
    }

    Array result;
    if (hasCachedData()) {
      result = super.reallyRead(client, section, cancelTask);
    } else if (orgVar != null) {
      result = orgVar.read(section);
    } else {
      throw new IllegalStateException("StructureDS has no way to get data");
      // Object data = smProxy.getFillValue(getDataType());
      // return Array.factoryConstant(dataType.getPrimitiveClassType(), section.getShape(), data);
    }

    // do any needed conversions (enum/scale/offset/missing/unsigned, etc)
    StructureDataEnhancer enhancer = new StructureDataEnhancer(this);
    return enhancer.enhance((ArrayStructure) result, section);
  }

  @Override
  public ucar.array.Array<?> proxyReadArray(Variable client, Section section, CancelTask cancelTask)
      throws IOException, InvalidRangeException {
    if (section.computeSize() == getSize()) {
      return proxyReadArray(client, cancelTask);
    }

    ucar.array.Array<?> result;
    if (hasCachedData())
      result = super.proxyReadArray(client, section, cancelTask);
    else if (orgVar != null)
      result = orgVar.readArray(section);
    else {
      throw new IllegalStateException("StructureDS has no way to get data");
    }

    // do any needed conversions (enum/scale/offset/missing/unsigned, etc)
    StructureDataArrayEnhancer enhancer = new StructureDataArrayEnhancer(this, (ucar.array.StructureDataArray) result);
    return enhancer.enhance();
  }

  public ImmutableList<CoordinateSystem> getCoordinateSystems() {
    return proxy.getCoordinateSystems();
  }

  public java.lang.String getDescription() {
    return proxy.getDescription();
  }

  public java.lang.String getUnitsString() {
    return proxy.getUnitsString();
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////
  protected final EnhancementsImpl proxy;
  protected final Structure orgVar; // wrap this Variable
  protected final String orgName; // in case Variable was renamed, and we need the original name for aggregation

  protected StructureDS(Builder<?> builder, Group parentGroup) {
    super(builder, parentGroup);
    this.orgVar = builder.orgVar;
    this.orgName = builder.orgName;
    this.proxy = new EnhancementsImpl(this, builder.units, builder.desc);
  }

  @Override
  public Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  // Add local fields to the passed - in builder.
  protected Builder<?> addLocalFieldsToBuilder(Builder<? extends Builder<?>> b) {
    b.setOriginalVariable(this.orgVar).setOriginalName(this.orgName).setUnits(this.proxy.getUnitsString())
        .setDesc(this.proxy.getDescription());
    return (Builder<?>) super.addLocalFieldsToBuilder(b);
  }

  public static Builder<?> builder() {
    return new Builder2();
  }

  private static class Builder2 extends Builder<Builder2> {
    @Override
    protected Builder2 self() {
      return this;
    }
  }

  public static abstract class Builder<T extends Builder<T>> extends Structure.Builder<T> {
    private Structure orgVar; // wrap this Variable
    protected String orgName; // in case Variable was renamed, and we need the original name for aggregation
    protected String units;
    protected String desc;
    private boolean built;

    public T setOriginalVariable(Structure orgVar) {
      this.orgVar = orgVar;
      return self();
    }

    public T setOriginalName(String orgName) {
      this.orgName = orgName;
      return self();
    }

    public T setUnits(String units) {
      this.units = units;
      if (units != null) {
        addAttribute(new Attribute(CDM.UNITS, units));
      }
      return self();
    }

    public T setDesc(String desc) {
      this.desc = desc;
      if (desc != null) {
        addAttribute(new Attribute(CDM.LONG_NAME, desc));
      }
      return self();
    }

    /** Copy metadata from orgVar. */
    public T copyFrom(Structure orgVar) {
      super.copyFrom(orgVar);
      for (Variable v : orgVar.getVariables()) {
        Variable.Builder<?> newVar;
        if (v instanceof Sequence) {
          newVar = SequenceDS.builder().copyFrom((Sequence) v);
        } else if (v instanceof Structure) {
          newVar = StructureDS.builder().copyFrom((Structure) v);
        } else {
          newVar = VariableDS.builder().copyFrom(v);
        }
        addMemberVariable(newVar);
      }
      setOriginalVariable(orgVar);
      setOriginalName(orgVar.getShortName());
      return self();
    }

    /** Normally this is called by Group.build() */
    public StructureDS build(Group parentGroup) {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      this.setDataType(DataType.STRUCTURE);
      return new StructureDS(this, parentGroup);
    }
  }

}
