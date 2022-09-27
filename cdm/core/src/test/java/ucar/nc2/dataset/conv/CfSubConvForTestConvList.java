/*
 * Copyright (c) 1998-2022 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset.conv;

import java.util.List;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.internal.dataset.CoordSystemBuilder;
import ucar.nc2.internal.dataset.spi.CFSubConventionProvider;

/**
 * An implementation of CoordSystemBuilder for testing CFSubConvention hooks.
 *
 * @see ucar.nc2.dataset.TestCfSubConventionProvider
 */
public class CfSubConvForTestConvList extends CoordSystemBuilder {
  private static final String SUBCONVENTAION_NAME = "HECK-1.x";
  // public for testing
  public static String CONVENTAION_NAME_STARTS_WITH = "CF-1.X";
  public static String CONVENTION_NAME = CONVENTAION_NAME_STARTS_WITH + "/" + SUBCONVENTAION_NAME;

  private CfSubConvForTestConvList(NetcdfDataset.Builder<?> datasetBuilder) {
    super(datasetBuilder);
    this.conventionName = CONVENTION_NAME;
  }

  public static class Factory implements CFSubConventionProvider {
    @Override
    public boolean isMine(NetcdfFile ncfile) {
      return false;
    }

    @Override
    public boolean isMine(List<String> convs) {
      boolean mine = false;
      for (String conv : convs) {
        if (conv.startsWith(SUBCONVENTAION_NAME)) {
          mine = true;
          break;
        }
      }
      return mine;
    }

    @Override
    public String getConventionName() {
      return CONVENTION_NAME;
    }

    @Override
    public CoordSystemBuilder open(NetcdfDataset.Builder datasetBuilder) {
      return new CfSubConvForTestConvList(datasetBuilder);
    }
  }
}
