/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import javax.annotation.Nullable;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.*;
import ucar.nc2.constants.AxisType;
import ucar.nc2.internal.dataset.CoordinatesHelper;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * <p>
 * {@code NetcdfDataset} extends the netCDF API, adding standard attribute parsing such as
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
 *
 * @author caron
 * @see ucar.nc2.NetcdfFile
 */

/*
 * Implementation notes.
 * 1) NetcdfDataset wraps a NetcdfFile.
 * orgFile = NetcdfFile
 * variables are wrapped by VariableDS, but are not reparented. VariableDS uses original variable for read.
 * Groups get reparented.
 * 2) NcML standard
 * NcML location is read in as the NetcdfDataset, then modified by the NcML
 * orgFile = null
 * 3) NcML explicit
 * NcML location is read in, then transferred to new NetcdfDataset as needed
 * orgFile = file defined by NcML location
 * NetcdfDataset defined only by NcML, data is set to FillValue unless explicitly defined
 * 4) NcML new
 * NcML location = null
 * orgFile = null
 */

public class NetcdfDataset extends ucar.nc2.NetcdfFile {
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NetcdfDataset.class);

  public static final String AGGREGATION = "Aggregaation";

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
     * {@link EnhanceScaleMissingUnsignedImpl#nextLarger larger data type}.
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

  private static Set<Enhance> EnhanceAll = Collections.unmodifiableSet(EnumSet.of(Enhance.ConvertEnums,
      Enhance.ConvertUnsigned, Enhance.ApplyScaleOffset, Enhance.ConvertMissing, Enhance.CoordSystems));
  private static Set<Enhance> EnhanceNone = Collections.unmodifiableSet(EnumSet.noneOf(Enhance.class));
  private static Set<Enhance> defaultEnhanceMode = EnhanceAll;

  public static Set<Enhance> getEnhanceAll() {
    return EnhanceAll;
  }

  public static Set<Enhance> getEnhanceNone() {
    return EnhanceNone;
  }

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

  protected static boolean fillValueIsMissing = true;
  protected static boolean invalidDataIsMissing = true;
  protected static boolean missingDataIsMissing = true;

  /**
   * Set if _FillValue attribute is considered isMissing()
   *
   * @param b true if _FillValue are missing (default true)
   * @deprecated do not use
   */
  @Deprecated
  public static void setFillValueIsMissing(boolean b) {
    fillValueIsMissing = b;
  }

  /**
   * Get if _FillValue attribute is considered isMissing()
   *
   * @return if _FillValue attribute is considered isMissing()
   * @deprecated do not use
   */
  @Deprecated
  public static boolean getFillValueIsMissing() {
    return fillValueIsMissing;
  }

  /**
   * Set if valid_range attribute is considered isMissing()
   *
   * @param b true if valid_range are missing (default true)
   * @deprecated do not use
   */
  @Deprecated
  public static void setInvalidDataIsMissing(boolean b) {
    invalidDataIsMissing = b;
  }

  /**
   * Get if valid_range attribute is considered isMissing()
   *
   * @return if valid_range attribute is considered isMissing()
   * @deprecated do not use
   */
  @Deprecated
  public static boolean getInvalidDataIsMissing() {
    return invalidDataIsMissing;
  }

  /**
   * Set if missing_data attribute is considered isMissing()
   *
   * @param b true if missing_data are missing (default true)
   * @deprecated do not use
   */
  @Deprecated
  public static void setMissingDataIsMissing(boolean b) {
    missingDataIsMissing = b;
  }

  /**
   * Get if missing_data attribute is considered isMissing()
   *
   * @return if missing_data attribute is considered isMissing()
   * @deprecated do not use
   */
  @Deprecated
  public static boolean getMissingDataIsMissing() {
    return missingDataIsMissing;
  }

  ////////////////////////////////////////////////////////////////////////////////////

  /**
   * Get the list of all CoordinateSystem objects used by this dataset.
   *
   * @return list of type CoordinateSystem; may be empty, not null.
   */
  public ImmutableList<CoordinateSystem> getCoordinateSystems() {
    return ImmutableList.copyOf(coordSys);
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
   * Get the list of all CoordinateTransform objects used by this dataset.
   *
   * @return list of type CoordinateTransform; may be empty, not null.
   */
  public ImmutableList<CoordinateTransform> getCoordinateTransforms() {
    return ImmutableList.copyOf(coordTransforms);
  }

  /**
   * Get the list of all CoordinateAxis objects used by this dataset.
   *
   * @return list of type CoordinateAxis; may be empty, not null.
   */
  public ImmutableList<CoordinateAxis> getCoordinateAxes() {
    return ImmutableList.copyOf(coordAxes);
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
    for (CoordinateAxis v : coordAxes) {
      if (type == v.getAxisType())
        return v;
    }
    return null;
  }

  /**
   * Retrieve the CoordinateAxis with the specified type.
   *
   * @param fullName full escaped name of the coordinate axis
   * @return the CoordinateAxis, or null if not found
   */
  public CoordinateAxis findCoordinateAxis(String fullName) {
    if (fullName == null)
      return null;
    for (CoordinateAxis v : coordAxes) {
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
    for (CoordinateSystem v : coordSys) {
      if (name.equals(v.getName()))
        return v;
    }
    return null;
  }

  /**
   * Retrieve the CoordinateTransform with the specified name.
   *
   * @param name String which identifies the desired CoordinateSystem
   * @return the CoordinateSystem, or null if not found
   */
  public CoordinateTransform findCoordinateTransform(String name) {
    if (name == null)
      return null;
    for (CoordinateTransform v : coordTransforms) {
      if (name.equals(v.getName()))
        return v;
    }
    return null;
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

    if (orgFile != null)
      orgFile.close();
    orgFile = null;
  }

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
  public long getLastModified() {
    if (agg != null) {
      return agg.getLastModified();
    }
    return (orgFile != null) ? orgFile.getLastModified() : 0;
  }

  // sort by coord sys, then name
  private static class VariableComparator implements java.util.Comparator {
    public int compare(Object o1, Object o2) {
      VariableEnhanced v1 = (VariableEnhanced) o1;
      VariableEnhanced v2 = (VariableEnhanced) o2;
      List list1 = v1.getCoordinateSystems();
      String cs1 = (!list1.isEmpty()) ? ((CoordinateSystem) list1.get(0)).getName() : "";
      List list2 = v2.getCoordinateSystems();
      String cs2 = (!list2.isEmpty()) ? ((CoordinateSystem) list2.get(0)).getName() : "";

      if (cs2.equals(cs1))
        return v1.getShortName().compareToIgnoreCase(v2.getShortName());
      else
        return cs1.compareToIgnoreCase(cs2);
    }
  }

  //////////////////////////////////////////////////////////////////////////////
  // used by NcMLReader for NcML without a referenced dataset

  /**
   * A NetcdfDataset usually wraps a NetcdfFile, where the actual I/O happens.
   * This is called the "referenced file". CAUTION : this may have been modified in ways that make it
   * unsuitable for general use.
   *
   * @return underlying NetcdfFile, or null if none.
   * @deprecated Do not use
   */
  @Deprecated
  public NetcdfFile getReferencedFile() {
    return orgFile;
  }

  /**
   * Set underlying file. CAUTION - normally only done through the constructor.
   *
   * @param ncfile underlying "referenced file"
   * @deprecated Use NetcdfDataset.builder()
   */
  @Deprecated
  public void setReferencedFile(NetcdfFile ncfile) {
    orgFile = ncfile;
  }

  protected String toStringDebug(Object o) {
    return "";
  }

  ///////////////////////////////////////////////////////////////////////////////////
  // constructor methods

  /**
   * Add a CoordinateSystem to the dataset.
   *
   * @param cs add this CoordinateSystem to the dataset
   * @deprecated Use NetcdfDataset.builder()
   */
  @Deprecated
  public void addCoordinateSystem(CoordinateSystem cs) {
    coordSys.add(cs);
  }

  /**
   * Add a CoordinateTransform to the dataset.
   *
   * @param ct add this CoordinateTransform to the dataset
   * @deprecated Use NetcdfDataset.builder()
   */
  @Deprecated
  public void addCoordinateTransform(CoordinateTransform ct) {
    if (!coordTransforms.contains(ct))
      coordTransforms.add(ct);
  }

  /**
   * is this enhancement already done ?
   *
   * @param want enhancements wanted
   * @return true if wanted enhancement is not done
   * @deprecated Do not use.
   */
  @Deprecated
  public boolean enhanceNeeded(Set<Enhance> want) {
    if (want == null)
      return false;
    for (Enhance mode : want) {
      if (!this.enhanceMode.contains(mode))
        return true;
    }
    return false;
  }


  ///////////////////////////////////////////////////////////////////////
  // setting variable data values

  /**
   * Generate the list of values from a starting value and an increment.
   * Will reshape to variable if needed.
   *
   * @param v for this variable
   * @param npts number of values, must = v.getSize()
   * @param start starting value
   * @param incr increment
   * @deprecated use Variable.setValues()
   */
  @Deprecated
  public void setValues(Variable v, int npts, double start, double incr) {
    if (npts != v.getSize())
      throw new IllegalArgumentException("bad npts = " + npts + " should be " + v.getSize());
    Array data = Array.makeArray(v.getDataType(), npts, start, incr);
    if (v.getRank() != 1)
      data = data.reshape(v.getShape());
    v.setCachedData(data, true);
  }

  /**
   * Set the data values from a list of Strings.
   *
   * @param v for this variable
   * @param values list of Strings
   * @throws IllegalArgumentException if values array not correct size, or values wont parse to the correct type
   * @deprecated use Variable.setValues()
   */
  @Deprecated
  public void setValues(Variable v, List<String> values) throws IllegalArgumentException {
    Array data = Array.makeArray(v.getDataType(), values);

    if (data.getSize() != v.getSize())
      throw new IllegalArgumentException("Incorrect number of values specified for the Variable " + v.getFullName()
          + " needed= " + v.getSize() + " given=" + data.getSize());

    if (v.getRank() != 1) // dont have to reshape for rank 1
      data = data.reshape(v.getShape());

    v.setCachedData(data, true);
  }

  /**
   * Make a 1D array from a list of strings.
   *
   * @param dtype data type of the array.
   * @param stringValues list of strings.
   * @return resulting 1D array.
   * @throws NumberFormatException if string values not parssable to specified data type
   * @deprecated use Array#makeArray directly
   */
  @Deprecated
  public static Array makeArray(DataType dtype, List<String> stringValues) throws NumberFormatException {
    return Array.makeArray(dtype, stringValues);
  }

  ////////////////////////////////////////////////////////////////////
  // debugging

  /**
   * Show debug / underlying implementation details
   */
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
      showCached(f);
      showProxies(f);
    } else {
      f.format("%nReferenced File:%n");
      f.format("%s", orgFile.getDetailInfo());
    }
  }

  private void dumpClasses(Group g, PrintWriter out) {

    out.println("Dimensions:");
    for (Dimension ds : g.getDimensions()) {
      out.println("  " + ds.getShortName() + " " + ds.getClass().getName());
    }

    out.println("Atributes:");
    for (Attribute a : g.attributes()) {
      out.println("  " + a.getShortName() + " " + a.getClass().getName());
    }

    out.println("Variables:");
    dumpVariables(g.getVariables(), out);

    out.println("Groups:");
    for (Group nested : g.getGroups()) {
      out.println("  " + nested.getFullName() + " " + nested.getClass().getName());
      dumpClasses(nested, out);
    }
  }

  private void dumpVariables(List<Variable> vars, PrintWriter out) {
    for (Variable v : vars) {
      out.print("  " + v.getFullName() + " " + v.getClass().getName()); // +" "+Integer.toHexString(v.hashCode()));
      if (v instanceof CoordinateAxis)
        out.println("  " + ((CoordinateAxis) v).getAxisType());
      else
        out.println();

      if (v instanceof Structure)
        dumpVariables(((Structure) v).getVariables(), out);
    }
  }

  /**
   * Debugging
   *
   * @param out write here
   * @param ncd info about this
   * @deprecated do not use
   */
  @Deprecated
  public static void debugDump(PrintWriter out, NetcdfDataset ncd) {
    String referencedLocation = ncd.orgFile == null ? "(null)" : ncd.orgFile.getLocation();
    out.println("\nNetcdfDataset dump = " + ncd.getLocation() + " url= " + referencedLocation + "\n");
    ncd.dumpClasses(ncd.getRootGroup(), out);
  }

  @Override
  public String getFileTypeId() {
    if (orgFile != null)
      return orgFile.getFileTypeId();
    if (agg != null)
      return agg.getFileTypeId();
    return "N/A";
  }

  @Override
  public String getFileTypeDescription() {
    if (orgFile != null)
      return orgFile.getFileTypeDescription();
    if (agg != null)
      return agg.getFileTypeDescription();
    return "N/A";
  }

  /** @deprecated do not use */
  @Deprecated
  public void check(Formatter f) {
    for (Variable v : getVariables()) {
      VariableDS vds = (VariableDS) v;
      if (vds.getOriginalDataType() != vds.getDataType()) {
        f.format("Variable %s has type %s, org = %s%n", vds.getFullName(), vds.getOriginalDataType(),
            vds.getDataType());
      }

      Variable orgVar = vds.getOriginalVariable();
      if (orgVar != null) {
        if (orgVar.getRank() != vds.getRank())
          f.format("Variable %s has rank %d, org = %d%n", vds.getFullName(), vds.getRank(), orgVar.getRank());
      }
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////
  // TODO make these final and immutable in 6.
  private NetcdfFile orgFile;
  private List<CoordinateAxis> coordAxes = new ArrayList<>();
  private List<CoordinateSystem> coordSys = new ArrayList<>();
  private List<CoordinateTransform> coordTransforms = new ArrayList<>();
  private String convUsed;
  private Set<Enhance> enhanceMode = EnumSet.noneOf(Enhance.class); // enhancement mode for this specific dataset
  private ucar.nc2.internal.ncml.Aggregation agg;

  private NetcdfDataset(Builder<?> builder) {
    super(builder);
    this.orgFile = builder.orgFile;
    this.convUsed = builder.convUsed;
    this.enhanceMode = builder.getEnhanceMode();
    this.agg = builder.agg;

    // LOOK the need to reference the NetcdfDataset means we cant build the axes or system until now.
    // LOOK this assumes the dataset has already been enhanced. Where does that happen?
    CoordinatesHelper coords = builder.coords.build(this);
    this.coordAxes = coords.getCoordAxes();
    this.coordSys = coords.getCoordSystems();
    this.coordTransforms = coords.getCoordTransforms();

    // TODO goes away in version 6
    // LOOK how do we get the variableDS to reference the coordinate system?
    // CoordinatesHelper has to wire the coordinate systems together
    // Perhaps a VariableDS uses NetcdfDataset or CoordinatesHelper to manage its CoordinateSystems and Transforms ??
    // So it doesnt need a reference directly to them.
    for (Variable v : this.getVariables()) {
      // TODO anything needed to do for a StructureDS ??
      if (v instanceof VariableDS) {
        VariableDS vds = (VariableDS) v;
        vds.setCoordinateSystems(coords);
      }
    }
  }

  public Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  public NetcdfDataset(NetcdfFile.Builder<?> builder) {
    super(builder);
  }

  // Add local fields to the passed - in builder.
  private Builder<?> addLocalFieldsToBuilder(Builder<? extends Builder<?>> b) {
    this.coordAxes.forEach(axis -> b.coords.addCoordinateAxis(axis.toBuilder()));
    this.coordSys.forEach(sys -> b.coords.addCoordinateSystem(sys.toBuilder()));
    this.coordTransforms.forEach(trans -> b.coords.addCoordinateTransform(trans.toBuilder()));

    b.setOrgFile(this.orgFile).setConventionUsed(this.convUsed).setEnhanceMode(this.enhanceMode)
        .setAggregation(this.agg);

    return (Builder<?>) super.addLocalFieldsToBuilder(b);
  }

  /**
   * Get Builder for this class that allows subclassing.
   * 
   * @see "https://community.oracle.com/blogs/emcmanus/2010/10/24/using-builder-pattern-subclasses"
   */
  public static Builder<?> builder() {
    return new Builder2();
  }

  // LOOK this is wrong, cant do it this way.
  public static NetcdfDataset.Builder builder(NetcdfFile from) {
    NetcdfDataset.Builder builder = NetcdfDataset.builder().copyFrom(from).setOrgFile(from);
    return builder;
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

    private boolean built;

    protected abstract T self();

    /**
     * Add a CoordinateAxis to the dataset coordinates and to the list of variables.
     * Replaces any existing Variable and CoordinateAxis with the same name.
     */
    public void replaceCoordinateAxis(Group.Builder group, CoordinateAxis.Builder axis) {
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

    public T setConventionUsed(String convUsed) {
      this.convUsed = convUsed;
      return self();
    }

    public T setEnhanceMode(Set<Enhance> enhanceMode) {
      this.enhanceMode = enhanceMode;
      return self();
    }

    public T setDefaultEnhanceMode() {
      this.enhanceMode = NetcdfDataset.getDefaultEnhanceMode();
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
        g.addDimension(d.toBuilder().build()); // can use without copy after ver 6.
      }

      g.addAttributes(from.attributes()); // copy

      for (Variable v : from.getVariables()) {
        g.addVariable(convertVariable(v)); // convert
      }

      for (Group nested : from.getGroups()) {
        Group.Builder nnested = Group.builder();
        g.addGroup(nnested);
        convertGroup(nnested, nested); // convert
      }
    }

    private Variable.Builder convertVariable(Variable v) {
      Variable.Builder newVar;
      if (v instanceof Sequence) {
        newVar = SequenceDS.builder().copyFrom((Sequence) v);
      } else if (v instanceof Structure) {
        newVar = StructureDS.builder().copyFrom((Structure) v);
      } else {
        newVar = VariableDS.builder().copyFrom(v);
      }
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
