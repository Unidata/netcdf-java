/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */



package thredds.server.opendap.servlet.www;

import java.io.PrintWriter;
import opendap.dap.DAS;


/**
 */
public interface BrowserForm {


  /**
   * Returns an string representation of the variables value. This
   * is really foreshadowing functionality for Server types, but
   * as it may come in useful for clients it is added here. Simple
   * types (example: DFloat32) will return a single value. DConstuctor
   * and DVector types will be flattened. DStrings and DURL's will
   * have double quotes around them.
   */
  public void printBrowserForm(PrintWriter pw, DAS das);


}


