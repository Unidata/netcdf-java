/*
 * Copyright 2012, UCAR/Unidata.
 * See the LICENSE file for more information.
 */

package dap4.dap4lib;

import dap4.core.dmr.parser.DOM4Parser;
import dap4.core.dmr.parser.Dap4Parser;
import dap4.core.util.*;

import java.io.*;
import java.net.URISyntaxException;

/**
 * Provide a DSP interface to raw data
 */

public class RawDSP extends D4DSP {
  //////////////////////////////////////////////////
  // Constants

  protected static final String[] EXTENSIONS = new String[] {".dap", ".raw"};

  //////////////////////////////////////////////////
  // Instance variables

  //////////////////////////////////////////////////
  // Constructor(s)

  public RawDSP() {}

  //////////////////////////////////////////////////
  // D4DSP API

  // Note that there is no point in delaying the compilation of the
  // DMR and DAP since we are reading the whole DAP anyway
  @Override
  public D4DSP open(String fileurl, ChecksumMode cmode) throws DapException {
    mode = RequestMode.DAP; // force it
    super.open(fileurl, cmode);
    String methodurl = getMethodUrl(mode, this.checksummode);
    parseURL(methodurl); // reparse
    String realpath = this.xuri.getRealPath();
    try {
      FileInputStream stream = new FileInputStream(realpath);
      setStream(stream, RequestMode.DAP);
    } catch (IOException ioe) {
      throw new DapException(ioe).setCode(DapCodes.SC_INTERNAL_SERVER_ERROR);
    }
    return this;
  }

  /**
   * A path is file if it has no base protocol or is file:
   *
   * @param location file:/ or possibly an absolute path
   * @param context Any parameters that may help to decide.
   * @return true if this path appears to be processible by this DSP
   */
  public boolean dspMatch(String location, DapContext context) {
    try {
      XURI xuri = new XURI(location);
      if (xuri.isFile()) {
        String path = xuri.getPath();
        for (String ext : EXTENSIONS) {
          if (path.endsWith(ext))
            return true;
        }
      }
    } catch (URISyntaxException use) {
      return false;
    }
    return false;
  }

  //////////////////////////////////////////////////
  // Load Operations

  /**
   * LoadDMR actually loads DAP since we know that we will need that eventually.
   * 
   * @throws DapException
   */

  public void loadDMR() throws DapException {
    String methodurl = getMethodUrl(mode, this.checksummode);
    parseURL(methodurl); // reparse
    String realpath = this.xuri.getRealPath();
    try {
      FileInputStream stream = new FileInputStream(realpath);
      setStream(stream, RequestMode.DAP);
      super.loadDMR();
    } catch (IOException ioe) {
      throw new DapException(ioe).setCode(DapCodes.SC_INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * loadDMR will have already loaded the DAP stream,
   * so all that is left is to compile the data stream,
   * 
   * @throws DapException
   */
  public void loadDAP() throws IOException {
    super.loadDAP();
  }

}
