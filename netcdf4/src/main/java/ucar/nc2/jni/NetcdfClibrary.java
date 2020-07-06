/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.jni;

import com.sun.jna.Native;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import ucar.nc2.jni.netcdf.Nc4prototypes;
import ucar.nc2.jni.netcdf.Nc4wrapper;

/** Static methods to load the netcdf C library. */
public class NetcdfClibrary {
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NetcdfClibrary.class);
  private static org.slf4j.Logger startupLog = org.slf4j.LoggerFactory.getLogger("serverStartup");

  private static final String JNA_PATH = "jna.library.path";
  private static final String JNA_PATH_ENV = "JNA_PATH"; // environment var
  private static final String JNA_LOG_LEVEL = "jna.library.loglevel";

  private static String DEFAULT_NETCDF4_LIBNAME = "netcdf";
  private static final boolean DEBUG = false;

  private static String jnaPath;
  private static String libName = DEFAULT_NETCDF4_LIBNAME;

  private static Nc4prototypes nc4;
  private static int log_level;

  /**
   * Set the path and name of the netcdf c library.
   * Must be called before load() is called.
   *
   * @param jna_path path to shared libraries
   * @param lib_name library name
   */
  public static void setLibraryAndPath(String jna_path, String lib_name) {
    lib_name = nullify(lib_name);

    if (lib_name == null) {
      lib_name = DEFAULT_NETCDF4_LIBNAME;
    }

    jna_path = nullify(jna_path);

    if (jna_path == null) {
      jna_path = nullify(System.getProperty(JNA_PATH)); // First, try system property (-D flag).
    }
    if (jna_path == null) {
      jna_path = nullify(System.getenv(JNA_PATH_ENV)); // Next, try environment variable.
    }

    if (jna_path != null) {
      System.setProperty(JNA_PATH, jna_path);
    }

    libName = lib_name;
    jnaPath = jna_path;
  }

  private static Nc4prototypes load() {
    if (nc4 == null) {
      if (jnaPath == null) {
        // netCDF-C will return strings encoded as UTF-8 (not default C behavior).
        // JNA assumes c code is returning using the system encoding by default
        // So, if the system encoding isn't UTF_8 (hello, windows!), set the jna
        // encoding to utf_8. LOOK: This might hose other application use JNA and
        // not expect their c code to return UTF-8
        if (!Charset.defaultCharset().equals(StandardCharsets.UTF_8)) {
          log.info("Setting System Property jna.encoding to UTF8");
          // System.setProperty("jna.encoding", StandardCharsets.UTF_8.name());
        }
        setLibraryAndPath(null, null);
      }
      try {
        // jna_path may still be null, but try to load anyway;
        // the necessary libs may be on the system PATH or on LD_LIBRARY_PATH
        nc4 = Native.load(libName, Nc4prototypes.class);
        // Make the library synchronized
        // nc4 = (Nc4prototypes) Native.synchronizedLibrary(nc4);
        nc4 = new Nc4wrapper(nc4);
        startupLog.info("Nc4Iosp: NetCDF-4 C library loaded (jna_path='{}', libname='{}').", jnaPath, libName);
        startupLog.debug("Netcdf nc_inq_libvers='{}' isProtected={}", nc4.nc_inq_libvers(), Native.isProtected());
      } catch (Throwable t) {
        String message =
            String.format("Nc4Iosp: NetCDF-4 C library not present (jna_path='%s', libname='%s').", jnaPath, libName);
        startupLog.warn(message, t);
      }
      String slevel = nullify(System.getProperty(JNA_LOG_LEVEL));
      if (slevel != null) {
        try {
          log_level = Integer.parseInt(slevel);
        } catch (NumberFormatException nfe) {
          // no change
        }
      }
      try {
        int oldlevel = setLogLevel(log_level);
        startupLog.info(String.format("Nc4Iosp: set log level: old=%d new=%d", oldlevel, log_level));
      } catch (Throwable t) {
        String message = String.format("Nc4Iosp: could not set log level (level=%d jna_path='%s', libname='%s').",
            log_level, jnaPath, libName);
        startupLog.warn("Nc4Iosp: " + t.getMessage());
        startupLog.warn(message);
      }
    }
    return nc4;
  }

  // Shared mutable state. Only read/written in isClibraryPresent().
  private static Boolean isClibraryPresent;

  /**
   * Test if the netcdf C library is present and loaded
   *
   * @return true if present
   */
  public static synchronized boolean isClibraryPresent() {
    if (isClibraryPresent == null) {
      isClibraryPresent = load() != null;
    }
    return isClibraryPresent;
  }

  public static synchronized Nc4prototypes getCLibrary() {
    return isClibraryPresent() ? nc4 : null;
  }

  /**
   * Set the log level for loaded library.
   * Do nothing if set_log_level is not available.
   */
  public static synchronized int setLogLevel(int level) {
    int oldlevel = -1;
    log_level = level;
    if (nc4 != null) {
      try {
        oldlevel = nc4.nc_set_log_level(log_level);
        startupLog.info(String.format("NetcdfLoader: set log level: old=%d new=%d", oldlevel, log_level));
      } catch (UnsatisfiedLinkError e) {
        // ignore
      }
    }
    return oldlevel;
  }


  /**
   * Convert a zero-length string to null
   *
   * @param s the string to check for length
   * @return null if s.length() == 0, s otherwise
   */
  private static String nullify(String s) {
    if (s != null && s.isEmpty())
      s = null;
    return s;
  }

  //////////////////////////////////////////////////
  // Do not construct

  private NetcdfClibrary() {
    throw new UnsupportedOperationException();
  }

}
