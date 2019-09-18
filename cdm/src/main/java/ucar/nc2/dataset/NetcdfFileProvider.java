package ucar.nc2.dataset;

import java.io.IOException;
import ucar.nc2.NetcdfFile;

public interface NetcdfFileProvider {

  boolean isOwnerOf(DatasetUrl location);

  NetcdfFile open(DatasetUrl location, ucar.nc2.util.CancelTask cancelTask) throws IOException;

}
