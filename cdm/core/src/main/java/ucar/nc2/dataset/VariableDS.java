/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import ucar.array.ArrayType;
import ucar.array.ArraysConvert;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.dataset.NetcdfDataset.Enhance;
import ucar.nc2.internal.dataset.CoordinatesHelper;
import ucar.nc2.internal.dataset.DataEnhancer;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.Indent;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

/**
 * A wrapper around a Variable, creating an "enhanced" Variable. The original Variable is used for the I/O.
 * There are several distinct uses:
 * <ol>
 * <li>Handle scale/offset/missing/enum/unsigned conversion; this can change DataType and data values</li>
 * <li>Container for coordinate system information</li>
 * <li>NcML modifications to underlying Variable</li>
 * </ol>
 */
public class VariableDS extends Variable implements EnhanceScaleMissingUnsigned, VariableEnhanced {

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
  public NetcdfFile getNetcdfFile() {
    // TODO can group really be null? Variable says no.
    return getParentGroup() == null ? null : getParentGroup().getNetcdfFile();
  }

  public boolean convertNeeded() {
    if (enhanceMode.contains(Enhance.ConvertEnums)
        && (dataType.isEnum() || (orgDataType != null && orgDataType.isEnum()))) {
      return true;
    }
    if (enhanceMode.contains(Enhance.ConvertMissing) && hasMissing()) {
      return true;
    }
    if (enhanceMode.contains(Enhance.ApplyScaleOffset) && hasScaleOffset()) {
      return true;
    }
    if (enhanceMode.contains(Enhance.ConvertUnsigned) && dataType.isUnsigned()) {
      return true;
    }
    return false;
  }

  boolean needConvert() {
    Set<Enhance> enhancements = getEnhanceMode();
    return enhancements.contains(Enhance.ConvertEnums) || enhancements.contains(Enhance.ConvertUnsigned)
        || enhancements.contains(Enhance.ApplyScaleOffset) || enhancements.contains(Enhance.ConvertMissing);
  }

  ucar.ma2.Array convert(ucar.ma2.Array data) {
    return dataEnhancer.convert(data, enhanceMode);
  }

  ucar.ma2.Array convert(ucar.ma2.Array data, Set<NetcdfDataset.Enhance> enhancements) {
    return dataEnhancer.convert(data, enhancements);
  }

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

  /** @deprecated use getOriginalArrayType() */
  public ucar.ma2.DataType getOriginalDataType() {
    return getOriginalArrayType().getDataType();
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
  public String lookupEnumString(int val) {
    if (dataType.isEnum())
      return super.lookupEnumString(val);
    return orgVar.lookupEnumString(val);
  }

  @Override
  public String toStringDebug() {
    return (orgVar != null) ? orgVar.toStringDebug() : "";
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
  @Deprecated
  protected ucar.ma2.Array _read() throws IOException {
    ucar.ma2.Array result;

    // check if already cached - caching in VariableDS only done explicitly by app
    if (hasCachedData())
      result = super._read();
    else
      result = proxyReader.reallyRead(this, null);

    return convert(result);
  }

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
  @Deprecated
  public ucar.ma2.Array reallyRead(Variable client, CancelTask cancelTask) throws IOException {
    if (orgVar == null) {
      return getMissingDataArray(shape);
    }

    return orgVar.read();
  }

  @Override
  public ucar.array.Array<?> proxyReadArray(Variable client, CancelTask cancelTask) throws IOException {
    if (orgVar == null) {
      // LOOK where is this used? Do we need to make fast?
      return ArraysConvert.convertToArray(getMissingDataArray(shape));
    }

    return orgVar.readArray();
  }

  // section of regular Variable
  @Override
  @Deprecated
  protected ucar.ma2.Array _read(ucar.ma2.Section section) throws IOException, ucar.ma2.InvalidRangeException {
    // really a full read
    if ((null == section) || section.computeSize() == getSize()) {
      return _read();
    }

    ucar.ma2.Array result;
    if (hasCachedData())
      result = super._read(section);
    else
      result = proxyReader.reallyRead(this, section, null);

    return convert(result);
  }

  @Override
  @Deprecated
  public ucar.ma2.Array reallyRead(Variable client, ucar.ma2.Section section, CancelTask cancelTask)
      throws IOException, ucar.ma2.InvalidRangeException {
    // see if its really a full read
    if ((null == section) || section.computeSize() == getSize()) {
      return reallyRead(client, cancelTask);
    }

    if (orgVar == null) {
      return getMissingDataArray(section.getShape());
    }

    return orgVar.read(section);
  }

  @Override
  public ucar.array.Array<?> readArray(ucar.array.Section section)
      throws IOException, ucar.array.InvalidRangeException {
    // really a full read
    if ((null == section) || section.computeSize() == getSize()) {
      return readArray();
    }

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
      // LOOK where is this used? Do we need to make fast?
      return ArraysConvert.convertToArray(getMissingDataArray(section.getShape()));
    }

    return orgVar.readArray(section);
  }

  @Override
  public long readToStream(ucar.ma2.Section section, OutputStream out)
      throws IOException, ucar.ma2.InvalidRangeException {
    if (orgVar == null)
      return super.readToStream(section, out);

    return orgVar.readToStream(section, out);
  }

  /**
   * Return Array with missing data
   *
   * @param shape of this shape
   * @return Array with given shape
   * @deprecated use Arrays.getMissingDataArray()
   */
  @Deprecated
  public ucar.ma2.Array getMissingDataArray(int[] shape) {
    Object storage;

    switch (getDataType()) {
      case BOOLEAN:
        storage = new boolean[1];
        break;
      case BYTE:
      case UBYTE:
      case ENUM1:
        storage = new byte[1];
        break;
      case CHAR:
        storage = new char[1];
        break;
      case SHORT:
      case USHORT:
      case ENUM2:
        storage = new short[1];
        break;
      case INT:
      case UINT:
      case ENUM4:
        storage = new int[1];
        break;
      case LONG:
      case ULONG:
        storage = new long[1];
        break;
      case FLOAT:
        storage = new float[1];
        break;
      case DOUBLE:
        storage = new double[1];
        break;
      default:
        storage = new Object[1];
    }

    ucar.ma2.Array array = ucar.ma2.Array.factoryConstant(getDataType(), shape, storage);
    if (scaleMissingUnsignedProxy.hasFillValue()) {
      array.setObject(0, scaleMissingUnsignedProxy.getFillValue());
    }
    return array;
  }

  /**
   * public for debugging
   *
   * @param f put info here
   * @deprecated use Arrays.getMissingDataArray()
   */
  @Deprecated
  public void showScaleMissingProxy(Formatter f) {
    f.format("has missing = %s%n", scaleMissingUnsignedProxy.hasMissing());
    if (scaleMissingUnsignedProxy.hasMissing()) {
      if (scaleMissingUnsignedProxy.hasMissingValue()) {
        f.format("   missing value(s) = ");
        for (double d : scaleMissingUnsignedProxy.getMissingValues())
          f.format(" %f", d);
        f.format("%n");
      }
      if (scaleMissingUnsignedProxy.hasFillValue())
        f.format("   fillValue = %f%n", scaleMissingUnsignedProxy.getFillValue());
      if (scaleMissingUnsignedProxy.hasValidData())
        f.format("   valid min/max = [%f,%f]%n", scaleMissingUnsignedProxy.getValidMin(),
            scaleMissingUnsignedProxy.getValidMax());
    }
    f.format("FillValue or default = %s%n", scaleMissingUnsignedProxy.getFillValue());

    f.format("%nhas scale/offset = %s%n", scaleMissingUnsignedProxy.hasScaleOffset());
    if (scaleMissingUnsignedProxy.hasScaleOffset()) {
      double offset = scaleMissingUnsignedProxy.applyScaleOffset(0.0);
      double scale = scaleMissingUnsignedProxy.applyScaleOffset(1.0) - offset;
      f.format("   scale_factor = %f add_offset = %f%n", scale, offset);
    }
    f.format("original data type = %s%n", orgDataType);
    f.format("converted data type = %s%n", getDataType());
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

  //////////////////////////////////////////// EnhanceScaleMissingUnsigned ////////////////////////////////////////////

  /** @deprecated do not use */
  @Override
  @Deprecated
  public boolean hasScaleOffset() {
    return scaleMissingUnsignedProxy.hasScaleOffset();
  }

  /** @deprecated do not use */
  @Override
  @Deprecated
  public double getScaleFactor() {
    return scaleMissingUnsignedProxy.getScaleFactor();
  }

  /** @deprecated do not use */
  @Override
  @Deprecated
  public double getOffset() {
    return scaleMissingUnsignedProxy.getOffset();
  }

  /** @deprecated do not use */
  @Override
  @Deprecated
  public boolean hasMissing() {
    return scaleMissingUnsignedProxy.hasMissing();
  }

  /** @deprecated do not use */
  @Deprecated
  public boolean isMissing(double val) {
    return scaleMissingUnsignedProxy.isMissing(val);
  }

  @Override
  public boolean hasValidData() {
    return scaleMissingUnsignedProxy.hasValidData();
  }

  /** @deprecated do not use */
  @Override
  @Deprecated
  public double getValidMin() {
    return scaleMissingUnsignedProxy.getValidMin();
  }

  /** @deprecated do not use */
  @Override
  @Deprecated
  public double getValidMax() {
    return scaleMissingUnsignedProxy.getValidMax();
  }

  /** @deprecated do not use */
  @Override
  @Deprecated
  public boolean isInvalidData(double val) {
    return scaleMissingUnsignedProxy.isInvalidData(val);
  }

  /** @deprecated do not use */
  @Override
  @Deprecated
  public boolean hasFillValue() {
    return scaleMissingUnsignedProxy.hasFillValue();
  }

  /** @deprecated do not use */
  @Override
  @Deprecated
  public double getFillValue() {
    return scaleMissingUnsignedProxy.getFillValue();
  }

  /** @deprecated do not use */
  @Override
  @Deprecated
  public boolean isFillValue(double val) {
    return scaleMissingUnsignedProxy.isFillValue(val);
  }

  /** @deprecated do not use */
  @Override
  @Deprecated
  public boolean hasMissingValue() {
    return scaleMissingUnsignedProxy.hasMissingValue();
  }

  /** @deprecated do not use */
  @Override
  @Deprecated
  public double[] getMissingValues() {
    return scaleMissingUnsignedProxy.getMissingValues();
  }

  /** @deprecated do not use */
  @Override
  @Deprecated
  public boolean isMissingValue(double val) {
    return scaleMissingUnsignedProxy.isMissingValue(val);
  }

  /** @deprecated do not use */
  @Override
  @Deprecated
  @Nullable
  public ucar.ma2.DataType getScaledOffsetType() {
    return scaleMissingUnsignedProxy.getScaledOffsetType();
  }

  /** @deprecated do not use */
  @Override
  @Deprecated
  public ucar.ma2.DataType getUnsignedConversionType() {
    return scaleMissingUnsignedProxy.getUnsignedConversionType();
  }

  /** @deprecated do not use */
  @Override
  @Deprecated
  public ucar.ma2.DataType.Signedness getSignedness() {
    return scaleMissingUnsignedProxy.getSignedness();
  }

  /** @deprecated do not use */
  @Override
  @Deprecated
  public double applyScaleOffset(Number value) {
    return scaleMissingUnsignedProxy.applyScaleOffset(value);
  }

  /** @deprecated do not use */
  @Override
  @Deprecated
  public ucar.ma2.Array applyScaleOffset(ucar.ma2.Array data) {
    return scaleMissingUnsignedProxy.applyScaleOffset(data);
  }

  /** @deprecated do not use */
  @Override
  @Deprecated
  public Number convertUnsigned(Number value) {
    return scaleMissingUnsignedProxy.convertUnsigned(value);
  }

  /** @deprecated do not use */
  @Override
  @Deprecated
  public ucar.ma2.Array convertUnsigned(ucar.ma2.Array in) {
    return scaleMissingUnsignedProxy.convertUnsigned(in);
  }

  /** @deprecated do not use */
  @Override
  @Deprecated
  public Number convertMissing(Number value) {
    return scaleMissingUnsignedProxy.convertMissing(value);
  }

  /** @deprecated do not use */
  @Override
  @Deprecated
  public ucar.ma2.Array convertMissing(ucar.ma2.Array in) {
    return scaleMissingUnsignedProxy.convertMissing(in);
  }

  /** @deprecated do not use */
  @Override
  @Deprecated
  public ucar.ma2.Array convert(ucar.ma2.Array in, boolean convertUnsigned, boolean applyScaleOffset,
      boolean convertMissing) {
    return scaleMissingUnsignedProxy.convert(in, convertUnsigned, applyScaleOffset, convertMissing);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////
  private final EnhancementsImpl enhanceProxy;
  private final EnhanceScaleMissingUnsignedImpl scaleMissingUnsignedProxy;
  private final Set<Enhance> enhanceMode; // The set of enhancements that were made.
  private final DataEnhancer dataEnhancer;

  protected final @Nullable Variable orgVar; // wrap this Variable : use it for the I/O
  protected final ArrayType orgDataType; // keep separate for the case where there is no orgVar. TODO @Nullable?
  protected final @Nullable String orgName; // in case Variable was renamed, and we need to keep track of the original
                                            // name
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
    this.scaleMissingUnsignedProxy = new EnhanceScaleMissingUnsignedImpl(this, this.enhanceMode);
    this.scaleMissingUnsignedProxy.setFillValueIsMissing(builder.fillValueIsMissing);
    this.scaleMissingUnsignedProxy.setInvalidDataIsMissing(builder.invalidDataIsMissing);
    this.scaleMissingUnsignedProxy.setMissingDataIsMissing(builder.missingDataIsMissing);

    if (this.enhanceMode.contains(Enhance.ConvertEnums) && dataType.isEnum()) {
      this.dataType = ArrayType.STRING; // LOOK promote enum data type to STRING ????
    }

    if (this.enhanceMode.contains(Enhance.ConvertUnsigned) && !dataType.isEnum()) {
      // We may need a larger data type to hold the results of the unsigned conversion.
      this.dataType = scaleMissingUnsignedProxy.getUnsignedConversionType().getArrayType();
    }

    if (this.enhanceMode.contains(Enhance.ApplyScaleOffset) && (dataType.isNumeric() || dataType == ArrayType.CHAR)
        && scaleMissingUnsignedProxy.hasScaleOffset()) {
      this.dataType = scaleMissingUnsignedProxy.getScaledOffsetType().getArrayType();
    }

    // We have to complete this after the NetcdfDataset is built.
    this.dataEnhancer = new DataEnhancer(this, this.scaleMissingUnsignedProxy);
  }

  public Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  // Add local fields to the passed - in builder.
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

    /** @deprecated use setOriginalArrayType() */
    @Deprecated
    public T setOriginalDataType(ucar.ma2.DataType orgDataType) {
      this.orgDataType = orgDataType.getArrayType();
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
