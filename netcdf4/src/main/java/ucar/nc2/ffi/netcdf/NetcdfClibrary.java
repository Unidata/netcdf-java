/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ffi.netcdf;

import com.google.common.base.Strings;
import com.sun.jna.Native;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;
import ucar.nc2.jni.netcdf.Nc4prototypes;

/** Static methods to load the netcdf C library. */
public class NetcdfClibrary {
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NetcdfClibrary.class);
  private static org.slf4j.Logger startupLog = org.slf4j.LoggerFactory.getLogger("serverStartup");

  private static final String JNA_PATH = "jna.library.path";
  private static final String JNA_PATH_ENV = "JNA_PATH"; // environment var
  private static final String JNA_LOG_LEVEL = "jna.library.loglevel";

  private static String DEFAULT_NETCDF4_LIBNAME = "netcdf";

  private static String jnaPath;
  private static String libName = DEFAULT_NETCDF4_LIBNAME;
  private static Nc4prototypes nc4;
  private static int log_level;
  private static String version;

  // Track if already tested for library presence.
  private static Boolean isClibraryPresent;

  /**
   * Set the path and name of the netcdf c library.
   * Must be called before load() is called.
   *
   * @param jna_path path to shared libraries, may be null. If null, will look for system property
   *        "jna.library.path", then environment variable "JNA_PATH". If set, will set
   *        the environment variable "JNA_PATH".
   * @param lib_name library name, may be null. If null, will use "netcdf".
   */
  public static void setLibraryNameAndPath(@Nullable String jna_path, @Nullable String lib_name) {
    lib_name = Strings.emptyToNull(lib_name);

    if (lib_name == null) {
      lib_name = DEFAULT_NETCDF4_LIBNAME;
    }

    jna_path = Strings.emptyToNull(jna_path);

    if (jna_path == null) {
      jna_path = Strings.emptyToNull(System.getProperty(JNA_PATH)); // First, try system property (-D flag).
    }
    if (jna_path == null) {
      jna_path = Strings.emptyToNull(System.getenv(JNA_PATH_ENV)); // Next, try environment variable.
    }

    if (jna_path != null) {
      System.setProperty(JNA_PATH, jna_path);
    }

    libName = lib_name;
    jnaPath = jna_path;
  }

  /**
   * Test if the netcdf C library is present and loaded
   * 
   * @return true if present
   */
  public static synchronized boolean isLibraryPresent() {
    if (isClibraryPresent == null) {
      isClibraryPresent = load() != null;
    }
    return isClibraryPresent;
  }

  /** Get the interface to the Nectdf C library. */
  public static synchronized Nc4prototypes getForeignFunctionInterface() {
    return isLibraryPresent() ? nc4 : null;
  }

  /** Get the version of the loaded Nectdf C library. Call isClibraryPresent() first. */
  @Nullable
  public static String getVersion() {
    if (nc4 != null && version == null) {
      try {
        version = nc4.nc_inq_libvers();
      } catch (UnsatisfiedLinkError e) {
        // ignore
      }
    }
    return version;
  }

  /**
   * Set the log level for Netcdf C library.
   * This calls the Netcdf C library nc_set_log_level() method.
   * If the system property "jna.library.loglevel" is set, that is the default, this method overrides it.
   * 
   * @return the previous level, as returned from nc_set_log_level().
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
        setLibraryNameAndPath(null, null);
      }
      try {
        // jna_path may still be null, but try to load anyway;
        // the necessary libs may be on the system PATH or on LD_LIBRARY_PATH
        nc4 = Native.load(libName, Nc4prototypes.class);
        // Make the library synchronized
        // nc4 = (Nc4prototypes) Native.synchronizedLibrary(nc4);
        nc4 = new Nc4wrapper(nc4);
        startupLog.info("Nc4Iosp: NetCDF-4 C library loaded (jna_path='{}', libname='{}' version='{}').", jnaPath,
            libName, getVersion());
        startupLog.debug("Netcdf nc_inq_libvers='{}' isProtected={}", nc4.nc_inq_libvers(), Native.isProtected());
      } catch (Throwable t) {
        String message =
            String.format("Nc4Iosp: NetCDF-4 C library not present (jna_path='%s', libname='%s').", jnaPath, libName);
        startupLog.warn(message, t);
      }
      String slevel = Strings.emptyToNull(System.getProperty(JNA_LOG_LEVEL));
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

  //////////////////////////////////////////////////
  // Do not construct

  private NetcdfClibrary() {
    throw new UnsupportedOperationException();
  }

}
