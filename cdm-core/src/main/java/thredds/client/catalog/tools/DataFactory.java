/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.client.catalog.tools;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Optional;

import thredds.client.catalog.Access;
import thredds.client.catalog.Catalog;
import thredds.client.catalog.Dataset;
import thredds.client.catalog.ServiceType;
import thredds.client.catalog.builder.CatalogBuilder;
import ucar.nc2.constants.DataFormatType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.grid2.GridDataset;
import ucar.nc2.grid2.GridDatasetFactory;
import ucar.nc2.util.CancelTask;
import ucar.unidata.util.StringUtil2;

import javax.annotation.Nullable;

/** DataFactory for THREDDS client catalogs */
public class DataFactory {
  public static final String PROTOCOL = "thredds";
  public static final String SCHEME = PROTOCOL + ":";
  private static ServiceType[] preferAccess;

  public static void setPreferCdm(boolean prefer) {
    preferAccess = prefer ? new ServiceType[] {ServiceType.CdmRemote} : null;
  }

  public static void setPreferAccess(ServiceType... prefer) {
    preferAccess = prefer;
  }

  public static void setDebugFlags(ucar.nc2.util.DebugFlags debugFlag) {
    debugOpen = debugFlag.isSet("thredds/debugOpen");
  }

  private static boolean debugOpen;

  /**
   * The result of trying to open a THREDDS dataset.
   * If fatalError is true, the operation failed, errLog should indicate why.
   * Otherwise, the FeatureType and FeatureDataset is valid.
   * There may still be warning or diagnostic errors in errLog.
   */
  public static class Result implements Closeable {
    public boolean fatalError;
    public Formatter errLog = new Formatter();

    public FeatureType featureType;
    public GridDataset featureDataset; // Right now, we only have grids. This may become FeatureDataset again.
    public String imageURL;

    public String location;
    public Access accessUsed;

    @Override
    public String toString() {
      return "Result{" + "fatalError=" + fatalError + ", errLog=" + errLog + ", featureType=" + featureType
          + ", imageURL='" + imageURL + '\'' + ", location='" + location + '\'' + ", accessUsed=" + accessUsed + '}';
    }

    @Override
    public void close() throws IOException {
      if (featureDataset != null) {
        featureDataset.close();
      }
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////
  /**
   * Open a FeatureDataset from a URL location string. Example URLS:
   * <ul>
   * <li>http://localhost:8080/test/addeStationDataset.xml#surfaceHourly
   * <li>thredds:http://localhost:8080/test/addeStationDataset.xml#surfaceHourly
   * <li>thredds://localhost:8080/test/addeStationDataset.xml#surfaceHourly
   * <li>thredds:file:c:/test/data/catalog/addeStationDataset.xml#AddeSurfaceData (absolute file)
   * <li>thredds:resolve:resolveURL
   * </ul>
   *
   * @param urlString [thredds:]catalog.xml#datasetId
   * @param task may be null
   * @return DataFactory.Result check fatalError for validity
   */
  public DataFactory.Result openThreddsDataset(String urlString, CancelTask task) throws IOException {
    DataFactory.Result result = new DataFactory.Result();
    Dataset dataset = openCatalogFromLocation(urlString, task, result);
    if (result.fatalError || dataset == null) {
      return result;
    }
    return openThreddsDataset(dataset, task, result);
  }

  /**
   * Open a FeatureDataset from a Dataset object.
   *
   * @param ds use this to figure out what type, how to open, etc
   * @param task allow user to cancel; may be null
   * @return ThreddsDataFactory.Result check fatalError for validity
   */
  public DataFactory.Result openThreddsDataset(Dataset ds, CancelTask task) throws IOException {
    return openThreddsDataset(ds, task, new DataFactory.Result());
  }

  private DataFactory.Result openThreddsDataset(Dataset ds, CancelTask task, DataFactory.Result result)
      throws IOException {
    // deal with RESOLVER type
    Access resolverAccess = findAccessByServiceType(ds.getAccess(), ServiceType.Resolver);
    if (resolverAccess != null) {
      Dataset rds = openResolver(resolverAccess.getStandardUrlName(), task, result);
      if (rds != null) {
        ds = rds;
      }
    }

    try {
      result.featureType = ds.getFeatureType();

      // first choice would be remote FeatureDataset
      Access access = findAccessByServiceType(ds.getAccess(), ServiceType.CdmrFeature);
      if (access != null) {
        return openThreddsDataset(access.getDataset(), task, result);
      }

      // otherwise, try to open as GridDataset,
      NetcdfDataset ncd = openDataset(ds, true, task, result);
      if (null != ncd) {
        Optional<GridDataset> gridDataset = GridDatasetFactory.wrapGridDataset(ncd, result.errLog);
        if (gridDataset.isPresent()) {
          result.featureDataset = gridDataset.get();
          result.location = result.featureDataset.getLocation();
          result.featureType = result.featureDataset.getFeatureType();
        }
      }

      if (null == result.featureDataset) {
        result.fatalError = true;
      }

      return result;

    } catch (Throwable t) {
      result.close();
      if (t instanceof IOException)
        throw (IOException) t;
      throw new RuntimeException(t);
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Open a NetcdfDataset from a URL location string. Example URLS:
   * <ul>
   * <li>http://localhost:8080/test/addeStationDataset.xml#surfaceHourly
   * <li>thredds:http://localhost:8080/test/addeStationDataset.xml#surfaceHourly
   * <li>thredds://localhost:8080/test/addeStationDataset.xml#surfaceHourly
   * <li>thredds:file:c:/dev/netcdf-java-2.2/test/data/catalog/addeStationDataset.xml#AddeSurfaceData (absolute file)
   * <li>thredds:resolve:resolveURL
   * </ul>
   *
   * @param location catalog.xml#datasetId, may optionally start with "thredds:"
   * @param task may be null
   * @param log error messages gp here, may be null
   * @param acquire if true, aquire the dataset, else open it
   * @return NetcdfDataset
   * @throws java.io.IOException on read error
   */
  public NetcdfDataset openDataset(String location, boolean acquire, CancelTask task, Formatter log)
      throws IOException {
    Result result = new Result();
    Dataset dataset = openCatalogFromLocation(location, task, result);
    if (result.fatalError || dataset == null) {
      if (log != null)
        log.format("%s", result.errLog);
      result.close();
      return null;
    }

    return openDataset(dataset, acquire, task, result);
  }

  private Dataset openCatalogFromLocation(String location, CancelTask task, Result result) {
    location = location.trim();
    location = StringUtil2.replace(location, '\\', "/");

    if (location.startsWith(SCHEME))
      location = location.substring(8);

    if (location.startsWith("resolve:")) {
      location = location.substring(8);
      return openResolver(location, task, result);
    }

    if (!location.startsWith("http:") && !location.startsWith("file:")) // LOOK whats this for??
      location = "http:" + location;

    Catalog catalog;
    String datasetId;

    int pos = location.indexOf('#');
    if (pos < 0) {
      result.fatalError = true;
      result.errLog.format("Must have the form catalog.xml#datasetId%n");
      return null;
    }

    CatalogBuilder catFactory = new CatalogBuilder();
    String catalogLocation = location.substring(0, pos);
    catalog = catFactory.buildFromLocation(catalogLocation, null);
    if (catalog == null) {
      result.errLog.format("Invalid catalog from Resolver <%s>%n%s%n", catalogLocation, catFactory.getErrorMessage());
      result.fatalError = true;
      return null;
    }

    datasetId = location.substring(pos + 1);
    Dataset ds = catalog.findDatasetByID(datasetId);
    if (ds == null) {
      result.fatalError = true;
      result.errLog.format("Could not find dataset %s in %s %n", datasetId, catalogLocation);
      return null;
    }

    return ds;
  }

  /**
   * Try to open as a NetcdfDataset.
   *
   * @param Dataset open this
   * @param acquire if true, aquire the dataset, else open it
   * @param task may be null
   * @param log error message, may be null
   * @return NetcdfDataset or null if failure
   * @throws IOException on read error
   */
  public NetcdfDataset openDataset(Dataset Dataset, boolean acquire, CancelTask task, Formatter log)
      throws IOException {
    Result result = new Result();
    NetcdfDataset ncd = openDataset(Dataset, acquire, task, result);
    if (log != null)
      log.format("%s", result.errLog);
    if (result.fatalError) {
      result.close();
      if (ncd != null)
        ncd.close();
    }
    return (result.fatalError) ? null : ncd;
  }

  @Nullable
  private NetcdfDataset openDataset(Dataset dataset, boolean acquire, CancelTask task, Result result)
      throws IOException {

    IOException saveException = null;

    List<Access> accessList = new ArrayList<>(dataset.getAccess()); // a list of all the accesses
    while (!accessList.isEmpty()) {
      Access access = chooseDatasetAccess(accessList);

      // no valid access
      if (access == null) {
        result.errLog.format("No access that could be used in dataset %s %n", dataset);
        if (saveException != null) {
          throw saveException;
        }
        return null;
      }

      String datasetLocation = access.getStandardUrlName();
      ServiceType serviceType = access.getService().getType();
      if (debugOpen)
        System.out.println("ThreddsDataset.openDataset try " + datasetLocation + " " + serviceType);

      // deal with RESOLVER type
      if (serviceType == ServiceType.Resolver) {
        Dataset rds = openResolver(datasetLocation, task, result);
        if (rds == null)
          return null;
        accessList = new ArrayList<>(rds.getAccess());
        continue;
      }

      // ready to open it through netcdf API
      NetcdfDataset ds;

      // try to open
      try {
        ds = openDataset(access, acquire, task, result);

      } catch (IOException e) {
        result.errLog.format("Cant open %s %n err=%s%n", datasetLocation, e.getMessage());
        if (debugOpen) {
          System.out.println("Cant open= " + datasetLocation + " " + serviceType);
          e.printStackTrace();
        }
        String mess = e.getMessage();
        if (mess != null && mess.contains("Unauthorized")) {
          break; // bail out
        }

        accessList.remove(access);
        saveException = e;
        continue;
      }

      result.accessUsed = access;
      return ds;
    } // loop over accesses

    return null;
  }

  /**
   * Try to open Access as a NetcdfDataset.
   *
   * @param access open this Access
   * @param acquire if true, aquire the dataset, else open it
   * @param task may be null
   * @param log error message, may be null
   * @return NetcdfDataset or null if failure
   * @throws IOException on read error
   */
  public NetcdfDataset openDataset(Access access, boolean acquire, CancelTask task, Formatter log) throws IOException {
    try (Result result = new Result()) {
      NetcdfDataset ncd = openDataset(access, acquire, task, result);
      if (log != null)
        log.format("%s", result.errLog);
      if (result.fatalError && ncd != null)
        ncd.close();
      return (result.fatalError) ? null : ncd;

      // result.close() will be called at the end of this block, which will close the Result's FeatureDataset, if it
      // exists. That data member won't have been set by any of the code above, so the close() will essentially be a
      // no-op.
    }
  }

  private NetcdfDataset openDataset(Access access, boolean acquire, CancelTask task, Result result) throws IOException {
    Dataset ds = access.getDataset();

    String datasetLocation = access.getStandardUrlName();
    ServiceType serviceType = access.getService().getType();
    if (debugOpen)
      System.out.println("ThreddsDataset.openDataset= " + datasetLocation);

    // deal with RESOLVER type
    if (serviceType == ServiceType.Resolver) {
      Dataset rds = openResolver(datasetLocation, task, result);
      if (rds == null)
        return null;
      return openDataset(rds, acquire, task, result);
    }

    // ready to open it through netcdf API
    DatasetUrl durl = DatasetUrl.create(serviceType, datasetLocation);
    NetcdfDataset ncd = acquire ? NetcdfDatasets.acquireDataset(durl, true, task)
        : NetcdfDatasets.openDataset(durl, null, -1, task, null);

    result.accessUsed = access;
    return ncd;
  }

  /**
   * Find the "best" access in case theres more than one, based on what the CDM knows
   * how to open and use.
   *
   * @param accessList choose from this list.
   * @return best access method.
   */
  public Access chooseDatasetAccess(List<Access> accessList) {
    if (accessList.isEmpty())
      return null;

    Access access = null;
    if (preferAccess != null) {
      for (ServiceType type : preferAccess) {
        access = findAccessByServiceType(accessList, type);
        if (access != null)
          break;
      }
    }

    // the order indicates preference
    if (access == null)
      access = findAccessByServiceType(accessList, ServiceType.CdmRemote);
    if (access == null)
      access = findAccessByServiceType(accessList, ServiceType.DODS);
    if (access == null)
      access = findAccessByServiceType(accessList, ServiceType.OPENDAP);
    if (access == null)
      access = findAccessByServiceType(accessList, ServiceType.File); // can be opened through netcdf API
    if (access == null)
      access = findAccessByServiceType(accessList, ServiceType.HTTPServer); // should mean that it can be opened through
                                                                            // netcdf API

    /*
     * look for HTTP with format we can read
     * if (access == null) {
     * Access tryAccess = findAccessByServiceType(accessList, ServiceType.HTTPServer);
     * 
     * if (tryAccess != null) {
     * DataFormatType format = tryAccess.getDataFormatType();
     * 
     * // these are the file types we can read
     * if ((DataFormatType.NCML == format) || (DataFormatType.NETCDF == format)) { // removed 4/4/2015 jc
     * //if ((DataFormatType.BUFR == format) || (DataFormatType.GINI == format) || (DataFormatType.GRIB1 == format)
     * // || (DataFormatType.GRIB2 == format) || (DataFormatType.HDF5 == format) || (DataFormatType.NCML == format)
     * // || (DataFormatType.NETCDF == format) || (DataFormatType.NEXRAD2 == format) || (DataFormatType.NIDS == format))
     * {
     * access = tryAccess;
     * }
     * }
     * }
     */

    // ADDE
    if (access == null)
      access = findAccessByServiceType(accessList, ServiceType.ADDE);

    // RESOLVER
    if (access == null) {
      access = findAccessByServiceType(accessList, ServiceType.Resolver);
    }

    return access;
  }

  private Dataset openResolver(String urlString, CancelTask task, Result result) {
    CatalogBuilder catFactory = new CatalogBuilder();
    Catalog catalog = catFactory.buildFromLocation(urlString, null);
    if (catalog == null) {
      result.errLog.format("Couldnt open Resolver %s err=%s%n ", urlString, catFactory.getErrorMessage());
      return null;
    }

    for (Dataset ds : catalog.getDatasetsLocal()) {
      if (ds.hasAccess()) {
        return ds;
      }
      for (Dataset nested : ds.getDatasetsLocal()) // cant be more than one deep
        if (nested.hasAccess()) {
          return nested;
        }
    }

    return null;
  }

  ///////////////////////////////////////////////////////////////////

  // works against the accessList instead of the dataset list, so we can remove and try again

  private Access findAccessByServiceType(List<Access> accessList, ServiceType type) {
    for (Access a : accessList) {
      ServiceType stype = a.getService().getType();
      if (stype != null && stype.toString().equalsIgnoreCase(type.toString())) {
        return a;
      }
    }
    return null;
  }

  // works against the accessList instead of the dataset list, so we can remove and try again

  private Access findAccessByDataFormatType(List<Access> accessList, DataFormatType type) {
    for (Access a : accessList) {
      DataFormatType has = a.getDataFormatType();
      if (has != null && type.toString().equalsIgnoreCase(has.toString())) {
        return a;
      }
    }
    return null;
  }
}
