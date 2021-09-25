/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import javax.annotation.Nullable;

import ucar.array.ArrayType;
import ucar.nc2.*;
import ucar.nc2.constants.AxisType;
import ucar.unidata.geoloc.*;
import java.util.*;
import ucar.unidata.geoloc.projection.LatLonProjection;
import ucar.unidata.util.StringUtil2;

/**
 * Specifies the coordinates of a Variable's values.
 *
 * <pre>
 * Mathematically it is a vector function F from index space to Sn:
 *  F(i,j,k,...) -&gt; (S1, S2, ...Sn)
 *  where i,j,k are integers, and S is the set of reals (R) or Strings.
 * </pre>
 * 
 * The components of F are just its coordinate axes:
 * 
 * <pre>
 *  F = (A1, A2, ...An)
 *    A1(i,j,k,...) -&gt; S1
 *    A2(i,j,k,...) -&gt; S2
 *    An(i,j,k,...) -&gt; Sn
 * </pre>
 *
 * Concretely, a CoordinateSystem is a set of coordinate axes, and an optional set
 * of coordinate transforms.
 * The domain rank of F is the number of dimensions it is a function of. The range rank is the number
 * of coordinate axes.
 *
 * <p>
 * An important class of CoordinateSystems are <i>georeferencing</i> Coordinate Systems, that locate a
 * Variable's values in space and time. A CoordinateSystem that has a Lat and Lon axis, or a GeoX and GeoY
 * axis and a Projection CoordinateTransform will have <i>isGeoReferencing()</i> true.
 * A CoordinateSystem that has a Height, Pressure, or GeoZ axis will have <i>hasVerticalAxis()</i> true.
 * <p>
 * Further CoordinateSystems specialization is done by "data type specific" classes such as ucar.nc2.grid.
 *
 * @author caron
 * @see <a href="https://www.unidata.ucar.edu/software/netcdf-java/reference/CSObjectModel.html">
 *      Coordinate System Object Model</a>
 *      TODO: make Immutable in ver7.
 */
public class CoordinateSystem {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CoordinateSystem.class);

  /**
   * Create standard name from list of axes. Sort the axes first
   * 
   * @param axes list of CoordinateAxis
   * @return CoordinateSystem name, created from axes names
   */
  public static String makeName(List<CoordinateAxis> axes) {
    List<CoordinateAxis> axesSorted = new ArrayList<>(axes);
    axesSorted.sort(new CoordinateAxis.AxisComparator());
    ArrayList<String> names = new ArrayList<>();
    axesSorted.forEach(axis -> names.add(NetcdfFiles.makeFullName(axis)));
    return String.join(" ", names);
  }

  /** Get the List of CoordinateAxes */
  public ImmutableList<CoordinateAxis> getCoordinateAxes() {
    return ImmutableList.copyOf(coordAxes);
  }

  /**
   * Get the List of CoordinateTransforms.
   * 
   * @deprecated use getProjection() or getVerticalCT()
   */
  @Deprecated
  public ImmutableList<CoordinateTransform> getCoordinateTransforms() {
    return ImmutableList.copyOf(coordTrans);
  }

  /** Get the name of the Coordinate System */
  public String getName() {
    return name;
  }

  /**
   * Get the underlying NetcdfDataset
   *
   * @deprecated do not use
   */
  @Deprecated
  public NetcdfDataset getNetcdfDataset() {
    return ds;
  }

  /** Get the Collection of Dimensions used by any of the CoordinateAxes. */
  public ImmutableCollection<Dimension> getDomain() {
    return ImmutableList.copyOf(domain);
  }

  /**
   * Get the domain rank of the coordinate system = number of dimensions it is a function of.
   * 
   * @return domain.size()
   * @deprecated use getDomain().size();
   */
  @Deprecated
  public int getRankDomain() {
    return domain.size();
  }

  /**
   * Get the range rank of the coordinate system = number of coordinate axes.
   * 
   * @return coordAxes.size()
   * @deprecated use getCoordinateAxes().size();
   */
  @Deprecated
  public int getRankRange() {
    return coordAxes.size();
  }

  ///////////////////////////////////////////////////////////////////////////
  // Convenience routines for finding georeferencing axes

  /**
   * Find the CoordinateAxis that has the given AxisType.
   * If more than one, return the one with lesser rank.
   * 
   * @param type look for this axisType
   * @return CoordinateAxis of the given AxisType, else null.
   */
  @Nullable
  public CoordinateAxis findAxis(AxisType type) {
    CoordinateAxis result = null;
    for (CoordinateAxis axis : coordAxes) {
      AxisType axisType = axis.getAxisType();
      if ((axisType != null) && (axisType == type)) {
        result = lesserRank(result, axis);
      }
    }
    return result;
  }

  // prefer smaller ranks, in case there's more than one
  private CoordinateAxis lesserRank(CoordinateAxis a1, CoordinateAxis a2) {
    if (a1 == null) {
      return a2;
    }
    return (a1.getRank() <= a2.getRank()) ? a1 : a2;
  }

  /** Find CoordinateAxis of one of the given types, in the order given. */
  @Nullable
  public CoordinateAxis findAxis(AxisType... axisType) {
    for (AxisType type : axisType) {
      CoordinateAxis result = findAxis(type);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  /**
   * get the CoordinateAxis with AxisType.GeoX, or null if none.
   * if more than one, choose one with smallest rank
   * 
   * @return axis of type AxisType.GeoX, or null if none
   * @deprecated use findAxis(AxisType.GeoX)
   */
  @Deprecated
  public CoordinateAxis getXaxis() {
    return xAxis;
  }

  /**
   * get the CoordinateAxis with AxisType.GeoY, or null if none.
   * if more than one, choose one with smallest rank
   * 
   * @return axis of type AxisType.GeoY, or null if none
   * @deprecated use findAxis(AxisType.GeoY)
   */
  @Deprecated
  public CoordinateAxis getYaxis() {
    return yAxis;
  }

  /**
   * get the CoordinateAxis with AxisType.GeoZ, or null if none.
   * if more than one, choose one with smallest rank
   * 
   * @return axis of type AxisType.GeoZ, or null if none
   * @deprecated use findAxis(AxisType.GeoZ)
   */
  @Deprecated
  public CoordinateAxis getZaxis() {
    return zAxis;
  }

  /**
   * get the CoordinateAxis with AxisType.Time, or null if none.
   * if more than one, choose one with smallest rank
   * 
   * @return axis of type AxisType.Time, or null if none
   * @deprecated use findAxis(AxisType.Time)
   */
  @Deprecated
  public CoordinateAxis getTaxis() {
    return tAxis;
  }

  /**
   * get the CoordinateAxis with AxisType.Lat, or null if none.
   * if more than one, choose one with smallest rank
   * 
   * @return axis of type AxisType.Lat, or null if none
   * @deprecated use findAxis(AxisType.Lat)
   */
  @Deprecated
  public CoordinateAxis getLatAxis() {
    return latAxis;
  }

  /**
   * get the CoordinateAxis with AxisType.Lon, or null if none.
   * if more than one, choose one with smallest rank *
   * 
   * @return axis of type AxisType.Lon, or null if none
   * @deprecated use findAxis(AxisType.Lon)
   */
  @Deprecated
  public CoordinateAxis getLonAxis() {
    return lonAxis;
  }

  /**
   * get the CoordinateAxis with AxisType.Height, or null if none.
   * if more than one, choose one with smallest rank
   * 
   * @return axis of type AxisType.Height, or null if none
   * @deprecated use findAxis(AxisType.Height)
   */
  @Deprecated
  public CoordinateAxis getHeightAxis() {
    return hAxis;
  }

  /**
   * get the CoordinateAxis with AxisType.Pressure, or null if none.
   * if more than one, choose one with smallest rank.
   * 
   * @return axis of type AxisType.Pressure, or null if none
   * @deprecated use findAxis(AxisType.Pressure)
   */
  @Deprecated
  public CoordinateAxis getPressureAxis() {
    return pAxis;
  }


  /**
   * get the CoordinateAxis with AxisType.Ensemble, or null if none.
   * if more than one, choose one with smallest rank.
   * 
   * @return axis of type AxisType.Ensemble, or null if none
   * @deprecated use findAxis(AxisType.Ensemble)
   */
  @Deprecated
  public CoordinateAxis getEnsembleAxis() {
    return ensAxis;
  }

  /**
   * get the CoordinateAxis with AxisType.RadialAzimuth, or null if none.
   * if more than one, choose one with smallest rank
   * 
   * @return axis of type AxisType.RadialAzimuth, or null if none
   * @deprecated use findAxis(AxisType.RadialAzimuth)
   */
  @Deprecated
  public CoordinateAxis getAzimuthAxis() {
    return aziAxis;
  }

  /**
   * get the CoordinateAxis with AxisType.RadialDistance, or null if none.
   * if more than one, choose one with smallest rank
   * 
   * @return axis of type AxisType.RadialDistance, or null if none
   * @deprecated use findAxis(AxisType.RadialDistance)
   */
  @Deprecated
  public CoordinateAxis getRadialAxis() {
    return radialAxis;
  }

  /**
   * get the CoordinateAxis with AxisType.RadialElevation, or null if none.
   * if more than one, choose one with smallest rank
   * 
   * @return axis of type AxisType.RadialElevation, or null if none
   * @deprecated use findAxis(AxisType.RadialElevation)
   */
  @Deprecated
  public CoordinateAxis getElevationAxis() {
    return elevAxis;
  }

  /**
   * Find the first ProjectionCT from the list of CoordinateTransforms.
   * 
   * @return ProjectionCT or null if none.
   * @deprecated use getProjection()
   */
  @Deprecated
  public ProjectionCT getProjectionCT() {
    for (CoordinateTransform ct : coordTrans) {
      if (ct instanceof ProjectionCT)
        return (ProjectionCT) ct;
    }
    return null;
  }

  /**
   * Get the Projection for this coordinate system.
   * If isLatLon(), then returns a LatLonProjection. Otherwise, extracts the
   * projection from any ProjectionCT CoordinateTransform.
   *
   * @return Projection or null if none.
   */
  @Nullable
  public Projection getProjection() {
    if (projection == null) {
      if (isLatLon()) {
        projection = new LatLonProjection();
      }
      ProjectionCT projCT = getProjectionCT();
      if (null != projCT) {
        projection = projCT.getProjection();
      }
    }
    return projection;
  }

  /**
   * Get the Vertical Transform for this coordinate system, if any.
   * 
   * @deprecated use GridCooordinateSystem.getVerticalTransform()
   */
  @Deprecated
  @Nullable
  public VerticalCT getVerticalCT() {
    Optional<CoordinateTransform> result =
        coordTrans.stream().filter(t -> t.getTransformType() == TransformType.Vertical).findFirst();
    return (VerticalCT) result.orElse(null);
  }

  ////////////////////////////////////////////////////////////////////////////
  // classification

  /** True if it has X and Y CoordinateAxis, and a Projection */
  public boolean isGeoXY() {
    if ((xAxis == null) || (yAxis == null)) {
      return false;
    }
    return null != getProjection() && !(projection instanceof LatLonProjection);
  }

  /**
   * true if it has Lat and Lon CoordinateAxis
   * 
   * @return true if it has Lat and Lon CoordinateAxis
   */
  public boolean isLatLon() {
    return (latAxis != null) && (lonAxis != null);
  }

  /**
   * true if it has radial distance and azimuth CoordinateAxis
   * 
   * @return true if it has radial distance and azimuth CoordinateAxis
   */
  public boolean isRadial() {
    return (radialAxis != null) && (aziAxis != null);
  }

  /**
   * true if isGeoXY or isLatLon
   * 
   * @return true if isGeoXY or isLatLon
   */
  public boolean isGeoReferencing() {
    return isGeoXY() || isLatLon();
  }

  /**
   * true if all axes are CoordinateAxis1D
   * 
   * @return true if all axes are CoordinateAxis1D
   * @deprecated use GridCoordinateSystem.isProductSet()
   */
  @Deprecated
  public boolean isProductSet() {
    for (CoordinateAxis axis : coordAxes) {
      if (axis.getRank() != 1) {
        return false;
      }
    }
    return true;
  }

  /**
   * true if all axes are CoordinateAxis1D and are regular
   *
   * @return true if all axes are CoordinateAxis1D and are regular
   * @deprecated do not use
   */
  @Deprecated
  public boolean isRegular() {
    for (CoordinateAxis axis : coordAxes) {
      if (!(axis instanceof CoordinateAxis1D)) {
        return false;
      }
      if (!((CoordinateAxis1D) axis).isRegular()) {
        return false;
      }
    }
    return true;
  }

  /**
   * Check if this Coordinate System is complete for v, ie if all v's dimensions are used by the Coordinate System.
   * Exclude dimensions with length &lt; 2.
   */
  public boolean isComplete(Variable v) {
    return isComplete(v.getDimensionSet(), domain);
  }

  /** True if all variableDomain dimensions are contained in csysDomain, or have length &lt; 2. */
  public static boolean isComplete(Collection<Dimension> variableDomain, Collection<Dimension> csysDomain) {
    for (Dimension d : variableDomain) {
      if (!(csysDomain.contains(d)) && (d.getLength() > 1)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Check if this Coordinate System can be used for the given variable, by checking if each CoordinateAxis
   * can be used for the Variable.
   * A CoordinateAxis can only be used if the CoordinateAxis' set of Dimensions is a
   * subset of the Variable's set of Dimensions.
   */
  public boolean isCoordinateSystemFor(Variable v) {
    HashSet<Dimension> varDims = new HashSet<>(v.getDimensions());
    for (CoordinateAxis axis : getCoordinateAxes()) {
      Group groupv = v.getParentGroup();
      Group groupa = axis.getParentGroup();
      Group commonGroup = groupv.commonParent(groupa);

      // a CHAR variable must really be a STRING, so leave out the last (string length) dimension
      int checkDims = axis.getRank();
      if (axis.getArrayType() == ArrayType.CHAR) {
        checkDims--;
      }
      for (int i = 0; i < checkDims; i++) {
        Dimension axisDim = axis.getDimension(i);
        if (!axisDim.isShared()) { // anon dimensions dont count. TODO does this work?
          continue;
        }
        if (!varDims.contains(axisDim)) {
          return false;
        }
        // The dimension must be in the common parent group
        if (groupa != groupv && commonGroup.findDimension(axisDim) == null) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Test if all the Dimensions in subset are in set
   * 
   * @param subset is this a subset
   * @param set of this?
   * @return true if all the Dimensions in subset are in set
   * @deprecated use Dimensions.isSubset()
   */
  @Deprecated
  public static boolean isSubset(Collection<Dimension> subset, Collection<Dimension> set) {
    for (Dimension d : subset) {
      if (!(set.contains(d))) {
        return false;
      }
    }
    return true;
  }

  /** @deprecated use Dimensions.isSubset() */
  @Deprecated
  public static boolean isSubset(Set<String> subset, Set<String> set) {
    for (String d : subset) {
      if (!(set.contains(d))) {
        return false;
      }
    }
    return true;
  }

  /** @deprecated use {@link Dimensions#makeDomain} */
  @Deprecated
  public static Set<Dimension> makeDomain(Iterable<? extends Variable> axes) {
    Set<Dimension> domain = new HashSet<>();
    for (Variable axis : axes) {
      domain.addAll(axis.getDimensions());
    }
    return domain;
  }

  /** @deprecated use Dimensions.makeDomain().size() */
  @Deprecated
  public static int countDomain(Variable[] axes) {
    Set<Dimension> domain = new HashSet<>();
    for (Variable axis : axes) {
      domain.addAll(axis.getDimensions());
    }
    return domain.size();
  }

  /**
   * Implicit Coordinate System are constructed based on which Coordinate Variables exist for the Dimensions of the
   * Variable. This is in contrast to a Coordinate System that is explicitly specified in the file.
   */
  public boolean isImplicit() {
    return isImplicit;
  }

  /**
   * true if has Height, Pressure, or GeoZ axis
   * 
   * @return true if has a vertical axis
   * @deprecated use findAxis(...)
   */
  @Deprecated
  public boolean hasVerticalAxis() {
    return (hAxis != null) || (pAxis != null) || (zAxis != null);
  }

  /**
   * true if has Time axis
   * 
   * @return true if has Time axis
   * @deprecated use findAxis(...)
   */
  @Deprecated
  public boolean hasTimeAxis() {
    return (tAxis != null);
  }

  /**
   * Do we have all the axes in wantAxes, matching on full name.
   * 
   * @deprecated do not use
   */
  @Deprecated
  public boolean containsAxes(List<CoordinateAxis> wantAxes) {
    for (CoordinateAxis ca : wantAxes) {
      if (!containsAxis(ca.getFullName())) {
        return false;
      }
    }
    return true;
  }

  /**
   * Do we have the named axis?
   * 
   * @param axisFullName (full unescaped) name of axis
   * @return true if we have an axis of that name
   * @deprecated do not use
   */
  @Deprecated
  public boolean containsAxis(String axisFullName) {
    for (CoordinateAxis ca : coordAxes) {
      if (ca.getFullName().equals(axisFullName))
        return true;
    }
    return false;
  }

  /**
   * Do we have all the dimensions in wantDimensions?
   * 
   * @deprecated do not use
   */
  @Deprecated
  public boolean containsDomain(List<Dimension> wantDimensions) {
    for (Dimension d : wantDimensions) {
      if (!domain.contains(d)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Do we have all the axes types in wantAxes?
   * 
   * @deprecated use findAxis(...)
   */
  @Deprecated
  public boolean containsAxisTypes(List<AxisType> wantAxes) {
    for (AxisType wantAxisType : wantAxes) {
      if (!containsAxisType(wantAxisType))
        return false;
    }
    return true;
  }

  /**
   * Do we have an axis of the given type?
   * 
   * @param wantAxisType want this AxisType
   * @return true if we have at least one axis of that type.
   * @deprecated use findAxis(...) != null
   */
  @Deprecated
  public boolean containsAxisType(AxisType wantAxisType) {
    for (CoordinateAxis ca : coordAxes) {
      if (ca.getAxisType() == wantAxisType) {
        return true;
      }
    }
    return false;
  }

  ////////////////////////////////////////////////////////////////////////////

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    CoordinateSystem that = (CoordinateSystem) o;
    return com.google.common.base.Objects.equal(coordAxes, that.coordAxes)
        && com.google.common.base.Objects.equal(coordTrans, that.coordTrans)
        && com.google.common.base.Objects.equal(name, that.name);
  }

  @Override
  public int hashCode() {
    return com.google.common.base.Objects.hashCode(coordAxes, coordTrans, name);
  }

  public String toString() {
    return name;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////
  // TODO make these private, final and immutable in ver7.
  private final NetcdfDataset ds; // cant remove until dt.GridCoordSys can be removed
  private final ImmutableList<CoordinateAxis> coordAxes;
  // LOOK keep projection and vertical separate, and only allow one?
  private final List<CoordinateTransform> coordTrans = new ArrayList<>();
  private Projection projection;

  // these are calculated
  private final String name;
  private final Set<Dimension> domain = new HashSet<>(); // set of dimension
  private CoordinateAxis xAxis, yAxis, zAxis, tAxis, latAxis, lonAxis, hAxis, pAxis, ensAxis;
  private CoordinateAxis aziAxis, elevAxis, radialAxis;
  private final boolean isImplicit; // where set?

  protected CoordinateSystem(Builder<?> builder, NetcdfDataset ncd, List<CoordinateAxis> axesAll,
      List<CoordinateTransform> allTransforms) {
    this.ds = ncd;
    this.isImplicit = builder.isImplicit;

    // find referenced coordinate axes
    List<CoordinateAxis> axesList = new ArrayList<>();
    for (String axisName : StringUtil2.split(builder.coordAxesNames)) {
      Optional<CoordinateAxis> found = axesAll.stream().filter(a -> axisName.equals(a.getFullName())).findFirst();
      if (!found.isPresent()) {
        throw new RuntimeException("Cant find axis " + axisName);
      } else {
        axesList.add(found.get());
      }
    }
    axesList.sort(new CoordinateAxis.AxisComparator());
    this.coordAxes = ImmutableList.copyOf(axesList);

    // calculated
    this.name = makeName(coordAxes);

    for (CoordinateAxis axis : this.coordAxes) {
      // look for AxisType
      AxisType axisType = axis.getAxisType();
      if (axisType != null) {
        if (axisType == AxisType.GeoX)
          xAxis = lesserRank(xAxis, axis);
        if (axisType == AxisType.GeoY)
          yAxis = lesserRank(yAxis, axis);
        if (axisType == AxisType.GeoZ)
          zAxis = lesserRank(zAxis, axis);
        if (axisType == AxisType.Time)
          tAxis = lesserRank(tAxis, axis);
        if (axisType == AxisType.Lat)
          latAxis = lesserRank(latAxis, axis);
        if (axisType == AxisType.Lon)
          lonAxis = lesserRank(lonAxis, axis);
        if (axisType == AxisType.Height)
          hAxis = lesserRank(hAxis, axis);
        if (axisType == AxisType.Pressure)
          pAxis = lesserRank(pAxis, axis);
        if (axisType == AxisType.Ensemble)
          ensAxis = lesserRank(ensAxis, axis);

        if (axisType == AxisType.RadialAzimuth)
          aziAxis = lesserRank(aziAxis, axis);
        if (axisType == AxisType.RadialDistance)
          radialAxis = lesserRank(radialAxis, axis);
        if (axisType == AxisType.RadialElevation)
          elevAxis = lesserRank(elevAxis, axis);
      }
      // collect dimensions
      domain.addAll(Dimensions.makeDimensionsAll(axis));
    }

    // Find the named coordinate transforms in allTransforms.
    for (String want : builder.transNames) {
      // TODO what is the case where wantTransName matches attribute collection name?
      allTransforms.stream()
          .filter(ct -> (want.equals(ct.getName())
              || (ct.getCtvAttributes() != null && want.equals(ct.getCtvAttributes().getName()))))
          .findFirst().ifPresent(got -> coordTrans.add(got));
    }
  }

  /** Convert to a mutable Builder. */
  public Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  // Add local fields to the passed - in builder.
  protected Builder<?> addLocalFieldsToBuilder(Builder<? extends Builder<?>> b) {
    return b.setImplicit(this.isImplicit).setCoordAxesNames(this.name).addCoordinateTransforms(this.coordTrans);
  }

  /** Get a Builder of CoordinateSystem */
  public static Builder<?> builder() {
    return new Builder2();
  }

  private static class Builder2 extends Builder<Builder2> {
    @Override
    protected Builder2 self() {
      return this;
    }
  }

  public static abstract class Builder<T extends Builder<T>> {
    public String coordAxesNames = "";
    private final List<String> transNames = new ArrayList<>();
    private boolean isImplicit;
    private boolean built;

    protected abstract T self();

    /** @param names list of axes full names, space delimited. Doesnt have to be sorted. */
    public T setCoordAxesNames(String names) {
      this.coordAxesNames = names;
      return self();
    }

    public T addCoordinateTransformByName(String ct) {
      transNames.add(ct);
      return self();
    }

    public T addCoordinateTransforms(Collection<CoordinateTransform> transforms) {
      transforms.forEach(trans -> addCoordinateTransformByName(trans.name));
      return self();
    }

    public T setImplicit(boolean isImplicit) {
      this.isImplicit = isImplicit;
      return self();
    }

    /**
     * Build a CoordinateSystem
     * 
     * @param ncd The containing dataset, TODO remove after dt.GridCoordSys is deleted in ver7
     * @param axes Must contain all axes that are named in coordAxesNames
     * @param transforms Must contain all transforms that are named by addCoordinateTransformByName
     */
    public CoordinateSystem build(NetcdfDataset ncd, List<CoordinateAxis> axes, List<CoordinateTransform> transforms) {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      return new CoordinateSystem(this, ncd, axes, transforms);
    }
  }

}
