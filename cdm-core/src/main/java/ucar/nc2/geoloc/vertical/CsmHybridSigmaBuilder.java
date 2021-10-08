/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.geoloc.vertical;

import ucar.nc2.AttributeContainer;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.NetcdfDataset;

import javax.annotation.concurrent.Immutable;
import java.io.IOException;
import java.util.Formatter;
import java.util.Optional;

import static ucar.nc2.geoloc.vertical.AbstractVerticalTransform.getRank;
import static ucar.nc2.geoloc.vertical.AbstractVerticalTransform.getUnits;

/**
 * Implement CSM "ocean_sigma_coordinate".
 * 
 * <pre>
 * pressure(x, y, z) = a(z) * p0 + b(z) * surfacePressure(x, y)
 * </pre>
 * 
 * Get parameters from ctv, use AtmosHybridSigmaPressure.
 */
@Immutable
public class CsmHybridSigmaBuilder implements AbstractVerticalTransform.Builder {
  public static final String transform_name = "csm_hybrid_sigma_pressure";

  public Optional<VerticalTransform> create(NetcdfDataset ds, CoordinateSystem csys, AttributeContainer params,
      Formatter errlog) {

    String aName = params.findAttributeString("A_var", null);
    String bName = params.findAttributeString("B_var", null);
    String psName = params.findAttributeString("PS_var", null);
    String p0Name = params.findAttributeString("P0_var", null);
    String units = getUnits(ds, psName);

    int rank = getRank(ds, aName);
    if (rank != 1) {
      errlog.format("CsmHybridSigma %s: aName has rank %d should be 1%n", params.getName(), rank);
      return Optional.empty();
    }
    rank = getRank(ds, bName);
    if (rank != 1) {
      errlog.format("CsmHybridSigma %s: bName has rank %d should be 1%n", params.getName(), rank);
      return Optional.empty();
    }

    int psRank = getRank(ds, psName);
    if (psRank < 2 || psRank > 3) {
      errlog.format("AtmosHybridSigmaPressure %s: ps has rank %d should be 2 or 3%n", params.getName(), psRank);
      return Optional.empty();
    }

    try {
      return Optional
          .of(new AtmosHybridSigmaPressure(ds, params.getName(), units, aName, bName, psName, p0Name, psRank));
    } catch (IOException e) {
      errlog.format("CsmHybridSigma %s: failed err = %s%n", params.getName(), e.getMessage());
      return Optional.empty();
    }
  }
}

