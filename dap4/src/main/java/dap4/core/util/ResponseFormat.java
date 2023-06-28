/*
 * Copyright 2012, UCAR/Unidata.
 * See the LICENSE file for more information.
 */


package dap4.core.util;

/**
 * Define the enum for the possible Response/Response modes
 */
public enum ResponseFormat {
  TEXT("text", "txt"), XML("xml", "xml"), HTML("html", "html"), NONE("none", null);

  private String id;
  private String format;

  ResponseFormat(String id, String format) {
    this.id = id;
    this.format = format;
  }

  public String id() {
    return id;
  }

  public String format() {
    return format;
  }

  public static ResponseFormat formatFor(String s) {
    if (s == null)
      return null;
    for (ResponseFormat format : ResponseFormat.values()) {
      if (s.equalsIgnoreCase(format.format) || s.equalsIgnoreCase("." + format.format))
        return format;
    }
    return null;
  }

  public static ResponseFormat idFormat(String id) {
    if (id == null)
      return null;
    for (ResponseFormat format : ResponseFormat.values()) {
      if (id.equalsIgnoreCase(format.id))
        return format;
    }
    return null;
  }

  public String toString() {
    return id;
  }
}


