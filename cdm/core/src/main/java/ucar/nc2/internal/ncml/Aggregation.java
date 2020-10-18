/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 */
package ucar.nc2.internal.ncml;

import org.jdom2.Element;
import thredds.inventory.DateExtractor;
import thredds.inventory.DateExtractorFromName;
import thredds.inventory.MFile;
import thredds.inventory.MFileCollectionManager;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.DiskCache2;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executor;

/**
 * Superclass for NcML Aggregation Builder.
 * An Aggregation acts as a ProxyReader for VariableDS. That, is it must implement:
 * 
 * <pre>
 * public Array read(Variable mainv);
 * 
 * public Array read(Variable mainv, Section section);
 * </pre>
 *
 * @author caron
 */
public abstract class Aggregation implements ucar.nc2.ncml.AggregationIF {

  protected enum Type {
    forecastModelRunCollection, forecastModelRunSingleCollection, joinExisting, joinExistingOne, joinNew, tiled, union
  }

  protected enum TypicalDataset {
    FIRST, RANDOM, LATEST, PENULTIMATE
  }

  protected static TypicalDataset typicalDatasetMode = TypicalDataset.FIRST;

  protected static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Aggregation.class);
  protected static DiskCache2 diskCache2;

  // this is where persist() reads/writes files
  public static void setPersistenceCache(DiskCache2 dc) {
    diskCache2 = dc;
    if (diskCache2 != null)
      diskCache2.setAlwaysUseCache(true); // the persistence cache file has same name as the ncml - must put it into the
    // cache else clobber ncml 7/31/2014
  }

  // experimental multithreading
  protected static Executor executor;

  public static void setExecutor(Executor exec) {
    executor = exec;
  }

  public static void setTypicalDatasetMode(String mode) {
    if (mode.equalsIgnoreCase("random"))
      typicalDatasetMode = TypicalDataset.RANDOM;
    else if (mode.equalsIgnoreCase("latest"))
      typicalDatasetMode = TypicalDataset.LATEST;
    else if (mode.equalsIgnoreCase("penultimate"))
      typicalDatasetMode = TypicalDataset.PENULTIMATE;
    else if (mode.equalsIgnoreCase("first"))
      typicalDatasetMode = TypicalDataset.FIRST;
    else
      logger.error("Unknown setTypicalDatasetMode= " + mode);
  }

  protected static boolean debug, debugOpenFile, debugSyncDetail, debugProxy, debugRead, debugDateParse, debugConvert;

  //////////////////////////////////////////////////////////////////////////////////////////

  protected NetcdfDataset.Builder ncDataset; // the aggregation belongs to this dataset
  protected Type type; // the aggregation type
  protected Object spiObject = null; // not implemented in nested <netcdf> or <scan>

  protected List<AggDataset> explicitDatasets = new ArrayList<>(); // explicitly created Dataset objects from
  // netcdf elements
  protected List<AggDataset> datasets = new ArrayList<>(); // all : explicit and scanned
  protected MFileCollectionManager datasetManager; // manages scanning
  protected boolean cacheDirty = true; // aggCache persist file needs updating

  protected String dimName; // the aggregation dimension name

  Element ncmlElem;

  // experimental
  protected String dateFormatMark;
  // protected EnumSet<NetcdfDataset.Enhance> enhance = null; // default no enhancement
  protected boolean isDate;
  protected DateFormatter dateFormatter = new DateFormatter();

  /**
   * Create an Aggregation for the given NetcdfDataset.
   * The following addXXXX methods are called, then build(), before the object is ready for use.
   *
   * @param ncd Aggregation belongs to this NetcdfDataset
   * @param dimName the aggregation dimension name
   * @param type the Aggregation.Type
   * @param recheckS how often to check if files have changes
   */
  protected Aggregation(NetcdfDataset.Builder ncd, String dimName, Type type, String recheckS) {
    this.ncDataset = ncd;
    this.dimName = dimName;
    this.type = type;
    String name = ncd.location;
    if (name == null)
      name = "Agg-" + ncd.hashCode();
    datasetManager = MFileCollectionManager.openWithRecheck(name, recheckS);
  }

  /**
   * Add a nested dataset, specified by an explicit netcdf element.
   * enhance is handled by the reader, so its always false here.
   *
   * @param cacheName a unique name to use for caching
   * @param location attribute "location" on the netcdf element
   * @param id attribute "id" on the netcdf element
   * @param ncoordS attribute "ncoords" on the netcdf element
   * @param coordValueS attribute "coordValue" on the netcdf element
   * @param sectionSpec attribute "section" on the netcdf element
   * @param reader factory for reading this netcdf dataset
   */
  public void addExplicitDataset(String cacheName, String location, String id, String ncoordS, String coordValueS,
      String sectionSpec, ucar.nc2.util.cache.FileFactory reader) {

    AggDataset nested = makeDataset(cacheName, location, id, ncoordS, coordValueS, sectionSpec, null, reader);
    explicitDatasets.add(nested);
  }

  public void addDataset(AggDataset nested) {
    explicitDatasets.add(nested);
  }

  /**
   * Add a dataset scan
   *
   * @param crawlableDatasetElement defines a CrawlableDataset, or null
   * @param dirName scan this directory
   * @param suffix filter on this suffix (may be null)
   * @param regexpPatternString include if full name matches this regular expression (may be null)
   * @param dateFormatMark create dates from the filename (may be null)
   * @param enhanceMode how should files be enhanced
   * @param subdirs equals "false" if should not descend into subdirectories
   * @param olderThan files must be older than this time (now - lastModified >= olderThan); must be a time unit, may ne
   *        bull
   */
  public void addDatasetScan(Element crawlableDatasetElement, String dirName, String suffix, String regexpPatternString,
      String dateFormatMark, Set<NetcdfDataset.Enhance> enhanceMode, String subdirs, String olderThan) {

    datasetManager.addDirectoryScan(dirName, suffix, regexpPatternString, subdirs, olderThan, enhanceMode);

    this.dateFormatMark = dateFormatMark;
    if (dateFormatMark != null) {
      isDate = true;
      if (type == Type.joinExisting)
        type = Type.joinExistingOne; // tricky
      DateExtractor dateExtractor = new DateExtractorFromName(dateFormatMark, true);
      datasetManager.setDateExtractor(dateExtractor);
    }
  }

  // experimental
  public void addCollection(String spec, String olderThan) {
    datasetManager = MFileCollectionManager.open(spec, spec, olderThan, new Formatter());
  }

  public void setModifications(Element ncmlMods) {
    this.ncmlElem = ncmlMods;
  }

  /**
   * Get type of aggregation
   *
   * @return type of aggregation
   */
  public Type getType() {
    return type;
  }

  /**
   * Get dimension name to join on
   *
   * @return dimension name or null if type union/tiled
   */
  public String getDimensionName() {
    return dimName;
  }


  protected String getLocation() {
    return ncDataset.location;
  }

  /////////////////////////////////////////////////////////////////////

  public void close() throws IOException {
    persistWrite();
  }

  /**
   * Check to see if its time to rescan directory, and if so, rescan and extend dataset if needed.
   * Note that this just calls sync(), so structural metadata may be modified (!!)
   *
   * @return true if directory was rescanned and dataset may have been updated
   * @throws IOException on io error
   */
  @Override
  public synchronized boolean syncExtend() throws IOException {
    return false; // LOOK datasetManager.isScanNeeded() && _sync();
  }

  // public synchronized boolean sync() throws IOException {
  // return datasetManager.isScanNeeded() && _sync();
  // }

  // LOOK could also use syncExtend()
  @Override
  public long getLastModified() {
    try {
      datasetManager.scanIfNeeded();
    } catch (IOException e) {
      logger.error("Aggregation scan failed, e");
    }
    return datasetManager.getLastChanged();
  }

  /*
   * LOOK
   * private boolean _sync() throws IOException {
   * if (!datasetManager.scan(true))
   * return false; // nothing changed LOOK what about grib extention ??
   * cacheDirty = true;
   * makeDatasets(null);
   * 
   * // rebuild the metadata
   * rebuildDataset();
   * ncDataset.finish();
   * if (ncDataset.getEnhanceMode().contains(NetcdfDataset.Enhance.CoordSystems)) { // force recreation of the
   * coordinate
   * // systems
   * ncDataset.clearCoordinateSystems();
   * ncDataset.enhance(ncDataset.getEnhanceMode());
   * ncDataset.finish();
   * }
   * 
   * return true;
   * }
   */

  @Override
  public String getFileTypeId() { // LOOK - should cache ??
    AggDataset ds = null;
    NetcdfFile ncfile = null;
    try {
      ds = getTypicalDataset();
      ncfile = ds.acquireFile(null);
      return ncfile.getFileTypeId();

    } catch (Exception e) {
      logger.error("failed to open " + ds);

    } finally {
      if (ds != null)
        try {
          ds.close(ncfile);
        } catch (IOException e) {
          logger.error("failed to close " + ds);
        }
    }
    return "N/A";
  }

  @Override
  public String getFileTypeDescription() { // LOOK - should cache ??
    AggDataset ds = null;
    NetcdfFile ncfile = null;
    try {
      ds = getTypicalDataset();
      ncfile = ds.acquireFile(null);
      return ncfile.getFileTypeDescription();

    } catch (Exception e) {
      logger.error("failed to open " + ds);

    } finally {
      if (ds != null)
        try {
          ds.close(ncfile);
        } catch (IOException e) {
          logger.error("failed to close " + ds);
        }
    }
    return "N/A";
  }


  ///////////////////////////////////////////////////////////////////////////////////////////////////////
  // stuff for subclasses to override

  /**
   * Call this to build the dataset objects in the NetcdfDataset
   *
   * @param cancelTask maybe cancel
   * @throws IOException on read error
   */
  protected abstract void buildNetcdfDataset(CancelTask cancelTask) throws IOException;

  /**
   * Allow information to be made persistent. Overridden in AggregationExisting
   *
   * @throws IOException on error
   */
  @Override
  public void persistWrite() throws IOException {}

  /**
   * read info from the persistent XML file, if it exists; overridden in AggregationExisting
   */
  protected void persistRead() {}

  @Override
  public void getDetailInfo(Formatter f) {
    f.format("  Type=%s%n", type);
    f.format("  dimName=%s%n", dimName);
    f.format("  Datasets (%d) %n", datasets.size());
    for (AggDataset ds : datasets)
      ds.show(f);
  }


  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  // all elements are processed, finish construction

  public void build(CancelTask cancelTask) throws IOException {
    datasetManager.scan(true); // Make the list of Datasets, by scanning if needed.
    cacheDirty = true;
    makeDatasets(cancelTask);
    buildNetcdfDataset(cancelTask);
  }

  public List<AggDataset> getDatasets() {
    return datasets;
  }

  /**
   * Make the list of Datasets, from explicit and scans.
   *
   * @param cancelTask user can cancel
   * @throws IOException on i/o error
   */
  protected void makeDatasets(CancelTask cancelTask) throws IOException {

    // heres where the results will go
    datasets = new ArrayList<>();

    for (MFile cd : datasetManager.getFilesSorted()) {
      datasets.add(makeDataset(cd));
    }

    // sort using Dataset as Comparator.
    // Sort by date if it exists, else sort by filename.
    Collections.sort(datasets);

    // add the explicit datasets - these need to be kept in order
    // LOOK - should they be before or after scanned? Does it make sense to mix scan and explicit?
    // AggFmrcSingle sets explicit datasets - the scan is empty
    datasets.addAll(explicitDatasets);

    // Remove unreadable files (i.e. due to permissions) from the aggregation.
    // LOOK: Is this logic we should install "upstream", perhaps in MFileCollectionManager?
    // It would affect other collections than just NcML aggregation in that case.
    for (Iterator<AggDataset> datasetsIter = datasets.iterator(); datasetsIter.hasNext();) {
      AggDataset dataset = datasetsIter.next();

      if ((dataset.getMFile() != null) && (!dataset.getMFile().isReadable())) { // File.canRead() is broken on Windows,
                                                                                // but the JDK7 methods work.
        logger
            .warn("Aggregation member isn't readable (permissions issue?). Skipping: " + dataset.getMFile().getPath());
        datasetsIter.remove();
      }
    }

    // check for duplicate location
    Set<String> dset = new HashSet<>(2 * datasets.size());
    for (AggDataset dataset : datasets) {
      if (dset.contains(dataset.cacheLocation))
        logger.warn("Duplicate dataset in aggregation = " + dataset.cacheLocation);
      dset.add(dataset.cacheLocation);
    }

    if (datasets.isEmpty()) {
      throw new IllegalStateException("There are no datasets in the aggregation " + datasetManager);
    }
  }

  /**
   * Open one of the nested datasets as a template for the aggregation dataset.
   *
   * @return a typical Dataset
   * @throws IOException if there are no datasets
   */
  protected AggDataset getTypicalDataset() throws IOException {
    List<AggDataset> nestedDatasets = getDatasets();
    int n = nestedDatasets.size();
    if (n == 0)
      throw new FileNotFoundException("No datasets in this aggregation");

    int select;
    if (typicalDatasetMode == TypicalDataset.LATEST)
      select = n - 1;
    else if (typicalDatasetMode == TypicalDataset.PENULTIMATE)
      select = (n < 2) ? 0 : n - 2;
    else if (typicalDatasetMode == TypicalDataset.FIRST)
      select = 0;
    else { // random is default
      if (r == null)
        r = new Random();
      select = (n < 2) ? 0 : r.nextInt(n);
    }

    return nestedDatasets.get(select);
  }

  private Random r;


  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Dataset factory, so subclasses can override
   *
   * @param cacheName a unique name to use for caching
   * @param location attribute "location" on the netcdf element
   * @param id attribute "id" on the netcdf element
   * @param ncoordS attribute "ncoords" on the netcdf element
   * @param coordValueS attribute "coordValue" on the netcdf element
   * @param sectionSpec attribute "sectionSpec" on the netcdf element
   * @param enhance open dataset in enhance mode NOT USED
   * @param reader factory for reading this netcdf dataset
   * @return a Dataset
   */
  protected AggDataset makeDataset(String cacheName, String location, String id, String ncoordS, String coordValueS,
      String sectionSpec, EnumSet<NetcdfDataset.Enhance> enhance, ucar.nc2.util.cache.FileFactory reader) {
    return new AggDataset(cacheName, location, id, enhance, reader, spiObject, ncmlElem); // overridden in OuterDim,
                                                                                          // tiled
  }

  protected AggDataset makeDataset(MFile dset) {
    return new AggDataset(dset, spiObject, ncmlElem);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * All non-agg variables use a proxy to acquire the file before reading.
   * If the variable is caching, read data into cache now.
   * If not caching, VariableEnhanced.setProxyReader() is called.
   *
   * @param typicalDataset read from a "typical dataset"
   * @param newds containing dataset
   * @throws IOException on i/o error
   */
  void setDatasetAcquireProxy(AggDataset typicalDataset, NetcdfDataset.Builder newds) throws IOException {
    AggProxyReader proxy = new AggProxyReader(typicalDataset);
    setDatasetAcquireProxy(proxy, newds.rootGroup);
  }

  private void setDatasetAcquireProxy(AggProxyReader proxy, Group.Builder g) throws IOException {

    // all normal (non agg) variables must use a proxy to lock the file
    for (Variable.Builder v : g.vbuilders) {

      if (v.proxyReader != v && v.proxyReader != null) {
        if (debugProxy)
          System.out.println(" debugProxy: hasProxyReader " + v.shortName);
        continue; // dont mess with agg variables
      }
      /*
       * LOOK no caching
       * if (v.isCaching()) { // cache the small ones
       * v.setCachedData(v.read()); // cache the variableDS directly
       * 
       * } else { // put proxy on the rest
       */
      v.setProxyReader(proxy);
      if (debugProxy)
        System.out.println(" debugProxy: set proxy on " + v.shortName);
    }

    // recurse
    for (Group.Builder nested : g.gbuilders) {
      setDatasetAcquireProxy(proxy, nested);
    }
  }

}
