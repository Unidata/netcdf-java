/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.ncml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jdom2.Element;
import thredds.client.catalog.Catalog;
import thredds.inventory.MFile;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.util.CancelTask;

/**
 * "JoinExisting" Aggregation. Existing means that the dimension already exists in the component variables.
 * For every variable with that dimension, replace it with its "aggreation" whose dimensions lebgth is the
 * sum of the the component lengths. The dimensions must be the outer dimension.
 *
 * @author caron
 */
class AggregationExisting extends AggregationOuter {

  AggregationExisting(NetcdfDataset.Builder ncd, String dimName, String recheckS) {
    super(ncd, dimName, Type.joinExisting, recheckS);
  }

  protected void buildNetcdfDataset(CancelTask cancelTask) throws IOException {
    // open a "typical" nested dataset and copy it to newds
    AggDataset typicalDataset = getTypicalDataset();
    NetcdfFile typical = typicalDataset.acquireFile(null);
    BuilderHelper.transferDataset(typical, ncDataset, null);
    Group.Builder rootGroup = ncDataset.rootGroup;

    // LOOK not dealing with groups

    // a little tricky to get the coord var cached if we have to read through the datasets on the buildCoords()
    String dimName = getDimensionName();
    CacheVar coordCacheVar = null;
    if (type != Type.joinExistingOne) {
      Variable tcv = typical.findVariable(dimName);
      if (tcv != null) {
        coordCacheVar = new CoordValueVar(dimName, tcv.getDataType(), tcv.getUnitsString());
      } else {
        // docs claim we can add the coord variable in the outer ncml, so make a fake one for now, make
        // sure it gets removed if user adds it outside of aggregation.
        VariableDS.Builder fake = VariableDS.builder().setName(dimName).setDataType(DataType.INT)
            .setParentGroupBuilder(rootGroup).setDimensionsByName(dimName);
        fake.setAutoGen(0, 1);
        rootGroup.addVariable(fake);
        System.out.printf("HEY adding a fake coord var for %s%n", dimName);
      }

    } else {
      coordCacheVar = new CoordValueVar(dimName, DataType.STRING, "");
    }
    if (coordCacheVar != null) {
      cacheList.add(coordCacheVar); // coordinate variable is always cached
    }

    // gotta check persistence info - before buildCoords - if its going to do any good
    persistRead();

    // now find out how many coordinates we have, caching values if needed
    buildCoords(cancelTask);

    // create aggregation dimension, now that we know the size
    Dimension aggDim = new Dimension(dimName, getTotalCoords());
    rootGroup.removeDimension(dimName); // remove previous declaration, if any
    rootGroup.addDimension(aggDim);

    promoteGlobalAttributes((AggDatasetOuter) typicalDataset);

    // now create the agg variables
    // all variables with the named aggregation dimension
    for (Variable v : typical.getVariables()) {
      if (v.getRank() < 1) {
        continue;
      }
      // TODO whats with the full name ?
      // String outerName = v.getDimension(0).makeFullName();
      String outerName = v.getDimension(0).getShortName();
      if (!dimName.equals(outerName)) {
        continue;
      }

      // LOOK was Group.Builder newGroup = BuilderHelper.findGroup(rootGroup, v.getParentGroup());
      VariableDS.Builder vagg = VariableDS.builder().setName(v.getShortName()).setDataType(v.getDataType())
          .setParentGroupBuilder(rootGroup).setDimensionsByName(v.getDimensionsString());
      vagg.setProxyReader(this);
      BuilderHelper.transferAttributes(v.attributes(), vagg.getAttributeContainer());

      rootGroup.replaceVariable(vagg);
      aggVars.add(vagg);

      if (cancelTask != null && cancelTask.isCancel()) {
        return;
      }
    }

    // handle the agg coordinate variable
    Optional<Variable.Builder<?>> joinAggCoordOpt = rootGroup.findVariableLocal(dimName); // long name of dimension,
                                                                                          // coord
    // variable
    if (!joinAggCoordOpt.isPresent() && (type == Type.joinExisting)) {
      typicalDataset.close(typical); // clean up
      throw new IllegalArgumentException("No existing coordinate variable for joinExisting on " + getLocation());
    }
    VariableDS.Builder joinAggCoord = (VariableDS.Builder) joinAggCoordOpt.orElse(null);

    if (type == Type.joinExistingOne) {
      // replace aggregation coordinate variable
      joinAggCoordOpt.ifPresent(joinAgg -> rootGroup.removeVariable(joinAgg.shortName));

      joinAggCoord = VariableDS.builder().setName(dimName).setDataType(DataType.STRING).setParentGroupBuilder(rootGroup)
          .setDimensionsByName(dimName);
      joinAggCoord.setProxyReader(this);
      rootGroup.addVariable(joinAggCoord);
      aggVars.add(joinAggCoord);

      joinAggCoord.addAttribute(new Attribute(_Coordinate.AxisType, "Time"));
      joinAggCoord.addAttribute(new Attribute(CDM.LONG_NAME, "time coordinate"));
      joinAggCoord.addAttribute(new Attribute(CF.STANDARD_NAME, "time"));
    }

    if (timeUnitsChange && joinAggCoord != null) {
      readTimeCoordinates(joinAggCoord, cancelTask);
    }

    // make it a cacheVar
    if (joinAggCoord != null) {
      joinAggCoord.setSPobject(coordCacheVar);
    }

    // check persistence info - may have cached values other than coordinate LOOK ????
    persistRead();

    setDatasetAcquireProxy(typicalDataset, ncDataset);
    typicalDataset.close(typical);

    if (debugInvocation) {
      System.out.println(ncDataset.location + " invocation count = " + AggregationOuter.invocation);
    }

    // LOOK ncDataset.finish();
  }

  /**
   * Persist info (ncoords, coordValues) from joinExisting, since that can be expensive to
   * recreate.
   */
  public void persistWrite() throws IOException {
    if (diskCache2 == null) {
      return;
    }

    String cacheName = getCacheName();
    if (cacheName == null) {
      return;
    }
    if (cacheName.startsWith("file:")) { // LOOK HACK
      cacheName = cacheName.substring(5);
    }
    File cacheFile = diskCache2.getCacheFile(cacheName);
    if (cacheFile == null) {
      throw new IllegalStateException();
    }

    // only write out if something changed after the cache file was last written, or if the file has been deleted
    if (!cacheDirty && cacheFile.exists()) {
      return;
    }

    File dir = cacheFile.getParentFile();
    if (!dir.exists()) {
      if (!dir.mkdirs()) {
        logger.error("Cant make cache directory= " + cacheFile);
      }
    }

    // Get a file channel for the file
    try (FileOutputStream fos = new FileOutputStream(cacheFile);
        FileChannel channel = fos.getChannel();
        PrintWriter out = new PrintWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8))) {

      // Try acquiring the lock without blocking. This method returns
      // null or throws an exception if the file is already locked.
      FileLock lock;
      try {
        lock = channel.tryLock();
      } catch (OverlappingFileLockException e) {
        // File is already locked in this thread or virtual machine
        return; // give up
      }
      if (lock == null) {
        return;
      }

      out.print("<?xml version='1.0' encoding='UTF-8'?>\n");
      out.print("<aggregation xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2' version='3' ");
      out.print("type='" + type + "' ");
      if (dimName != null) {
        out.print("dimName='" + dimName + "' ");
      }
      if (datasetManager.getRecheck() != null) {
        out.print("recheckEvery='" + datasetManager.getRecheck() + "' ");
      }
      out.print(">\n");

      List<AggDataset> nestedDatasets = getDatasets();
      for (AggDataset dataset : nestedDatasets) {
        AggDatasetOuter dod = (AggDatasetOuter) dataset;
        if (dod.getId() == null) {
          logger.warn("id is null");
        }

        out.print("  <netcdf id='" + dod.getId() + "' ");
        out.print("ncoords='" + dod.getNcoords(null) + "' >\n");

        for (CacheVar pv : cacheList) {
          Array data = pv.getData(dod.getId());
          if (data != null) {
            out.print("    <cache varName='" + pv.varName + "' >");
            while (data.hasNext()) {
              out.printf("%s ", data.next());
            }
            out.print("</cache>\n");
            if (logger.isDebugEnabled()) {
              logger.debug(
                  " wrote array = " + pv.varName + " nelems= " + data.getSize() + " for " + dataset.getLocation());
            }
          }
        }
        out.print("  </netcdf>\n");
      }

      out.print("</aggregation>\n");

      long time = datasetManager.getLastScanned();
      if (time == 0) {
        time = System.currentTimeMillis(); // no scans (eg all static) will have a 0
      }

      if (!cacheFile.setLastModified(time)) {
        logger.warn("FAIL to set lastModified on {}", cacheFile.getPath());
      }
      cacheDirty = false;

      if (logger.isDebugEnabled()) {
        logger.debug("Aggregation persisted = " + cacheFile.getPath() + " lastModified= "
            + new Date(datasetManager.getLastScanned()));
      }
    }
  }

  // read info from the persistent XML file, if it exists
  protected void persistRead() {
    if (diskCache2 == null) {
      return;
    }

    String cacheName = getCacheName();
    if (cacheName == null) {
      return;
    }
    if (cacheName.startsWith("file:")) // LOOK
    {
      cacheName = cacheName.substring(5);
    }

    File cacheFile = diskCache2.getCacheFile(cacheName);
    if (cacheFile == null) {
      throw new IllegalStateException();
    }
    if (!cacheFile.exists()) {
      return;
    }
    long lastWritten = cacheFile.lastModified();

    if (logger.isDebugEnabled()) {
      logger.debug(" Try to Read cache {} " + cacheFile.getPath());
    }

    Element aggElem;
    try {
      aggElem = ucar.nc2.util.xml.Parse.readRootElement("file:" + cacheFile.getPath());
    } catch (IOException e) {
      if (debugCache) {
        System.out.println(" No cache for " + cacheName + " - " + e.getMessage());
      }
      return;
    }

    String version = aggElem.getAttributeValue("version");
    if (!"3".equals(version)) {
      return; // dont read old cache files, recreate
    }

    // use a map to find datasets to avoid O(n**2) searching
    Map<String, AggDataset> map = new HashMap<>();
    for (AggDataset ds : getDatasets()) {
      map.put(ds.getId(), ds);
    }

    List<Element> ncList = aggElem.getChildren("netcdf", Catalog.ncmlNS);
    for (Element netcdfElemNested : ncList) {
      String id = netcdfElemNested.getAttributeValue("id");
      AggDatasetOuter dod = (AggDatasetOuter) map.get(id);

      if (null == dod) {
        // this should mean that the dataset has been deleted. so not a problem
        if (logger.isDebugEnabled()) {
          logger.debug(" have cache but no dataset= {}", id);
        }
        continue;
      }
      if (logger.isDebugEnabled()) {
        logger.debug(" use cache for dataset= {}", id);
      }

      MFile mfile = dod.getMFile();
      if (mfile != null && mfile.getLastModified() > lastWritten) { // skip datasets that have changed
        if (logger.isDebugEnabled()) {
          logger.debug(" dataset was changed= {}", mfile);
        }
        continue;
      }

      if (dod.ncoord == 0) {
        String ncoordsS = netcdfElemNested.getAttributeValue("ncoords");
        try {
          dod.ncoord = Integer.parseInt(ncoordsS);
          if (logger.isDebugEnabled()) {
            logger.debug(" Read the cache; ncoords = {}", dod.ncoord);
          }
        } catch (NumberFormatException e) {
          logger.error("bad ncoord attribute on dataset=" + id);
        }
      }

      // if (dod.coordValue != null) continue; // allow ncml to override

      List<Element> cacheElemList = netcdfElemNested.getChildren("cache", Catalog.ncmlNS);
      for (Element cacheElemNested : cacheElemList) {
        String varName = cacheElemNested.getAttributeValue("varName");
        CacheVar pv = findCacheVariable(varName);
        if (pv != null) {
          String sdata = cacheElemNested.getText();
          if (sdata.isEmpty()) {
            continue;
          }
          if (logger.isDebugEnabled()) {
            logger.debug(" read data for var = " + varName + " size= " + sdata.length());
          }

          // long start = System.nanoTime();
          String[] vals = sdata.split(" ");

          try {
            Array data = Array.makeArray(pv.dtype, vals);
            pv.putData(id, data);
            countCacheUse++;

          } catch (Exception e) {
            logger.warn("Error reading cached data ", e);
          }

        } else {
          logger.warn("not a cache var=" + varName);
        }
      }
    }

  }

  // name to use in the DiskCache2 for the persistent XML info.
  // Document root is aggregation

  // has the name getCacheName()
  private String getCacheName() {
    String cacheName = ncDataset.location;
    // if (cacheName == null) { LOOK
    // cacheName = ncDataset.getCacheName();
    // }
    return cacheName;
  }

}
