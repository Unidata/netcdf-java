package ucar.cdmr.client;

import thredds.client.catalog.ServiceType;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.spi.GridDatasetProvider;
import ucar.nc2.grid.GridDataset;
import ucar.nc2.util.CancelTask;

public class CdmrGridDatasetProvider implements GridDatasetProvider {
  @Override
  public String getProtocol() {
    return "cdmr";
  }

  @Override
  public boolean isOwnerOf(DatasetUrl url) {
    return url.getServiceType() == ServiceType.Cdmr;
  }

  @Override
  public GridDataset open(String location, CancelTask cancelTask) {
    return CdmrGridDataset.builder().setRemoteURI(location).build();
  }

}
