/*
 * Copyright (c) 2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp.zarr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.util.CancelTask;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.io.zarr.RandomAccessDirectory;

import java.io.IOException;

/**
 * IOSP for reading/writing Zarr/NCZarr formats
 */
public class ZarrIosp extends AbstractIOServiceProvider {

  private static final Logger logger = LoggerFactory.getLogger(ZarrIosp.class);

  private static final String fileTypeId = "Zarr";
  private static final String fileTypeDescription = "Zarr v2 formatted dataset";

  private ZarrHeader header;

  @Override
  public boolean isValidFile(RandomAccessFile raf) {
    return raf.isDirectory();
  }

  @Override
  public String getFileTypeId() {
    return fileTypeId;
  }

  @Override
  public String getFileTypeDescription() {
    return fileTypeDescription;
  }

  @Override
  public boolean isBuilder() {
    return true;
  }

  @Override
  public void build(RandomAccessFile raf, Group.Builder rootGroup, CancelTask cancelTask) throws IOException {
    super.open(raf, null, cancelTask);
    header = new ZarrHeader((RandomAccessDirectory) raf, rootGroup);
    header.read(); // build CDM from Zarr
  }

  @Override
  public void buildFinish(NetcdfFile ncfile) {} // NO-OP

  // TODO: to be implemented
  @Override
  public Array readData(Variable v2, Section section) throws IOException, InvalidRangeException {
    return null;
  }
}
