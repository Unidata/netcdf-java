/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.dataset.conv;

import java.io.IOException;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.spi.CoordSystemBuilderFactory;
import ucar.nc2.internal.dataset.CoordSystemBuilder;
import ucar.nc2.internal.dataset.CoordSystemFactory;
import ucar.nc2.internal.ncml.NcMLReaderNew;
import ucar.nc2.util.CancelTask;

/** Zebra ATD files. */
public class ZebraConvention extends CoordSystemBuilder {
  private static final String CONVENTION_NAME = "Zebra";

  ZebraConvention(NetcdfDataset.Builder datasetBuilder) {
    super(datasetBuilder);
    this.conventionName = CONVENTION_NAME;
  }

  @Override
  protected void augmentDataset(CancelTask cancelTask) throws IOException {
    NcMLReaderNew.wrapNcMLresource(datasetBuilder, CoordSystemFactory.resourcesDir + "Zebra.ncml", cancelTask);

    // special time handling
    // the time coord var is created in the NcML
    // set its values = base_time + time_offset(time)
    Dimension timeDim = rootGroup.findDimension("time").orElse(null);
    VariableDS.Builder base_time = (VariableDS.Builder) rootGroup.findVariable("base_time").orElse(null);
    VariableDS.Builder time_offset = (VariableDS.Builder) rootGroup.findVariable("time_offset").orElse(null);
    Variable.Builder time = rootGroup.findVariable("time").orElse(null);
    if ((timeDim == null) || (base_time == null) || (time_offset == null) || (time == null))
      return;

    String units =
        base_time.getAttributeContainer().findAttValueIgnoreCase(CDM.UNITS, "seconds since 1970-01-01 00:00 UTC");
    time.addAttribute(new Attribute(CDM.UNITS, units));

    Array data;
    try {
      double baseValue = base_time.orgVar.readScalarDouble();

      data = time_offset.orgVar.read();
      IndexIterator iter = data.getIndexIterator();
      while (iter.hasNext())
        iter.setDoubleCurrent(iter.getDoubleNext() + baseValue);

    } catch (IOException ioe) {
      parseInfo.format("ZebraConvention failed to create time Coord Axis for file %s err= %s%n",
          datasetBuilder.location, ioe);
      return;
    }

    time.setCachedData(data, true);
  }

  public static class Factory implements CoordSystemBuilderFactory {
    @Override
    public String getConventionName() {
      return CONVENTION_NAME;
    }

    @Override
    public boolean isMine(NetcdfFile ncfile) {
      String s = ncfile.getRootGroup().findAttValueIgnoreCase("Convention", "none");
      return s.startsWith("Zebra");
    }

    @Override
    public CoordSystemBuilder open(NetcdfDataset.Builder datasetBuilder) {
      return new ZebraConvention(datasetBuilder);
    }
  }

}
