/*
 * Copyright (c) 2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp.zarr;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * To be implemented
 */
public abstract class ZarrFilter {

  public abstract String getId();

  public abstract void compress(InputStream is, OutputStream os) throws IOException;
}
