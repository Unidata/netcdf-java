/*
 * Copyright (c) 2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.filter;

import java.util.Map;
//package ucar.nc2.dataset;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.dataset.NetcdfDataset.Enhance;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.filter.*;
import ucar.nc2.internal.dataset.CoordinatesHelper;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.Misc;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

/**
 * A Service Provider of {@link Filter}.
 */
public interface EnhancementProvider {

 String getName();

 boolean canDo (Set<ucar.nc2.dataset.NetcdfDataset.Enhance> enhancements);

 boolean appliesTo(Enhance enhance, AttributeContainer attributes);

 boolean appliesTo(Enhance enhance, VariableDS var);

 void Create(VariableDS var);
 Enhancement returnObject(VariableDS var);


//  void applyEnhancement(Object instance);

}


