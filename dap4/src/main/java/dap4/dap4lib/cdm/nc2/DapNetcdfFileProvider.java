package dap4.dap4lib.cdm.nc2;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.client.catalog.ServiceType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.spi.NetcdfFileProvider;
import ucar.nc2.util.CancelTask;

public class DapNetcdfFileProvider implements NetcdfFileProvider {
  private static final Logger logger = LoggerFactory.getLogger(DapNetcdfFileProvider.class);

  @Override
  public String getProtocol() {
    return "dap4";
  }

  @Override
  public boolean isOwnerOf(DatasetUrl url) {
    return url.serviceType == ServiceType.DAP4;
  }

  @Override
  public NetcdfFile open(String location, CancelTask cancelTask) throws IOException {
    return new DapNetcdfFile(location, cancelTask);
  }

}

