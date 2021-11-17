/*
 * Copyright (c) 1998-2017 John Caron and University Corporation for Atmospheric Research/Unidata
 */
package ucar.nc2.internal.ncml;

import java.io.IOException;
import java.util.List;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.util.CancelTask;

/**
 * Aggregation on datasets to be simply combined - aka "union".
 * The variables are transferred from the component files to the ncml dataset
 */
class AggregationUnion extends Aggregation {
  AggregationUnion(NetcdfDataset.Builder<?> ncd, String dimName, String recheckS) {
    super(ncd, dimName, Type.union, recheckS);
  }

  @Override
  protected void buildNetcdfDataset(CancelTask cancelTask) throws IOException {
    // each Dataset just gets "transfered" into the resulting NetcdfDataset
    List<AggDataset> nestedDatasets = getDatasets();
    for (AggDataset vnested : nestedDatasets) {
      NetcdfFile ncfile = vnested.acquireFile(cancelTask);
      BuilderHelper.transferDataset(ncfile, ncDataset, null);

      setDatasetAcquireProxy(vnested, ncDataset);
      vnested.close(ncfile); // close it because we use AggProxyReader to acquire
    }
  }

}
