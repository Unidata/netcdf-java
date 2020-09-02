/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.server.opendap.servers;

import java.io.PrintWriter;
import java.util.ArrayList;
import opendap.dap.BaseType;

/**
 * Represents a sub-clause that is a URL reference to remote data.
 * This feature is not yet supported in Java. Thus this class
 * throws an exception in its constructor.
 *
 * @author joew
 * @see ClauseFactory
 */
public class DereferenceClause extends AbstractClause implements SubClause {

  /**
   * Creates a new DereferenceClause
   */
  protected DereferenceClause(String url) throws DAP2ServerSideException {
    this.url = url;
    this.constant = true;
    this.defined = true;
    this.value = retrieve(url);
    this.children = new ArrayList();
  }

  public BaseType getValue() {
    return value;
  }

  public BaseType evaluate() {
    return value;
  }

  public Clause getParent() {
    return parent;
  }

  public void setParent(Clause parent) {
    this.parent = parent;
  }

  public String getURL() {
    return url;
  }

  protected BaseType retrieve(String url) throws DAP2ServerSideException {

    throw new DAP2ServerSideException(opendap.dap.DAP2Exception.UNKNOWN_ERROR, "dereferencing not supported");
  }

  public void printConstraint(PrintWriter os) {
    os.print("*\"" + url + "\"");
  }

  protected String url;
  protected Clause parent;
  protected BaseType value;
}


