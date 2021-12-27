/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.geoloc.vertical.VerticalTransform;

import java.io.Closeable;
import java.util.Formatter;
import java.util.List;
import java.util.Optional;

/**
 * A Dataset that contains Grids.
 */
public interface GridDataset extends Closeable {

  /** The name of the gridDataset. */
  String getName();

  /** The location, eg filename or url. */
  String getLocation();

  /** Global attributes. */
  AttributeContainer attributes();

  /** FeatureType.GRID or FeatureType.CURVILINEAR. */
  FeatureType getFeatureType();

  /** All of the GridCoordinateSystem. */
  List<GridCoordinateSystem> getGridCoordinateSystems();

  /** All of the GridAxes. */
  List<GridAxis<?>> getGridAxes();

  /** All of the Grids. */
  List<Grid> getGrids();

  /** Find grid using its full name. */
  default Optional<Grid> findGrid(String name) {
    return getGrids().stream().filter(g -> g.getName().equals(name)).findFirst();
  }

  /** Find first grid whose name and String value match those given. */
  default Optional<Grid> findGridByAttribute(String attName, String attValue) {
    for (Grid cov : getGrids()) {
      for (Attribute att : cov.attributes())
        if (attName.equals(att.getShortName()) && attValue.equals(att.getStringValue()))
          return Optional.of(cov);
    }
    return Optional.empty();
  }

  /** Find VerticalTransform using its hashCode. */
  default Optional<VerticalTransform> findVerticalTransformByHash(int hash) {
    return getGridCoordinateSystems().stream().map(cs -> cs.getVerticalTransform())
        .filter(vt -> (vt != null) && vt.hashCode() == hash).findFirst();
  }

  default void toString(Formatter buf) {
    buf.format("name = %s%n", getName());
    buf.format("location = %s%n", getLocation());
    buf.format("featureType = %s%n", getFeatureType());
    for (Attribute att : attributes()) {
      buf.format("  %s%n", att);
    }

    buf.format("%nGridCoordinateSystem%n");
    for (GridCoordinateSystem gcs : getGridCoordinateSystems()) {
      buf.format("%s%n", gcs);
    }

    buf.format("%nGridAxes%n");
    for (GridAxis<?> axis : getGridAxes()) {
      buf.format("%s%n", axis);
    }

    buf.format("%nGrids%n");
    for (Grid grid : getGrids()) {
      buf.format("%s%n", grid);
    }
  }

}
