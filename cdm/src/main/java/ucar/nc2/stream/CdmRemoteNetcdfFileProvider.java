package ucar.nc2.stream;

import java.io.IOException;
import thredds.client.catalog.ServiceType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfFileProvider;
import ucar.nc2.util.CancelTask;

public class CdmRemoteNetcdfFileProvider implements NetcdfFileProvider {

  @Override
  public String getProtocol() {
    return "cdmremote";
  }

  @Override
  public boolean isOwnerOf(DatasetUrl url) {
    return url.serviceType == ServiceType.CdmRemote;
  }

  @Override
  public NetcdfFile open(String location, CancelTask cancelTask) throws IOException {
    return new CdmRemote(location);
  }

}
