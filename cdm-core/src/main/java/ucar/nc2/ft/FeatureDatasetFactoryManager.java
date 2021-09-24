/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft;

import javax.annotation.Nullable;
import thredds.inventory.MFileCollectionManager;
import ucar.nc2.NetcdfFile;
import ucar.nc2.constants.CF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.ft.point.standard.PointDatasetStandardFactory;
import ucar.nc2.ft.point.collection.CompositeDatasetFactory;
import java.util.List;
import java.util.ArrayList;
import java.util.Formatter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ServiceLoader;

/**
 * Manager of factories for FeatureDatasets opened as NetcdfDatasets.
 * <p>
 * Grids, FMRC, Swaths are using GridDatasetStandardFactory
 * All point datasets are going through PointDatasetStandardFactory, which uses TableAnalyzer to deal
 * with specific dataset conventions.
 *
 * @author caron
 * @see FeatureDatasetFactory
 * @since Mar 19, 2008
 */
public class FeatureDatasetFactoryManager {
  private static final List<Factory> factoryList = new ArrayList<>();
  private static final boolean userMode;
  private static final boolean debug = false;

  // search in the order added
  static {
    // user can override
    for (FeatureDatasetFactory csb : ServiceLoader.load(FeatureDatasetFactory.class)) {
      registerFactory(csb.getClass());
    }

    registerFactory(FeatureType.ANY_POINT, PointDatasetStandardFactory.class);
    registerFactory(FeatureType.UGRID, "ucar.nc2.ft.ugrid.UGridDatasetStandardFactory");

    // further calls to registerFactory are by the user
    userMode = true;
  }

  /**
   * Register a class that implements a FeatureDatasetFactory.
   *
   * @param datatype scientific data type
   * @param className name of class that implements FeatureDatasetFactory.
   * @return true if successfully loaded
   */
  public static boolean registerFactory(FeatureType datatype, String className) {
    try {
      Class c = Class.forName(className);
      registerFactory(datatype, c);
      return true;

    } catch (ClassNotFoundException e) {
      // ok - these are optional
      return false;
    }
  }

  /**
   * Register a class that implements a FeatureDatasetFactory.
   *
   * @param datatype scientific data type
   * @param c class that implements FeatureDatasetFactory.
   */
  public static void registerFactory(FeatureType datatype, Class c) {
    if (!(FeatureDatasetFactory.class.isAssignableFrom(c)))
      throw new IllegalArgumentException("Class " + c.getName() + " must implement FeatureDatasetFactory");

    // fail fast - get Instance
    Object instance;
    try {
      instance = c.newInstance();
    } catch (InstantiationException e) {
      throw new IllegalArgumentException("FeatureDatasetFactoryManager Class " + c.getName()
          + " cannot instantiate, probably need default Constructor");
    } catch (IllegalAccessException e) {
      throw new IllegalArgumentException("FeatureDatasetFactoryManager Class " + c.getName() + " is not accessible");
    }

    // user stuff gets put at top
    if (userMode)
      factoryList.add(0, new Factory(datatype, c, (FeatureDatasetFactory) instance));
    else
      factoryList.add(new Factory(datatype, c, (FeatureDatasetFactory) instance));

  }

  /**
   * Register a class that implements a FeatureDatasetFactory.
   *
   * @param className name of class that implements FeatureDatasetFactory.
   * @throws ClassNotFoundException if loading error
   */
  public static void registerFactory(String className) throws ClassNotFoundException {
    Class c = Class.forName(className);
    registerFactory(c);
  }

  /**
   * Register a class that implements a FeatureDatasetFactory.
   * Find out which type by calling getFeatureType().
   *
   * @param c class that implements FeatureDatasetFactory.
   */
  public static void registerFactory(Class c) {

    if (!(FeatureDatasetFactory.class.isAssignableFrom(c)))
      throw new IllegalArgumentException("Class " + c.getName() + " must implement FeatureDatasetFactory");

    // fail fast - get Instance
    Object instance;
    try {
      instance = c.newInstance();
    } catch (InstantiationException e) {
      throw new IllegalArgumentException("FeatureDatasetFactoryManager Class " + c.getName()
          + " cannot instantiate, probably need default Constructor");
    } catch (IllegalAccessException e) {
      throw new IllegalArgumentException("FeatureDatasetFactoryManager Class " + c.getName() + " is not accessible");
    }

    // find out what type of Features
    try {
      Method m = c.getMethod("getFeatureTypes");
      FeatureType[] result = (FeatureType[]) m.invoke(instance, new Object[0]);
      for (FeatureType ft : result) {
        if (userMode)
          factoryList.add(0, new Factory(ft, c, (FeatureDatasetFactory) instance));
        else
          factoryList.add(new Factory(ft, c, (FeatureDatasetFactory) instance));
      }
    } catch (Exception ex) {
      throw new IllegalArgumentException(
          "FeatureDatasetFactoryManager Class " + c.getName() + " failed invoking getFeatureType()", ex);
    }
  }

  private static class Factory {
    FeatureType featureType;
    Class c;
    FeatureDatasetFactory factory;

    Factory(FeatureType featureType, Class c, FeatureDatasetFactory factory) {
      this.featureType = featureType;
      this.c = c;
      this.factory = factory;
    }

    @Override
    public String toString() {
      return "featureType=" + featureType + ", factory=" + factory.getClass();
    }
  }

  public static FeatureDataset open(String location) throws IOException {
    return open(null, location, null, new Formatter());
  }

  /**
   * @deprecated use open(FeatureType wantFeatureType, String location, ucar.nc2.util.CancelTask task, Formatter errlog)
   */
  public static FeatureDataset open(FeatureType wantFeatureType, String location, ucar.nc2.util.CancelTask task)
      throws IOException, NoFactoryFoundException {

    Formatter errlog = new Formatter();
    FeatureDataset fd = FeatureDatasetFactoryManager.open(wantFeatureType, location, task, errlog);

    if (fd == null) {
      throw new NoFactoryFoundException(errlog.toString());
    } else {
      return fd;
    }
  }

  /**
   * Open a dataset as a FeatureDataset.
   *
   * @param wantFeatureType open this kind of FeatureDataset; may be null, which means search all factories.
   *        If datatype is not null, only return correct FeatureDataset (eg PointFeatureDataset for DataType.POINT).
   * @param location URL or file location of the dataset. This may be a
   *        <ol>
   *        <li>thredds catalog#dataset (with a thredds: prefix)
   *        <li>cdmrFeature dataset (with a cdmrFeature: prefix)
   *        <li>cdmremote dataset (with a cdmremote: prefix)
   *        <li>collection dataset (with a collection: prefix)
   *        <li>file location for a CDM dataset opened with NetcdfDataset.acquireDataset()
   *        </ol>
   * @param task user may cancel
   * @param errlog place errors here, may not be null
   * @return a subclass of FeatureDataset, or null if no suitable factory was found, message in errlog
   * @throws java.io.IOException on io error
   */
  @Nullable
  public static FeatureDataset open(@Nullable FeatureType wantFeatureType, String location,
      ucar.nc2.util.CancelTask task, Formatter errlog) throws IOException {

    if (location.startsWith(ucar.nc2.ft.point.collection.CompositeDatasetFactory.SCHEME)) {
      String spec = location.substring(CompositeDatasetFactory.SCHEME.length());
      MFileCollectionManager dcm = MFileCollectionManager.open(spec, spec, null, errlog); // LOOK we dont have a name
      return CompositeDatasetFactory.factory(location, wantFeatureType, dcm, errlog);
    }

    DatasetUrl durl = DatasetUrl.findDatasetUrl(location); // Cache ServiceType so we don't have to keep figuring it out
    NetcdfDataset ncd = NetcdfDatasets.acquireDataset(durl, task); // Open with enhancements
    if (wantFeatureType != null && wantFeatureType.isPointFeatureType()) {
      ncd = NetcdfDatasets.addNetcdf3RecordStructure(ncd);
    }
    FeatureDataset fd = wrap(wantFeatureType, ncd, task, errlog);
    if (fd == null)
      ncd.close();
    return fd;
  }

  /**
   * Wrap a NetcdfDataset as a FeatureDataset.
   *
   * @param wantFeatureType open this kind of FeatureDataset; may be null, which means search all factories.
   *        If datatype is not null, only return FeatureDataset with objects of that type
   * @param ncd the NetcdfDataset to wrap as a FeatureDataset
   * @param task user may cancel
   * @param errlog place errors here, may not be null
   * @return a subclass of FeatureDataset, or null if no suitable factory was found
   * @throws java.io.IOException on io error
   */
  public static FeatureDataset wrap(@Nullable FeatureType wantFeatureType, NetcdfDataset ncd,
      ucar.nc2.util.CancelTask task, Formatter errlog) throws IOException {
    if (debug)
      System.out.println("wrap " + ncd.getLocation() + " want = " + wantFeatureType);

    // the case where we dont know what type it is
    if ((wantFeatureType == null) || (wantFeatureType == FeatureType.ANY)) {
      return wrapUnknown(ncd, task, errlog);
    }

    // find a Factory that claims this dataset by passing back an "analysis result" object
    Object analysis = null;
    FeatureDatasetFactory useFactory = null;
    for (Factory fac : factoryList) {
      if (!featureTypeOk(wantFeatureType, fac.featureType))
        continue;
      if (debug)
        System.out.println(" wrap try factory " + fac.factory.getClass().getName());

      analysis = fac.factory.isMine(wantFeatureType, ncd, errlog);
      if (analysis != null) {
        useFactory = fac.factory;
        break;
      }
    }

    if (null == useFactory) {
      errlog.format("**Failed to find FeatureDatasetFactory for= %s datatype=%s%n", ncd.getLocation(), wantFeatureType);
      return null;
    }

    // this call must be thread safe - done by implementation
    return useFactory.open(wantFeatureType, ncd, analysis, task, errlog);
  }

  private static FeatureDataset wrapUnknown(NetcdfDataset ncd, ucar.nc2.util.CancelTask task, Formatter errlog)
      throws IOException {
    FeatureType ft = findFeatureType(ncd);
    if (ft != null)
      return wrap(ft, ncd, task, errlog);

    // find a Factory that claims this dataset
    Object analysis = null;
    FeatureDatasetFactory useFactory = null;
    for (Factory fac : factoryList) {
      if (debug)
        System.out.println(" wrapUnknown try factory " + fac.factory.getClass().getName());

      analysis = fac.factory.isMine(null, ncd, errlog);
      if (null != analysis) {
        useFactory = fac.factory;
        break;
      }
    }

    // Fail
    if (null == useFactory) {
      errlog.format("Failed (wrapUnknown) to find Datatype Factory for= %s%n", ncd.getLocation());
      return null;
    }

    // this call must be thread safe - done by implementation
    return useFactory.open(null, ncd, analysis, task, errlog);
  }

  /**
   * Determine if factory type matches wanted feature type.
   *
   * @param want want this FeatureType
   * @param facType factory is of this type
   * @return true if match
   */
  public static boolean featureTypeOk(FeatureType want, FeatureType facType) {
    if (want == null)
      return true;
    if (want == facType)
      return true;

    if (want == FeatureType.ANY_POINT) {
      return facType.isPointFeatureType();
    }

    if (facType == FeatureType.ANY_POINT) {
      return want.isPointFeatureType();
    }

    if (want == FeatureType.COVERAGE) {
      return facType.isCoverageFeatureType();
    }

    if (want == FeatureType.GRID) { // for backwards compatibility
      return facType.isCoverageFeatureType();
    }

    if (want == FeatureType.SIMPLE_GEOMETRY) {
      return facType.isCoverageFeatureType();
    }

    if (want == FeatureType.UGRID) {
      return facType.isUnstructuredGridFeatureType();
    }

    return false;
  }

  /**
   * Try to determine the feature type of the dataset, by examining its metadata.
   *
   * @param ncd the dataset
   * @return FeatureType if found, else null
   */
  public static FeatureType findFeatureType(NetcdfFile ncd) {
    // search for explicit featureType global attribute
    String cdm_datatype = ncd.getRootGroup().findAttributeString(CF.FEATURE_TYPE, null);
    if (cdm_datatype == null)
      cdm_datatype = ncd.getRootGroup().findAttributeString("cdm_data_type", null);
    if (cdm_datatype == null)
      cdm_datatype = ncd.getRootGroup().findAttributeString("cdm_datatype", null);
    if (cdm_datatype == null)
      cdm_datatype = ncd.getRootGroup().findAttributeString("thredds_data_type", null);

    if (cdm_datatype != null) {
      for (FeatureType ft : FeatureType.values())
        if (cdm_datatype.equalsIgnoreCase(ft.name())) {
          if (debug)
            System.out.println(" wrapUnknown found cdm_datatype " + cdm_datatype);
          return ft;
        }
    }

    CF.FeatureType cff = CF.FeatureType.getFeatureTypeFromGlobalAttribute(ncd);
    if (cff != null)
      return CF.FeatureType.convert(cff);


    return null;
  }


}
