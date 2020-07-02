/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.dataset.conv;

import java.io.IOException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.spi.CoordSystemBuilderFactory;
import ucar.nc2.internal.dataset.CoordSystemBuilder;
import ucar.nc2.internal.dataset.CoordSystemFactory;
import ucar.nc2.internal.ncml.NcmlReader;
import ucar.nc2.util.CancelTask;

/** ATD Radar file (ad hoc guesses). */
public class ATDRadarConvention extends CoordSystemBuilder {
  private static final String CONVENTION_NAME = "ATDRadar";

  ATDRadarConvention(NetcdfDataset.Builder datasetBuilder) {
    super(datasetBuilder);
    this.conventionName = CONVENTION_NAME;
  }

  @Override
  public void augmentDataset(CancelTask cancelTask) throws IOException {
    NcmlReader.wrapNcmlResource(datasetBuilder, CoordSystemFactory.resourcesDir + "ATDRadar.ncml", cancelTask);
  }

  public static class Factory implements CoordSystemBuilderFactory {
    @Override
    public String getConventionName() {
      return CONVENTION_NAME;
    }

    @Override
    public boolean isMine(NetcdfFile ncfile) {
      // not really sure until we can examine more files
      String s = ncfile.getRootGroup().findAttributeString("sensor_name", "none");
      return s.equalsIgnoreCase("CRAFT/NEXRAD");
    }

    @Override
    public CoordSystemBuilder open(NetcdfDataset.Builder datasetBuilder) {
      return new ATDRadarConvention(datasetBuilder);
    }
  }

}
