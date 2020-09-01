/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.server.opendap.servlet;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Aug 25, 2003
 * Time: 11:13:10 AM
 * To change this template use Options | File Templates.
 */
public class BadURLException extends opendap.dap.DAP2Exception {
  public BadURLException(String msg) {
    super(msg);
  }

  public BadURLException() {
    super("");
  }
}
