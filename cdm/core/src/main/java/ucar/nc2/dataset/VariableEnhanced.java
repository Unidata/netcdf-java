/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import com.google.common.collect.ImmutableList;
import javax.annotation.Nullable;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;

/** Interface to an "enhanced Variable". */
public interface VariableEnhanced {

  /** Get the full name of this Variable, with Group names */
  String getFullName();

  /** Get the short name of this Variable, local to its parent Group. */
  String getShortName();

  /** A VariableDS usually wraps another Variable. */
  @Nullable
  ucar.nc2.Variable getOriginalVariable();

  /** The original name of the Variable (in case it was renamed in NcML). */
  @Nullable
  String getOriginalName();

  /** Get the description of the Variable, or null if none. */
  @Nullable
  String getDescription();

  /** Get the Unit String for the Variable, or null if none. */
  @Nullable
  String getUnitsString();

  /** Get the containing Group. */
  Group getParentGroup();

  /** Get the list of Coordinate Systems for this Variable, larger number of axes first. */
  ImmutableList<CoordinateSystem> getCoordinateSystems();

  String toString();
}
