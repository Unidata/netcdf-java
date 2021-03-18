/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.collection;

import javax.annotation.Nullable;
import org.jdom2.Element;
import thredds.client.catalog.Catalog;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionUpdateType;
import ucar.array.Arrays;
import ucar.nc2.*;
import ucar.nc2.grib.*;
import ucar.nc2.grib.coord.Coordinate;
import ucar.nc2.grib.coord.CoordinateTime2D;
import ucar.nc2.grib.grib2.Grib2Utils;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.util.CancelTask;
import ucar.unidata.io.RandomAccessFile;
import java.io.IOException;
import java.util.Formatter;
import java.util.List;

/**
 * Grib Collection IOSP. Handles both collections and single GRIB files.
 * Immutable after open() is called.
 */
public abstract class GribIosp extends AbstractIOServiceProvider {
  public static int debugIndexOnlyCount; // count number of data accesses

  // store custom tables in here
  protected final FeatureCollectionConfig config = new FeatureCollectionConfig();

  public void setParamTable(Element paramTable) {
    config.gribConfig.paramTable = paramTable;
  }

  public void setLookupTablePath(String lookupTablePath) {
    config.gribConfig.lookupTablePath = lookupTablePath;
  }

  public void setParamTablePath(String paramTablePath) {
    config.gribConfig.paramTablePath = paramTablePath;
  }

  @Override
  @Nullable
  public Object sendIospMessage(Object special) {
    if (special instanceof String) {
      String s = (String) special;
      if (s.startsWith("gribParameterTableLookup")) {
        int pos = s.indexOf("=");
        if (pos > 0) {
          config.gribConfig.lookupTablePath = s.substring(pos + 1).trim();
        }

      } else if (s.startsWith("gribParameterTable")) {
        int pos = s.indexOf("=");
        if (pos > 0) {
          config.gribConfig.paramTablePath = s.substring(pos + 1).trim();
        }
      }
      return null;
    }

    if (special instanceof org.jdom2.Element) { // the root element will be <iospParam>
      Element root = (org.jdom2.Element) special;
      config.gribConfig.configFromXml(root, Catalog.ncmlNS);
      return null;
    }

    return super.sendIospMessage(special);
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  protected final boolean isGrib1;
  protected final org.slf4j.Logger logger;

  protected GribCollectionImmutable gribCollection;
  protected GribCollectionImmutable.GroupGC gHcs;
  protected GribCollectionImmutable.Type gtype; // only used if gHcs was set
  protected boolean isPartitioned;
  protected boolean owned; // if Iosp is owned by GribCollection; affects close() LOOK get rid of this
  protected ucar.nc2.grib.GribTables gribTable;

  public GribIosp(boolean isGrib1, org.slf4j.Logger logger) {
    this.isGrib1 = isGrib1;
    this.logger = logger;
  }

  protected abstract ucar.nc2.grib.GribTables createCustomizer() throws IOException;

  protected abstract String makeVariableName(GribCollectionImmutable.VariableIndex vindex);

  protected abstract String makeVariableLongName(GribCollectionImmutable.VariableIndex vindex);

  protected abstract String makeVariableUnits(GribCollectionImmutable.VariableIndex vindex);

  protected abstract String getVerticalCoordDesc(int vc_code);

  protected abstract GribTables.Parameter getParameter(GribCollectionImmutable.VariableIndex vindex);

  @Override
  public void build(RandomAccessFile raf, Group.Builder rootGroup, CancelTask cancelTask) throws IOException {
    super.open(raf, rootGroup.getNcfile(), cancelTask);

    if (gHcs != null) { // just use the one group that was set in the constructor
      this.gribCollection = gHcs.getGribCollection();
      if (this.gribCollection instanceof PartitionCollectionImmutable) {
        isPartitioned = true;
      }
      gribTable = createCustomizer();
      GribIospBuilder helper = new GribIospBuilder(this, isGrib1, logger, gribCollection, gribTable);

      helper.addGroup(rootGroup, gHcs, gtype, false);

    } else if (gribCollection == null) { // may have been set in the constructor

      this.gribCollection =
          GribCdmIndex.openGribCollectionFromRaf(raf, config, CollectionUpdateType.testIndexOnly, logger);
      if (gribCollection == null) {
        throw new IllegalStateException("Not a GRIB data file or index file " + raf.getLocation());
      }

      isPartitioned = (this.gribCollection instanceof PartitionCollectionImmutable);
      gribTable = createCustomizer();
      GribIospBuilder helper = new GribIospBuilder(this, isGrib1, logger, gribCollection, gribTable);

      boolean useDatasetGroup = gribCollection.getDatasets().size() > 1;
      for (GribCollectionImmutable.Dataset ds : gribCollection.getDatasets()) {
        Group.Builder topGroup;
        if (useDatasetGroup) {
          topGroup = Group.builder().setName(ds.getType().toString());
          rootGroup.addGroup(topGroup);
        } else {
          topGroup = rootGroup;
        }

        Iterable<GribCollectionImmutable.GroupGC> groups = ds.getGroups();
        boolean useGroups = ds.getGroupsSize() > 1;
        for (GribCollectionImmutable.GroupGC g : groups) {
          helper.addGroup(topGroup, g, ds.getType(), useGroups);
        }
      }
    }

    for (Attribute att : gribCollection.getGlobalAttributes()) {
      rootGroup.addAttribute(att);
    }
  }

  enum Time2DinfoType {
    off, offU, intv, intvU, bounds, boundsU, is1Dtime, isUniqueRuntime, reftime, timeAuxRef
  }

  static class Time2Dinfo {
    final Time2DinfoType which;
    final CoordinateTime2D time2D;
    final Coordinate time1D;

    Time2Dinfo(Time2DinfoType which, CoordinateTime2D time2D, Coordinate time1D) {
      this.which = which;
      this.time2D = time2D;
      this.time1D = time1D;
    }
  }

  @Nullable
  String searchCoord(Grib2Utils.LatLonCoordType type, List<GribCollectionImmutable.VariableIndex> list) {
    if (type == null) {
      return null;
    }

    GribCollectionImmutable.VariableIndex lat, lon;
    switch (type) {
      case U:
        lat = findParameter(list, 198);
        lon = findParameter(list, 199);
        return (lat != null && lon != null) ? makeVariableName(lat) + " " + makeVariableName(lon) : null;
      case V:
        lat = findParameter(list, 200);
        lon = findParameter(list, 201);
        return (lat != null && lon != null) ? makeVariableName(lat) + " " + makeVariableName(lon) : null;
      case P:
        lat = findParameter(list, 202);
        lon = findParameter(list, 203);
        return (lat != null && lon != null) ? makeVariableName(lat) + "  " + makeVariableName(lon) : null;
    }
    return null;
  }

  @Nullable
  private GribCollectionImmutable.VariableIndex findParameter(List<GribCollectionImmutable.VariableIndex> list, int p) {
    for (GribCollectionImmutable.VariableIndex vindex : list) {
      if ((vindex.getDiscipline() == 0) && (vindex.getCategory() == 2) && (vindex.getParameter() == p)) {
        return vindex;
      }
    }
    return null;
  }

  @Override
  public void close() throws java.io.IOException {
    if (!owned && gribCollection != null) // LOOK klugerino
    {
      gribCollection.close();
    }
    gribCollection = null;
    super.close();
  }

  @Override
  public String getDetailInfo() {
    Formatter f = new Formatter();
    f.format("%s", super.getDetailInfo());
    if (gribCollection != null) {
      gribCollection.showIndex(f);
    }
    return f.toString();
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public ucar.ma2.Array readData(Variable v2, ucar.ma2.Section section)
      throws IOException, ucar.ma2.InvalidRangeException {
    // see if its time2D - then generate data on the fly
    if (v2.getSPobject() instanceof Time2Dinfo) {
      Time2Dinfo info = (Time2Dinfo) v2.getSPobject();
      ucar.ma2.Array data = Time2DLazyCoordinate.makeLazyCoordinateData(v2, info, gribCollection);
      ucar.ma2.Section sectionFilled = ucar.ma2.Section.fill(section, v2.getShape());
      return data.sectionNoReduce(sectionFilled.getRanges());
    }

    try {
      ucar.ma2.Array result;
      GribCollectionImmutable.VariableIndex vindex = (GribCollectionImmutable.VariableIndex) v2.getSPobject();
      GribDataReader dataReader = GribDataReader.factory(gribCollection, vindex);
      ucar.ma2.SectionIterable sectionIter = new ucar.ma2.SectionIterable(section, v2.getShape());
      result = dataReader.readData(sectionIter);

      return result;

    } catch (IOException ioe) {
      logger.error("Failed to readData ", ioe);
      throw ioe;
    }
  }

  @Override
  public ucar.array.Array<?> readArrayData(Variable v2, ucar.array.Section section)
      throws java.io.IOException, ucar.array.InvalidRangeException {
    // see if its time2D - then generate data on the fly
    if (v2.getSPobject() instanceof Time2Dinfo) {
      Time2Dinfo info = (Time2Dinfo) v2.getSPobject();
      ucar.array.Array<?> data = Time2DLazyCoordinate.makeLazyCoordinateArray(v2, info, gribCollection);
      ucar.array.Section sectionFilled = ucar.array.Section.fill(section, v2.getShape());
      return Arrays.section(data, sectionFilled.getRanges());
    }

    try {
      GribCollectionImmutable.VariableIndex vindex = (GribCollectionImmutable.VariableIndex) v2.getSPobject();
      GribArrayReader dataReader = GribArrayReader.factory(gribCollection, vindex);
      ucar.array.SectionIterable sectionIter = new ucar.array.SectionIterable(section, v2.getShape());
      return dataReader.readData(sectionIter);

    } catch (IOException ioe) {
      logger.error("Failed to readData ", ioe);
      throw ioe;
    }
  }

  ///////////////////////////////////////
  // debugging back door
  public abstract Object getLastRecordRead();

  public abstract void clearLastRecordRead();

  public abstract Object getGribCustomizer();
}
