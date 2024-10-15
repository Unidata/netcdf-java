/*
 * Copyright (c) 2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.filter;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset.Enhance;
import ucar.nc2.dataset.VariableDS;

import java.util.Set;


/**
 * A Service Provider of {@link Enhancement}.
 */
public interface EnhancementProvider {
  String getAttributeName();

  boolean appliesTo(Set<Enhance> enhance, DataType dt);

  Enhancement create(VariableDS var);


}


