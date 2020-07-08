/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.ncml;

import java.io.IOException;
import thredds.client.catalog.ServiceType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.spi.NetcdfFileProvider;
import ucar.nc2.util.CancelTask;

public class NcmlNetcdfFileProvider implements NetcdfFileProvider {

  @Override
  public String getProtocol() {
    return "ncml";
  }

  @Override
  public NetcdfFile open(String location, CancelTask cancelTask) throws IOException {
    return NcmlReader.readNcml(location, null, cancelTask).build();
  }

  @Override
  public boolean isOwnerOf(String location) {
    return location.startsWith("ncml:") || location.endsWith(".ncml");
  }

  @Override
  public boolean isOwnerOf(DatasetUrl url) {
    return url.getServiceType() == ServiceType.NCML;
  }

}
