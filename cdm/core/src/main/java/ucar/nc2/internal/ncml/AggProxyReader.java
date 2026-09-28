/*
 * Copyright (c) 2019-2025 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE.txt for license information.
 */

package ucar.nc2.internal.ncml;

import java.io.IOException;
import javax.annotation.concurrent.Immutable;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.NetcdfFile;
import ucar.nc2.ProxyReader;
import ucar.nc2.Variable;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.VariableEnhanced;
import ucar.nc2.util.CancelTask;

/**
 * A Proxy reader for a specific Variable.
 *
 * @author caron
 * @since 10/3/2019.
 */
@Immutable
public class AggProxyReader implements ProxyReader {
  private final AggDataset dataset;

  AggProxyReader(AggDataset dataset) {
    this.dataset = dataset;
  }

  @Override
  public Array reallyRead(Variable mainV, CancelTask cancelTask) throws IOException {
    NetcdfFile ncfile = null;
    try {
      ncfile = dataset.acquireFile(cancelTask);
      if ((cancelTask != null) && cancelTask.isCancel())
        return null;
      Variable proxyV = readTarget(ncfile, mainV);
      return proxyV.read();
    } finally {
      dataset.close(ncfile);
    }
  }

  @Override
  public Array reallyRead(Variable mainV, Section section, CancelTask cancelTask)
      throws IOException, InvalidRangeException {
    NetcdfFile ncfile = null;
    try {
      ncfile = dataset.acquireFile(cancelTask);
      Variable proxyV = readTarget(ncfile, mainV);
      if ((cancelTask != null) && cancelTask.isCancel())
        return null;
      return proxyV.read(section);

    } finally {
      dataset.close(ncfile);
    }
  }


  /**
   * Find the Variable to read from the member dataset. If the member dataset was itself enhanced, and the
   * aggregation variable will apply the same enhancements again, read the data as stored instead.
   */
  private Variable readTarget(NetcdfFile ncfile, Variable mainV) {
    return VariableDS.unenhancedProxy(findVariable(ncfile, mainV), mainV);
  }

  protected Variable findVariable(NetcdfFile ncfile, Variable mainV) {
    Variable v = ncfile.findVariable(mainV.getFullNameEscaped());
    if (v == null) { // might be renamed
      VariableEnhanced ve = (VariableEnhanced) mainV;
      v = ncfile.findVariable(ve.getOriginalName()); // LOOK not escaped
    }
    return v;
  }
}
