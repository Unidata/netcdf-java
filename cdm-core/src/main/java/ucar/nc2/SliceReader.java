/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.Preconditions;
import ucar.array.Array;
import ucar.array.Arrays;
import ucar.array.InvalidRangeException;
import ucar.array.Range;
import ucar.array.Section;
import ucar.nc2.util.CancelTask;
import java.io.IOException;

/**
 * A ProxyReader for slices.
 * 
 * {@link Variable#slice(int, int)}
 */
@Immutable
class SliceReader implements ProxyReader {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SliceReader.class);

  private final Variable orgClient;
  private final Group parentGroup;
  private final String orgName;
  private final int sliceDim; // dimension index into original
  private final Section slice; // section of the original

  /**
   * Reads slice of orgClient
   * @param dim slice this dimension
   * @param slice at this value
   */
  SliceReader(Variable orgClient, int dim, Section slice) {
    int[] orgShape = orgClient.getShape();
    Preconditions.checkArgument(dim >= 0 && dim < orgShape.length);
    Preconditions.checkArgument(slice.getRank() == orgShape.length);
    for (int i=0; i<slice.getRank(); i++) {
      Range r = slice.getRange(i);
      if (i == dim) {
        Preconditions.checkArgument(r.length() == 1);
        Preconditions.checkArgument(r.first() < orgShape[i]);
      } else {
        Preconditions.checkArgument(r.length() == orgShape[i]);
        Preconditions.checkArgument(r.first() == 0);
      }
    }
    this.orgClient = orgClient;
    this.sliceDim = dim;
    this.slice = slice;

    this.orgName = orgClient.getShortName();
    this.parentGroup = orgClient.getParentGroup();
  }

  // This is used from Builder when we dont yet have the variables built.
  SliceReader(Group parentGroup, String orgName, int dim, Section slice) {
    this.parentGroup = parentGroup;
    this.orgName = orgName;
    this.sliceDim = dim;
    this.slice = slice;

    this.orgClient = null;
  }

  @Override
  public Array<?> proxyReadArray(Variable client, CancelTask cancelTask) throws IOException {
    Variable orgClient = this.orgClient != null ? this.orgClient : parentGroup.findVariableLocal(orgName);
    Preconditions.checkNotNull(orgClient);
    try {
      Array<?> data = orgClient._read(slice);
      data = Arrays.reduce(data, sliceDim);
      return data;
    } catch (InvalidRangeException e) {
      log.error("InvalidRangeException in slice, var=" + client);
      throw new IllegalStateException(e.getMessage());
    }
  }

  @Override
  public Array<?> proxyReadArray(Variable client, Section section, CancelTask cancelTask)
      throws IOException, InvalidRangeException {
    Variable orgClient = parentGroup.findVariableLocal(orgName);
    Preconditions.checkNotNull(orgClient);
    Section.Builder orgSection = Section.builder().appendRanges(section.getRanges());
    orgSection.insertRange(sliceDim, slice.getRange(sliceDim));
    Array<?> data = orgClient._read(orgSection.build());
    data = Arrays.reduce(data, sliceDim);
    return data;
  }

}
