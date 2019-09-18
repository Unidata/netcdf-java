package ucar.nc2.dods;

import java.io.IOException;
import thredds.client.catalog.ServiceType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfFileProvider;
import ucar.nc2.util.CancelTask;

public class DODSNetcdfFileProvider implements NetcdfFileProvider {

  @Override
  public boolean isOwnerOf(DatasetUrl url) {
    return url.serviceType == ServiceType.OPENDAP;
  }

  @Override
  public NetcdfFile open(DatasetUrl url, CancelTask cancelTask) throws IOException {
    return new DODSNetcdfFile(url.trueurl, cancelTask);
  }
}
