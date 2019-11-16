package thredds.client.catalog.tools;

import java.io.IOException;
import java.util.Formatter;
import thredds.client.catalog.ServiceType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.spi.NetcdfFileProvider;
import ucar.nc2.util.CancelTask;

public class CatalogNetcdfFileProvider implements NetcdfFileProvider {

  @Override
  public String getProtocol() {
    return "thredds";
  }

  @Override
  public boolean isOwnerOf(DatasetUrl url) {
    return url.serviceType == ServiceType.THREDDS;
  }

  @Override
  public NetcdfFile open(String location, CancelTask cancelTask) throws IOException {
    Formatter log = new Formatter();
    DataFactory tdf = new DataFactory();
    NetcdfFile ncfile = tdf.openDataset(location, false, cancelTask, log);
    if (ncfile == null)
      throw new IOException(log.toString());
    return ncfile;
  }
}
