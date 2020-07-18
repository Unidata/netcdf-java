/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.dataset;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import javax.annotation.concurrent.Immutable;
import ucar.nc2.Group;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.CoordinateTransform;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.internal.dataset.transform.vertical.VerticalCTBuilder;

/** A helper class for NetcdfDataset to build and manage coordinates. */
@Immutable
public class CoordinatesHelper {
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CoordinatesHelper.class);
  private final ImmutableList<CoordinateAxis> coordAxes;
  private final List<CoordinateSystem> coordSystems;
  private final List<CoordinateTransform> coordTransforms;

  public List<CoordinateAxis> getCoordAxes() {
    return coordAxes;
  }

  public List<CoordinateSystem> getCoordSystems() {
    return coordSystems;
  }

  public Optional<CoordinateSystem> findCoordSystem(String name) {
    return coordSystems.stream().filter(cs -> cs.getName().equals(name)).findFirst();
  }

  public List<CoordinateTransform> getCoordTransforms() {
    return coordTransforms;
  }

  private CoordinatesHelper(Builder builder, NetcdfDataset ncd) {
    List<CoordinateAxis> axes = new ArrayList<>();
    addAxes(ncd.getRootGroup(), axes);
    this.coordAxes = ImmutableList.copyOf(axes);

    coordTransforms =
        builder.coordTransforms.stream().map(ct -> ct.build(ncd)).filter(Objects::nonNull).collect(Collectors.toList());

    coordTransforms.addAll(builder.verticalCTBuilders.stream().map(ct -> ct.makeVerticalCT(ncd))
        .filter(Objects::nonNull).collect(Collectors.toList()));

    this.coordSystems = builder.coordSys.stream().map(s -> s.build(ncd, this.coordAxes, this.coordTransforms))
        .collect(Collectors.toList());
  }

  private void addAxes(Group group, List<CoordinateAxis> axes) {
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

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    public List<CoordinateAxis.Builder> coordAxes = new ArrayList<>();
    public List<CoordinateSystem.Builder> coordSys = new ArrayList<>();
    public List<CoordinateTransform.Builder> coordTransforms = new ArrayList<>();
    List<VerticalCTBuilder> verticalCTBuilders = new ArrayList<>();
    private boolean built;

    public Builder addCoordinateAxis(CoordinateAxis.Builder axis) {
      if (axis == null) {
        return this;
      }
      coordAxes.add(axis);
      return this;
    }

    public Builder addCoordinateAxes(Collection<CoordinateAxis.Builder> axes) {
      Preconditions.checkNotNull(axes);
      axes.forEach(a -> addCoordinateAxis(a));
      return this;
    }

    private Optional<CoordinateAxis.Builder> findAxisByVerticalSearch(Variable.Builder vb, String shortName) {
      Optional<Variable.Builder<?>> axis = vb.getParentGroupBuilder().findVariableOrInParent(shortName);
      if (axis.isPresent()) {
        if (axis.get() instanceof CoordinateAxis.Builder) {
          return Optional.of((CoordinateAxis.Builder) axis.get());
        }
      }
      return Optional.empty();
    }


    private Optional<CoordinateAxis.Builder> findAxisByFullName(String fullName) {
      return coordAxes.stream().filter(axis -> axis.getFullName().equals(fullName)).findFirst();
    }

    public Optional<CoordinateAxis.Builder> findAxisByType(CoordinateSystem.Builder csys, AxisType type) {
      for (CoordinateAxis.Builder<?> axis : getAxesForSystem(csys)) {
        if (axis.axisType == type) {
          return Optional.of(axis);
        }
      }
      return Optional.empty();
    }

    public boolean replaceCoordinateAxis(CoordinateAxis.Builder<?> axis) {
      Optional<CoordinateAxis.Builder> want = findAxisByFullName(axis.getFullName());
      want.ifPresent(v -> coordAxes.remove(v));
      addCoordinateAxis(axis);
      return want.isPresent();
    }

    // LOOK dedup
    public Builder addCoordinateSystem(CoordinateSystem.Builder cs) {
      Preconditions.checkNotNull(cs);
      coordSys.add(cs);
      return this;
    }

    public Builder addVerticalCTBuilder(VerticalCTBuilder vctb) {
      verticalCTBuilders.add(vctb);
      return this;
    }

    Optional<CoordinateSystem.Builder> findCoordinateSystem(String coordAxesNames) {
      Preconditions.checkNotNull(coordAxesNames);
      return coordSys.stream().filter(cs -> cs.coordAxesNames.equals(coordAxesNames)).findFirst();
    }

    public Builder addCoordinateSystems(Collection<CoordinateSystem.Builder> systems) {
      Preconditions.checkNotNull(systems);
      coordSys.addAll(systems);
      return this;
    }

    public Builder addCoordinateTransform(CoordinateTransform.Builder ct) {
      Preconditions.checkNotNull(ct);
      if (!findCoordinateTransform(ct.name).isPresent()) {
        coordTransforms.add(ct);
      }
      return this;
    }

    public Builder addCoordinateTransforms(Collection<CoordinateTransform.Builder> transforms) {
      Preconditions.checkNotNull(transforms);
      transforms.forEach(ct -> addCoordinateTransform(ct));
      return this;
    }

    private Optional<CoordinateTransform.Builder> findCoordinateTransform(String ctName) {
      Preconditions.checkNotNull(ctName);
      return coordTransforms.stream().filter(ct -> ct.name.equals(ctName)).findFirst();
    }

    private List<CoordinateAxis.Builder<?>> getAxesForSystem(CoordinateSystem.Builder cs) {
      Preconditions.checkNotNull(cs);
      List<CoordinateAxis.Builder<?>> axes = new ArrayList<>();
      StringTokenizer stoker = new StringTokenizer(cs.coordAxesNames);
      while (stoker.hasMoreTokens()) {
        String vname = stoker.nextToken();
        Optional<CoordinateAxis.Builder> vbOpt = findAxisByFullName(vname);
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
      List<CoordinateAxis.Builder> axes = new ArrayList<>();
      StringTokenizer stoker = new StringTokenizer(axesNames);
      while (stoker.hasMoreTokens()) {
        String vname = stoker.nextToken();
        Optional<CoordinateAxis.Builder> vbOpt = findAxisByFullName(vname);
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

    String makeCanonicalName(List<CoordinateAxis.Builder> axes) {
      Preconditions.checkNotNull(axes);
      return axes.stream().sorted(new AxisComparator()).map(a -> a.getFullName()).collect(Collectors.joining(" "));
    }

    public CoordinatesHelper build(NetcdfDataset ncd) {
      Preconditions.checkNotNull(ncd);
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      return new CoordinatesHelper(this, ncd);
    }

    // Check if this Coordinate System is complete for v, ie if v dimensions are a subset..
    public boolean isComplete(CoordinateSystem.Builder<?> cs, Variable.Builder<?> vb) {
      Preconditions.checkNotNull(cs);
      Preconditions.checkNotNull(vb);
      // TODO using strings instead of Dimensions, to avoid exposing mutable Dimension objects.
      // TODO Might reconsider in 6.
      Set<String> varDomain = ImmutableSet.copyOf(vb.getDimensionsAll().iterator());
      HashSet<String> csDomain = new HashSet<>();
      getAxesForSystem(cs).forEach(axis -> csDomain.addAll(axis.getDimensionsAll()));
      return CoordinateSystem.isSubset(varDomain, csDomain);
    }

    public boolean containsAxes(CoordinateSystem.Builder cs, List<CoordinateAxis.Builder> dataAxes) {
      Preconditions.checkNotNull(cs);
      Preconditions.checkNotNull(dataAxes);
      List<CoordinateAxis.Builder<?>> csAxes = getAxesForSystem(cs);
      return csAxes.containsAll(dataAxes);
    }

    public boolean containsAxisTypes(CoordinateSystem.Builder cs, List<AxisType> axisTypes) {
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
      for (CoordinateAxis.Builder axis : axes) {
        if (axis.axisType == want)
          return true;
      }
      return false;
    }
  }

  private static class AxisComparator implements java.util.Comparator<CoordinateAxis.Builder> {
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
