/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import com.google.common.collect.ImmutableList;
import javax.annotation.Nullable;
import ucar.nc2.Group;

/** Interface to an "enhanced Variable". */
public interface VariableEnhanced {

  String getFullName();

  String getShortName();

  ucar.nc2.Variable getOriginalVariable();

  String getOriginalName();

  /** Get the description of the Variable, or null if none. */
  @Nullable
  String getDescription();

  /** Get the Unit String for the Variable, or null if none. */
  @Nullable
  String getUnitsString();

  /** Get the containing Group. */
  Group getParentGroup();

  /** Get the list of Coordinate Systems for this Variable. */
  ImmutableList<CoordinateSystem> getCoordinateSystems();

}
