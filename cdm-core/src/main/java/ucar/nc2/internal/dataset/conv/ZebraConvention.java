/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.dataset.conv;

import java.io.IOException;

import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
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
import ucar.nc2.internal.ncml.NcmlReader;
import ucar.nc2.util.CancelTask;

/** Zebra ATD files. */
public class ZebraConvention extends CoordSystemBuilder {
  private static final String CONVENTION_NAME = "Zebra";

  ZebraConvention(NetcdfDataset.Builder<?> datasetBuilder) {
    super(datasetBuilder);
    this.conventionName = CONVENTION_NAME;
  }

  @Override
  protected void augmentDataset(CancelTask cancelTask) throws IOException {
    NcmlReader.wrapNcmlResource(datasetBuilder, CoordSystemFactory.resourcesDir + "Zebra.ncml", cancelTask);

    // special time handling
    // the time coord var is created in the NcML
    // set its values = base_time + time_offset(time)
    Dimension timeDim = rootGroup.findDimension("time").orElse(null);
    VariableDS.Builder<?> base_time = (VariableDS.Builder<?>) rootGroup.findVariableLocal("base_time").orElse(null);
    VariableDS.Builder<?> time_offset = (VariableDS.Builder<?>) rootGroup.findVariableLocal("time_offset").orElse(null);
    Variable.Builder<?> time = rootGroup.findVariableLocal("time").orElse(null);
    if ((timeDim == null) || (base_time == null) || (time_offset == null) || (time == null))
      return;

    String units =
        base_time.getAttributeContainer().findAttributeString(CDM.UNITS, "seconds since 1970-01-01 00:00 UTC");
    time.addAttribute(new Attribute(CDM.UNITS, units));

    Array<Number> orgData;
    Array<Number> modData;
    try {
      double baseValue = ((Number) base_time.orgVar.readArray().getScalar()).doubleValue();
      orgData = (Array<Number>) time_offset.orgVar.readArray();

      int n = (int) orgData.length();
      double[] storage = new double[n];
      int count = 0;
      for (Number val : orgData) {
        storage[count++] = val.doubleValue() + baseValue;
      }
      modData = Arrays.factory(ArrayType.DOUBLE, orgData.getShape(), storage);

    } catch (IOException ioe) {
      parseInfo.format("ZebraConvention failed to create time Coord Axis for file %s err= %s%n",
          datasetBuilder.location, ioe);
      return;
    }

    time.setSourceData(modData);
  }

  public static class Factory implements CoordSystemBuilderFactory {
    @Override
    public String getConventionName() {
      return CONVENTION_NAME;
    }

    @Override
    public boolean isMine(NetcdfFile ncfile) {
      String s = ncfile.getRootGroup().findAttributeString("Convention", "none");
      return s.startsWith("Zebra");
    }

    @Override
    public CoordSystemBuilder open(NetcdfDataset.Builder<?> datasetBuilder) {
      return new ZebraConvention(datasetBuilder);
    }
  }

}
