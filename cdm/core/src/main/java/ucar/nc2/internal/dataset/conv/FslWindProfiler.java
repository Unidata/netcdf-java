/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.dataset.conv;

import java.io.IOException;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.spi.CoordSystemBuilderFactory;
import ucar.nc2.internal.dataset.CoordSystemBuilder;
import ucar.nc2.util.CancelTask;

/** FslWindProfiler netcdf files - identify coordinates */
public class FslWindProfiler extends CoordSystemBuilder {
  private static final String CONVENTION_NAME = "FslWindProfiler";

  public static class Factory implements CoordSystemBuilderFactory {
    @Override
    public String getConventionName() {
      return CONVENTION_NAME;
    }

    @Override
    public boolean isMine(NetcdfFile ncfile) {
      String title = ncfile.getRootGroup().attributes().findAttributeString("title", null);
      return title != null && (title.startsWith("WPDN data"));
    }

    @Override
    public CoordSystemBuilder open(NetcdfDataset.Builder datasetBuilder) {
      return new FslWindProfiler(datasetBuilder);
    }
  }

  private FslWindProfiler(NetcdfDataset.Builder datasetBuilder) {
    super(datasetBuilder);
    this.conventionName = CONVENTION_NAME;
  }

  @Override
  public void augmentDataset(CancelTask cancelTask) throws IOException {
    for (Variable.Builder v : rootGroup.vbuilders) {
      switch (v.shortName) {
        case "staName":
          v.addAttribute(new Attribute("standard_name", "station_name"));
          break;
        case "staLat":
          v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));
          break;
        case "staLon":
          v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));
          break;
        case "staElev":
        case "levels":
          v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Height.toString()));
          break;
        case "timeObs":
          v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));
          break;
      }
    }
  }

}
