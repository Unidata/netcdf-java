/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import javax.annotation.concurrent.Immutable;
import ucar.ma2.*;
import ucar.nc2.util.CancelTask;
import java.io.IOException;

/**
 * A ProxyReader for slices.
 * 
 * @see {@link Variable#slice(int, int)}
 */
@Immutable
class SliceReader implements ProxyReader {
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SliceReader.class);

  private final Variable orgClient;
  private final Group parentGroup;
  private final String orgName;
  private final int sliceDim; // dimension index into original
  private final Section slice; // section of the original

  // LOOK could do check that slice is compatible with client
  SliceReader(Variable orgClient, int dim, Section slice) {
    this.orgClient = orgClient;
    this.sliceDim = dim;
    this.slice = slice;

    this.orgName = orgClient.getShortName();
    this.parentGroup = orgClient.getParentGroup();
  }

  // This is used from Builder when we dont yet have all variables built.
  SliceReader(Group parentGroup, String orgName, int dim, Section slice) {
    this.parentGroup = parentGroup;
    this.orgName = orgName;
    this.sliceDim = dim;
    this.slice = slice;

    this.orgClient = null;
  }

  @Override
  public Array reallyRead(Variable client, CancelTask cancelTask) throws IOException {
    Variable orgClient = this.orgClient != null ? this.orgClient : parentGroup.findVariableLocal(orgName);
    Array data;
    try {
      data = orgClient._read(slice);
    } catch (InvalidRangeException e) {
      log.error("InvalidRangeException in slice, var=" + client);
      throw new IllegalStateException(e.getMessage());
    }
    data = data.reduce(sliceDim);
    return data;
  }

  @Override
  public Array reallyRead(Variable client, Section section, CancelTask cancelTask)
      throws IOException, InvalidRangeException {
    Variable orgClient = parentGroup.findVariableLocal(orgName);
    Section.Builder orgSection = Section.builder().appendRanges(section.getRanges());
    orgSection.insertRange(sliceDim, slice.getRange(sliceDim));
    Array data = orgClient._read(orgSection.build());
    data = data.reduce(sliceDim);
    return data;
  }

}
