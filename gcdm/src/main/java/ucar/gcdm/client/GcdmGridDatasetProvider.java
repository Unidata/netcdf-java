package ucar.gcdm.client;

import thredds.client.catalog.ServiceType;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.spi.GridDatasetProvider;
import ucar.nc2.grid.GridDataset;
import ucar.nc2.util.CancelTask;

public class GcdmGridDatasetProvider implements GridDatasetProvider {
  @Override
  public String getProtocol() {
    return "gcdm";
  }

  @Override
  public boolean isOwnerOf(DatasetUrl url) {
    return url.getServiceType() == ServiceType.GCDM;
  }

  @Override
  public GridDataset open(String location, CancelTask cancelTask) {
    return GcdmGridDataset.builder().setRemoteURI(location).build();
  }

}
