package ucar.gcdm.client;

import thredds.client.catalog.ServiceType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.spi.NetcdfFileProvider;
import ucar.nc2.util.CancelTask;

public class GcdmNetcdfFileProvider implements NetcdfFileProvider {
  @Override
  public String getProtocol() {
    return "cdmr";
  }

  @Override
  public boolean isOwnerOf(DatasetUrl url) {
    return url.getServiceType() == ServiceType.Cdmr;
  }

  @Override
  public NetcdfFile open(String location, CancelTask cancelTask) {
    return GcdmNetcdfFile.builder().setRemoteURI(location).build();
  }

}
