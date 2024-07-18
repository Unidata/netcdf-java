/*
 * Copyright (c) 2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.filter;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset.Enhance;
import ucar.nc2.dataset.VariableDS;


/**
 * A Service Provider of {@link Enhancement}.
 */
public interface EnhancementProvider {

  boolean appliesTo(Enhance enhance, AttributeContainer attributes, DataType dt);

  Enhancement create(VariableDS var);


}


