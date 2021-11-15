/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.dataset.NetcdfDataset.Enhance;
import ucar.nc2.internal.dataset.CoordinatesHelper;
import ucar.nc2.internal.dataset.DataEnhancer;
import ucar.nc2.internal.dataset.EnhanceScaleMissingUnsigned;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.Indent;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Formatter;
import java.util.Set;

/**
 * A wrapper around a Variable, creating an "enhanced" Variable. The original Variable is used for the I/O.
 * There are several distinct uses:
 * <ol>
 * <li>Handle scale/offset/missing/enum/unsigned conversion; this can change DataType and data values</li>
 * <li>Container for coordinate system information</li>
 * <li>NcML modifications to underlying Variable</li>
 * </ol>
 */
@Immutable
public class VariableDS extends Variable implements VariableEnhanced {

  /**
   * Create a VariableDS from orgVar, with default enhancements if requested.
   * 
   * @param group Reparent to this Group.
   * @param orgVar Wrap this Variable.
   * @param enhance opriona default enhancements.
   */
  public static VariableDS fromVar(Group group, Variable orgVar, boolean enhance) {
    Preconditions.checkArgument(!(orgVar instanceof Structure),
        "VariableDS must not wrap a Structure; name=" + orgVar.getFullName());
    VariableDS.Builder<?> builder = VariableDS.builder().copyFrom(orgVar);
    if (enhance) {
      builder.setEnhanceMode(NetcdfDataset.getDefaultEnhanceMode());
    }
    // Add this so that old VariableDS units agrees with new VariableDS units.
    String units = orgVar.getUnitsString();
    if (units != null) {
      builder.setUnits(units.trim());
    }
    return builder.build(group);
  }

  @Override
  public ArrayType getArrayType() {
    if (convertedDataType != null) {
      return convertedDataType;
    }
    return super.getArrayType();
  }

  @Override
  public NetcdfFile getNetcdfFile() {
    // TODO can group really be null? Variable says no.
    return getParentGroup() == null ? null : getParentGroup().getNetcdfFile();
  }

  /** true if Variable has missing data values */
  public boolean hasMissing() {
    return scaleMissingUnsignedProxy.hasMissing();
  }

  /**
   * true if the argument is a missing value.
   * Note that {@link Float#NaN} and {@link Double#NaN} are always considered missing data.
   * 
   * @param val an unpacked value.
   */
  public boolean isMissing(double val) {
    return scaleMissingUnsignedProxy.isMissing(val);
  }

  /** Does data need to be converted? */
  public boolean convertNeeded() {
    if (enhanceMode.contains(Enhance.ConvertEnums)
        && (getArrayType().isEnum() || (orgDataType != null && orgDataType.isEnum()))) {
      return true;
    }
    if (enhanceMode.contains(Enhance.ConvertMissing) && scaleMissingUnsignedProxy.hasMissing()) {
      return true;
    }
    if (enhanceMode.contains(Enhance.ApplyScaleOffset) && scaleMissingUnsignedProxy.hasScaleOffset()) {
      return true;
    }
    if (enhanceMode.contains(Enhance.ConvertUnsigned) && getArrayType().isUnsigned()) {
      return true;
    }
    return false;
  }

  /** Convert the data using the VariableDS enhancements. Generally the user does not have to call this. */
  public ucar.array.Array<?> convertArray(ucar.array.Array<?> data) {
    return dataEnhancer.convertArray(data, enhanceMode);
  }

  /**
   * Returns the enhancements applied to this variable. If this variable wraps another variable, the returned set will
   * also contain the enhancements applied to the nested variable, recursively.
   *
   * @return the enhancements applied to this variable.
   */
  public Set<Enhance> getEnhanceMode() {
    if (!(orgVar instanceof VariableDS)) {
      return Collections.unmodifiableSet(enhanceMode);
    } else {
      VariableDS orgVarDS = (VariableDS) orgVar;
      return Sets.union(enhanceMode, orgVarDS.getEnhanceMode());
    }
  }

  /** A VariableDS usually wraps another Variable. */
  @Nullable
  @Override
  public Variable getOriginalVariable() {
    return orgVar;
  }

  /**
   * When this wraps another Variable, get the original Variable's ArrayType.
   * 
   * @return original Variable's ArrayType, or current data type if it doesnt wrap another variable
   */
  public ArrayType getOriginalArrayType() {
    return orgDataType != null ? orgDataType : getArrayType();
  }

  /**
   * When this wraps another Variable, get the original Variable's name.
   *
   * @return original Variable's name, or null.
   */
  @Override
  @Nullable
  public String getOriginalName() {
    return orgName;
  }

  @Override
  @Nullable
  public String lookupEnumString(int val) {
    if (getArrayType().isEnum()) {
      return super.lookupEnumString(val);
    }
    if (orgVar != null) {
      return orgVar.lookupEnumString(val);
    }
    return null;
  }

  @Override
  public String getDatasetLocation() {
    String result = super.getDatasetLocation();
    if (result != null)
      return result;
    if (orgVar != null)
      return orgVar.getDatasetLocation();
    return null;
  }

  @Override
  public void setCaching(boolean caching) {
    if (caching && orgVar != null) {
      orgVar.setCaching(true); // propagate down only if true LOOK why?
    }
  }

  @Override
  public String toString() {
    Formatter f = new Formatter();
    writeCDL(f, new Indent(2), false, false);
    if (orgVar != null) {
      f.format("%nOriginal: %s %s%n", orgDataType, orgVar.getNameAndDimensions());
    }
    f.format("Coordinate Systems%n");
    for (CoordinateSystem csys : getCoordinateSystems()) {
      f.format(" %s (%d)%n", csys.getName(), csys.getCoordinateAxes().size());
    }
    return f.toString();
  }

  ////////////////////////////////////////////////////////////////////////

  @Override
  protected Array<?> _read() throws IOException {
    Array<?> result = super._read();
    return convertArray(result);
  }

  @Override
  public ucar.array.Array<?> readArray() throws IOException {
    ucar.array.Array<?> result;

    // check if already cached - caching in VariableDS only done explicitly by app
    if (hasCachedData())
      result = super.readArray();
    else
      result = proxyReader.proxyReadArray(this, null);

    return convertArray(result);
  }

  @Override
  public ucar.array.Array<?> proxyReadArray(Variable client, CancelTask cancelTask) throws IOException {
    if (orgVar == null) {
      return getMissingDataArray(getShape());
    }

    return orgVar.readArray();
  }

  @Override
  public ucar.array.Array<?> readArray(ucar.array.Section section)
      throws IOException, ucar.array.InvalidRangeException {
    // really a full read
    if ((null == section) || section.computeSize() == getSize()) {
      return readArray();
    }
    section = ucar.array.Section.fill(section, getShape());

    ucar.array.Array<?> result;
    if (hasCachedData()) {
      result = super.readArray(section);
    } else {
      result = proxyReader.proxyReadArray(this, section, null);
    }

    return convertArray(result);
  }

  @Override
  public ucar.array.Array<?> proxyReadArray(Variable client, ucar.array.Section section, CancelTask cancelTask)
      throws IOException, ucar.array.InvalidRangeException {
    // see if its really a full read
    if ((null == section) || section.computeSize() == getSize()) {
      return proxyReadArray(client, cancelTask);
    }

    if (orgVar == null) {
      return getMissingDataArray(section.getShape());
    }

    return orgVar.readArray(section);
  }

  /**
   * Return Array with missing data
   * 
   * @param shape of this shape
   * @return Array with given shape
   */
  private Array<?> getMissingDataArray(int[] shape) {
    double fillValue = 0;
    if (scaleMissingUnsignedProxy.hasFillValue()) {
      fillValue = scaleMissingUnsignedProxy.getFillValue();
    }

    return Arrays.factoryFill(getArrayType(), shape, fillValue);
  }

  /** Visible for testing, do not use directly. */
  @VisibleForTesting
  public EnhanceScaleMissingUnsigned scaleMissingUnsignedProxy() {
    return scaleMissingUnsignedProxy;
  }

  ////////////////////////////////////////////// Enhancements //////////////////////////////////////////////

  @Override
  public String getDescription() {
    return enhanceProxy.getDescription();
  }

  @Override
  public String getUnitsString() {
    return enhanceProxy.getUnitsString();
  }

  @Override
  public ImmutableList<CoordinateSystem> getCoordinateSystems() {
    return this.coordinateSystems == null ? ImmutableList.of() : this.coordinateSystems;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////
  private final EnhancementsImpl enhanceProxy;
  private final EnhanceScaleMissingUnsigned scaleMissingUnsignedProxy;
  private final Set<Enhance> enhanceMode; // The set of enhancements that were made.
  private final DataEnhancer dataEnhancer;
  private final ArrayType convertedDataType;

  protected final @Nullable Variable orgVar; // wrap this Variable : use it for the I/O
  protected final ArrayType orgDataType; // keep separate for the case where there is no orgVar. TODO @Nullable?
  protected final @Nullable String orgName; // Variable was renamed, must keep track of the original name
  final String orgFileTypeId; // the original fileTypeId. TODO @Nullable?

  // Not technically immutable because of this
  private ImmutableList<CoordinateSystem> coordinateSystems;

  protected VariableDS(Builder<?> builder, Group parentGroup) {
    super(builder, parentGroup);

    this.enhanceMode = builder.enhanceMode != null ? builder.enhanceMode : EnumSet.noneOf(Enhance.class);
    this.orgVar = builder.orgVar;
    this.orgDataType = builder.orgDataType;
    this.orgName = builder.orgName;

    // Make sure that units has been trimmed.
    // Replace with correct case
    // TODO Can simplify when doesnt have to agree with old VariableDS
    Attribute units = builder.getAttributeContainer().findAttributeIgnoreCase(CDM.UNITS);
    if (units != null && units.isString()) {
      builder.getAttributeContainer()
          .addAttribute(Attribute.builder(CDM.UNITS).setStringValue(units.getStringValue().trim()).build());
    }

    this.orgFileTypeId = builder.orgFileTypeId;
    this.enhanceProxy = new EnhancementsImpl(this, builder.units, builder.getDescription());
    this.scaleMissingUnsignedProxy = new EnhanceScaleMissingUnsigned(this, this.enhanceMode);
    this.scaleMissingUnsignedProxy.setFillValueIsMissing(builder.fillValueIsMissing);
    this.scaleMissingUnsignedProxy.setInvalidDataIsMissing(builder.invalidDataIsMissing);
    this.scaleMissingUnsignedProxy.setMissingDataIsMissing(builder.missingDataIsMissing);

    ArrayType useArrayType = null;
    if (this.enhanceMode.contains(Enhance.ConvertEnums) && builder.dataType.isEnum()) {
      useArrayType = ArrayType.STRING;
    }
    if (this.enhanceMode.contains(Enhance.ConvertUnsigned) && !builder.dataType.isEnum()) {
      // We may need a larger data type to hold the results of the unsigned conversion.
      useArrayType = scaleMissingUnsignedProxy.getUnsignedConversionType();
    }

    if (this.enhanceMode.contains(Enhance.ApplyScaleOffset)
        && (builder.dataType.isNumeric() || builder.dataType == ArrayType.CHAR)
        && scaleMissingUnsignedProxy.hasScaleOffset()) {
      useArrayType = scaleMissingUnsignedProxy.getScaledOffsetType();
    }
    this.convertedDataType = useArrayType;

    // We have to complete this after the NetcdfDataset is built.
    this.dataEnhancer = new DataEnhancer(this, this.scaleMissingUnsignedProxy);
  }

  public Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  // Add local fields to the builder.
  protected Builder<?> addLocalFieldsToBuilder(Builder<? extends Builder<?>> builder) {
    builder.setOriginalVariable(this.orgVar).setOriginalArrayType(this.orgDataType).setOriginalName(this.orgName)
        .setOriginalFileTypeId(this.orgFileTypeId).setEnhanceMode(this.enhanceMode)
        .setUnits(this.enhanceProxy.getUnitsString()).setDesc(this.enhanceProxy.getDescription());

    return (VariableDS.Builder<?>) super.addLocalFieldsToBuilder(builder);
  }

  // Not technically immutable because of this
  void setCoordinateSystems(CoordinatesHelper coords) {
    if (this.coordinateSystems != null) {
      throw new RuntimeException("Cant call twice");
    }
    this.coordinateSystems = coords.makeCoordinateSystemsFor(this);
  }

  /** Get Builder for this class that allows subclassing. */
  public static Builder<?> builder() {
    return new Builder2();
  }

  private static class Builder2 extends Builder<Builder2> {
    @Override
    protected Builder2 self() {
      return this;
    }
  }

  public static abstract class Builder<T extends Builder<T>> extends Variable.Builder<T> {
    public Set<Enhance> enhanceMode = EnumSet.noneOf(Enhance.class);
    public Variable orgVar; // wrap this Variable : use it for the I/O
    public ArrayType orgDataType; // keep separate for the case where there is no orgVar.
    public String orgFileTypeId; // the original fileTypeId.
    String orgName; // in case Variable was renamed, and we need to keep track of the original name
    private String units;
    private String desc;

    private boolean invalidDataIsMissing = NetcdfDataset.invalidDataIsMissing;
    private boolean fillValueIsMissing = NetcdfDataset.fillValueIsMissing;
    private boolean missingDataIsMissing = NetcdfDataset.missingDataIsMissing;

    private boolean built;

    protected abstract T self();

    public T setEnhanceMode(Set<Enhance> enhanceMode) {
      this.enhanceMode = enhanceMode;
      return self();
    }

    public T addEnhanceMode(Set<Enhance> enhanceMode) {
      this.enhanceMode.addAll(enhanceMode);
      return self();
    }

    public T setOriginalVariable(Variable orgVar) {
      this.orgVar = orgVar;
      return self();
    }

    public T setOriginalArrayType(ArrayType orgDataType) {
      this.orgDataType = orgDataType;
      return self();
    }

    public T setOriginalName(String orgName) {
      this.orgName = orgName;
      return self();
    }

    public T setOriginalFileTypeId(String orgFileTypeId) {
      this.orgFileTypeId = orgFileTypeId;
      return self();
    }

    public T setUnits(String units) {
      this.units = units;
      if (units != null) {
        this.units = units.trim();
        addAttribute(new Attribute(CDM.UNITS, this.units));
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

    public void setFillValueIsMissing(boolean b) {
      this.fillValueIsMissing = b;
    }

    public void setInvalidDataIsMissing(boolean b) {
      this.invalidDataIsMissing = b;
    }

    public void setMissingDataIsMissing(boolean b) {
      this.missingDataIsMissing = b;
    }

    /** Copy of this builder. */
    @Override
    public Variable.Builder<?> copy() {
      return new Builder2().copyFrom(this);
    }

    /** Copy metadata from orgVar. */
    @Override
    public T copyFrom(Variable orgVar) {
      super.copyFrom(orgVar);
      setSPobject(null);
      // resetCache();
      setOriginalVariable(orgVar);
      setOriginalArrayType(orgVar.getArrayType());
      setOriginalName(orgVar.getShortName());

      this.orgFileTypeId = orgVar.getFileTypeId();
      return self();
    }

    public T copyFrom(VariableDS.Builder<?> builder) {
      super.copyFrom(builder);

      setDesc(builder.desc);
      setEnhanceMode(builder.enhanceMode);
      setFillValueIsMissing(builder.fillValueIsMissing);
      setInvalidDataIsMissing(builder.invalidDataIsMissing);
      setMissingDataIsMissing(builder.missingDataIsMissing);
      this.orgVar = builder.orgVar;
      this.orgDataType = builder.orgDataType;
      this.orgFileTypeId = builder.orgFileTypeId;
      this.orgName = builder.orgName;
      setUnits(builder.units);

      return self();
    }

    public String getUnits() {
      String result = units;
      if (result == null) {
        result = getAttributeContainer().findAttributeString(CDM.UNITS, null);
      }
      if (result == null && orgVar != null) {
        result = orgVar.attributes().findAttributeString(CDM.UNITS, null);
      }
      return (result == null) ? null : result.trim();
    }

    public String getDescription() {
      String result = desc;
      if (result == null) {
        result = getAttributeContainer().findAttributeString(CDM.LONG_NAME, null);
      }
      if (result == null && orgVar != null) {
        result = orgVar.attributes().findAttributeString(CDM.LONG_NAME, null);
      }
      return (result == null) ? null : result.trim();
    }

    /** Normally this is called by Group.build() */
    public VariableDS build(Group parentGroup) {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      return new VariableDS(this, parentGroup);
    }
  }
}
