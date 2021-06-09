package ucar.nc2.grib.collection;

import ucar.nc2.grib.GribTables;
import ucar.nc2.grid.GridSubset;
import ucar.unidata.io.RandomAccessFile;
import java.io.IOException;

/** internal class for debugging. */
public interface GribDataValidator {
  void validate(GribTables cust, RandomAccessFile rafData, long pos, GridSubset coords) throws IOException;
}
