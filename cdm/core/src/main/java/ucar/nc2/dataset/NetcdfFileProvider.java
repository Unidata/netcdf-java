package ucar.nc2.dataset;

import java.io.IOException;
import ucar.nc2.NetcdfFile;

/** A Service Provider of NetcdfFile. */
public interface NetcdfFileProvider {

  /** The leading protocol string (without a trailing ":"). */
  String getProtocol();

  /** Determine if this Provider owns this DatasetUrl. */
  boolean isOwnerOf(DatasetUrl durl);

  /** Determine if this Provider owns this location. */
  default boolean isOwnerOf(String location) {
    return location.startsWith(getProtocol() + ":");
  }

  /** Open a location that this Provider is the owner of. */
  NetcdfFile open(String location, ucar.nc2.util.CancelTask cancelTask) throws IOException;
}
