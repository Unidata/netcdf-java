package ucar.nc2.internal.iosp.hdf4;

import java.io.IOException;
import ucar.nc2.Group;
import ucar.nc2.Variable;

/** Interface for HDF5 and HDF4 headers, needed by HdfEos. */
public interface HdfHeaderIF {

  Group.Builder getRootGroup();

  void makeVinfoForDimensionMapVariable(Group.Builder parent, Variable.Builder<?> v);

  String readStructMetadata(Variable.Builder<?> structMetadataVar) throws IOException;

}
