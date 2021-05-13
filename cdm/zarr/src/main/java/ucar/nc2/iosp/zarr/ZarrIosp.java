/*
 * Copyright (c) 2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp.zarr;

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
 * Stubbed Zarr Iosp with only isValidFile implemented
 * Other methods under construction
 */
public class ZarrIosp extends AbstractIOServiceProvider {
  private static final String fileTypeId = "Zarr";
  private static final String fileTypeDescription = "Zarr dataset";

  private RandomAccessFile raf;

  // TODO
  @Override
  public boolean isValidFile(RandomAccessFile raf) {
    return raf.isDirectory();
  }

  // TODO
  @Override
  public Array readData(Variable v2, Section section) throws IOException, InvalidRangeException {
    return null;
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

  // TODO
  @Override
  public void build(RandomAccessFile raf, Group.Builder rootGroup, CancelTask cancelTask) throws IOException {
    this.raf = raf;
  }

  @Override
  public void buildFinish(NetcdfFile ncfile) {}
}
