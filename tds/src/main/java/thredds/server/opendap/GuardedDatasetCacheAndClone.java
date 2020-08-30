/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.opendap;

import java.io.IOException;
import javax.annotation.concurrent.Immutable;
import thredds.server.opendap.servlet.GuardedDataset;
import thredds.server.opendap.servers.ServerDDS;
import ucar.nc2.NetcdfFile;

/**
 * This creates and caches DDS, DAS, then clones them when they are needed.
 */
@Immutable
public class GuardedDatasetCacheAndClone implements GuardedDataset {
  static protected org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GuardedDataset.class);

  private final boolean hasSession;
  private final NetcdfFile org_file;
  private final NcDDS dds;
  private final NcDAS das;

  public void release() {
    if (!hasSession)
      close();
  }

  public void close() {
    try {
      org_file.close();
    } catch (IOException e) {
      log.error("GuardedDatasetImpl close", e);
    }
  }

  public GuardedDatasetCacheAndClone(String reqPath, NetcdfFile ncfile, boolean hasSession) {
    this.org_file = ncfile;
    this.dds = new NcDDS(reqPath, ncfile);
    this.das = new NcDAS(ncfile);
    this.hasSession = hasSession;
  }

  public ServerDDS getDDS() {
    return (ServerDDS) dds.clone();
  }

  public opendap.dap.DAS getDAS() {
    return (opendap.dap.DAS) das.clone();
  }

  public String toString() {
    String name = org_file.getLocation();
    return name == null ? "UNKNOWN" : name;
  }
}
