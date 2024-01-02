/*
 * Copyright 2009, UCAR/Unidata and OPeNDAP, Inc.
 * See the LICENSE file for more information.
 */

/**
 * Wrap the logging functionality
 * (essentially org.slf4j.Logger)
 * so we can replace it if needed.
 * Currently wraps org.slf4j.Logger
 */

package dap4.dap4lib;

public class DapLog {
  //////////////////////////////////////////////////
  // Static variables

  private static org.slf4j.Logger log = null;

  private static synchronized void getLog() {
    if (log == null)
      log = org.slf4j.LoggerFactory.getLogger("dap4");
  }

  public static synchronized void error(String s) {
    if (log == null)
      getLog();
    log.error(s);
  }

  public static synchronized void warn(String s) {
    if (log == null)
      getLog();
    log.warn(s);
  }

  public static synchronized void info(String s) {
    if (log == null)
      getLog();
    log.info(s);
  }

  public static synchronized void debug(String s) {
    if (log == null)
      getLog();
    log.debug(s);
  }
}
