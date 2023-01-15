/*
 * Copyright 2012, UCAR/Unidata.
 * See the LICENSE file for more information.
 */

package dap4.core.dmr.parser;

import dap4.core.dmr.*;
import org.xml.sax.SAXException;

public interface Dap4Parser {
  public ErrorResponse getErrorResponse();

  public DapDataset getDMR();

  public boolean parse(String input) throws SAXException;

  public void setDebugLevel(int level);
}
