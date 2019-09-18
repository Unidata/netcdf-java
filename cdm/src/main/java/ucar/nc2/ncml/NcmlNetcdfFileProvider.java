package ucar.nc2.ncml;

import java.io.IOException;
import thredds.client.catalog.ServiceType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfFileProvider;
import ucar.nc2.util.CancelTask;

public class NcmlNetcdfFileProvider implements NetcdfFileProvider {

  @Override
  public boolean isOwnerOf(DatasetUrl url) {
    return url.serviceType == ServiceType.NCML;
  }

  @Override
  public NetcdfFile open(DatasetUrl url, CancelTask cancelTask) throws IOException {
    return NcMLReader.readNcML(url.trueurl, cancelTask);
  }
}
