/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.dataset.transform.vertical;

import ucar.nc2.Dimension;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.geoloc.vertical.VerticalTransform;

public interface VerticalTransformBuilder {

  /**
   * Make a VerticalTransform.
   * We need to defer making the transform until we've identified the time coordinate dimension.
   * 
   * @param ds the dataset
   * @param timeDim the time dimension
   * @return ucar.unidata.geoloc.vertical.VerticalTransform math transform
   */
  VerticalTransform makeVerticalTransform(NetcdfDataset ds, Dimension timeDim);
}
