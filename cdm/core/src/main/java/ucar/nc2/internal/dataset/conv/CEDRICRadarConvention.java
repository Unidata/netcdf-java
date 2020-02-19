/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.dataset.conv;

import java.io.IOException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.spi.CoordSystemBuilderFactory;
import ucar.nc2.internal.dataset.CoordSystemBuilder;
import ucar.nc2.internal.dataset.CoordSystemFactory;
import ucar.nc2.internal.ncml.NcMLReaderNew;
import ucar.nc2.util.CancelTask;

public class CEDRICRadarConvention extends CF1Convention {
  private static final String CONVENTION_NAME = "CEDRICRadar";

  public static class Factory implements CoordSystemBuilderFactory {
    @Override
    public String getConventionName() {
      return CONVENTION_NAME;
    }

    @Override
    public boolean isMine(NetcdfFile ncfile) {
      Dimension s = ncfile.findDimension("cedric_general_scaling_factor");
      Variable v = ncfile.findVariable("cedric_run_date");
      return v != null && s != null;
    }

    @Override
    public CoordSystemBuilder open(NetcdfDataset.Builder datasetBuilder) {
      return new CEDRICRadarConvention(datasetBuilder);
    }
  }

  CEDRICRadarConvention(NetcdfDataset.Builder datasetBuilder) {
    super(datasetBuilder);
    this.conventionName = CONVENTION_NAME;
  }

  @Override
  protected void augmentDataset(CancelTask cancelTask) throws IOException {
    NcMLReaderNew.wrapNcMLresource(datasetBuilder, CoordSystemFactory.resourcesDir + "CEDRICRadar.ncml", cancelTask);

    VariableDS.Builder lat = (VariableDS.Builder) rootGroup.findVariable("radar_latitude")
        .orElseThrow(() -> new IllegalStateException("Must have radar_latitude variable"));
    VariableDS.Builder lon = (VariableDS.Builder) rootGroup.findVariable("radar_longitude")
        .orElseThrow(() -> new IllegalStateException("Must have radar_longitude variable"));
    float latv = (float) lat.orgVar.readScalarDouble();
    float lonv = (float) lon.orgVar.readScalarDouble();

    VariableDS.Builder pv = (VariableDS.Builder) rootGroup.findVariable("Projection")
        .orElseThrow(() -> new IllegalStateException("Must have Projection variable"));
    pv.addAttribute(new Attribute("longitude_of_projection_origin", lonv));
    pv.addAttribute(new Attribute("latitude_of_projection_origin", latv));

    super.augmentDataset(cancelTask);
  }

}


