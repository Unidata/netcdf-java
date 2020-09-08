package ucar.cdmr.client;

import thredds.client.catalog.ServiceType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.spi.NetcdfFileProvider;
import ucar.nc2.util.CancelTask;

public class CdmrNetcdfFileProvider implements NetcdfFileProvider {
  // LOOK do we need to keep the old cdmremote? probably
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
    return CdmrNetcdfFile.builder().setRemoteURI(location).build();
  }

}
