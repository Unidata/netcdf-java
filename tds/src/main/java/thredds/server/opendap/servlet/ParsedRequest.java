/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */


package thredds.server.opendap.servlet;


/**
 * This holds the parsed information from one particular user request.
 * We neeed this to maintain thread-safety.
 * This Object is immutable, except for the "user object".
 *
 * @author jcaron
 */

public class ParsedRequest {
  private String dataSet;
  private String requestSuffix;
  private String CE;
  private boolean acceptsCompressed;
  private Object obj = null;

  public ParsedRequest(String dataSet, String requestSuffix, String CE, boolean acceptsCompressed) {
    // this.dataSet = (dataSet == null) ? "" : dataSet;
    // this.requestSuffix = (requestSuffix == null) ? "" : requestSuffix;
    this.dataSet = dataSet;
    this.requestSuffix = requestSuffix;
    this.CE = CE;
    this.acceptsCompressed = acceptsCompressed;
  }

  public String getDataSet() {
    return dataSet;
  }

  public String getRequestSuffix() {
    return requestSuffix;
  }

  public String getConstraintExpression() {
    return CE;
  }

  public boolean getAcceptsCompressed() {
    return acceptsCompressed;
  }

  // for debugging, extra state, etc
  public Object getUserObject() {
    return obj;
  }

  public void setUserObject(Object userObj) {
    this.obj = userObj;
  }

  public String toString() {
    return " dataset: " + dataSet + " suffix: " + requestSuffix + " CE: '" + CE + "' compressOK: " + acceptsCompressed;
  }


}


