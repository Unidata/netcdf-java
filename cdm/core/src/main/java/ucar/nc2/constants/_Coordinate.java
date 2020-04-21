/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.constants;

/**
 * Constants for _Coordinate Conventions.
 * Used to annotate Variables, using Attributes.
 *
 * @see <a href="https://www.unidata.ucar.edu/software/netcdf-java/reference/CoordinateAttributes.html">_Coordinate
 *      Conventions</a>
 * @author caron
 */
public class _Coordinate {
  public static final String AliasForDimension = "_CoordinateAliasForDimension";
  public static final String Axes = "_CoordinateAxes";
  public static final String AxisType = "_CoordinateAxisType";
  public static final String AxisTypes = "_CoordinateAxisTypes";
  public static final String Convention = "_Coordinates";
  public static final String ModelBaseDate = "_CoordinateModelBaseDate"; // experimental
  public static final String ModelRunDate = "_CoordinateModelRunDate"; // experimental
  public static final String Stagger = "_CoordinateStagger";
  public static final String SystemFor = "_CoordinateSystemFor";
  public static final String Systems = "_CoordinateSystems";
  public static final String Transforms = "_CoordinateTransforms";
  public static final String TransformType = "_CoordinateTransformType";
  public static final String ZisLayer = "_CoordinateZisLayer";
  public static final String ZisPositive = "_CoordinateZisPositive";

  // global attributes
  public static final String _CoordSysBuilder = "_CoordSysBuilder";

  // class not interface, per Bloch edition 2 item 19
  private _Coordinate() {} // disable instantiation
}
