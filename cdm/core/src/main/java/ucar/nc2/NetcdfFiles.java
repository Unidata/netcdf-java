/* Copyright Unidata */
package ucar.nc2;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import ucar.nc2.internal.iosp.netcdf3.N3headerNew;
import ucar.nc2.internal.iosp.netcdf3.N3iospNew;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.iosp.IOServiceProvider;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.DiskCache;
import ucar.nc2.util.EscapeStrings;
import ucar.nc2.util.IO;
import ucar.nc2.util.rc.RC;
import ucar.unidata.io.InMemoryRandomAccessFile;
import ucar.unidata.io.UncompressInputStream;
import ucar.unidata.io.bzip2.CBZip2InputStream;
import ucar.unidata.util.StringUtil2;

/**
 * Static helper methods for NetcdfFile objects.
 * These use builders and new versions of Iosp's when available.
 *
 * @author caron
 * @since 10/3/2019.
 */
public class NetcdfFiles {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NetcdfFile.class);
  private static final List<IOServiceProvider> registeredProviders = new ArrayList<>();
  private static final int default_buffersize = 8092;
  private static final StringLocker stringLocker = new StringLocker();

  private static boolean loadWarnings = false;
  private static boolean userLoads;

  // IOSPs can be loaded by reflection.
  // Most IOSPs are loaded using the ServiceLoader mechanism. One problem with this is that we no longer
  // control the order which IOSPs try to open. So its harder to avoid mis-behaving and slow IOSPs from
  // making open() slow.
  static {
    // Make sure RC gets loaded
    RC.initialize();

    // Register iosp's that are part of cdm-core. This ensures that they are tried first.
    try {
      registerIOProvider("ucar.nc2.internal.iosp.hdf5.H5iospNew");
    } catch (Throwable e) {
      if (loadWarnings)
        log.info("Cant load class H5iosp", e);
    }
    try {
      registerIOProvider("ucar.nc2.stream.NcStreamIosp");
    } catch (Throwable e) {
      if (loadWarnings)
        log.info("Cant load class NcStreamIosp", e);
    }
    try {
      registerIOProvider("ucar.nc2.internal.iosp.hdf4.H4iosp");
    } catch (Throwable e) {
      if (loadWarnings)
        log.info("Cant load class H4iosp", e);
    }

    userLoads = true;
  }

  //////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Register an IOServiceProvider, using its class string name.
   *
   * @param className Class that implements IOServiceProvider.
   * @throws IllegalAccessException if class is not accessible.
   * @throws InstantiationException if class doesnt have a no-arg constructor.
   * @throws ClassNotFoundException if class not found.
   */
  public static void registerIOProvider(String className)
      throws IllegalAccessException, InstantiationException, ClassNotFoundException {
    Class ioClass = NetcdfFile.class.getClassLoader().loadClass(className);
    registerIOProvider(ioClass);
  }

  /**
   * Register an IOServiceProvider. A new instance will be created when one of its files is opened.
   *
   * @param iospClass Class that implements IOServiceProvider.
   * @throws IllegalAccessException if class is not accessible.
   * @throws InstantiationException if class doesnt have a no-arg constructor.
   * @throws ClassCastException if class doesnt implement IOServiceProvider interface.
   */
  public static void registerIOProvider(Class iospClass) throws IllegalAccessException, InstantiationException {
    registerIOProvider(iospClass, false);
  }

  /**
   * Register an IOServiceProvider. A new instance will be created when one of its files is opened.
   *
   * @param iospClass Class that implements IOServiceProvider.
   * @param last true=>insert at the end of the list; otherwise front
   * @throws IllegalAccessException if class is not accessible.
   * @throws InstantiationException if class doesnt have a no-arg constructor.
   * @throws ClassCastException if class doesnt implement IOServiceProvider interface.
   */
  private static void registerIOProvider(Class iospClass, boolean last)
      throws IllegalAccessException, InstantiationException {
    IOServiceProvider spi;
    spi = (IOServiceProvider) iospClass.newInstance(); // fail fast
    if (userLoads && !last)
      registeredProviders.add(0, spi); // put user stuff first
    else
      registeredProviders.add(spi);
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Open an existing netcdf file (read only).
   *
   * @param location location of file.
   * @return the NetcdfFile.
   * @throws java.io.IOException if error
   */
  public static NetcdfFile open(String location) throws IOException {
    return open(location, null);
  }

  /**
   * Open an existing file (read only), with option of cancelling.
   *
   * @param location location of the file.
   * @param cancelTask allow task to be cancelled; may be null.
   * @return NetcdfFile object, or null if cant find IOServiceProver
   * @throws IOException if error
   */
  public static NetcdfFile open(String location, ucar.nc2.util.CancelTask cancelTask) throws IOException {
    return open(location, -1, cancelTask);
  }

  /**
   * Open an existing file (read only), with option of cancelling, setting the RandomAccessFile buffer size for
   * efficiency.
   *
   * @param location location of file.
   * @param buffer_size RandomAccessFile buffer size, if <= 0, use default size
   * @param cancelTask allow task to be cancelled; may be null.
   * @return NetcdfFile object, or null if cant find IOServiceProver
   * @throws IOException if error
   */
  public static NetcdfFile open(String location, int buffer_size, ucar.nc2.util.CancelTask cancelTask)
      throws IOException {
    return open(location, buffer_size, cancelTask, null);
  }

  /**
   * Open an existing file (read only), with option of cancelling, setting the RandomAccessFile buffer size for
   * efficiency,
   * with an optional special object for the iosp.
   *
   * @param location location of file. This may be a
   *        <ol>
   *        <li>local netcdf-3 filename (with a file: prefix or no prefix)
   *        <li>remote netcdf-3 filename (with an http: prefix)
   *        <li>local netcdf-4 filename (with a file: prefix or no prefix)
   *        <li>local hdf-5 filename (with a file: prefix or no prefix)
   *        <li>local iosp filename (with a file: prefix or no prefix)
   *        </ol>
   *        http://thredds.ucar.edu/thredds/fileServer/grib/NCEP/GFS/Alaska_191km/files/GFS_Alaska_191km_20130416_0600.grib1
   *        If file ends with ".Z", ".zip", ".gzip", ".gz", or ".bz2", it will uncompress/unzip and write to new file
   *        without the suffix,
   *        then use the uncompressed file. It will look for the uncompressed file before it does any of that. Generally
   *        it prefers to
   *        place the uncompressed file in the same directory as the original file. If it does not have write permission
   *        on that directory,
   *        it will use the directory defined by ucar.nc2.util.DiskCache class.
   * @param buffer_size RandomAccessFile buffer size, if <= 0, use default size
   * @param cancelTask allow task to be cancelled; may be null.
   * @param iospMessage special iosp tweaking (sent before open is called), may be null
   * @return NetcdfFile object, or null if cant find IOServiceProver
   * @throws IOException if error
   */
  public static NetcdfFile open(String location, int buffer_size, ucar.nc2.util.CancelTask cancelTask,
      Object iospMessage) throws IOException {

    ucar.unidata.io.RandomAccessFile raf = getRaf(location, buffer_size);

    try {
      return open(raf, location, cancelTask, iospMessage);
    } catch (Throwable t) {
      raf.close();
      throw new IOException(t);
    }
  }

  /**
   * Open an existing file (read only), specifying which IOSP is to be used.
   *
   * @param location location of file
   * @param iospClassName fully qualified class name of the IOSP class to handle this file
   * @param bufferSize RandomAccessFile buffer size, if <= 0, use default size
   * @param cancelTask allow task to be cancelled; may be null.
   * @param iospMessage special iosp tweaking (sent before open is called), may be null
   * @return NetcdfFile object, or null if cant find IOServiceProver
   * @throws IOException if read error
   * @throws ClassNotFoundException cannat find iospClassName in thye class path
   * @throws InstantiationException if class cannot be instantiated
   * @throws IllegalAccessException if class is not accessible
   */
  public static NetcdfFile open(String location, String iospClassName, int bufferSize, CancelTask cancelTask,
      Object iospMessage) throws ClassNotFoundException, IllegalAccessException, InstantiationException, IOException {

    Class iospClass = NetcdfFile.class.getClassLoader().loadClass(iospClassName);
    IOServiceProvider spi = (IOServiceProvider) iospClass.newInstance(); // fail fast

    // send iospMessage before iosp is opened
    if (iospMessage != null)
      spi.sendIospMessage(iospMessage);

    if (bufferSize <= 0)
      bufferSize = default_buffersize;

    ucar.unidata.io.RandomAccessFile raf =
        ucar.unidata.io.RandomAccessFile.acquire(canonicalizeUriString(location), bufferSize);

    NetcdfFile result = build(spi, raf, location, cancelTask);

    // send after iosp is opened
    if (iospMessage != null)
      spi.sendIospMessage(iospMessage);

    return result;
  }

  /**
   * Removes the {@code "file:"} or {@code "file://"} prefix from the location, if necessary. Also replaces
   * back slashes with forward slashes.
   *
   * @param location a URI string.
   * @return a canonical URI string.
   */
  public static String canonicalizeUriString(String location) {
    // get rid of file prefix, if any
    String uriString = location.trim();
    if (uriString.startsWith("file://"))
      uriString = uriString.substring(7);
    else if (uriString.startsWith("file:"))
      uriString = uriString.substring(5);

    // get rid of crappy microsnot \ replace with happy /
    return StringUtil2.replace(uriString, '\\', "/");
  }

  private static ucar.unidata.io.RandomAccessFile getRaf(String location, int buffer_size) throws IOException {
    String uriString = location.trim();

    if (buffer_size <= 0)
      buffer_size = default_buffersize;

    ucar.unidata.io.RandomAccessFile raf;
    if (uriString.startsWith("http:") || uriString.startsWith("https:")) { // open through URL
      raf = new ucar.unidata.io.http.HTTPRandomAccessFile(uriString);

    } else if (uriString.startsWith("nodods:")) { // deprecated use httpserver
      uriString = "http" + uriString.substring(6);
      raf = new ucar.unidata.io.http.HTTPRandomAccessFile(uriString);

    } else if (uriString.startsWith("httpserver:")) { // open through URL
      uriString = "http" + uriString.substring(10);
      raf = new ucar.unidata.io.http.HTTPRandomAccessFile(uriString);

    } else if (uriString.startsWith("slurp:")) { // open through URL
      uriString = "http" + uriString.substring(5);
      byte[] contents = IO.readURLContentsToByteArray(uriString); // read all into memory
      raf = new InMemoryRandomAccessFile(uriString, contents);

    } else {
      // get rid of crappy microsnot \ replace with happy /
      uriString = StringUtil2.replace(uriString, '\\', "/");

      if (uriString.startsWith("file:")) {
        // uriString = uriString.substring(5);
        uriString = StringUtil2.unescape(uriString.substring(5)); // 11/10/2010 from erussell@ngs.org
      }

      String uncompressedFileName = null;
      try {
        stringLocker.control(uriString); // Avoid race condition where the decompressed file is trying to be read by one
        // thread while another is decompressing it
        uncompressedFileName = makeUncompressed(uriString);
      } catch (Exception e) {
        log.warn("Failed to uncompress {}, err= {}; try as a regular file.", uriString, e.getMessage());
        // allow to fall through to open the "compressed" file directly - may be a misnamed suffix
      } finally {
        stringLocker.release(uriString);
      }

      if (uncompressedFileName != null) {
        // open uncompressed file as a RandomAccessFile.
        raf = ucar.unidata.io.RandomAccessFile.acquire(uncompressedFileName, buffer_size);
        // raf = new ucar.unidata.io.MMapRandomAccessFile(uncompressedFileName, "r");

      } else {
        // normal case - not compressed
        raf = ucar.unidata.io.RandomAccessFile.acquire(uriString, buffer_size);
        // raf = new ucar.unidata.io.MMapRandomAccessFile(uriString, "r");
      }
    }

    return raf;
  }

  private static String makeUncompressed(String filename) throws IOException {
    // see if its a compressed file
    int pos = filename.lastIndexOf('.');
    if (pos < 0)
      return null;

    String suffix = filename.substring(pos + 1);
    String uncompressedFilename = filename.substring(0, pos);

    if (!suffix.equalsIgnoreCase("Z") && !suffix.equalsIgnoreCase("zip") && !suffix.equalsIgnoreCase("gzip")
        && !suffix.equalsIgnoreCase("gz") && !suffix.equalsIgnoreCase("bz2"))
      return null;

    // coverity claims resource leak, but attempts to fix break. so beware
    // see if already decompressed, check in cache as needed
    File uncompressedFile = DiskCache.getFileStandardPolicy(uncompressedFilename);
    if (uncompressedFile.exists() && uncompressedFile.length() > 0) {
      // see if its locked - another thread is writing it
      FileLock lock = null;
      try (FileInputStream stream = new FileInputStream(uncompressedFile)) {
        // obtain the lock
        while (true) { // loop waiting for the lock
          try {
            lock = stream.getChannel().lock(0, 1, true); // wait till its unlocked
            break;

          } catch (OverlappingFileLockException oe) { // not sure why lock() doesnt block
            log.warn("OverlappingFileLockException", oe);
            try {
              Thread.sleep(100); // msecs
            } catch (InterruptedException e1) {
              break;
            }
          }
        }

        if (NetcdfFile.debugCompress)
          log.info("found uncompressed {} for {}", uncompressedFile, filename);
        return uncompressedFile.getPath();

      } finally {
        if (lock != null && lock.isValid())
          lock.release();
      }
    }

    // ok gonna write it
    // make sure compressed file exists
    File file = new File(filename);
    if (!file.exists()) {
      return null; // bail out */
    }

    try (FileOutputStream fout = new FileOutputStream(uncompressedFile)) {

      // obtain the lock
      FileLock lock;
      while (true) { // loop waiting for the lock
        try {
          lock = fout.getChannel().lock(0, 1, false);
          break;

        } catch (OverlappingFileLockException oe) { // not sure why lock() doesnt block
          log.warn("OverlappingFileLockException2", oe);
          try {
            Thread.sleep(100); // msecs
          } catch (InterruptedException e1) {
          }
        }
      }

      try {
        if (suffix.equalsIgnoreCase("Z")) {
          try (InputStream in = new UncompressInputStream(new FileInputStream(filename))) {
            copy(in, fout, 100000);
          }
          if (NetcdfFile.debugCompress)
            log.info("uncompressed {} to {}", filename, uncompressedFile);

        } else if (suffix.equalsIgnoreCase("zip")) {

          try (ZipInputStream zin = new ZipInputStream(new FileInputStream(filename))) {
            ZipEntry ze = zin.getNextEntry();
            if (ze != null) {
              copy(zin, fout, 100000);
              if (NetcdfFile.debugCompress)
                log.info("unzipped {} entry {} to {}", filename, ze.getName(), uncompressedFile);
            }
          }

        } else if (suffix.equalsIgnoreCase("bz2")) {
          try (InputStream in = new CBZip2InputStream(new FileInputStream(filename), true)) {
            copy(in, fout, 100000);
          }
          if (NetcdfFile.debugCompress)
            log.info("unbzipped {} to {}", filename, uncompressedFile);

        } else if (suffix.equalsIgnoreCase("gzip") || suffix.equalsIgnoreCase("gz")) {

          try (InputStream in = new GZIPInputStream(new FileInputStream(filename))) {
            copy(in, fout, 100000);
          }

          if (NetcdfFile.debugCompress)
            log.info("ungzipped {} to {}", filename, uncompressedFile);
        }
      } catch (Exception e) {
        log.warn("Failed to uncompress file {}", filename, e);
        // dont leave bad files around
        if (uncompressedFile.exists()) {
          if (!uncompressedFile.delete())
            log.warn("failed to delete uncompressed file (IOException) {}", uncompressedFile);
        }
        throw e;

      } finally {
        if (lock != null)
          lock.release();
      }
    }

    return uncompressedFile.getPath();
  }

  // LOOK why not use util.IO ?
  private static void copy(InputStream in, OutputStream out, int bufferSize) throws IOException {
    byte[] buffer = new byte[bufferSize];
    while (true) {
      int bytesRead = in.read(buffer);
      if (bytesRead == -1)
        break;
      out.write(buffer, 0, bytesRead);
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Read a local CDM file into memory. All reads are then done from memory.
   *
   * @param filename location of CDM file, must be a local file.
   * @return a NetcdfFile, which is completely in memory
   * @throws IOException if error reading file
   */
  public static NetcdfFile openInMemory(String filename) throws IOException {
    File file = new File(filename);
    ByteArrayOutputStream bos = new ByteArrayOutputStream((int) file.length());
    try (InputStream in = new BufferedInputStream(new FileInputStream(filename))) {
      IO.copy(in, bos);
    }
    return openInMemory(filename, bos.toByteArray());
  }

  /**
   * Read a remote CDM file into memory. All reads are then done from memory.
   *
   * @param uri location of CDM file, must be accessible through url.toURL().openStream().
   * @return a NetcdfFile, which is completely in memory
   * @throws IOException if error reading file
   */
  public static NetcdfFile openInMemory(URI uri) throws IOException {
    URL url = uri.toURL();
    byte[] contents = IO.readContentsToByteArray(url.openStream());
    return openInMemory(uri.toString(), contents);
  }

  /**
   * Open an in-memory netcdf file.
   *
   * @param name name of the dataset. Typically use the filename or URI.
   * @param data in-memory netcdf file
   * @return memory-resident NetcdfFile
   * @throws java.io.IOException if error
   */
  public static NetcdfFile openInMemory(String name, byte[] data) throws IOException {
    ucar.unidata.io.InMemoryRandomAccessFile raf = new ucar.unidata.io.InMemoryRandomAccessFile(name, data);
    return open(raf, name, null, null);
  }

  /**
   * Open an in-memory netcdf file, with a specific iosp.
   *
   * @param name name of the dataset. Typically use the filename or URI.
   * @param data in-memory netcdf file
   * @param iospClassName fully qualified class name of the IOSP class to handle this file
   * @return NetcdfFile object, or null if cant find IOServiceProver
   * @throws IOException if read error
   * @throws ClassNotFoundException cannat find iospClassName in the class path
   * @throws InstantiationException if class cannot be instantiated
   * @throws IllegalAccessException if class is not accessible
   */
  public static NetcdfFile openInMemory(String name, byte[] data, String iospClassName)
      throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {

    ucar.unidata.io.InMemoryRandomAccessFile raf = new ucar.unidata.io.InMemoryRandomAccessFile(name, data);
    Class iospClass = NetcdfFile.class.getClassLoader().loadClass(iospClassName);
    IOServiceProvider spi = (IOServiceProvider) iospClass.newInstance();

    return build(spi, raf, name, null);
  }

  /**
   * Open a RandomAccessFile as a NetcdfFile, if possible.
   *
   * @param raf The open raf, is not cloised by this method.
   * @param location human readable locatoin of this dataset.
   * @param cancelTask used to monitor user cancellation; may be null.
   * @param iospMessage send this message to iosp; may be null.
   * @return NetcdfFile or throw an Exception.
   * @throws IOException if cannot open as a CDM NetCDF.
   */
  public static NetcdfFile open(ucar.unidata.io.RandomAccessFile raf, String location,
      ucar.nc2.util.CancelTask cancelTask, Object iospMessage) throws IOException {

    IOServiceProvider spi = null;
    if (NetcdfFile.debugSPI)
      log.info("NetcdfFile try to open = {}", location);

    // Registered providers override defaults.
    for (IOServiceProvider registeredSpi : registeredProviders) {
      if (NetcdfFile.debugSPI)
        log.info(" try iosp = {}", registeredSpi.getClass().getName());

      if (registeredSpi.isValidFile(raf)) {
        // need a new instance for thread safety
        Class c = registeredSpi.getClass();
        try {
          spi = (IOServiceProvider) c.newInstance();
        } catch (InstantiationException e) {
          throw new IOException("IOServiceProvider " + c.getName() + "must have no-arg constructor."); // shouldnt
          // happen
        } catch (IllegalAccessException e) {
          throw new IOException("IOServiceProvider " + c.getName() + " IllegalAccessException: " + e.getMessage()); // shouldnt
          // happen
        }
        break;
      }
    }

    if (N3headerNew.isValidFile(raf)) {
      spi = new N3iospNew();

    } else {

      // look for dynamically loaded IOSPs
      for (IOServiceProvider loadedSpi : ServiceLoader.load(IOServiceProvider.class)) {
        if (loadedSpi.isValidFile(raf)) {
          Class c = loadedSpi.getClass();
          try {
            spi = (IOServiceProvider) c.newInstance();
          } catch (InstantiationException e) {
            throw new IOException("IOServiceProvider " + c.getName() + "must have no-arg constructor."); // shouldnt
            // happen
          } catch (IllegalAccessException e) {
            throw new IOException("IOServiceProvider " + c.getName() + " IllegalAccessException: " + e.getMessage()); // shouldnt
            // happen
          }
          break;
        }
      }
    }

    if (spi == null) {
      raf.close();
      throw new IOException("Cant read " + location + ": not a valid CDM file.");
    }

    // send iospMessage before the iosp is opened
    if (iospMessage != null)
      spi.sendIospMessage(iospMessage);

    if (log.isDebugEnabled())
      log.debug("Using IOSP {}", spi.getClass().getName());

    NetcdfFile ncfile =
        spi.isBuilder() ? build(spi, raf, location, cancelTask) : new NetcdfFile(spi, raf, location, cancelTask);

    // send iospMessage after iosp is opened
    if (iospMessage != null)
      spi.sendIospMessage(iospMessage);

    return ncfile;
  }

  private static NetcdfFile build(IOServiceProvider spi, ucar.unidata.io.RandomAccessFile raf, String location,
      ucar.nc2.util.CancelTask cancelTask) throws IOException {

    NetcdfFile.Builder builder = NetcdfFile.builder().setIosp((AbstractIOServiceProvider) spi).setLocation(location);

    try {
      Group.Builder root = Group.builder(null).setName("");
      spi.build(raf, root, cancelTask);
      builder.setRootGroup(root);

      String id = root.getAttributeContainer().findAttValueIgnoreCase("_Id", null);
      if (id != null) {
        builder.setId(id);
      }
      String title = root.getAttributeContainer().findAttValueIgnoreCase("_Title", null);
      if (title != null) {
        builder.setTitle(title);
      }

    } catch (IOException | RuntimeException e) {
      try {
        raf.close();
      } catch (Throwable t2) {
      }
      try {
        spi.close();
      } catch (Throwable t1) {
      }
      throw e;

    } catch (Throwable t) {
      try {
        spi.close();
      } catch (Throwable t1) {
      }
      try {
        raf.close();
      } catch (Throwable t2) {
      }
      throw new RuntimeException(t);
    }

    return builder.build();
  }

  ///////////////////////////////////////////////////////////////////////
  // All CDM naming convention enforcement should be here.
  // DAP conventions should be in DODSNetcdfFile
  // TODO move this elsewhere.

  // reservedFullName defines the characters that must be escaped
  // when a short name is inserted into a full name
  private static final String reservedFullName = ".\\";

  // reservedSectionSpec defines the characters that must be escaped
  // when a short name is inserted into a section specification.
  private static final String reservedSectionSpec = "();,.\\";

  // reservedSectionCdl defines the characters that must be escaped
  // when what?
  private static final String reservedCdl = "[ !\"#$%&'()*,:;<=>?[]^`{|}~\\";

  /**
   * Create a valid CDM object name.
   * Control chars (< 0x20) are not allowed.
   * Trailing and leading blanks are not allowed and are stripped off.
   * A space is converted into an underscore "_".
   * A forward slash "/" is converted into an underscore "_".
   *
   * @param shortName from this name
   * @return valid CDM object name
   */
  public static String makeValidCdmObjectName(String shortName) {
    if (shortName == null)
      return null;
    return StringUtil2.makeValidCdmObjectName(shortName);
  }

  /**
   * Escape special characters in a netcdf short name when
   * it is intended for use in CDL.
   *
   * @param vname the name
   * @return escaped version of it
   */
  public static String makeValidCDLName(String vname) {
    return EscapeStrings.backslashEscape(vname, reservedCdl);
  }

  /**
   * Escape special characters in a netcdf short name when
   * it is intended for use in a fullname
   *
   * @param vname the name
   * @return escaped version of it
   */
  public static String makeValidPathName(String vname) {
    return EscapeStrings.backslashEscape(vname, reservedFullName);
  }

  /**
   * Escape special characters in a netcdf short name when
   * it is intended for use in a sectionSpec
   *
   * @param vname the name
   * @return escaped version of it
   */
  public static String makeValidSectionSpecName(String vname) {
    return EscapeStrings.backslashEscape(vname, reservedSectionSpec);
  }

  /**
   * Unescape any escaped characters in a name.
   *
   * @param vname the escaped name
   * @return unescaped version of it
   */
  public static String makeNameUnescaped(String vname) {
    return EscapeStrings.backslashUnescape(vname);
  }

  /**
   * Given a CDMNode, create its full name with
   * appropriate backslash escaping.
   * Warning: do not use for a section spec.
   *
   * @param v the cdm node
   * @return full name
   */
  protected static String makeFullName(CDMNode v) {
    return makeFullName(v, reservedFullName);
  }

  /**
   * Given a CDMNode, create its full name with
   * appropriate backslash escaping for use in a section spec.
   *
   * @param v the cdm node
   * @return full name
   */
  protected static String makeFullNameSectionSpec(CDMNode v) {
    return makeFullName(v, reservedSectionSpec);
  }

  /**
   * Given a CDMNode, create its full name with
   * appropriate backslash escaping of the specified characters.
   *
   * @param node the cdm node
   * @param reservedChars the set of characters to escape
   * @return full name
   */
  protected static String makeFullName(CDMNode node, String reservedChars) {
    Group parent = node.getParentGroup();
    if (((parent == null) || parent.isRoot()) && !node.isMemberOfStructure()) // common case?
      return EscapeStrings.backslashEscape(node.getShortName(), reservedChars);
    StringBuilder sbuff = new StringBuilder();
    appendGroupName(sbuff, parent, reservedChars);
    appendStructureName(sbuff, node, reservedChars);
    return sbuff.toString();
  }

  private static void appendGroupName(StringBuilder sbuff, Group g, String reserved) {
    if (g == null)
      return;
    if (g.getParentGroup() == null)
      return;
    appendGroupName(sbuff, g.getParentGroup(), reserved);
    sbuff.append(EscapeStrings.backslashEscape(g.getShortName(), reserved));
    sbuff.append("/");
  }

  private static void appendStructureName(StringBuilder sbuff, CDMNode n, String reserved) {
    if (n.isMemberOfStructure()) {
      appendStructureName(sbuff, n.getParentStructure(), reserved);
      sbuff.append(".");
    }
    sbuff.append(EscapeStrings.backslashEscape(n.getShortName(), reserved));
  }

  /**
   * Create a synthetic full name from a group plus a string
   *
   * @param parent parent group
   * @param name synthetic name string
   * @return synthetic name
   */
  protected static String makeFullNameWithString(Group parent, String name) {
    name = makeValidPathName(name); // escape for use in full name
    StringBuilder sbuff = new StringBuilder();
    appendGroupName(sbuff, parent, null);
    sbuff.append(name);
    return sbuff.toString();
  }

}
