package ucar.nc2.ncml;

import java.io.IOException;
import thredds.client.catalog.ServiceType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfFileProvider;
import ucar.nc2.util.CancelTask;

public class NcmlNetcdfFileProvider implements NetcdfFileProvider {

  @Override
  public String getProtocol() {
    return "ncml";
  }

  @Override
  public NetcdfFile open(String location, CancelTask cancelTask) throws IOException {
    return NcMLReader.readNcML(location, cancelTask);
  }

  @Override
  public boolean isOwnerOf(String location) {
    return location.startsWith("ncml:") || location.endsWith(".ncml");
  }

  @Override
  public boolean isOwnerOf(DatasetUrl url) {
    return url.serviceType == ServiceType.NCML;
  }

}
