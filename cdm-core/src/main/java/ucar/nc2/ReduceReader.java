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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A ProxyReader that allows dimensions of length 1 to be removed.
 * 
 * {@link Variable#reduce(List)}
 */
@Immutable
class ReduceReader implements ProxyReader {
  private final Variable orgClient;
  private final List<Integer> dims; // The dimension indexes we want to reduce

  /**
   * Reduce 1 or more dimension of length 1
   * 
   * @param orgClient original variable
   * @param dims index(es) in original variable of the dimension to reduce; dimension must be length 1.
   */
  ReduceReader(Variable orgClient, List<Integer> dims) {
    int[] orgShape = orgClient.getShape();
    for (int reduceDim : dims) {
      Preconditions.checkArgument(orgShape[reduceDim] == 1);
    }
    this.orgClient = orgClient;
    this.dims = new ArrayList<>(dims);
    Collections.sort(this.dims);
  }

  @Override
  public Array<?> proxyReadArray(Variable client, CancelTask cancelTask) throws IOException {
    Array<?> data = orgClient._read();
    for (int i = dims.size() - 1; i >= 0; i--) {
      data = Arrays.reduce(data, dims.get(i)); // highest first
    }
    return data;
  }

  @Override
  public Array<?> proxyReadArray(Variable client, Section section, CancelTask cancelTask)
      throws IOException, InvalidRangeException {
    Section.Builder orgSection = Section.builder().appendRanges(section.getRanges());
    for (int dim : dims) {
      orgSection.insertRange(dim, Range.SCALAR); // lowest first
    }

    Array<?> data = orgClient._read(orgSection.build());
    for (int i = dims.size() - 1; i >= 0; i--) {
      data = Arrays.reduce(data, dims.get(i)); // highest first
    }

    return data;
  }
}
