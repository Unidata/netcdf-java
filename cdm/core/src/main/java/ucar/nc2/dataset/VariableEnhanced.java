/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import java.util.Set;

/** Interface to an "enhanced Variable", implemented by the ucar.nc2.dataset package. */
public interface VariableEnhanced extends Enhancements {

  String getFullName();

  String getShortName();

  ucar.nc2.Variable getOriginalVariable();

  String getOriginalName();

  /** @deprecated do not use */
  @Deprecated
  void setOriginalVariable(ucar.nc2.Variable orgVar);

  /**
   * Set the Unit String for this Variable. Default is to use the CDM.UNITS attribute.
   * 
   * @param units unit string
   * @deprecated do not use
   */
  @Deprecated
  void setUnitsString(String units);

  /**
   * Enhance using the given set of NetcdfDataset.Enhance
   * 
   * @deprecated do not use
   */
  @Deprecated
  void enhance(Set<NetcdfDataset.Enhance> mode);

  /**
   * clear previous coordinate systems. if any
   * 
   * @deprecated do not use
   */
  @Deprecated
  void clearCoordinateSystems();
}
