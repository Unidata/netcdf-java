/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import com.google.common.collect.ImmutableList;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import ucar.array.ArrayType;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.internal.dataset.CoordinatesHelper;
import ucar.nc2.internal.dataset.StructureDataArrayEnhancer;
import ucar.nc2.util.CancelTask;
import java.io.IOException;
import java.util.List;

/** An "enhanced" Structure. */
@Immutable
public class StructureDS extends ucar.nc2.Structure implements StructureEnhanced {

  /** A StructureDS may wrap another Structure. */
  @Nullable
  public Variable getOriginalVariable() {
    return orgVar;
  }

  /** When this wraps another Variable, get the original Variable's name. */
  public String getOriginalName() {
    return orgName;
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
    }

    StructureDataArrayEnhancer enhancer = new StructureDataArrayEnhancer(this, (ucar.array.StructureDataArray) result);
    return enhancer.enhance();
  }

  @Override
  public ucar.array.Array<?> proxyReadArray(Variable client, ucar.array.Section section, CancelTask cancelTask)
      throws IOException, ucar.array.InvalidRangeException {
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

  @Override
  public List<CoordinateSystem> getCoordinateSystems() {
    return this.coordinateSystems == null ? ImmutableList.of() : this.coordinateSystems;
  }

  public java.lang.String getDescription() {
    return proxy.getDescription();
  }

  public java.lang.String getUnitsString() {
    return proxy.getUnitsString();
  }

  // Not technically immutable because of this
  void setCoordinateSystems(CoordinatesHelper coords) {
    if (this.coordinateSystems != null) {
      throw new RuntimeException("Cant call twice");
    }
    this.coordinateSystems = ImmutableList.copyOf(coords.makeCoordinateSystemsFor(this));
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////
  protected final EnhancementsImpl proxy;
  protected final Structure orgVar; // wrap this Variable
  protected final String orgName; // in case Variable was renamed, and we need the original name for aggregation

  // Not technically immutable because of this
  private ImmutableList<CoordinateSystem> coordinateSystems;

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

  /** Get a Builder of StructureDS. */
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
      this.setArrayType(ArrayType.STRUCTURE);
      return new StructureDS(this, parentGroup);
    }
  }

}
