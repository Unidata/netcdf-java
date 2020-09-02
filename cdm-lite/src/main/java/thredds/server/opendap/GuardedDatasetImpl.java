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
 * Regenerate DDS, DAS each time
 */
@Immutable
public class GuardedDatasetImpl implements GuardedDataset {
  static protected org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GuardedDataset.class);

  private final boolean hasSession;
  private final NetcdfFile org_file;
  private final String reqPath;
  private boolean closed = false;

  public void release() {
    if (!hasSession)
      close();
    closed = true;
  }

  public void close() {
    try {
      org_file.close();
    } catch (IOException e) {
      log.error("GuardedDatasetImpl close", e);
    }
  }

  public GuardedDatasetImpl(String reqPath, NetcdfFile ncfile, boolean hasSession) {
    this.org_file = ncfile;
    this.reqPath = reqPath;
    this.hasSession = hasSession;
  }

  public ServerDDS getDDS() {
    if (closed)
      throw new IllegalStateException("getDDS(): " + this + " closed");
    return new NcDDS(reqPath, org_file);
  }

  public opendap.dap.DAS getDAS() {
    if (closed)
      throw new IllegalStateException("getDAS(): " + this + " closed");
    return new NcDAS(org_file);
  }

  public String toString() {
    String name = org_file.getLocation();
    return name == null ? "UNKNOWN" : name;
  }
}
