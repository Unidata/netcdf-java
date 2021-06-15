/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import com.google.common.collect.ImmutableList;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.FeatureType;

import java.io.Closeable;
import java.util.Formatter;
import java.util.Optional;

/** A Dataset that contains Grids. */
public interface GridDataset extends Closeable {

  String getName();

  String getLocation();

  AttributeContainer attributes();

  FeatureType getFeatureType();

  ImmutableList<GridCoordinateSystem> getGridCoordinateSystems();

  ImmutableList<GridAxis> getGridAxes();

  ImmutableList<Grid> getGrids();

  /** Find grid using its full name. */
  Optional<Grid> findGrid(String name);

  /** Find first grid whose name and String value match those given. */
  default Optional<Grid> findGridByAttribute(String attName, String attValue) {
    for (Grid cov : getGrids()) {
      for (Attribute att : cov.attributes())
        if (attName.equals(att.getShortName()) && attValue.equals(att.getStringValue()))
          return Optional.of(cov);
    }
    return Optional.empty();
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
    for (GridAxis axis : getGridAxes()) {
      buf.format("%s%n", axis);
    }

    buf.format("%nGrids%n");
    for (Grid grid : getGrids()) {
      buf.format("%s%n", grid);
    }
  }

}
