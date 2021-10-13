/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.dataset;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import javax.annotation.concurrent.Immutable;

import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.*;
import ucar.nc2.internal.dataset.transform.horiz.ProjectionCTV;
import ucar.nc2.internal.dataset.transform.horiz.ProjectionFactory;

/** An immutable helper class for NetcdfDataset to build and manage coordinates. */
@Immutable
public class CoordinatesHelper {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CoordinatesHelper.class);

  public ImmutableList<CoordinateAxis> getCoordAxes() {
    return coordAxes;
  }

  public ImmutableList<CoordinateSystem> getCoordSystems() {
    return coordSystems;
  }

  public Optional<CoordinateSystem> findCoordSystem(String name) {
    return coordSystems.stream().filter(cs -> cs.getName().equals(name)).findFirst();
  }

  public ImmutableList<ProjectionCTV> getCoordTransforms() {
    return coordTransforms;
  }

  public ImmutableList<CoordinateSystem> makeCoordinateSystemsFor(Variable v) {
    ArrayList<CoordinateSystem> result = new ArrayList<>();
    for (CoordinateSystem csys : coordSystems) {
      if (csys.isCoordinateSystemFor(v) && csys.isComplete(v)) {
        result.add(csys);
      }
    }
    result.sort((cs1, cs2) -> cs2.getCoordinateAxes().size() - cs1.getCoordinateAxes().size());
    return ImmutableList.copyOf(result);
  }

  public static ImmutableList<CoordinateAxis> makeAxes(NetcdfDataset ncd) {
    List<CoordinateAxis> axes = new ArrayList<>();
    addAxes(ncd.getRootGroup(), axes);
    return ImmutableList.copyOf(axes);
  }

  // TODO this assumes that the CoordinateAxis have been added to the NetcdfDataset
  // and appears to ignore builder.axes
  private static void addAxes(Group group, List<CoordinateAxis> axes) {
    for (Variable v : group.getVariables()) {
      if (v instanceof CoordinateAxis) {
        axes.add((CoordinateAxis) v);
      }
      if (v instanceof Structure) {
        Structure s = (Structure) v;
        for (Variable nested : s.getVariables()) {
          if (nested instanceof CoordinateAxis) {
            axes.add((CoordinateAxis) nested);
          }
        }
      }
    }
    for (Group nestedGroup : group.getGroups()) {
      addAxes(nestedGroup, axes);
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////
  private final ImmutableList<CoordinateAxis> coordAxes;
  private final ImmutableList<CoordinateSystem> coordSystems;
  private final ImmutableList<ProjectionCTV> coordTransforms;

  private CoordinatesHelper(Builder builder, NetcdfDataset ncd, ImmutableList<CoordinateAxis> axes) {
    this.coordAxes = axes;

    ImmutableList.Builder<ProjectionCTV> ctBuilders = ImmutableList.builder();
    ctBuilders.addAll(builder.coordTransforms.stream().filter(Objects::nonNull).collect(Collectors.toList()));
    coordTransforms = ctBuilders.build();

    List<ProjectionCTV> allProjections =
        coordTransforms.stream().filter(ProjectionFactory::hasProjectionFor).collect(Collectors.toList());

    // TODO remove coordSys not used by a variable....
    this.coordSystems = builder.coordSys.stream().map(csb -> csb.build(ncd, this.coordAxes, allProjections))
        .filter(Objects::nonNull).collect(ImmutableList.toImmutableList());
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////

  public static Builder builder() {
    return new Builder();
  }

  // Mutable Builder. */
  public static class Builder {
    public List<CoordinateAxis.Builder<?>> coordAxes = new ArrayList<>();
    public List<CoordinateSystem.Builder<?>> coordSys = new ArrayList<>();
    public List<ProjectionCTV> coordTransforms = new ArrayList<>();
    private boolean built;

    public Builder addCoordinateAxis(CoordinateAxis.Builder<?> axis) {
      if (axis == null) {
        return this;
      }
      coordAxes.add(axis);
      return this;
    }

    public Builder addCoordinateAxes(Collection<CoordinateAxis.Builder<?>> axes) {
      Preconditions.checkNotNull(axes);
      axes.forEach(this::addCoordinateAxis);
      return this;
    }

    private Optional<CoordinateAxis.Builder<?>> findAxisByVerticalSearch(Variable.Builder<?> vb, String shortName) {
      Optional<Variable.Builder<?>> axis = vb.getParentGroupBuilder().findVariableOrInParent(shortName);
      if (axis.isPresent()) {
        if (axis.get() instanceof CoordinateAxis.Builder) {
          return Optional.of((CoordinateAxis.Builder<?>) axis.get());
        }
      }
      return Optional.empty();
    }

    private Optional<CoordinateAxis.Builder<?>> findAxisByFullName(String fullName) {
      return coordAxes.stream().filter(axis -> axis.getFullName().equals(fullName)).findFirst();
    }

    public Optional<CoordinateAxis.Builder<?>> findAxisByType(CoordinateSystem.Builder<?> csys, AxisType type) {
      for (CoordinateAxis.Builder<?> axis : getAxesForSystem(csys)) {
        if (axis.axisType == type) {
          return Optional.of(axis);
        }
      }
      return Optional.empty();
    }

    public boolean replaceCoordinateAxis(CoordinateAxis.Builder<?> axis) {
      Optional<CoordinateAxis.Builder<?>> want = findAxisByFullName(axis.getFullName());
      want.ifPresent(v -> coordAxes.remove(v));
      addCoordinateAxis(axis);
      return want.isPresent();
    }

    // LOOK dedup
    public Builder addCoordinateSystem(CoordinateSystem.Builder<?> cs) {
      Preconditions.checkNotNull(cs);
      coordSys.add(cs);
      return this;
    }

    Optional<CoordinateSystem.Builder<?>> findCoordinateSystem(String coordAxesNames) {
      Preconditions.checkNotNull(coordAxesNames);
      return coordSys.stream().filter(cs -> cs.coordAxesNames.equals(coordAxesNames)).findFirst();
    }

    public Builder addCoordinateSystems(Collection<CoordinateSystem.Builder<?>> systems) {
      Preconditions.checkNotNull(systems);
      coordSys.addAll(systems);
      return this;
    }

    // this is used when making a copy, we've thrown away the TransformBuilder
    public Builder addCoordinateTransform(ProjectionCTV ct) {
      Preconditions.checkNotNull(ct);
      if (coordTransforms.stream().noneMatch(old -> old.getName().equals(ct.getName()))) {
        coordTransforms.add(ct);
      }
      return this;
    }

    private List<CoordinateAxis.Builder<?>> getAxesForSystem(CoordinateSystem.Builder<?> cs) {
      Preconditions.checkNotNull(cs);
      List<CoordinateAxis.Builder<?>> axes = new ArrayList<>();
      StringTokenizer stoker = new StringTokenizer(cs.coordAxesNames);
      while (stoker.hasMoreTokens()) {
        String vname = stoker.nextToken();
        Optional<CoordinateAxis.Builder<?>> vbOpt = findAxisByFullName(vname);
        if (vbOpt.isPresent()) {
          axes.add(vbOpt.get());
        } else {
          throw new IllegalArgumentException("Cant find axis " + vname);
        }
      }
      return axes;
    }

    String makeCanonicalName(Variable.Builder<?> vb, String axesNames) {
      Preconditions.checkNotNull(axesNames);
      List<CoordinateAxis.Builder<?>> axes = new ArrayList<>();
      StringTokenizer stoker = new StringTokenizer(axesNames);
      while (stoker.hasMoreTokens()) {
        String vname = stoker.nextToken();
        Optional<CoordinateAxis.Builder<?>> vbOpt = findAxisByFullName(vname);
        if (!vbOpt.isPresent()) {
          vbOpt = findAxisByVerticalSearch(vb, vname);
        }
        if (vbOpt.isPresent()) {
          axes.add(vbOpt.get());
        } else {
          // TODO this should fail, leaving it here to match current behavior.
          log.warn("No axis named {}", vname);
          // throw new IllegalArgumentException("Cant find axis " + vname);
        }
      }
      return makeCanonicalName(axes);
    }

    String makeCanonicalName(List<CoordinateAxis.Builder<?>> axes) {
      Preconditions.checkNotNull(axes);
      return axes.stream().sorted(new AxisComparator()).map(a -> a.getFullName()).collect(Collectors.joining(" "));
    }

    // Check if this Coordinate System is complete for v, ie if v dimensions are a subset..
    public boolean isComplete(CoordinateSystem.Builder<?> cs, Variable.Builder<?> vb) {
      Preconditions.checkNotNull(cs);
      Preconditions.checkNotNull(vb);
      HashSet<Dimension> csDomain = new HashSet<>();
      getAxesForSystem(cs).forEach(axis -> csDomain.addAll(axis.getDimensions()));
      return CoordinateSystem.isComplete(vb.getDimensions(), csDomain);
    }

    public boolean containsAxes(CoordinateSystem.Builder<?> cs, List<CoordinateAxis.Builder<?>> dataAxes) {
      Preconditions.checkNotNull(cs);
      Preconditions.checkNotNull(dataAxes);
      List<CoordinateAxis.Builder<?>> csAxes = getAxesForSystem(cs);
      return csAxes.containsAll(dataAxes);
    }

    public boolean containsAxisTypes(CoordinateSystem.Builder<?> cs, List<AxisType> axisTypes) {
      Preconditions.checkNotNull(cs);
      Preconditions.checkNotNull(axisTypes);
      List<CoordinateAxis.Builder<?>> csAxes = getAxesForSystem(cs);
      for (AxisType axisType : axisTypes) {
        if (!containsAxisTypes(csAxes, axisType))
          return false;
      }
      return true;
    }

    private boolean containsAxisTypes(List<CoordinateAxis.Builder<?>> axes, AxisType want) {
      for (CoordinateAxis.Builder<?> axis : axes) {
        if (axis.axisType == want)
          return true;
      }
      return false;
    }

    // Note that only ncd.axes can be accessed, not coordsys or transforms.
    public CoordinatesHelper build(NetcdfDataset ncd, ImmutableList<CoordinateAxis> coordAxes) {
      Preconditions.checkNotNull(ncd);
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      return new CoordinatesHelper(this, ncd, coordAxes);
    }
  }

  private static class AxisComparator implements java.util.Comparator<CoordinateAxis.Builder<?>> {
    public int compare(CoordinateAxis.Builder c1, CoordinateAxis.Builder c2) {
      AxisType t1 = c1.axisType;
      AxisType t2 = c2.axisType;

      if ((t1 == null) && (t2 == null))
        return c1.getFullName().compareTo(c2.getFullName());
      if (t1 == null)
        return -1;
      if (t2 == null)
        return 1;

      return t1.axisOrder() - t2.axisOrder();
    }
  }
}
