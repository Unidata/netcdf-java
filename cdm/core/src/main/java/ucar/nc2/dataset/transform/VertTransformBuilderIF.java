/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset.transform;

import ucar.nc2.AttributeContainer;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VerticalCT;
import java.util.Formatter;
import ucar.unidata.geoloc.VerticalTransform;

/**
 * Implement this interface to add a Vertical Transform.
 * Must be able to know how to build one from the info in a Coordinate Transform Variable.
 */
public interface VertTransformBuilderIF {

  /**
   * Make a vertical VerticalCT from a Coordinate Transform Variable.
   * A VerticalCT is just a container for the metadata, the real work is in the VerticalTransform
   *
   * @param ds the containing dataset
   * @param ctv the coordinate transform variable.
   * @return CoordinateTransform
   */
  VerticalCT.Builder<?> makeCoordinateTransform(NetcdfFile ds, AttributeContainer ctv);

  /**
   * Make a VerticalTransform.
   * We need to defer making the transform until we've identified the time coordinate dimension.
   * 
   * @param ds the dataset
   * @param timeDim the time dimension
   * @param vCT the vertical coordinate transform
   * @return ucar.unidata.geoloc.VerticalTransform math transform
   */
  VerticalTransform makeMathTransform(NetcdfDataset ds, Dimension timeDim, VerticalCT vCT);

  /** Get the Transform name. */
  String getTransformName();

  /*** Pass in a Formatter where error messages can be appended. */
  void setErrorBuffer(Formatter sb);
}
