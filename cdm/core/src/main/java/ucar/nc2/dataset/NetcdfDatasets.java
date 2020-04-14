/* Copyright Unidata */
package ucar.nc2.dataset;

import java.io.IOException;
import java.util.EnumSet;
import java.util.ServiceLoader;
import java.util.Set;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.dataset.NetcdfDataset.Enhance;
import ucar.nc2.dataset.spi.NetcdfFileProvider;
import ucar.nc2.internal.dataset.DatasetEnhancer;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.cache.FileCache;
import ucar.nc2.util.cache.FileCacheIF;
import ucar.nc2.util.cache.FileFactory;

/**
 * Static helper methods for NetcdfDataset
 *
 * @author caron
 * @since 10/3/2019.
 */
public class NetcdfDatasets {

  ////////////////////////////////////////////////////////////////////////////////////
  // NetcdfFile caching

  private static ucar.nc2.util.cache.FileCache netcdfFileCache;
  private static ucar.nc2.util.cache.FileFactory defaultNetcdfFileFactory = new StandardFileFactory();

  // no state, so a singleton is ok
  private static class StandardFileFactory implements ucar.nc2.util.cache.FileFactory {
    public NetcdfFile open(DatasetUrl location, int buffer_size, CancelTask cancelTask, Object iospMessage)
        throws IOException {
      return openFile(location, buffer_size, cancelTask, iospMessage);
    }
  }

  /**
   * Enable file caching. call this before calling acquireFile().
   * When application terminates, call NetcdfDataset.shutdown().
   *
   * @param minElementsInMemory keep this number in the cache
   * @param maxElementsInMemory trigger a cleanup if it goes over this number.
   * @param period (secs) do periodic cleanups every this number of seconds. set to < 0 to not cleanup
   */
  public static synchronized void initNetcdfFileCache(int minElementsInMemory, int maxElementsInMemory, int period) {
    initNetcdfFileCache(minElementsInMemory, maxElementsInMemory, -1, period);
  }

  /**
   * Enable file caching. call this before calling acquireFile().
   * When application terminates, call NetcdfDataset.shutdown().
   *
   * @param minElementsInMemory keep this number in the cache
   * @param maxElementsInMemory trigger a cleanup if it goes over this number.
   * @param hardLimit if > 0, never allow more than this many elements. This causes a cleanup to be done in
   *        the calling thread.
   * @param period (secs) do periodic cleanups every this number of seconds.
   */
  public static synchronized void initNetcdfFileCache(int minElementsInMemory, int maxElementsInMemory, int hardLimit,
      int period) {
    netcdfFileCache = new FileCache("NetcdfFileCache ", minElementsInMemory, maxElementsInMemory, hardLimit, period);
  }

  public static synchronized void disableNetcdfFileCache() {
    if (null != netcdfFileCache)
      netcdfFileCache.disable();
    netcdfFileCache = null;
  }

  /**
   * Call when application exits, if you have previously called initNetcdfFileCache.
   * This shuts down any background threads in order to get a clean process shutdown.
   */
  public static synchronized void shutdown() {
    disableNetcdfFileCache();
    FileCache.shutdown();
  }

  /**
   * Get the File Cache
   *
   * @return File Cache or null if not enabled.
   */
  public static synchronized FileCacheIF getNetcdfFileCache() {
    return netcdfFileCache;
  }

  ////////////////////////////////////////////////////////////////////////////////////
  // enhancing

  /**
   * Factory method for opening a dataset through the netCDF API, and identifying its coordinate variables.
   *
   * @param location location of file
   * @return NetcdfDataset object
   * @throws java.io.IOException on read error
   */
  public static NetcdfDataset openDataset(String location) throws IOException {
    return openDataset(location, true, null);
  }

  /**
   * Factory method for opening a dataset through the netCDF API, and identifying its coordinate variables.
   *
   * @param location location of file
   * @param enhance if true, use defaultEnhanceMode, else no enhancements
   * @param cancelTask allow task to be cancelled; may be null.
   * @return NetcdfDataset object
   * @throws java.io.IOException on read error
   */
  public static NetcdfDataset openDataset(String location, boolean enhance, ucar.nc2.util.CancelTask cancelTask)
      throws IOException {
    return openDataset(location, enhance, -1, cancelTask, null);
  }

  /**
   * Factory method for opening a dataset through the netCDF API, and identifying its coordinate variables.
   *
   * @param location location of file
   * @param enhance if true, use defaultEnhanceMode, else no enhancements
   * @param buffer_size RandomAccessFile buffer size, if <= 0, use default size
   * @param cancelTask allow task to be cancelled; may be null.
   * @param spiObject sent to iosp.setSpecial() if not null
   * @return NetcdfDataset object
   * @throws java.io.IOException on read error
   */
  public static NetcdfDataset openDataset(String location, boolean enhance, int buffer_size,
      ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws IOException {
    DatasetUrl durl = DatasetUrl.findDatasetUrl(location);
    return openDataset(durl, enhance ? NetcdfDataset.getDefaultEnhanceMode() : null, buffer_size, cancelTask,
        spiObject);
  }

  /**
   * Factory method for opening a dataset through the netCDF API, and identifying its coordinate variables.
   *
   * @param location location of file
   * @param enhanceMode set of enhancements. If null, then none
   * @param buffer_size RandomAccessFile buffer size, if <= 0, use default size
   * @param cancelTask allow task to be cancelled; may be null.
   * @param spiObject sent to iosp.setSpecial() if not null
   * @return NetcdfDataset object
   * @throws java.io.IOException on read error
   */
  public static NetcdfDataset openDataset(DatasetUrl location, Set<Enhance> enhanceMode, int buffer_size,
      CancelTask cancelTask, Object spiObject) throws IOException {
    NetcdfFile ncfile = openProtocolOrFile(location, buffer_size, cancelTask, spiObject);
    return enhance(ncfile, enhanceMode, cancelTask);
  }

  /**
   * Make NetcdfFile into NetcdfDataset and enhance if needed
   *
   * @param ncfile wrap this
   * @param mode using this enhance mode (may be null, meaning no enhance)
   * @return NetcdfDataset.Builder wrapping the given ncfile
   * @throws IOException on io error
   */
  public static NetcdfDataset enhance(NetcdfFile ncfile, Set<Enhance> mode, CancelTask cancelTask) throws IOException {
    if (ncfile instanceof NetcdfDataset) {
      NetcdfDataset ncd = (NetcdfDataset) ncfile;
      NetcdfDataset.Builder builder = ncd.toBuilder();
      if (DatasetEnhancer.enhanceNeeded(mode, ncd.getEnhanceMode())) {
        DatasetEnhancer enhancer = new DatasetEnhancer(builder, mode, cancelTask);
        return enhancer.enhance().build();
      } else {
        return ncd;
      }
    }

    // original file not a NetcdfDataset
    NetcdfDataset.Builder builder = NetcdfDataset.builder(ncfile);
    if (DatasetEnhancer.enhanceNeeded(mode, null)) {
      DatasetEnhancer enhancer = new DatasetEnhancer(builder, mode, cancelTask);
      return enhancer.enhance().build();
    }
    return builder.build();
  }

  ////////////////////////////////////////////////////////////////////////////////////
  // acquiring through the cache

  /**
   * Same as openDataset, but file is acquired through the File Cache, with defaultEnhanceMode,
   * without the need of setting the enhanceMode via the signature.
   * You still close with NetcdfDataset.close(), the release is handled automatically.
   * You must first call initNetcdfFileCache() for caching to actually take place.
   *
   * @param location location of file, passed to FileFactory
   * @param cancelTask allow task to be cancelled; may be null.
   * @return NetcdfDataset object
   * @throws java.io.IOException on read error
   */
  public static NetcdfDataset acquireDataset(DatasetUrl location, ucar.nc2.util.CancelTask cancelTask)
      throws IOException {
    return acquireDataset(null, location, NetcdfDataset.getDefaultEnhanceMode(), -1, cancelTask, null);
  }

  /**
   * Same as openDataset, but file is acquired through the File Cache, with defaultEnhanceMode.
   * You still close with NetcdfDataset.close(), the release is handled automatically.
   * You must first call initNetcdfFileCache() for caching to actually take place.
   *
   * @param location location of file, passed to FileFactory
   * @param enhanceMode how to enhance. if null, then no enhancement
   * @param cancelTask allow task to be cancelled; may be null.
   * @return NetcdfDataset object
   * @throws java.io.IOException on read error
   */
  public static NetcdfDataset acquireDataset(DatasetUrl location, boolean enhanceMode,
      ucar.nc2.util.CancelTask cancelTask) throws IOException {
    return acquireDataset(null, location, enhanceMode ? NetcdfDataset.getDefaultEnhanceMode() : null, -1, cancelTask,
        null);
  }

  /**
   * Same as openDataset, but file is acquired through the File Cache, with specified enhancements.
   * You still close with NetcdfDataset.close(), the release is handled automatically.
   * You must first call initNetcdfFileCache() for caching to actually take place.
   *
   * @param location location of file, passed to FileFactory
   * @param enhanceMode how to enhance. if null, then no enhancement
   * @param cancelTask allow task to be cancelled; may be null.
   * @return NetcdfDataset object
   * @throws java.io.IOException on read error
   */
  public static NetcdfDataset acquireDataset(DatasetUrl location, Set<Enhance> enhanceMode,
      ucar.nc2.util.CancelTask cancelTask) throws IOException {
    return acquireDataset(null, location, enhanceMode, -1, cancelTask, null);
  }

  /**
   * Same as openDataset, but file is acquired through the File Cache.
   * You must first call initNetcdfFileCache() for caching to actually take place.
   * You still close with NetcdfDataset.close(), the release is handled automatically.
   *
   * @param fac if not null, use this factory if the file is not in the cache. If null, use the default factory.
   * @param durl location of file, passed to FileFactory
   * @param enhanceMode how to enhance. if null, then no enhancement
   * @param buffer_size RandomAccessFile buffer size, if <= 0, use default size
   * @param cancelTask allow task to be cancelled; may be null.
   * @param iospMessage sent to iosp.setSpecial() if not null
   *
   * @return NetcdfDataset or throw Exception
   */
  public static NetcdfDataset acquireDataset(FileFactory fac, DatasetUrl durl, Set<Enhance> enhanceMode,
      int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object iospMessage) throws IOException {

    // caching not turned on
    if (netcdfFileCache == null) {
      if (fac == null)
        return openDataset(durl, enhanceMode, buffer_size, cancelTask, iospMessage);
      else
        // must use the factory if there is one
        return (NetcdfDataset) fac.open(durl, buffer_size, cancelTask, iospMessage);
    }

    if (fac != null)
      return (NetcdfDataset) openOrAcquireFile(netcdfFileCache, fac, null, durl, buffer_size, cancelTask, iospMessage);

    fac = new StandardDatasetFactory(durl, enhanceMode);
    return (NetcdfDataset) openOrAcquireFile(netcdfFileCache, fac, fac.hashCode(), durl, buffer_size, cancelTask,
        iospMessage);
  }

  private static class StandardDatasetFactory implements ucar.nc2.util.cache.FileFactory {
    DatasetUrl location;
    EnumSet<Enhance> enhanceMode;

    StandardDatasetFactory(DatasetUrl location, Set<Enhance> enhanceMode) {
      this.location = location;
      this.enhanceMode = (enhanceMode == null) ? EnumSet.noneOf(Enhance.class) : EnumSet.copyOf(enhanceMode);
    }

    public NetcdfFile open(DatasetUrl location, int buffer_size, CancelTask cancelTask, Object iospMessage)
        throws IOException {
      return openDataset(location, enhanceMode, buffer_size, cancelTask, iospMessage);
    }

    // unique key, must be different than a plain NetcdfFile, deal with possible different enhancing.
    public int hashCode() {
      int result = location.hashCode();
      result += 37 * result + enhanceMode.hashCode();
      return result;
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public static NetcdfFile openFile(String location, ucar.nc2.util.CancelTask cancelTask) throws IOException {
    DatasetUrl durl = DatasetUrl.findDatasetUrl(location);
    return openFile(durl, -1, cancelTask, null);
  }

  /**
   * Factory method for opening a NetcdfFile through the netCDF API. May be any kind of file that
   * can be read through the netCDF API, including OpenDAP and NcML.
   * <ol>
   * <li>local filename (with a file: prefix or no prefix) for netCDF (version 3), hdf5 files, or any file type
   * registered with NetcdfFile.registerIOProvider().
   * <li>OpenDAP dataset URL (with a dods:, dap4:, or http: prefix).
   * <li>NcML file or URL if the location ends with ".xml" or ".ncml"
   * <li>NetCDF file through an HTTP server (http: prefix)
   * <li>thredds dataset (thredds: prefix), see DataFactory.openDataset(String location, ...));
   * </ol>
   * <p>
   * This does not necessarily return a NetcdfDataset, or enhance the dataset; use NetcdfDatasets.openDataset() method
   * for that.
   *
   * @param location location of dataset.
   * @param buffer_size RandomAccessFile buffer size, if <= 0, use default size
   * @param cancelTask allow task to be cancelled; may be null.
   * @param spiObject sent to iosp.setSpecial() if not null
   * @return NetcdfFile object
   * @throws java.io.IOException on read error
   */
  public static NetcdfFile openFile(DatasetUrl location, int buffer_size, ucar.nc2.util.CancelTask cancelTask,
      Object spiObject) throws IOException {
    return openProtocolOrFile(location, buffer_size, cancelTask, spiObject);
  }

  /**
   * Same as openFile, but file is acquired through the File Cache.
   * You still close with NetcdfFile.close(), the release is handled automatically.
   * You must first call initNetcdfFileCache() for caching to actually take place.
   *
   * @param location location of file, passed to FileFactory
   * @param cancelTask allow task to be cancelled; may be null.
   * @return NetcdfFile object
   * @throws java.io.IOException on read error
   */
  public static NetcdfFile acquireFile(DatasetUrl location, ucar.nc2.util.CancelTask cancelTask) throws IOException {
    return acquireFile(null, null, location, -1, cancelTask, null);
  }

  /**
   * Same as openFile, but file is acquired through the File Cache.
   * You still close with NetcdfFile.close(), the release is handled automatically.
   * You must first call initNetcdfFileCache() for caching to actually take place.
   *
   * @param factory if not null, use this factory to read the file. If null, use the default factory.
   * @param hashKey if not null, use as the cache key, else use the location
   * @param location location of file, passed to FileFactory
   * @param buffer_size RandomAccessFile buffer size, if <= 0, use default size
   * @param cancelTask allow task to be cancelled; may be null.
   * @param spiObject sent to iosp.setSpecial(); may be null
   * @return NetcdfFile object
   * @throws java.io.IOException on read error
   */
  public static NetcdfFile acquireFile(ucar.nc2.util.cache.FileFactory factory, Object hashKey, DatasetUrl location,
      int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws IOException {

    // must use the factory if there is one but no fileCache
    if ((netcdfFileCache == null) && (factory != null)) {
      return (NetcdfFile) factory.open(location, buffer_size, cancelTask, spiObject);
    }

    return openOrAcquireFile(netcdfFileCache, factory, hashKey, location, buffer_size, cancelTask, spiObject);
  }

  /**
   * Open or acquire a NetcdfFile.
   *
   * @param cache if not null, acquire through this NetcdfFileCache, otherwise simply open
   * @param factory if not null, use this factory if the file is not in the cache. If null, use the default factory.
   * @param hashKey if not null, use as the cache key, else use the location
   * @param durl location of file
   * @param buffer_size RandomAccessFile buffer size, if <= 0, use default size
   * @param cancelTask allow task to be cancelled; may be null.
   * @param spiObject sent to iosp.setSpecial() if not null
   * @return NetcdfFile or throw an Exception.
   */
  private static NetcdfFile openOrAcquireFile(FileCache cache, FileFactory factory, Object hashKey, DatasetUrl durl,
      int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws IOException {

    if (factory == null)
      factory = defaultNetcdfFileFactory;

    // Theres a cache
    if (cache != null) {
      return (NetcdfFile) cache.acquire(factory, hashKey, durl, buffer_size, cancelTask, spiObject);
    }

    // Use the factory to open.
    return (NetcdfFile) factory.open(durl, buffer_size, cancelTask, spiObject);
  }

  /**
   * Open through a protocol or a file. No cache, no factories.
   *
   * @param durl location of file, with protocol or as a file.
   * @param buffer_size RandomAccessFile buffer size, if <= 0, use default size
   * @param cancelTask allow task to be cancelled; may be null.
   * @param spiObject sent to iosp.setSpecial() if not null
   * @return NetcdfFile or throw an Exception.
   */
  private static NetcdfFile openProtocolOrFile(DatasetUrl durl, int buffer_size, ucar.nc2.util.CancelTask cancelTask,
      Object spiObject) throws IOException {

    // look for dynamically loaded NetcdfFileProvider
    for (NetcdfFileProvider provider : ServiceLoader.load(NetcdfFileProvider.class)) {
      if (provider.isOwnerOf(durl)) {
        return provider.open(durl.getTrueurl(), cancelTask);
      }
    }

    // look for providers who do not have an associated ServiceType.
    for (NetcdfFileProvider provider : ServiceLoader.load(NetcdfFileProvider.class)) {
      if (provider.isOwnerOf(durl.getTrueurl())) {
        return provider.open(durl.getTrueurl(), cancelTask);
      }
    }

    // Otherwise we are dealing with a file or a remote http file.
    if (durl.getServiceType() != null) {
      switch (durl.getServiceType()) {
        case File:
        case HTTPServer:
          break; // fall through

        default:
          throw new IOException("Unknown service type: " + durl.getServiceType());
      }
    }

    // Open as a file or remote file
    return NetcdfFiles.open(durl.getTrueurl(), buffer_size, cancelTask, spiObject);
  }
}
