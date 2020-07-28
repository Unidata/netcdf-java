/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.iosp.hdf4;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import org.jdom2.Element;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainerMutable;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.internal.dataset.CoordSystemBuilder;

/**
 * Parse structural metadata from HDF-EOS.
 * This allows us to use shared dimensions, identify Coordinate Axes, and the FeatureType.
 * <p>
 * <p>
 * from HDF-EOS.status.ppt:
 * 
 * <pre>
 * HDF-EOS is format for EOS  Standard Products
 * <ul>
 * <li>Landsat 7 (ETM+)
 * <li>Terra (CERES, MISR, MODIS, ASTER, MOPITT)
 * <li>Meteor-3M (SAGE III)
 * <li>Aqua (AIRS, AMSU-A, AMSR-E, CERES, MODIS)
 * <li>Aura(MLS, TES, HIRDLS, OMI
 * </ul>
 * HDF is used by other EOS missions
 * <ul>
 * <li>OrbView 2 (SeaWIFS)
 * <li>TRMM (CERES, VIRS, TMI, PR)
 * <li>Quickscat (SeaWinds)
 * <li>EO-1 (Hyperion, ALI)
 * <li>ICESat (GLAS)
 * <li>Calypso
 * </ul>
 * </pre>
 * </p>
 *
 * @author caron
 * @since Jul 23, 2007
 */
public class HdfEos {
  public static final String HDF5_GROUP = "HDFEOS_INFORMATION";
  public static final String HDFEOS_CRS = "_HDFEOS_CRS";
  public static final String HDFEOS_CRS_Projection = "Projection";
  public static final String HDFEOS_CRS_UpperLeft = "UpperLeftPointMtrs";
  public static final String HDFEOS_CRS_LowerRight = "LowerRightMtrs";
  public static final String HDFEOS_CRS_ProjParams = "ProjParams";
  public static final String HDFEOS_CRS_SphereCode = "SphereCode";

  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HdfEos.class);
  static boolean showWork; // set in debug
  private static final String GEOLOC_FIELDS = "Geolocation Fields";
  private static final String GEOLOC_FIELDS2 = "Geolocation_Fields";
  private static final String DATA_FIELDS = "Data Fields";
  private static final String DATA_FIELDS2 = "Data_Fields";

  /**
   * Amend the given NetcdfFile with metadata from HDF-EOS structMetadata.
   * All Variables named StructMetadata.n, where n= 1, 2, 3 ... are read in and their contents concatenated
   * to make the structMetadata String.
   *
   * @param eosGroup the group containing variables named StructMetadata.*
   * @return true if HDF-EOS info was found
   * @throws IOException on read error
   */
  public static boolean amendFromODL(String location, HdfHeaderIF header, Group.Builder eosGroup) throws IOException {
    String smeta = getStructMetadata(header, eosGroup);
    if (smeta == null) {
      return false;
    }

    HdfEos fixer = new HdfEos(location, header);
    fixer.fixAttributes(eosGroup);
    fixer.amendFromODL(header.getRootGroup(), smeta);
    return true;
  }

  public static boolean getEosInfo(String location, HdfHeaderIF header, Group.Builder eosGroup, Formatter f)
      throws IOException {
    String smeta = getStructMetadata(header, eosGroup);
    if (smeta == null) {
      f.format("No StructMetadata variables in group %s %n", eosGroup.shortName);
      return false;
    }
    f.format("raw = %n%s%n", smeta);
    ODLparser parser = new ODLparser();
    parser.parseFromString(smeta); // now we have the ODL as JDOM elements
    StringWriter sw = new StringWriter(5000);
    parser.showDoc(new PrintWriter(sw));
    f.format("parsed = %n%s%n", sw.toString());
    return true;
  }

  private static String getStructMetadata(HdfHeaderIF header, Group.Builder eosGroup) throws IOException {
    StringBuilder sbuff = null;
    String structMetadata = null;

    int n = 0;
    while (true) {
      Variable.Builder<?> structMetadataVar = eosGroup.findVariableLocal("StructMetadata." + n).orElse(null);
      if (structMetadataVar == null) {
        break;
      }
      if (structMetadata != null && sbuff == null) { // more than 1 StructMetadata
        sbuff = new StringBuilder(64000);
        sbuff.append(structMetadata);
      }

      // Since we dont have a Variable yet, we have to do something low-level
      structMetadata = header.readStructMetadata(structMetadataVar);

      if (sbuff != null) {
        sbuff.append(structMetadata);
      }
      n++;
    }
    return (sbuff != null) ? sbuff.toString() : structMetadata;
  }

  ///////////////////////////////////////////
  private String location; // for debug messages
  private HdfHeaderIF header;

  private HdfEos(String location, HdfHeaderIF header) {
    this.location = location;
    this.header = header;
  }

  /**
   * Amend the given NetcdfFile with metadata from HDF-EOS structMetadata
   *
   * @param rootg Amend this
   * @param structMetadata structMetadata as String
   */
  private void amendFromODL(Group.Builder rootg, String structMetadata) {
    ODLparser parser = new ODLparser();
    Element root = parser.parseFromString(structMetadata); // now we have the ODL in JDOM elements
    FeatureType featureType = null;

    // SWATH
    Element swathStructure = root.getChild("SwathStructure");
    if (swathStructure != null) {
      List<Element> swaths = swathStructure.getChildren();
      for (Element elemSwath : swaths) {
        Element swathNameElem = elemSwath.getChild("SwathName");
        if (swathNameElem == null) {
          log.warn("No SwathName element in {} {} ", elemSwath.getName(), location);
          continue;
        }
        String swathName = NetcdfFiles.makeValidCdmObjectName(swathNameElem.getText().trim());
        Group.Builder swathGroup = findGroupNested(rootg, swathName);
        // if (swathGroup == null)
        // swathGroup = findGroupNested(rootg, HdfHeaderIF.createValidObjectName(swathName));

        if (swathGroup != null) {
          featureType = amendSwath(elemSwath, swathGroup);
        } else {
          log.warn("Cant find swath group {} {}", swathName, location);
        }
      }
    }

    // GRID
    Element gridStructure = root.getChild("GridStructure");
    if (gridStructure != null) {
      List<Element> grids = gridStructure.getChildren();
      for (Element elemGrid : grids) {
        Element gridNameElem = elemGrid.getChild("GridName");
        if (gridNameElem == null) {
          log.warn("No GridName element in {} {} ", elemGrid.getName(), location);
          continue;
        }
        String gridName = NetcdfFiles.makeValidCdmObjectName(gridNameElem.getText().trim());
        Group.Builder gridGroup = findGroupNested(rootg, gridName);
        // if (gridGroup == null)
        // gridGroup = findGroupNested(rootg, HdfHeaderIF.createValidObjectName(gridName));
        if (gridGroup != null) {
          featureType = amendGrid(elemGrid, gridGroup, location);
        } else {
          log.warn("Cant find Grid group {} {}", gridName, location);
        }
      }
    }

    // POINT - NOT DONE YET
    Element pointStructure = root.getChild("PointStructure");
    if (pointStructure != null) {
      List<Element> pts = pointStructure.getChildren();
      for (Element elem : pts) {
        Element nameElem = elem.getChild("PointName");
        if (nameElem == null) {
          log.warn("No PointName element in {} {}", elem.getName(), location);
          continue;
        }
        String name = nameElem.getText().trim();
        Group.Builder ptGroup = findGroupNested(rootg, name);
        // if (ptGroup == null)
        // ptGroup = findGroupNested(rootg, HdfHeaderIF.createValidObjectName(name));
        if (ptGroup != null) {
          featureType = FeatureType.POINT;
        } else {
          log.warn("Cant find Point group {} {}", name, location);
        }
      }
    }

    if (featureType != null) {
      if (showWork) {
        log.debug("***EOS featureType= {}", featureType);
      }
      rootg.addAttribute(new Attribute(CF.FEATURE_TYPE, featureType.toString()));
      // rootg.addAttribute(new Attribute(CDM.CONVENTIONS, "HDFEOS"));
    }

  }

  private FeatureType amendSwath(Element swathElem, Group.Builder parent) {
    FeatureType featureType = FeatureType.SWATH;
    List<Dimension> unknownDims = new ArrayList<>();

    // Dimensions
    Element d = swathElem.getChild("Dimension");
    List<Element> dims = d.getChildren();
    for (Element elem : dims) {
      String name = elem.getChild("DimensionName").getText().trim();
      name = NetcdfFiles.makeValidCdmObjectName(name);

      if (name.equalsIgnoreCase("scalar")) {
        continue;
      }
      String sizeS = elem.getChild("Size").getText().trim();
      int length = Integer.parseInt(sizeS);
      if (length > 0) {
        Dimension dim = parent.findDimensionLocal(name).orElse(null);
        if (dim != null) { // already added - may be dimension scale ?
          if (dim.getLength() != length) { // ok as long as it matches
            log.error("Conflicting Dimensions = {} != {} in location {}", dim, length, location);
            // Assume that the ODL is incorrect.
            // throw new IllegalStateException("Conflicting Dimensions = " + name);
          }
        } else {
          dim = new Dimension(name, length);
          if (parent.addDimensionIfNotExists(dim)) {
            if (showWork)
              log.debug(" Add dimension {}", dim);
          }
        }
      } else {
        log.warn("Dimension {} has size {} {}", name, sizeS, location);
        Dimension udim = new Dimension(name, 1);
        unknownDims.add(udim);
        if (showWork) {
          log.debug(" Add dimension {}", udim);
        }
      }
    }

    // Dimension Maps
    Element dmap = swathElem.getChild("DimensionMap");
    List<Element> dimMaps = dmap.getChildren();
    for (Element elem : dimMaps) {
      String geoDimName = elem.getChild("GeoDimension").getText().trim();
      geoDimName = NetcdfFiles.makeValidCdmObjectName(geoDimName);
      String dataDimName = elem.getChild("DataDimension").getText().trim();
      dataDimName = NetcdfFiles.makeValidCdmObjectName(dataDimName);

      String offsetS = elem.getChild("Offset").getText().trim();
      String incrS = elem.getChild("Increment").getText().trim();
      int offset = Integer.parseInt(offsetS);
      int incr = Integer.parseInt(incrS);

      // make new variable for this dimension map
      Variable.Builder v = Variable.builder().setName(dataDimName);
      parent.addVariable(v);
      v.setDimensionsByName(geoDimName);
      v.setDataType(DataType.INT);
      v.setAutoGen(offset, incr);
      v.addAttribute(new Attribute("_DimensionMap", ""));
      header.makeVinfoForDimensionMapVariable(parent, v);

      if (showWork) {
        log.debug(" Add dimensionMap {}", v);
      }
    }

    // Geolocation Variables
    Group.Builder geoFieldsG =
        parent.findGroupLocal(GEOLOC_FIELDS).orElse(parent.findGroupLocal(GEOLOC_FIELDS2).orElse(null));
    if (geoFieldsG != null) {
      Variable.Builder<?> latAxis = null;
      Variable.Builder<?> lonAxis = null;
      Variable.Builder<?> timeAxis = null;

      Element floc = swathElem.getChild("GeoField");
      for (Element elem : floc.getChildren()) {
        String varname = elem.getChild("GeoFieldName").getText().trim();
        Variable.Builder<?> vb = geoFieldsG.findVariableLocal(varname).orElse(null);
        if (vb != null) {
          AxisType axis = addAxisType(vb);
          if (axis == AxisType.Lat) {
            latAxis = vb;
          }
          if (axis == AxisType.Lon) {
            lonAxis = vb;
          }
          if (axis == AxisType.Time) {
            timeAxis = vb;
          }

          Element dimList = elem.getChild("DimList");
          List<Element> values = dimList.getChildren("value");
          setSharedDimensions(geoFieldsG, vb, values, unknownDims, location);
          if (showWork) {
            log.debug(" set coordinate {}", vb);
          }
        }

        // Treat possibility that this is a discrete geometry featureType.
        // We check if lat and lon axes are 2D and if not (1) see if it looks like a
        // trajectory, or (2) otherwise tag it as a profile.
        // This could/should be expanded to consider other FTs.
        if ((latAxis != null) && (lonAxis != null)) {
          log.debug("found lonAxis and latAxis -- testing XY domain");
          int xyDomainSize = CoordSystemBuilder.countDomainSize(latAxis, lonAxis);
          log.debug("xyDomain size {}", xyDomainSize);
          if (xyDomainSize < 2) {
            if (timeAxis != null) {
              log.debug("found timeAxis -- testing if trajectory");
              String dd1 = timeAxis.getFirstDimensionName();
              String dd2 = latAxis.getFirstDimensionName();
              String dd3 = lonAxis.getFirstDimensionName();

              if (dd1.equals(dd2) && dd1.equals(dd3)) {
                featureType = FeatureType.TRAJECTORY;
              } else {
                featureType = FeatureType.PROFILE; // ??
              }
            } else {
              featureType = FeatureType.PROFILE; // ??
            }
          }
        }
      }
    }

    // Data Variables
    Group.Builder dataG = parent.findGroupLocal(DATA_FIELDS).orElse(parent.findGroupLocal(DATA_FIELDS2).orElse(null));
    if (dataG != null) {
      Element f = swathElem.getChild("DataField");
      List<Element> vars = f.getChildren();
      for (Element elem : vars) {
        Element dataFieldNameElem = elem.getChild("DataFieldName");
        if (dataFieldNameElem == null) {
          continue;
        }
        String varname = NetcdfFiles.makeValidCdmObjectName(dataFieldNameElem.getText().trim());
        Variable.Builder v = dataG.findVariableLocal(varname).orElse(null);
        if (v == null) {
          log.error("Cant find variable {} {}", varname, location);
          continue;
        }
        Element dimList = elem.getChild("DimList");
        List<Element> values = dimList.getChildren("value");
        setSharedDimensions(dataG, v, values, unknownDims, location);
      }
    }

    return featureType;
  }

  private AxisType addAxisType(Variable.Builder v) {
    String name = v.shortName;
    if (name.equalsIgnoreCase("Latitude") || name.equalsIgnoreCase("GeodeticLatitude")) {
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));
      v.addAttribute(new Attribute(CDM.UNITS, CDM.LAT_UNITS));
      return AxisType.Lat;

    } else if (name.equalsIgnoreCase("Longitude")) {
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));
      v.addAttribute(new Attribute(CDM.UNITS, CDM.LON_UNITS));
      return AxisType.Lon;

    } else if (name.equalsIgnoreCase("Time")) {
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));
      if (v.getAttributeContainer().findAttribute(CDM.UNITS) == null) {
        /*
         * from http://newsroom.gsfc.nasa.gov/sdptoolkit/hdfeosfaq.html
         * HDF-EOS uses the TAI93 (International Atomic Time) format. This means that time is stored as the number of
         * elapsed seconds since January 1, 1993 (negative values represent times prior to this date).
         * An 8 byte floating point number is used, producing microsecond accuracy from 1963 (when leap second records
         * became available electronically) to 2100. The SDP Toolkit provides conversions from other date formats to and
         * from TAI93. Other representations of time can be entered as ancillary data, if desired.
         * For lists and descriptions of other supported time formats, consult the Toolkit documentation or write to
         * landover_PGSTLKIT@raytheon.com.
         */
        v.addAttribute(new Attribute(CDM.UNITS, "seconds since 1993-01-01T00:00:00Z"));
        v.addAttribute(new Attribute(CF.CALENDAR, "TAI"));
        /*
         * String tit = ncfile.findAttValueIgnoreCase(v, "Title", null);
         * if (tit != null && tit.contains("TAI93")) {
         * // Time is given in the TAI-93 format, i.e. the number of seconds passed since 01-01-1993, 00:00 UTC.
         * v.addAttribute(new Attribute(CDM.UNITS, "seconds since 1993-01-01T00:00:00Z"));
         * v.addAttribute(new Attribute(CF.CALENDAR, "TAI"));
         * } else { // who the hell knows ??
         * v.addAttribute(new Attribute(CDM.UNITS, "seconds since 1970-01-01T00:00:00Z"));
         * }
         */
      }
      return AxisType.Time;

    } else if (name.equalsIgnoreCase("Pressure")) {
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Pressure.toString()));
      return AxisType.Pressure;

    } else if (name.equalsIgnoreCase("Altitude")) {
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Height.toString()));
      v.addAttribute(new Attribute(CF.POSITIVE, CF.POSITIVE_UP)); // probably
      return AxisType.Height;
    }

    return null;
  }

  // Use the ODL to amend the netcdf file
  private FeatureType amendGrid(Element gridElem, Group.Builder parentGroupBuilder, String location) {
    List<Dimension> unknownDims = new ArrayList<>();

    // always has x and y dimension
    String xdimSizeS = gridElem.getChild("XDim").getText().trim();
    String ydimSizeS = gridElem.getChild("YDim").getText().trim();
    int xdimSize = Integer.parseInt(xdimSizeS);
    int ydimSize = Integer.parseInt(ydimSizeS);
    parentGroupBuilder.addDimensionIfNotExists(new Dimension("XDim", xdimSize));
    parentGroupBuilder.addDimensionIfNotExists(new Dimension("YDim", ydimSize));

    /*
     * see HdfEosModisConvention
     * UpperLeftPointMtrs=(-20015109.354000,1111950.519667)
     * LowerRightMtrs=(-18903158.834333,-0.000000)
     * Projection=GCTP_SNSOID
     * ProjParams=(6371007.181000,0,0,0,0,0,0,0,0,0,0,0,0)
     * SphereCode=-1
     */
    Element proj = gridElem.getChild("Projection");
    if (proj != null) {
      Variable.Builder crs = Variable.builder().setName(HDFEOS_CRS);
      crs.setDataType(DataType.SHORT);
      crs.setIsScalar();
      crs.setAutoGen(0, 0); // fake data
      parentGroupBuilder.addVariable(crs);

      addAttributeIfExists(gridElem, HDFEOS_CRS_Projection, crs, false);
      addAttributeIfExists(gridElem, HDFEOS_CRS_UpperLeft, crs, true);
      addAttributeIfExists(gridElem, HDFEOS_CRS_LowerRight, crs, true);
      addAttributeIfExists(gridElem, HDFEOS_CRS_ProjParams, crs, true);
      addAttributeIfExists(gridElem, HDFEOS_CRS_SphereCode, crs, false);
    }

    // global Dimensions
    Element d = gridElem.getChild("Dimension");
    List<Element> dims = d.getChildren();
    for (Element elem : dims) {
      String name = elem.getChild("DimensionName").getText().trim();
      name = NetcdfFiles.makeValidCdmObjectName(name);
      if (name.equalsIgnoreCase("scalar")) {
        continue;
      }

      String sizeS = elem.getChild("Size").getText().trim();
      int length = Integer.parseInt(sizeS);
      Dimension old = parentGroupBuilder.findDimension(name).orElse(null);
      if ((old == null) || (old.getLength() != length)) {
        if (length > 0) {
          Dimension dim = new Dimension(name, length);
          if (parentGroupBuilder.addDimensionIfNotExists(dim) && showWork) {
            log.debug(" Add dimension {}", dim);
          }
        } else {
          log.warn("Dimension {} has size {} {} ", sizeS, name, location);
          Dimension udim = new Dimension(name, 1);
          unknownDims.add(udim);
          if (showWork) {
            log.debug(" Add unknown dimension {}", udim);
          }
        }
      }
    }

    // Geolocation Variables
    Group.Builder geoFieldsG = parentGroupBuilder.findGroupLocal(GEOLOC_FIELDS)
        .orElse(parentGroupBuilder.findGroupLocal(GEOLOC_FIELDS2).orElse(null));
    if (geoFieldsG != null) {
      Element floc = gridElem.getChild("GeoField");
      List<Element> varsLoc = floc.getChildren();
      for (Element elem : varsLoc) {
        String varname = elem.getChild("GeoFieldName").getText().trim();
        geoFieldsG.findVariableLocal(varname).ifPresent(vb -> {
          Element dimList = elem.getChild("DimList");
          List<Element> values = dimList.getChildren("value");
          setSharedDimensions(geoFieldsG, vb, values, unknownDims, location);
        });
      }
    }

    // Data Variables
    Group.Builder dataG = parentGroupBuilder.findGroupLocal(DATA_FIELDS)
        .orElse(parentGroupBuilder.findGroupLocal(DATA_FIELDS2).orElse(null));
    if (dataG != null) {
      Element f = gridElem.getChild("DataField");
      List<Element> vars = f.getChildren();
      for (Element elem : vars) {
        String varname = elem.getChild("DataFieldName").getText().trim();
        varname = NetcdfFiles.makeValidCdmObjectName(varname);
        dataG.findVariableLocal(varname).ifPresent(vb -> {
          Element dimList = elem.getChild("DimList");
          List<Element> values = dimList.getChildren("value");
          setSharedDimensions(dataG, vb, values, unknownDims, location);
        });
      }

      // get projection
      String projS = null;
      Element projElem = gridElem.getChild("Projection");
      if (projElem != null) {
        projS = projElem.getText().trim();
      }
      boolean isLatLon = "GCTP_GEO".equals(projS);

      // look for XDim, YDim coordinate variables
      if (isLatLon) {
        for (Variable.Builder v : dataG.vbuilders) {
          if (CoordSystemBuilder.isCoordinateVariable(v)) {
            if (v.shortName.equals("YDim")) {
              v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));
              v.addAttribute(new Attribute(CDM.UNITS, CDM.LAT_UNITS));
            }
            if (v.shortName.equals("XDim")) {
              v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));
              v.addAttribute(new Attribute(CDM.UNITS, CDM.LON_UNITS));
            }
          }
        }
      }
    }
    return FeatureType.GRID;
  }

  private void addAttributeIfExists(Element elem, String name, Variable.Builder<?> v, boolean isDoubleArray) {
    Element child = elem.getChild(name);
    if (child == null) {
      return;
    }
    if (isDoubleArray) {
      List<Element> vElems = child.getChildren();
      List<Double> values = new ArrayList<>();
      for (Element ve : vElems) {
        String valueS = ve.getText().trim();
        try {
          values.add(Double.parseDouble(valueS));
        } catch (NumberFormatException e) {
          log.warn("Cant parse double value " + valueS);
        }
      }
      Attribute att = Attribute.builder(name).setValues((List) values, false).build();
      v.addAttribute(att);
    } else {
      String value = child.getText().trim();
      Attribute att = new Attribute(name, value);
      v.addAttribute(att);
    }
  }

  // convert to shared dimensions
  private void setSharedDimensions(Group.Builder group, Variable.Builder<?> v, List<Element> values,
      List<Dimension> unknownDims, String location) {
    if (values.isEmpty()) {
      return;
    }

    // remove the "scalar" dumbension
    Iterator<Element> iter = values.iterator();
    while (iter.hasNext()) {
      Element value = iter.next();
      String dimName = value.getText().trim();
      if (dimName.equalsIgnoreCase("scalar")) {
        iter.remove();
      }
    }

    // gotta have same number of dimensions
    List<Dimension> oldDims = v.getDimensions();
    if (oldDims.size() != values.size()) {
      log.error("Different number of dimensions for {} {}", v.shortName, location);
      return;
    }

    List<Dimension> newDims = new ArrayList<>();

    for (int i = 0; i < values.size(); i++) {
      Element value = values.get(i);
      String dimName = value.getText().trim();
      dimName = NetcdfFiles.makeValidCdmObjectName(dimName);

      Dimension dim = group.findDimension(dimName).orElse(null);
      Dimension oldDim = oldDims.get(i);
      if (dim == null) {
        dim = checkUnknownDims(dimName, unknownDims, oldDim, location);
      }

      if (dim == null) {
        log.error("Unknown Dimension= {} for variable = {} {} ", dimName, v.shortName, location);
        return;
      }
      if (dim.getLength() != oldDim.getLength()) {
        log.error("Shared dimension ({}) has different length than data dimension ({}) shared={} org={} for {} {}",
            dim.getShortName(), oldDim.getShortName(), dim.getLength(), oldDim.getLength(), v, location);
        return;
      }
      newDims.add(dim);
    }
    v.setDimensions(newDims);
    if (showWork) {
      log.debug(" set shared dimensions for {}", v.shortName);
    }
  }

  // look if the wanted dimension is in the unknownDims list.
  // TODO this must be rewritten, stop using deprecated methods
  private Dimension checkUnknownDims(String wantDim, List<Dimension> unknownDims, Dimension oldDim, String location) {
    for (Dimension dim : unknownDims) {
      if (dim.getShortName().equals(wantDim)) {
        int len = oldDim.getLength();
        Dimension newDim = oldDim.toBuilder().setIsUnlimited(len == 0).setLength(len).build();
        // TODO
        // Group parent = dim.getGroup();
        // parent.addDimensionIfNotExists(newDim); // add to the parent
        unknownDims.remove(dim); // remove from list LOOK is this ok?
        log.warn("unknownDim {} length set to {} {}", wantDim, oldDim.getLength(), location);
        return newDim;
      }
    }
    return null;
  }

  // look for a group with the given name. recurse into subgroups if needed. breadth first
  private Group.Builder findGroupNested(Group.Builder parent, String name) {

    for (Group.Builder g : parent.gbuilders) {
      if (g.shortName.equals(name)) {
        return g;
      }
    }
    for (Group.Builder g : parent.gbuilders) {
      Group.Builder result = findGroupNested(g, name);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  private void fixAttributes(Group.Builder g) {
    for (Variable.Builder v : g.vbuilders) {
      AttributeContainerMutable attHelper = v.getAttributeContainer();
      for (Attribute a : ImmutableList.copyOf(attHelper)) {
        if (a.getShortName().equalsIgnoreCase("UNIT") || a.getShortName().equalsIgnoreCase("UNITS")) {
          attHelper.replace(a, CDM.UNITS);
        }
        if (a.getShortName().equalsIgnoreCase("SCALE_FACTOR") || a.getShortName().equalsIgnoreCase("FACTOR")) {
          attHelper.replace(a, CDM.SCALE_FACTOR);
        }
        if (a.getShortName().equalsIgnoreCase("OFFSET")) {
          attHelper.replace(a, CDM.ADD_OFFSET);
        }
      }
    }

    for (Group.Builder ng : g.gbuilders) {
      fixAttributes(ng);
    }

  }

}
