/*
 * Copyright (c) 1998-2022 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset.conv;

import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.constants.CDM;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.internal.dataset.CoordSystemBuilder;
import ucar.nc2.internal.dataset.spi.CFSubConventionProvider;

/**
 * An implementation of CoordSystemBuilder for testing CFSubConvention hooks.
 *
 * @see ucar.nc2.dataset.TestCfSubConventionProvider
 */
public class CfSubConvForTest extends CoordSystemBuilder {
  public static String CONVENTION_NAME = "CF-1.200/YOLO";

  private CfSubConvForTest(NetcdfDataset.Builder<?> datasetBuilder) {
    super(datasetBuilder);
    this.conventionName = CONVENTION_NAME;
  }

  public static class Factory implements CFSubConventionProvider {
    @Override
    public boolean isMine(NetcdfFile ncfile) {
      boolean mine = false;
      Attribute conventionAttr = ncfile.findGlobalAttributeIgnoreCase(CDM.CONVENTIONS);
      if (conventionAttr != null) {
        String conventionValue = conventionAttr.getStringValue();
        if (conventionValue != null) {
          mine = conventionValue.equals(CONVENTION_NAME);
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
      return new CfSubConvForTest(datasetBuilder);
    }
  }
}
