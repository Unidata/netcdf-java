/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import ucar.nc2.*;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.internal.dataset.CoordinatesHelper;
import ucar.nc2.internal.dataset.EnhanceScaleMissingUnsigned;

import java.io.IOException;
import java.util.*;

/**
 * <p>
 * An "enhanced" NetcdfFile, adding standard attribute parsing such as
 * scale and offset, and explicit support for Coordinate Systems.
 * A {@code NetcdfDataset} wraps a {@code NetcdfFile}, or is defined by an NcML document.
 * </p>
 *
 * <p>
 * Be sure to close the dataset when done.
 * Using statics in {@code NetcdfDatets}, best practice is to use try-with-resource:
 * </p>
 * 
 * <pre>
 * try (NetcdfDataset ncd = NetcdfDatasets.openDataset(fileName)) {
 *   ...
 * }
 * </pre>
 *
 * <p>
 * By default @code NetcdfDataset} is opened with all enhancements turned on. The default "enhance
 * mode" can be set through setDefaultEnhanceMode(). One can also explicitly set the enhancements
 * you want in the dataset factory methods. The enhancements are:
 * </p>
 *
 * <ul>
 * <li>ConvertEnums: convert enum values to their corresponding Strings. If you want to do this manually,
 * you can call Variable.lookupEnumString().</li>
 * <li>ConvertUnsigned: reinterpret the bit patterns of any negative values as unsigned.</li>
 * <li>ApplyScaleOffset: process scale/offset attributes, and automatically convert the data.</li>
 * <li>ConvertMissing: replace missing data with NaNs, for efficiency.</li>
 * <li>CoordSystems: extract CoordinateSystem using the CoordSysBuilder plug-in mechanism.</li>
 * </ul>
 *
 * <p>
 * Automatic scale/offset processing has some overhead that you may not want to incur up-front. If so, open the
 * NetcdfDataset without {@code ApplyScaleOffset}. The VariableDS data type is not promoted and the data is not
 * converted on a read, but you can call the convertScaleOffset() routines to do the conversion later.
 * </p>
 */
@Immutable
public class NetcdfDataset extends ucar.nc2.NetcdfFile {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NetcdfDataset.class);
  public static final String AGGREGATION = "Aggregation";

  /**
   * Possible enhancements for a NetcdfDataset
   */
  public enum Enhance {
    /** Convert enums to Strings. */
    ConvertEnums,
    /**
     * Convert unsigned values to signed values.
     * For {@link ucar.nc2.constants.CDM#UNSIGNED} variables, reinterpret the bit patterns of any
     * negative values as unsigned. The result will be positive values that must be stored in a
     * {@link EnhanceScaleMissingUnsigned#nextLarger larger data type}.
     */
    ConvertUnsigned,
    /** Apply scale and offset to values, promoting the data type if needed. */
    ApplyScaleOffset,
    /**
     * Replace {@link EnhanceScaleMissingUnsigned#isMissing missing} data with NaNs, for efficiency. Note that if the
     * enhanced data type is not {@code FLOAT} or {@code DOUBLE}, this has no effect.
     */
    ConvertMissing,
    /** Build coordinate systems. */
    CoordSystems,
    /**
     * Build coordinate systems allowing for incomplete coordinate systems (i.e. not
     * every dimension in a variable has a corresponding coordinate variable.
     */
    IncompleteCoordSystems,
  }

  private static final Set<Enhance> EnhanceAll = Collections.unmodifiableSet(EnumSet.of(Enhance.ConvertEnums,
      Enhance.ConvertUnsigned, Enhance.ApplyScaleOffset, Enhance.ConvertMissing, Enhance.CoordSystems));
  private static final Set<Enhance> EnhanceNone = Collections.unmodifiableSet(EnumSet.noneOf(Enhance.class));
  private static Set<Enhance> defaultEnhanceMode = EnhanceAll;

  /** The set of all enhancements. */
  public static Set<Enhance> getEnhanceAll() {
    return EnhanceAll;
  }

  /** The set of no enhancements. */
  public static Set<Enhance> getEnhanceNone() {
    return EnhanceNone;
  }

  /** The set of default enhancements. */
  public static Set<Enhance> getDefaultEnhanceMode() {
    return defaultEnhanceMode;
  }

  /**
   * Set the default set of Enhancements to do for all subsequent dataset opens and acquires.
   * 
   * @param mode the default set of Enhancements for open and acquire factory methods
   */
  public static void setDefaultEnhanceMode(Set<Enhance> mode) {
    defaultEnhanceMode = Collections.unmodifiableSet(mode);
  }

  public static final boolean fillValueIsMissing = true;
  public static final boolean invalidDataIsMissing = true;
  public static final boolean missingDataIsMissing = true;

  ////////////////////////////////////////////////////////////////////////////////////

  /**
   * Get the list of all CoordinateSystem objects used by this dataset.
   *
   * @return list of type CoordinateSystem; may be empty, not null.
   */
  public ImmutableList<CoordinateSystem> getCoordinateSystems() {
    return coords.getCoordSystems();
  }

  /**
   * Get conventions used to analyse coordinate systems.
   *
   * @return conventions used to analyse coordinate systems
   */
  public String getConventionUsed() {
    return convUsed;
  }

  /**
   * Get the current state of dataset enhancement.
   *
   * @return the current state of dataset enhancement.
   */
  public Set<Enhance> getEnhanceMode() {
    return enhanceMode;
  }

  /**
   * Get the list of all CoordinateAxis objects used by this dataset.
   *
   * @return list of type CoordinateAxis; may be empty, not null.
   */
  public ImmutableList<CoordinateAxis> getCoordinateAxes() {
    if (coords == null && coordAxes != null) { // LOOK when is this true?
      return coordAxes;
    }
    return coords.getCoordAxes();
  }

  /**
   * Retrieve the CoordinateAxis with the specified Axis Type.
   *
   * @param type axis type
   * @return the first CoordinateAxis that has that type, or null if not found
   */
  public CoordinateAxis findCoordinateAxis(AxisType type) {
    if (type == null)
      return null;
    for (CoordinateAxis v : coords.getCoordAxes()) {
      if (type == v.getAxisType())
        return v;
    }
    return null;
  }

  /**
   * Retrieve the CoordinateAxis with the specified fullName.
   *
   * @param fullName full escaped name of the coordinate axis
   * @return the CoordinateAxis, or null if not found
   */
  public CoordinateAxis findCoordinateAxis(String fullName) {
    if (fullName == null)
      return null;
    for (CoordinateAxis v : coords.getCoordAxes()) {
      if (fullName.equals(v.getFullName()))
        return v;
    }
    return null;
  }

  /**
   * Retrieve the CoordinateSystem with the specified name.
   *
   * @param name String which identifies the desired CoordinateSystem
   * @return the CoordinateSystem, or null if not found
   */
  public CoordinateSystem findCoordinateSystem(String name) {
    if (name == null)
      return null;
    for (CoordinateSystem v : coords.getCoordSystems()) {
      if (name.equals(v.getName()))
        return v;
    }
    return null;
  }

  /** Return true if axis is 1D with a unique dimension. */
  public boolean isIndependentCoordinate(CoordinateAxis axis) {
    if (axis.isCoordinateVariable()) {
      return true;
    }
    if (axis.getRank() != 1) {
      return false;
    }
    if (axis.attributes().hasAttribute(_Coordinate.AliasForDimension)) {
      return true;
    }
    Dimension dim = axis.getDimension(0);
    for (CoordinateAxis other : getCoordinateAxes()) {
      if (other == axis) {
        continue;
      }
      for (Dimension odim : other.getDimensions()) {
        if (dim.equals(odim)) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public Object sendIospMessage(Object message) {
    if (message == IOSP_MESSAGE_GET_IOSP) {
      return (orgFile == null) ? null : orgFile.sendIospMessage(message);
    }
    if (message == AGGREGATION) {
      return this.agg;
    }
    return super.sendIospMessage(message);
  }

  /**
   * Close all resources (files, sockets, etc) associated with this dataset.
   * If the underlying file was acquired, it will be released, otherwise closed.
   */
  @Override
  public synchronized void close() throws java.io.IOException {
    if (agg != null) {
      agg.persistWrite(); // LOOK maybe only on real close ??
      agg.close();
    }

    if (cache != null) {
      // unlocked = true;
      if (cache.release(this))
        return;
    }

    if (!wasClosed && orgFile != null) {
      orgFile.close();
    }
    wasClosed = true;
  }

  private boolean wasClosed = false;

  /** @deprecated do not use */
  @Deprecated
  public void release() throws IOException {
    if (orgFile != null)
      orgFile.release();
  }

  /** @deprecated do not use */
  @Deprecated
  public void reacquire() throws IOException {
    if (orgFile != null)
      orgFile.reacquire();
  }

  @Override
  @Deprecated
  public long getLastModified() {
    if (agg != null) {
      return agg.getLastModified();
    }
    return (orgFile != null) ? orgFile.getLastModified() : 0;
  }

  //////////////////////////////////////////////////////////////////////////////
  // used by NcMLReader for NcML without a referenced dataset

  /**
   * @return underlying NetcdfFile, or null if none.
   * @deprecated Do not use
   */
  @Deprecated
  public NetcdfFile getReferencedFile() {
    return orgFile;
  }

  ////////////////////////////////////////////////////////////////////
  // debugging

  /** Show debug / underlying implementation details */
  @Override
  public void getDetailInfo(Formatter f) {
    f.format("NetcdfDataset location= %s%n", getLocation());
    f.format("  title= %s%n", getTitle());
    f.format("  id= %s%n", getId());
    f.format("  fileType= %s%n", getFileTypeId());
    f.format("  fileDesc= %s%n", getFileTypeDescription());

    f.format("  class= %s%n", getClass().getName());

    if (agg == null) {
      f.format("  has no Aggregation element%n");
    } else {
      f.format("%nAggregation:%n");
      agg.getDetailInfo(f);
    }

    if (orgFile == null) {
      f.format("  has no referenced NetcdfFile%n");
    } else {
      f.format("%nReferenced File:%n");
      f.format("%s", orgFile.getDetailInfo());
    }
  }

  @Override
  @Nullable
  public String getFileTypeId() {
    String inner = null;
    if (orgFile != null) {
      inner = orgFile.getFileTypeId();
    }
    if (inner == null && agg != null) {
      inner = agg.getFileTypeId();
    }
    if (this.fileTypeId == null) {
      return inner;
    }
    if (inner == null) {
      return this.fileTypeId;
    }
    return (inner.startsWith(this.fileTypeId)) ? inner : this.fileTypeId + "/" + inner;
  }

  @Override
  public String getFileTypeDescription() {
    if (orgFile != null)
      return orgFile.getFileTypeDescription();
    if (agg != null)
      return agg.getFileTypeDescription();
    return "N/A";
  }

  ////////////////////////////////////////////////////////////////////////////////////////////
  private final @Nullable NetcdfFile orgFile; // can be null in Ncml
  private final CoordinatesHelper coords;
  private final String convUsed;
  private final ImmutableSet<Enhance> enhanceMode; // enhancement mode for this specific dataset
  private final ucar.nc2.internal.ncml.Aggregation agg; // LOOK not immutable
  private final String fileTypeId;

  private final ImmutableList<CoordinateAxis> coordAxes; // TODO get rid of if possible

  private NetcdfDataset(Builder<?> builder) {
    super(builder);
    this.orgFile = builder.orgFile;
    this.fileTypeId = builder.fileTypeId;
    this.convUsed = builder.convUsed;
    this.enhanceMode = ImmutableSet.copyOf(builder.getEnhanceMode());
    this.agg = builder.agg;

    // The need to reference the NetcdfDataset means we can't build the axes or system until now.
    // LOOK this assumes the dataset has already been enhanced. Where does that happen?
    // TODO: Problem, we are letting ds escape before its finished, namely coords is null at this point
    // TODO: 1) VerticalCTBuilder.makeVerticalCT(ds) and 2) AbstractTransformBuilder.getGeoCoordinateUnits(ds, ctv)
    this.coordAxes = CoordinatesHelper.makeAxes(this);
    // Note that only ncd.axes can be accessed, not coordsys or transforms.
    coords = builder.coords.build(this, this.coordAxes);

    // LOOK We have to break VariableDS Immutability here, because VariableDS is constructed in NetcdfFile, but needs a
    // link to the CoordinatesHelper, which isnt complete yet.
    for (Variable v : this.getVariables()) {
      if (v instanceof CoordinateAxis) {
        continue;
      }
      if (v instanceof VariableDS) {
        VariableDS vds = (VariableDS) v;
        vds.setCoordinateSystems(coords);
      }
      // TODO This implies Structures can have CoordinateSystems. Review and justify this.
      if (v instanceof StructureDS) {
        StructureDS sds = (StructureDS) v;
        sds.setCoordinateSystems(coords);
      }
    }
  }

  public Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  private Builder<?> addLocalFieldsToBuilder(Builder<? extends Builder<?>> b) {
    this.coords.getCoordAxes().forEach(axis -> b.coords.addCoordinateAxis(axis.toBuilder()));
    this.coords.getCoordSystems().forEach(sys -> b.coords.addCoordinateSystem(sys.toBuilder()));
    this.coords.getCoordTransforms().forEach(ct -> b.coords.addCoordinateTransform(ct));

    b.setOrgFile(this.orgFile).setConventionUsed(this.convUsed).setEnhanceMode(this.enhanceMode)
        .setAggregation(this.agg).setFileTypeId(this.fileTypeId);

    return (Builder<?>) super.addLocalFieldsToBuilder(b);
  }

  /** Get Builder for NetcdfDataset. LOOK no longer need to subclass. */
  // Subclassing: "https://community.oracle.com/blogs/emcmanus/2010/10/24/using-builder-pattern-subclasses"
  public static Builder<?> builder() {
    return new Builder2();
  }

  private static class Builder2 extends Builder<Builder2> {
    @Override
    protected Builder2 self() {
      return this;
    }
  }

  public static abstract class Builder<T extends Builder<T>> extends NetcdfFile.Builder<T> {
    @Nullable
    public NetcdfFile orgFile;
    public CoordinatesHelper.Builder coords = CoordinatesHelper.builder();
    private String convUsed;
    private Set<Enhance> enhanceMode = EnumSet.noneOf(Enhance.class); // LOOK should be default ??
    public ucar.nc2.internal.ncml.Aggregation agg; // If its an aggregation
    private String fileTypeId;

    private boolean built;

    protected abstract T self();

    /**
     * Add a CoordinateAxis to the dataset coordinates and to the list of variables.
     * Replaces any existing Variable and CoordinateAxis with the same name.
     */
    public void replaceCoordinateAxis(Group.Builder group, CoordinateAxis.Builder<?> axis) {
      if (axis == null)
        return;
      coords.replaceCoordinateAxis(axis);
      group.replaceVariable(axis);
      axis.setParentGroupBuilder(group);
    }

    public T setOrgFile(NetcdfFile orgFile) {
      this.orgFile = orgFile;
      return self();
    }

    public T setFileTypeId(String fileTypeId) {
      this.fileTypeId = fileTypeId;
      return self();
    }

    public T setConventionUsed(String convUsed) {
      this.convUsed = convUsed;
      return self();
    }

    public T setEnhanceMode(Set<Enhance> enhanceMode) {
      this.enhanceMode = enhanceMode;
      return self();
    }

    public Set<Enhance> getEnhanceMode() {
      return this.enhanceMode;
    }

    public void addEnhanceMode(Enhance addEnhanceMode) {
      ImmutableSet.Builder<Enhance> result = new ImmutableSet.Builder<>();
      result.addAll(this.enhanceMode);
      result.add(addEnhanceMode);
      this.enhanceMode = result.build();
    }

    public void removeEnhanceMode(Enhance removeEnhanceMode) {
      ImmutableSet.Builder<Enhance> result = new ImmutableSet.Builder<>();
      this.enhanceMode.stream().filter(e -> !e.equals(removeEnhanceMode)).forEach(result::add);
      this.enhanceMode = result.build();
    }

    public void addEnhanceModes(Set<Enhance> addEnhanceModes) {
      ImmutableSet.Builder<Enhance> result = new ImmutableSet.Builder<>();
      result.addAll(this.enhanceMode);
      result.addAll(addEnhanceModes);
      this.enhanceMode = result.build();
    }

    public T setAggregation(ucar.nc2.internal.ncml.Aggregation agg) {
      this.agg = agg;
      return self();
    }

    /** Copy metadata from orgFile. Do not copy the coordinates, etc */
    public T copyFrom(NetcdfFile orgFile) {
      setLocation(orgFile.getLocation());
      setId(orgFile.getId());
      setTitle(orgFile.getTitle());

      Group.Builder root = Group.builder().setName("");
      convertGroup(root, orgFile.getRootGroup());
      setRootGroup(root);

      return self();
    }

    private void convertGroup(Group.Builder g, Group from) {
      g.setName(from.getShortName());

      g.addEnumTypedefs(from.getEnumTypedefs()); // copy

      for (Dimension d : from.getDimensions()) {
        g.addDimension(d);
      }

      g.addAttributes(from.attributes()); // copy

      for (Variable v : from.getVariables()) {
        g.addVariable(convertVariable(g, v)); // convert
      }

      for (Group nested : from.getGroups()) {
        Group.Builder nnested = Group.builder();
        g.addGroup(nnested);
        convertGroup(nnested, nested); // convert
      }
    }

    private Variable.Builder<?> convertVariable(Group.Builder g, Variable v) {
      Variable.Builder<?> newVar;
      if (v instanceof Sequence) {
        newVar = SequenceDS.builder().copyFrom((Sequence) v);
      } else if (v instanceof Structure) {
        newVar = StructureDS.builder().copyFrom((Structure) v);
      } else {
        newVar = VariableDS.builder().copyFrom(v);
      }
      newVar.setParentGroupBuilder(g);
      return newVar;
    }

    public NetcdfDataset build() {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      return new NetcdfDataset(this);
    }
  }

}
