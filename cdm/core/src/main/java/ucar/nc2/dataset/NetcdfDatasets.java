/* Copyright Unidata */
package ucar.nc2.dataset;

import java.io.IOException;
import java.io.Reader;
import java.util.EnumSet;
import java.util.ServiceLoader;
import java.util.Set;
import javax.annotation.Nullable;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset.Enhance;
import ucar.nc2.dataset.spi.NetcdfFileProvider;
import ucar.nc2.internal.dataset.DatasetEnhancer;
import ucar.nc2.internal.iosp.netcdf3.N3iospNew;
import ucar.nc2.internal.ncml.NcmlReader;
import ucar.nc2.iosp.IOServiceProvider;
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
  // enhanced datasets

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
  public static NetcdfDataset openDataset(String location, boolean enhance, @Nullable CancelTask cancelTask)
      throws IOException {
    return openDataset(location, enhance ? NetcdfDataset.getDefaultEnhanceMode() : null, cancelTask);
  }

  /**
   * Factory method for opening a dataset through the netCDF API, and identifying its coordinate variables.
   *
   * @param location location of file
   * @param enhanceMode set of enhancements. If null, then none
   * @param cancelTask allow task to be cancelled; may be null.
   * @return NetcdfDataset object
   * @throws java.io.IOException on read error
   */
  public static NetcdfDataset openDataset(String location, @Nullable Set<Enhance> enhanceMode,
      @Nullable CancelTask cancelTask) throws IOException {
    DatasetUrl durl = DatasetUrl.findDatasetUrl(location);
    return openDataset(durl, enhanceMode, -1, cancelTask, null);
  }

  /**
   * Factory method for opening a dataset through the netCDF API, and identifying its coordinate variables.
   *
   * @param location location of file
   * @param enhance if true, use defaultEnhanceMode, else no enhancements
   * @param cancelTask allow task to be cancelled; may be null.
   * @param iospMessage send to iosp.sendIospMessage() if not null
   * @return NetcdfDataset object
   * @throws java.io.IOException on read error
   */
  public static NetcdfDataset openDataset(String location, boolean enhance, @Nullable CancelTask cancelTask,
      @Nullable Object iospMessage) throws IOException {
    DatasetUrl durl = DatasetUrl.findDatasetUrl(location);
    return openDataset(durl, enhance ? NetcdfDataset.getDefaultEnhanceMode() : null, -1, cancelTask, iospMessage);
  }

  /**
   * Factory method for opening a dataset through the netCDF API, and identifying its coordinate variables.
   *
   * @param location location of file
   * @param enhanceMode set of enhancements. If null, then none
   * @param buffer_size RandomAccessFile buffer size, if <= 0, use default size
   * @param cancelTask allow task to be cancelled; may be null.
   * @param iospMessage send to iosp.sendIospMessage() if not null
   * @return NetcdfDataset object
   * @throws java.io.IOException on read error
   */
  public static NetcdfDataset openDataset(DatasetUrl location, @Nullable Set<Enhance> enhanceMode, int buffer_size,
      @Nullable CancelTask cancelTask, @Nullable Object iospMessage) throws IOException {
    NetcdfFile ncfile = openProtocolOrFile(location, buffer_size, cancelTask, iospMessage);
    return enhance(ncfile, enhanceMode, cancelTask);
  }

  /**
   * Read NcML doc from a Reader, and construct a NetcdfDataset.Builder.
   * eg: NcmlReader.readNcml(new StringReader(ncml), location, null);
   *
   * @param reader the Reader containing the NcML document
   * @param ncmlLocation the URL location string of the NcML document, used to resolve reletive path of the referenced
   *        dataset, or may be just a unique name for caching purposes.
   * @param cancelTask allow user to cancel the task; may be null
   * @return the resulting NetcdfDataset.Builder
   * @throws IOException on read error, or bad referencedDatasetUri URI
   */
  public static NetcdfDataset openNcmlDataset(Reader reader, String ncmlLocation, @Nullable CancelTask cancelTask)
      throws IOException {
    NetcdfDataset.Builder<?> builder = NcmlReader.readNcml(reader, ncmlLocation, cancelTask);
    if (!builder.getEnhanceMode().isEmpty()) {
      DatasetEnhancer enhancer = new DatasetEnhancer(builder, builder.getEnhanceMode(), cancelTask);
      return enhancer.enhance().build();
    } else {
      return builder.build();
    }
  }

  /**
   * Make NetcdfFile into NetcdfDataset and enhance if needed
   *
   * @param ncfile wrap this NetcdfFile or NetcdfDataset.
   * @param mode using this enhance mode (may be null, meaning no enhance)
   * @return a new NetcdfDataset that wraps the given NetcdfFile or NetcdfDataset.
   * @throws IOException on io error
   */
  public static NetcdfDataset enhance(NetcdfFile ncfile, @Nullable Set<Enhance> mode, @Nullable CancelTask cancelTask)
      throws IOException {
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

  public static NetcdfDataset addNetcdf3RecordStructure(NetcdfDataset ncd) throws IOException {
    if (ncd.getReferencedFile() == null) {
      return ncd;
    }
    NetcdfFile orgFile = ncd.getReferencedFile();

    // Is it a netcdf3 file?
    IOServiceProvider iosp = (IOServiceProvider) orgFile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_GET_IOSP);

    if (iosp == null || !(iosp instanceof N3iospNew)) {
      return ncd;
    }

    // Does it have an unlimited dimension ?
    if (ncd.getUnlimitedDimension() == null) {
      return ncd;
    }

    // Does it already have a record variable ?
    if (ncd.findVariable("record") != null) {
      return ncd;
    }

    // reopen the file adding the record Structure
    DatasetUrl durl = DatasetUrl.findDatasetUrl(orgFile.getLocation());
    // LOOK we dont know if it was acquired or opened
    // LOOK we dont know what the buffer size was
    NetcdfFile ncfile = NetcdfDatasets.openFile(durl, -1, null, NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
    NetcdfDataset.Builder builder = ncd.toBuilder().setOrgFile(ncfile);
    ncd.close();

    // does the following code need to go in NetcdfDataset.Builder?
    Structure orgStructure = (Structure) ncfile.getRootGroup().findVariableLocal("record");
    Dimension udim = ncfile.getUnlimitedDimension();
    StructureDS.Builder<?> newStructure = StructureDS.builder().setName("record")
        .setParentGroupBuilder(builder.rootGroup).setDimensionsByName(udim.getShortName());
    newStructure.setOriginalVariable(orgStructure);

    for (Variable.Builder vb : builder.rootGroup.vbuilders) {
      vb.setNcfile(null);

      Variable orgVar = ncfile.findVariable(vb.shortName);
      VariableDS.Builder vdb = (VariableDS.Builder) vb;
      vdb.setOriginalVariable(orgVar);
      vdb.setProxyReader(null);

      if (!vb.isUnlimited()) {
        continue;
      }
      // set unlimited dimension to 0
      VariableDS.Builder memberV = (VariableDS.Builder) vb.makeSliceBuilder(0, 0);
      newStructure.addMemberVariable(memberV);
    }
    builder.rootGroup.addVariable(newStructure);

    return builder.build();
  }

  ////////////////////////////////////////////////////////////////////////////////////
  // acquiring through the cache

  /**
   * Same as openDataset, but file is acquired through the File Cache, with defaultEnhanceMode.
   * You still close with NetcdfDataset.close(), the release is handled automatically.
   * You must first call initNetcdfFileCache() for caching to actually take place.
   *
   * @param location location of file, passed to FileFactory
   * @param cancelTask allow task to be cancelled; may be null.
   * @return NetcdfDataset object
   * @throws java.io.IOException on read error
   */
  public static NetcdfDataset acquireDataset(DatasetUrl location, @Nullable CancelTask cancelTask) throws IOException {
    return acquireDataset(null, location, NetcdfDataset.getDefaultEnhanceMode(), -1, cancelTask, null);
  }

  /**
   * Same as openDataset, but file is acquired through the File Cache, with optional enhancement.
   * You still close with NetcdfDataset.close(), the release is handled automatically.
   * You must first call initNetcdfFileCache() for caching to actually take place.
   *
   * @param location location of file, passed to FileFactory
   * @param enhanceMode whether to enhance.
   * @param cancelTask allow task to be cancelled; may be null.
   * @return NetcdfDataset object
   * @throws java.io.IOException on read error
   */
  public static NetcdfDataset acquireDataset(DatasetUrl location, boolean enhanceMode, @Nullable CancelTask cancelTask)
      throws IOException {
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
   * @param iospMessage send to iosp.sendIospMessage() if not null
   * @return NetcdfDataset object
   * @throws java.io.IOException on read error
   */
  public static NetcdfDataset acquireDataset(DatasetUrl location, @Nullable Set<Enhance> enhanceMode,
      @Nullable CancelTask cancelTask, @Nullable Object iospMessage) throws IOException {
    return acquireDataset(null, location, enhanceMode, -1, cancelTask, iospMessage);
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
   * @param iospMessage send to iosp.sendIospMessage() if not null
   *
   * @return NetcdfDataset or throw Exception
   */
  public static NetcdfDataset acquireDataset(@Nullable FileFactory fac, DatasetUrl durl,
      @Nullable Set<Enhance> enhanceMode, int buffer_size, @Nullable CancelTask cancelTask,
      @Nullable Object iospMessage) throws IOException {

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
   * @param iospMessage send to iosp.sendIospMessage() if not null
   * @return NetcdfFile object
   */
  public static NetcdfFile openFile(DatasetUrl location, int buffer_size, ucar.nc2.util.CancelTask cancelTask,
      Object iospMessage) throws IOException {
    return openProtocolOrFile(location, buffer_size, cancelTask, iospMessage);
  }

  /**
   * Same as openFile, but file is acquired through the File Cache.
   * You still close with NetcdfFile.close(), the release is handled automatically.
   * You must first call initNetcdfFileCache() for caching to actually take place.
   *
   * @param location location of file, passed to FileFactory
   * @param cancelTask allow task to be cancelled; may be null.
   * @return NetcdfFile object
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
   * @param spiObject send to iosp.sendIospMessage(); may be null
   * @return NetcdfFile object
   * @throws java.io.IOException on read error
   */
  public static NetcdfFile acquireFile(@Nullable ucar.nc2.util.cache.FileFactory factory, @Nullable Object hashKey,
      DatasetUrl location, int buffer_size, @Nullable ucar.nc2.util.CancelTask cancelTask, @Nullable Object spiObject)
      throws IOException {

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
   * @param iospMessage send to iosp.sendIospMessage() if not null
   * @return NetcdfFile or throw an Exception.
   */
  private static NetcdfFile openOrAcquireFile(FileCache cache, FileFactory factory, Object hashKey, DatasetUrl durl,
      int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object iospMessage) throws IOException {

    if (factory == null)
      factory = defaultNetcdfFileFactory;

    // Theres a cache
    if (cache != null) {
      return (NetcdfFile) cache.acquire(factory, hashKey, durl, buffer_size, cancelTask, iospMessage);
    }

    // Use the factory to open.
    return (NetcdfFile) factory.open(durl, buffer_size, cancelTask, iospMessage);
  }

  /**
   * Open through a protocol or a file. No cache, no factories.
   *
   * @param durl location of file, with protocol or as a file.
   * @param buffer_size RandomAccessFile buffer size, if <= 0, use default size
   * @param cancelTask allow task to be cancelled; may be null.
   * @param iospMessage send to iosp.sendIospMessage() if not null
   * @return NetcdfFile or throw an Exception.
   */
  private static NetcdfFile openProtocolOrFile(DatasetUrl durl, int buffer_size, ucar.nc2.util.CancelTask cancelTask,
      Object iospMessage) throws IOException {

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
    return NetcdfFiles.open(durl.getTrueurl(), buffer_size, cancelTask, iospMessage);
  }
}
