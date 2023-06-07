/*
 * Copyright 2012, UCAR/Unidata.
 * See the LICENSE file for more information.
 */


package dap4.dap4lib;

/**
 * Define the enum for the possible Request/Response modes
 */
public enum RequestMode {
  DMR("dmr", "dmr"), DAP("dap", "dap"), DSR("dsr", "dsr"), CAPABILITIES("capabilities", ""), ERROR("error",
      null), NONE("none", null);

  private String id;
  private String extension;

  RequestMode(String id, String extension) {
    this.id = id;
    this.extension = extension;
  }

  public String id() {
    return id;
  }

  public String extension() {
    return extension;
  }

  public static RequestMode modeFor(String s) {
    for (RequestMode mode : RequestMode.values()) {
      if (mode.extension() != null && s.equalsIgnoreCase(mode.extension) || s.equalsIgnoreCase("." + mode.extension))
        return mode;
    }
    return null;
  }

  public static RequestMode idMode(String id) {
    for (RequestMode mode : RequestMode.values()) {
      if (mode.id() != null && id.equalsIgnoreCase(mode.id))
        return mode;
    }
    return null;
  }

  public String toString() {
    return id;
  }
}


