/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import com.google.common.collect.ImmutableList;
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

  Optional<Grid> findGrid(String name);

  void toString(Formatter f);

}
