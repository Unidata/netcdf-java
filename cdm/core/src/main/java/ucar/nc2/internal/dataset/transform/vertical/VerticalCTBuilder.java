/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.dataset.transform.vertical;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VerticalCT;

public interface VerticalCTBuilder {
  /**
   * Make a Vertical CoordinateTransform from a Coordinate Transform Variable.
   *
   * @param ds the containing dataset
   * @return CoordinateTransform
   */
  VerticalCT makeVerticalCT(NetcdfDataset ds);

  /**
   * Get the VerticalCT name.
   * 
   * @return name of the transform.
   */
  String getTransformName();
}
