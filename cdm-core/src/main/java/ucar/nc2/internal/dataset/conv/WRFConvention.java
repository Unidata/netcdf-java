/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.dataset.conv;

import com.google.common.collect.ImmutableList;
import com.google.re2j.Matcher;
import com.google.re2j.Pattern;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.ma2.ArrayDouble;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.*;
import ucar.nc2.internal.dataset.CoordSystemBuilder;
import ucar.nc2.dataset.spi.CoordSystemBuilderFactory;
import ucar.nc2.internal.dataset.TransformBuilder;
import ucar.nc2.internal.dataset.transform.vertical.VerticalCTBuilder;
import ucar.nc2.internal.dataset.transform.vertical.WRFEtaTransformBuilder;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.util.CancelTask;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.projection.FlatEarth;
import ucar.unidata.geoloc.projection.LambertConformal;
import ucar.unidata.geoloc.projection.Mercator;
import ucar.unidata.geoloc.projection.Stereographic;
import ucar.unidata.util.StringUtil2;

/**
 * WRF netcdf output files.
 * <p/>
 * Note: Apparently WRF netcdf files before version 2 didnt output the projection origin, so
 * we cant properly georeference them.
 * <p/>
 * This Convention currently only supports ARW output, identified as DYN_OPT=2 or GRIDTYPE=C
 */
/*
 * Implementation notes
 *
 * There are 2 WRFs : NMM ("Non-hydrostatic Mesoscale Model" developed at NOAA/NCEP) and
 * 1) NMM ("Non-hydrostatic Mesoscale Model" developed at NOAA/NCEP)
 * GRIDTYPE="E"
 * DYN_OPT = 4
 *
 * This is a staggered grid that requires special processing.
 *
 * 2) ARW ("Advanced Research WRF", developed at MMM)
 * GRIDTYPE="C"
 * DYN_OPT = 2
 * DX, DY grid spaceing in meters (must be equal)
 *
 * the Arakawa C staggered grid (see ARW 2.2 p 3-17)
 * the + are the "non-staggered" grid:
 *
 * + V + V + V +
 * U T U T U T U
 * + V + V + V +
 * U T U T U T U
 * + V + V + V +
 * U T U T U T U
 * + V + V + V +
 */

/*
 * ARW Users Guide p 3-19
 * <pre>
 * 7. MAP_PROJ_NAME: Character string specifying type of map projection. Valid entries are:
 * "polar" -> Polar stereographic
 * "lambert" -> Lambert conformal (secant and tangent)
 * "mercator" -> Mercator
 *
 * 8. MOAD_KNOWN_LAT/MOAD_KNOWN_LON (= CEN_LAT, CEN_LON):
 * Real latitude and longitude of the center point in the grid.
 *
 * 9. MOAD_STAND_LATS (= TRUE_LAT1/2):
 * 2 real values for the "true" latitudes (where grid spacing is exact).
 * Must be between -90 and 90, and the values selected depend on projection:
 * Polar-stereographic: First value must be the latitude at which the grid
 * spacing is true. Most users will set this equal to their center latitude.
 * Second value must be +/-90. for NH/SH grids.
 * Lambert Conformal: Both values should have the same sign as the center
 * latitude. For a tangential lambert conformal, set both to the same value
 * (often equal to the center latitude). For a secant Lambert Conformal, they
 * may be set to different values.
 * Mercator: The first value should be set to the latitude you wish your grid
 * spacing to be true (often your center latitude). Second value is not used.
 *
 * 10. MOAD_STAND_LONS (=STAND_LON): This is one entry specifying the longitude in degrees East (-
 * 180->180) that is parallel to the y-axis of your grid, (sometimes referred to as the
 * orientation of the grid). This should be set equal to the center longitude in most cases.
 *
 * ---------------------------------
 * version 3.1
 * http://www.mmm.ucar.edu/wrf/users/docs/user_guide_V3.1/users_guide_chap5.htm
 *
 * The definition for map projection options:
 *
 * map_proj = 1: Lambert Conformal
 * 2: Polar Stereographic
 * 3: Mercator
 * 6: latitude and longitude (including global)
 */
public class WRFConvention extends CoordSystemBuilder {
  private static final String CONVENTION_NAME = "WRF";

  public static class Factory implements CoordSystemBuilderFactory {

    @Override
    public String getConventionName() {
      return CONVENTION_NAME;
    }

    public boolean isMine(NetcdfFile ncfile) {
      if (null == ncfile.findDimension("south_north"))
        return false;

      // ARW only
      int dynOpt = ncfile.getRootGroup().attributes().findAttributeInteger("DYN_OPT", -1);
      if (dynOpt != -1 && dynOpt != 2) { // if it exists, it must equal 2.
        return false;
      } else {
        String gridType = ncfile.getRootGroup().findAttributeString("GRIDTYPE", "null");
        if (!gridType.equalsIgnoreCase("null") && !gridType.equalsIgnoreCase("C") && !gridType.equalsIgnoreCase("E"))
          return false;
      }
      return ncfile.findAttribute("MAP_PROJ") != null;
    }

    @Override
    public CoordSystemBuilder open(NetcdfDataset.Builder<?> datasetBuilder) {
      return new WRFConvention(datasetBuilder);
    }
  }

  private WRFConvention(NetcdfDataset.Builder<?> datasetBuilder) {
    super(datasetBuilder);
    this.conventionName = CONVENTION_NAME;
  }

  private double centerX, centerY;
  private ProjectionCT projCT;
  private boolean gridE;

  @Override
  public void augmentDataset(CancelTask cancelTask) {
    if (rootGroup.findVariableLocal("x").isPresent()) {
      return; // check if its already been done - aggregating enhanced datasets.
    }

    String type = rootGroup.getAttributeContainer().findAttributeString("GRIDTYPE", null);
    gridE = type != null && type.equalsIgnoreCase("E");

    // kludge in fixing the units
    for (Variable.Builder<?> v : rootGroup.vbuilders) {
      String units = v.getAttributeContainer().findAttributeString(CDM.UNITS, null);
      if (units != null) {
        ((VariableDS.Builder<?>) v).setUnits(normalize(units));
      }
    }

    // make projection transform
    int projType = rootGroup.getAttributeContainer().findAttributeInteger("MAP_PROJ", -1);
    if (projType == -1) {
      throw new IllegalStateException("WRF must have numeric MAP_PROJ attribute");
    }
    boolean isLatLon = false;

    if (projType == 203) {
      Optional<Variable.Builder<?>> glatOpt = rootGroup.findVariableLocal("GLAT");
      if (!glatOpt.isPresent()) {
        parseInfo.format("Projection type 203 - expected GLAT variable not found%n");
      } else {
        Variable.Builder<?> glat = glatOpt.get();
        glat.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));
        if (gridE)
          glat.addAttribute(new Attribute(_Coordinate.Stagger, CDM.ARAKAWA_E));
        glat.setDimensionsByName("south_north west_east");
        glat.setSourceData(convertToDegrees(glat));
        ((VariableDS.Builder<?>) glat).setUnits(CDM.LAT_UNITS);
      }

      Optional<Variable.Builder<?>> glonOpt = rootGroup.findVariableLocal("GLON");
      if (!glonOpt.isPresent()) {
        parseInfo.format("Projection type 203 - expected GLON variable not found%n");
      } else {
        Variable.Builder<?> glon = glonOpt.get();
        glon.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));
        if (gridE)
          glon.addAttribute(new Attribute(_Coordinate.Stagger, CDM.ARAKAWA_E));
        glon.setDimensionsByName("south_north west_east");
        glon.setSourceData(convertToDegrees(glon));
        ((VariableDS.Builder<?>) glon).setUnits(CDM.LON_UNITS);
      }

      // Make coordinate system variable
      VariableDS.Builder<?> v = VariableDS.builder().setName("LatLonCoordSys").setDataType(DataType.CHAR);
      v.addAttribute(new Attribute(_Coordinate.Axes, "GLAT GLON Time"));
      Array data = Array.factory(DataType.CHAR, new int[] {}, new char[] {' '});
      v.setSourceData(data);
      rootGroup.addVariable(v);

      rootGroup.findVariableLocal("LANDMASK")
          .ifPresent(dataVar -> dataVar.addAttribute(new Attribute(_Coordinate.Systems, "LatLonCoordSys")));

    } else {
      double lat1 = findAttributeDouble("TRUELAT1");
      double lat2 = findAttributeDouble("TRUELAT2");
      double centralLat = findAttributeDouble("CEN_LAT"); // center of grid
      double centralLon = findAttributeDouble("CEN_LON"); // center of grid

      double standardLon = findAttributeDouble("STAND_LON"); // true longitude
      double standardLat = findAttributeDouble("MOAD_CEN_LAT");

      Projection proj = null;
      switch (projType) {
        case 0: { // for diagnostic runs with no georeferencing
          proj = new FlatEarth();
          projCT = new ProjectionCT("flat_earth", "FGDC", proj);
          break;
        }
        case 1: {
          // TODO what should be the correct value when STAND_LON and MOAD_CEN_LAT is missing ??
          // TODO Just follow what appears to be correct for Stereographic.
          double lon0 = (Double.isNaN(standardLon)) ? centralLon : standardLon;
          double lat0 = (Double.isNaN(standardLat)) ? lat2 : standardLat;
          proj = new LambertConformal(lat0, lon0, lat1, lat2, 0.0, 0.0, 6370);
          projCT = new ProjectionCT("Lambert", "FGDC", proj);
          break;
        }
        case 2: {
          // Thanks to Heiko Klein for figuring out WRF Stereographic
          double lon0 = (Double.isNaN(standardLon)) ? centralLon : standardLon;
          double lat0 = (Double.isNaN(centralLat)) ? lat2 : centralLat; // ?? 7/20/2010
          double scaleFactor = (1 + Math.abs(Math.sin(Math.toRadians(lat1)))) / 2.; // R Schmunk 9/10/07
          proj = new Stereographic(lat0, lon0, scaleFactor, 0.0, 0.0, 6370);
          projCT = new ProjectionCT("Stereographic", "FGDC", proj);
          break;
        }
        case 3: {
          // thanks to Robert Schmunk with edits for non-MOAD grids
          proj = new Mercator(standardLon, lat1, 0.0, 0.0, 6370);
          projCT = new ProjectionCT("Mercator", "FGDC", proj);
          break;
        }
        case 6: {
          // version 3 "lat-lon", including global
          // http://www.mmm.ucar.edu/wrf/users/workshops/WS2008/presentations/1-2.pdf
          // use 2D XLAT, XLONG
          isLatLon = true;
          // Make copy because we will add new elements to it.
          for (Variable.Builder<?> v : ImmutableList.copyOf(rootGroup.vbuilders)) {
            if (v.shortName.startsWith("XLAT")) {
              v = removeConstantTimeDim(v);
              v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));

            } else if (v.shortName.startsWith("XLONG")) {
              v = removeConstantTimeDim(v);
              v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));

            } else if (v.shortName.equals("T")) { // TODO ANOTHER MAJOR KLUDGE to pick up 4D fields
              v.addAttribute(new Attribute(_Coordinate.Axes, "Time XLAT XLONG z"));
            } else if (v.shortName.equals("U")) {
              v.addAttribute(new Attribute(_Coordinate.Axes, "Time XLAT_U XLONG_U z"));
            } else if (v.shortName.equals("V")) {
              v.addAttribute(new Attribute(_Coordinate.Axes, "Time XLAT_V XLONG_V z"));
            } else if (v.shortName.equals("W")) {
              v.addAttribute(new Attribute(_Coordinate.Axes, "Time XLAT XLONG z_stag"));
            }
          }
          break;
        }
        default: {
          parseInfo.format("ERROR: unknown projection type = %s%n", projType);
          break;
        }
      }

      if (proj != null) {
        LatLonPoint lpt1 = LatLonPoint.create(centralLat, centralLon); // center of the grid
        ProjectionPoint ppt1 = proj.latLonToProj(lpt1);
        centerX = ppt1.getX();
        centerY = ppt1.getY();
        if (debug) {
          System.out.println("centerX=" + centerX);
          System.out.println("centerY=" + centerY);
        }
      }

      // make axes
      if (!isLatLon) {
        datasetBuilder.replaceCoordinateAxis(rootGroup, makeXCoordAxis("x", "west_east"));
        datasetBuilder.replaceCoordinateAxis(rootGroup, makeXCoordAxis("x_stag", "west_east_stag"));
        datasetBuilder.replaceCoordinateAxis(rootGroup, makeYCoordAxis("y", "south_north"));
        datasetBuilder.replaceCoordinateAxis(rootGroup, makeYCoordAxis("y_stag", "south_north_stag"));
      }
      datasetBuilder.replaceCoordinateAxis(rootGroup, makeZCoordAxis("z", "bottom_top"));
      datasetBuilder.replaceCoordinateAxis(rootGroup, makeZCoordAxis("z_stag", "bottom_top_stag"));

      if (projCT != null) {
        VariableDS.Builder<?> v = makeCoordinateTransformVariable(projCT);
        v.addAttribute(new Attribute(_Coordinate.AxisTypes, "GeoX GeoY"));
        if (gridE)
          v.addAttribute(new Attribute(_Coordinate.Stagger, CDM.ARAKAWA_E));
        rootGroup.addVariable(v);
      }
    }

    // time coordinate variations
    Optional<Variable.Builder<?>> timeVar = rootGroup.findVariableLocal("Time");
    if (!timeVar.isPresent()) { // Can skip this if its already there, eg from NcML
      CoordinateAxis.Builder<?> taxis = makeTimeCoordAxis("Time", "Time");
      if (taxis == null)
        taxis = makeTimeCoordAxis("Time", "Times");
      if (taxis != null)
        datasetBuilder.replaceCoordinateAxis(rootGroup, taxis);
    }

    datasetBuilder.replaceCoordinateAxis(rootGroup, makeSoilDepthCoordAxis("ZS"));
  }

  private VariableDS.Builder<?> removeConstantTimeDim(Variable.Builder<?> vb) {
    VariableDS.Builder<?> vds = (VariableDS.Builder<?>) vb;
    Variable v = vds.orgVar;
    int[] shape = v.getShape();
    if (v.getRank() == 3 && shape[0] == 1) { // remove time dependencies - TODO MAJOR KLUDGE
      Variable view;
      try {
        view = v.slice(0, 0);
      } catch (InvalidRangeException e) {
        parseInfo.format("Cant remove first dimension in variable %s", v);
        return vds;
      }
      // TODO test that this works
      VariableDS.Builder<?> vbnew = VariableDS.builder().copyFrom(view);
      rootGroup.replaceVariable(vbnew);
      return vbnew;
    }
    return vds;
  }

  /*
   * TODO this doesnt work, leaving original way to do it, should revisit
   * private Variable.Builder<?> removeConstantTimeDim(Variable.Builder<?> vb) {
   * VariableDS.Builder<?> vds = (VariableDS.Builder<?>) vb;
   * Variable v = vds.orgVar;
   * int[] shape = v.getShape();
   * if (v.getRank() == 3 && shape[0] == 1) {
   * Variable.Builder<?> vdslice = vds.makeSliceBuilder(0, 0);
   * rootGroup.replaceVariable(vdslice);
   * return vdslice;
   * }
   * return vds;
   * }
   */

  private Array convertToDegrees(Variable.Builder<?> vb) {
    VariableDS.Builder<?> vds = (VariableDS.Builder<?>) vb;
    Variable v = vds.orgVar;
    Array data;
    try {
      data = v.read();
      data = data.reduce();
    } catch (IOException ioe) {
      throw new RuntimeException("data read failed on " + v.getFullName() + "=" + ioe.getMessage());
    }
    IndexIterator ii = data.getIndexIterator();
    while (ii.hasNext()) {
      ii.setDoubleCurrent(Math.toDegrees(ii.getDoubleNext()));
    }
    return data;
  }

  // pretty much WRF specific
  private String normalize(String units) {
    switch (units) {
      case "fraction":
      case "dimensionless":
      case "-":
      case "NA":
        units = "";
        break;
      default:
        units = units.replace("**", "^");
        units = StringUtil2.remove(units, '}');
        units = StringUtil2.remove(units, '{');
        break;
    }
    return units;
  }
  /////////////////////////////////////////////////////////////////////////

  @Override
  protected void makeCoordinateTransforms() {
    if (projCT != null) {
      VarProcess vp = findVarProcess(projCT.getName(), null);
      if (vp != null) {
        vp.isCoordinateTransform = true;
        vp.ct = new TransformBuilder().setPreBuilt(projCT);
      }
    }
    super.makeCoordinateTransforms();
  }

  @Override
  @Nullable
  protected AxisType getAxisType(VariableDS.Builder<?> v) {
    String vname = v.shortName;

    if (vname.equalsIgnoreCase("x") || vname.equalsIgnoreCase("x_stag"))
      return AxisType.GeoX;

    if (vname.equalsIgnoreCase("lon"))
      return AxisType.Lon;

    if (vname.equalsIgnoreCase("y") || vname.equalsIgnoreCase("y_stag"))
      return AxisType.GeoY;

    if (vname.equalsIgnoreCase("lat"))
      return AxisType.Lat;

    if (vname.equalsIgnoreCase("z") || vname.equalsIgnoreCase("z_stag"))
      return AxisType.GeoZ;

    if (vname.equalsIgnoreCase("Z"))
      return AxisType.Height;

    if (vname.equalsIgnoreCase("time") || vname.equalsIgnoreCase("times"))
      return AxisType.Time;

    String unit = v.getUnits();
    if (unit != null) {
      if (SimpleUnit.isCompatible("millibar", unit))
        return AxisType.Pressure;

      if (SimpleUnit.isCompatible("m", unit))
        return AxisType.Height;
    }


    return null;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////

  @Nullable
  private CoordinateAxis.Builder<?> makeLonCoordAxis(String axisName, Dimension dim) {
    if (dim == null)
      return null;
    double dx = 4 * findAttributeDouble("DX");
    int nx = dim.getLength();
    double startx = centerX - dx * (nx - 1) / 2;

    CoordinateAxis.Builder<?> v = CoordinateAxis1D.builder().setName(axisName).setDataType(DataType.DOUBLE)
        .setDimensionsByName(dim.getShortName()).setUnits("degrees_east").setDesc("synthesized longitude coordinate");
    v.setAutoGen(startx, dx);
    v.setAxisType(AxisType.Lon);
    v.addAttribute(new Attribute(_Coordinate.AxisType, "Lon"));
    if (!axisName.equals(dim.getShortName()))
      v.addAttribute(new Attribute(_Coordinate.AliasForDimension, dim.getShortName()));

    return v;
  }

  @Nullable
  private CoordinateAxis.Builder<?> makeLatCoordAxis(String axisName, Dimension dim) {
    if (dim == null)
      return null;
    double dy = findAttributeDouble("DY");
    int ny = dim.getLength();
    double starty = centerY - dy * (ny - 1) / 2;

    CoordinateAxis.Builder<?> v = CoordinateAxis1D.builder().setName(axisName).setDataType(DataType.DOUBLE)
        .setDimensionsByName(dim.getShortName()).setUnits("degrees_north").setDesc("synthesized latitude coordinate");
    v.setAutoGen(starty, dy);
    v.setAxisType(AxisType.Lat);
    v.addAttribute(new Attribute(_Coordinate.AxisType, "Lat"));
    if (!axisName.equals(dim.getShortName()))
      v.addAttribute(new Attribute(_Coordinate.AliasForDimension, dim.getShortName()));

    return v;
  }

  @Nullable
  private CoordinateAxis.Builder<?> makeXCoordAxis(String axisName, String dimName) {
    Optional<Dimension> dimOpt = rootGroup.findDimension(dimName);
    if (!dimOpt.isPresent()) {
      return null;
    }
    Dimension dim = dimOpt.get();
    double dx = findAttributeDouble("DX") / 1000.0; // km ya just gotta know
    int nx = dim.getLength();
    double startx = centerX - dx * (nx - 1) / 2; // ya just gotta know

    CoordinateAxis.Builder<?> v = CoordinateAxis1D.builder().setName(axisName).setDataType(DataType.DOUBLE)
        .setParentGroupBuilder(rootGroup).setDimensionsByName(dim.getShortName()).setUnits("km")
        .setDesc("synthesized GeoX coordinate from DX attribute");
    v.setAutoGen(startx, dx);
    v.setAxisType(AxisType.GeoX);
    v.addAttribute(new Attribute(_Coordinate.AxisType, "GeoX"));
    if (!axisName.equals(dim.getShortName()))
      v.addAttribute(new Attribute(_Coordinate.AliasForDimension, dim.getShortName()));

    if (gridE)
      v.addAttribute(new Attribute(_Coordinate.Stagger, CDM.ARAKAWA_E));
    return v;
  }

  @Nullable
  private CoordinateAxis.Builder<?> makeYCoordAxis(String axisName, String dimName) {
    Optional<Dimension> dimOpt = rootGroup.findDimension(dimName);
    if (!dimOpt.isPresent()) {
      return null;
    }
    Dimension dim = dimOpt.get();
    double dy = findAttributeDouble("DY") / 1000.0;
    int ny = dim.getLength();
    double starty = centerY - dy * (ny - 1) / 2; // - dy/2; // ya just gotta know

    CoordinateAxis.Builder<?> v = CoordinateAxis1D.builder().setName(axisName).setDataType(DataType.DOUBLE)
        .setParentGroupBuilder(rootGroup).setDimensionsByName(dim.getShortName()).setUnits("km")
        .setDesc("synthesized GeoY coordinate from DY attribute");
    v.setAxisType(AxisType.GeoY);
    v.addAttribute(new Attribute(_Coordinate.AxisType, "GeoY"));
    v.setAutoGen(starty, dy);
    if (!axisName.equals(dim.getShortName()))
      v.addAttribute(new Attribute(_Coordinate.AliasForDimension, dim.getShortName()));

    if (gridE)
      v.addAttribute(new Attribute(_Coordinate.Stagger, CDM.ARAKAWA_E));
    return v;
  }

  @Nullable
  private CoordinateAxis.Builder<?> makeZCoordAxis(String axisName, String dimName) {
    Optional<Dimension> dimOpt = rootGroup.findDimension(dimName);
    if (dimOpt.isEmpty()) {
      return null;
    }
    Dimension dim = dimOpt.get();

    String fromWhere = axisName.endsWith("stag") ? "ZNW" : "ZNU";

    CoordinateAxis.Builder<?> v =
        CoordinateAxis1D.builder().setName(axisName).setDataType(DataType.DOUBLE).setParentGroupBuilder(rootGroup)
            .setDimensionsByName(dim.getShortName()).setUnits("").setDesc("eta values from variable " + fromWhere);
    v.addAttribute(new Attribute(CF.POSITIVE, CF.POSITIVE_DOWN)); // eta coordinate is 1.0 at bottom, 0 at top
    v.setAxisType(AxisType.GeoZ);
    v.addAttribute(new Attribute(_Coordinate.AxisType, "GeoZ"));
    if (!axisName.equals(dim.getShortName())) {
      v.addAttribute(new Attribute(_Coordinate.AliasForDimension, dim.getShortName()));
    }

    // create eta values from file variables: ZNU, ZNW
    // But they are a function of time though the values are the same in the sample file
    // NOTE: Use first time sample assuming all are the same!!
    Optional<Variable.Builder<?>> etaVarOpt = rootGroup.findVariableLocal(fromWhere);
    if (!etaVarOpt.isPresent()) {
      return makeFakeCoordAxis(axisName, dim);
    } else {
      VariableDS.Builder<?> etaVarDS = (VariableDS.Builder<?>) etaVarOpt.get();
      Variable etaVar = etaVarDS.orgVar;
      int n = etaVar.getShape(1); // number of eta levels
      int[] origin = {0, 0};
      int[] shape = {1, n};
      try {
        Array array = etaVar.read(origin, shape);// read first time slice
        ArrayDouble.D1 newArray = new ArrayDouble.D1(n);
        IndexIterator it = array.getIndexIterator();
        int count = 0;
        while (it.hasNext()) {
          double d = it.getDoubleNext();
          newArray.set(count++, d);
        }
        v.setSourceData(newArray);
      } catch (Exception e) {
        e.printStackTrace();
      } // ADD: error?

      return v;
    }
  }

  @Nullable
  private CoordinateAxis.Builder<?> makeFakeCoordAxis(String axisName, Dimension dim) {
    if (dim == null)
      return null;
    CoordinateAxis.Builder<?> v =
        CoordinateAxis1D.builder().setName(axisName).setDataType(DataType.SHORT).setParentGroupBuilder(rootGroup)
            .setDimensionsByName(dim.getShortName()).setUnits("").setDesc("synthesized coordinate: only an index");
    v.setAxisType(AxisType.GeoZ);
    v.addAttribute(new Attribute(_Coordinate.AxisType, "GeoZ"));
    if (!axisName.equals(dim.getShortName()))
      v.addAttribute(new Attribute(_Coordinate.AliasForDimension, dim.getShortName()));

    v.setAutoGen(0, 1);
    return v;
  }

  @Nullable
  private CoordinateAxis.Builder<?> makeTimeCoordAxis(String axisName, String dimName) {
    Optional<Dimension> dimOpt = rootGroup.findDimension(dimName);
    if (!dimOpt.isPresent()) {
      return null;
    }
    Dimension dim = dimOpt.get();
    int nt = dim.getLength();
    Optional<Variable.Builder<?>> timeOpt = rootGroup.findVariableLocal("Times");
    if (!timeOpt.isPresent()) {
      return null;
    }

    Variable timeV = ((VariableDS.Builder<?>) timeOpt.get()).orgVar;

    Array timeData;
    try {
      timeData = timeV.read();
    } catch (IOException ioe) {
      return null;
    }

    ArrayDouble.D1 values = new ArrayDouble.D1(nt);
    int count = 0;

    if (timeData instanceof ArrayChar) {
      ArrayChar.StringIterator iter = ((ArrayChar) timeData).getStringIterator();
      String testTimeStr = ((ArrayChar) timeData).getString(0);
      boolean isCanonicalIsoStr;
      // Maybe too specific to require WRF to give 10 digits or
      // dashes for the date (e.g. yyyy-mm-dd)?
      String wrfDateWithUnderscore = "([\\-\\d]{10})_";
      Pattern wrfDateWithUnderscorePattern = Pattern.compile(wrfDateWithUnderscore);
      Matcher m = wrfDateWithUnderscorePattern.matcher(testTimeStr);
      isCanonicalIsoStr = m.matches();

      while (iter.hasNext()) {
        String dateS = iter.next();
        CalendarDate cd;
        if (isCanonicalIsoStr) {
          cd = CalendarDate.fromUdunitIsoDate(null, dateS).orElse(null);
        } else {
          cd = CalendarDate.fromUdunitIsoDate(null, dateS.replaceFirst("_", "T")).orElse(null);
        }

        if (cd != null) {
          values.set(count++, (double) cd.getMillisFromEpoch() / 1000);
        } else {
          parseInfo.format("ERROR: cant parse Time string = <%s>%n", dateS);

          // one more try
          String startAtt = rootGroup.getAttributeContainer().findAttributeString("START_DATE", null);
          if ((nt == 1) && (null != startAtt)) {
            try {
              cd = CalendarDate.fromUdunitIsoDate(null, startAtt).orElseThrow();
              values.set(0, (double) cd.getMillisFromEpoch() / 1000);
            } catch (Exception e2) {
              parseInfo.format("ERROR: cant parse global attribute START_DATE = <%s> err=%s%n", startAtt,
                  e2.getMessage());
            }
          }
        }
      }
    } else {
      IndexIterator iter = timeData.getIndexIterator();
      while (iter.hasNext()) {
        String dateS = (String) iter.next();
        try {
          CalendarDate cd = CalendarDate.fromUdunitIsoDate(null, dateS).orElseThrow();
          values.set(count++, (double) cd.getMillisFromEpoch() / 1000);
        } catch (IllegalArgumentException e) {
          parseInfo.format("ERROR: cant parse Time string = %s%n", dateS);
        }
      }

    }

    CoordinateAxis.Builder<?> v = CoordinateAxis1D.builder().setName(axisName).setDataType(DataType.DOUBLE)
        .setParentGroupBuilder(rootGroup).setDimensionsByName(dim.getShortName())
        .setUnits("secs since 1970-01-01 00:00:00").setDesc("synthesized time coordinate from Times(time)");
    v.setAxisType(AxisType.Time);
    v.addAttribute(new Attribute(_Coordinate.AxisType, "Time"));
    if (!axisName.equals(dim.getShortName()))
      v.addAttribute(new Attribute(_Coordinate.AliasForDimension, dim.getShortName()));

    v.setSourceData(values);
    return v;
  }

  @Nullable
  private CoordinateAxis.Builder<?> makeSoilDepthCoordAxis(String coordVarName) {
    Optional<Variable.Builder<?>> varOpt = rootGroup.findVariableLocal(coordVarName);
    if (!varOpt.isPresent()) {
      return null;
    }
    VariableDS.Builder<?> coordVarB = (VariableDS.Builder<?>) varOpt.get();
    Variable coordVar = coordVarB.orgVar;

    Dimension soilDim = null;
    List<Dimension> dims = coordVar.getDimensions();
    for (Dimension d : dims) {
      if (d.getShortName().startsWith("soil_layers"))
        soilDim = d;
    }
    if (null == soilDim)
      return null;

    // One dimensional case, can convert existing variable
    if (coordVar.getRank() == 1) {
      coordVarB.addAttribute(new Attribute(CF.POSITIVE, CF.POSITIVE_DOWN)); // soil depth gets larger as you go down
      coordVarB.addAttribute(new Attribute(_Coordinate.AxisType, "GeoZ"));
      if (!coordVarName.equals(soilDim.getShortName()))
        coordVarB.addAttribute(new Attribute(_Coordinate.AliasForDimension, soilDim.getShortName()));
      return CoordinateAxis.fromVariableDS(coordVarB);
    }

    String units = coordVar.attributes().findAttributeString(CDM.UNITS, "");

    CoordinateAxis.Builder<?> v =
        CoordinateAxis1D.builder().setName("soilDepth").setDataType(DataType.DOUBLE).setParentGroupBuilder(rootGroup)
            .setDimensionsByName(soilDim.getShortName()).setUnits(units).setDesc("soil depth");
    v.addAttribute(new Attribute(CF.POSITIVE, CF.POSITIVE_DOWN)); // soil depth gets larger as you go down
    v.setAxisType(AxisType.GeoZ);
    v.addAttribute(new Attribute(_Coordinate.AxisType, "GeoZ"));
    v.setUnits(CDM.UNITS);
    if (!v.shortName.equals(soilDim.getShortName()))
      v.addAttribute(new Attribute(_Coordinate.AliasForDimension, soilDim.getShortName()));

    // read first time slice
    int n = coordVar.getShape(1);
    int[] origin = {0, 0};
    int[] shape = {1, n};
    try {
      Array array = coordVar.read(origin, shape);
      ArrayDouble.D1 newArray = new ArrayDouble.D1(n);
      IndexIterator it = array.getIndexIterator();
      int count = 0;
      while (it.hasNext()) {
        double d = it.getDoubleNext();
        newArray.set(count++, d);
      }
      v.setSourceData(newArray);
    } catch (Exception e) {
      e.printStackTrace();
    }

    return v;
  }

  private double findAttributeDouble(String attname) {
    return rootGroup.getAttributeContainer().findAttributeDouble(attname, Double.NaN);
  }

  @Override
  protected void assignCoordinateTransforms() {
    super.assignCoordinateTransforms();

    if (rootGroup.findVariableLocal("PH").isPresent() && rootGroup.findVariableLocal("PHB").isPresent()
        && rootGroup.findVariableLocal("P").isPresent() && rootGroup.findVariableLocal("PB").isPresent()) {

      // public Optional<CoordinateAxis.Builder> findZAxis(CoordinateSystem.Builder csys) {
      // any cs with a vertical coordinate with no units gets one
      for (CoordinateSystem.Builder<?> cs : coords.coordSys) {
        coords.findAxisByType(cs, AxisType.GeoZ).ifPresent(axis -> {
          String units = axis.getUnits();
          if ((units == null) || (units.trim().isEmpty())) {
            // LOOK each cs might have separate ct; but they might be equal....
            VerticalCTBuilder vctb = new WRFEtaTransformBuilder(coords, cs);
            TransformBuilder tb = new TransformBuilder().setVertCTBuilder(vctb);
            coords.addTransformBuilder(tb);
            cs.addCoordinateTransformByName(vctb.getTransformName());
            parseInfo.format("***Added WRFEtaTransformBuilderto '%s'%n", cs.coordAxesNames);
          }
        });
      }
    }
  }

}

/*
 * NMM . output from gridgen. these are the input files to WRF i think
 * netcdf Q:/grid/netcdf/wrf/geo_nmm.d01.nc {
 * dimensions:
 * Time = UNLIMITED; // (1 currently)
 * DateStrLen = 19;
 * west_east = 136;
 * south_north = 309;
 * land_cat = 24;
 * soil_cat = 16;
 * month = 12;
 * variables:
 * char Times(Time=1, DateStrLen=19);
 * float XLAT_M(Time=1, south_north=309, west_east=136);
 * :FieldType = 104; // int
 * :MemoryOrder = "XY ";
 * :units = "degrees latitude";
 * :description = "Latitude on mass grid";
 * :stagger = "M";
 * float XLONG_M(Time=1, south_north=309, west_east=136);
 * :FieldType = 104; // int
 * :MemoryOrder = "XY ";
 * :units = "degrees longitude";
 * :description = "Longitude on mass grid";
 * :stagger = "M";
 * float XLAT_V(Time=1, south_north=309, west_east=136);
 * :FieldType = 104; // int
 * :MemoryOrder = "XY ";
 * :units = "degrees latitude";
 * :description = "Latitude on velocity grid";
 * :stagger = "V";
 * float XLONG_V(Time=1, south_north=309, west_east=136);
 * :FieldType = 104; // int
 * :MemoryOrder = "XY ";
 * :units = "degrees longitude";
 * :description = "Longitude on velocity grid";
 * :stagger = "V";
 * float E(Time=1, south_north=309, west_east=136);
 * :FieldType = 104; // int
 * :MemoryOrder = "XY ";
 * :units = "-";
 * :description = "Coriolis E parameter";
 * :stagger = "M";
 * float F(Time=1, south_north=309, west_east=136);
 * :FieldType = 104; // int
 * :MemoryOrder = "XY ";
 * :units = "-";
 * :description = "Coriolis F parameter";
 * :stagger = "M";
 * float LANDMASK(Time=1, south_north=309, west_east=136);
 * :FieldType = 104; // int
 * :MemoryOrder = "XY ";
 * :units = "none";
 * :description = "Landmask : 1=land, 0=water";
 * :stagger = "M";
 * float LANDUSEF(Time=1, land_cat=24, south_north=309, west_east=136);
 * :FieldType = 104; // int
 * :MemoryOrder = "XYZ";
 * :units = "category";
 * :description = "24-category USGS landuse";
 * :stagger = "M";
 * float LU_INDEX(Time=1, south_north=309, west_east=136);
 * :FieldType = 104; // int
 * :MemoryOrder = "XY ";
 * :units = "category";
 * :description = "Dominant category";
 * :stagger = "M";
 * float HGT_M(Time=1, south_north=309, west_east=136);
 * :FieldType = 104; // int
 * :MemoryOrder = "XY ";
 * :units = "meters MSL";
 * :description = "Topography height";
 * :stagger = "M";
 * float HGT_V(Time=1, south_north=309, west_east=136);
 * :FieldType = 104; // int
 * :MemoryOrder = "XY ";
 * :units = "meters MSL";
 * :description = "Topography height";
 * :stagger = "V";
 * float SOILTEMP(Time=1, south_north=309, west_east=136);
 * :FieldType = 104; // int
 * :MemoryOrder = "XY ";
 * :units = "Kelvin";
 * :description = "Annual mean deep soil temperature";
 * :stagger = "M";
 * float SOILCTOP(Time=1, soil_cat=16, south_north=309, west_east=136);
 * :FieldType = 104; // int
 * :MemoryOrder = "XYZ";
 * :units = "category";
 * :description = "16-category top-layer soil type";
 * :stagger = "M";
 * float SOILCBOT(Time=1, soil_cat=16, south_north=309, west_east=136);
 * :FieldType = 104; // int
 * :MemoryOrder = "XYZ";
 * :units = "category";
 * :description = "16-category bottom-layer soil type";
 * :stagger = "M";
 * float ALBEDO12M(Time=1, month=12, south_north=309, west_east=136);
 * :FieldType = 104; // int
 * :MemoryOrder = "XYZ";
 * :units = "percent";
 * :description = "Monthly surface albedo";
 * :stagger = "M";
 * float GREENFRAC(Time=1, month=12, south_north=309, west_east=136);
 * :FieldType = 104; // int
 * :MemoryOrder = "XYZ";
 * :units = "fraction";
 * :description = "Monthly green fraction";
 * :stagger = "M";
 * float SNOALB(Time=1, south_north=309, west_east=136);
 * :FieldType = 104; // int
 * :MemoryOrder = "XY ";
 * :units = "percent";
 * :description = "Maximum snow albedo";
 * :stagger = "M";
 * float SLOPECAT(Time=1, south_north=309, west_east=136);
 * :FieldType = 104; // int
 * :MemoryOrder = "XY ";
 * :units = "category";
 * :description = "Dominant category";
 * :stagger = "M";
 * 
 * :TITLE = "OUTPUT FROM GRIDGEN";
 * :SIMULATION_START_DATE = "0000-00-00_00:00:00";
 * :WEST-EAST_GRID_DIMENSION = 136; // int
 * :SOUTH-NORTH_GRID_DIMENSION = 309; // int
 * :BOTTOM-TOP_GRID_DIMENSION = 0; // int
 * :WEST-EAST_PATCH_START_UNSTAG = 1; // int
 * :WEST-EAST_PATCH_END_UNSTAG = 136; // int
 * :WEST-EAST_PATCH_START_STAG = 1; // int
 * :WEST-EAST_PATCH_END_STAG = 136; // int
 * :SOUTH-NORTH_PATCH_START_UNSTAG = 1; // int
 * :SOUTH-NORTH_PATCH_END_UNSTAG = 309; // int
 * :SOUTH-NORTH_PATCH_START_STAG = 1; // int
 * :SOUTH-NORTH_PATCH_END_STAG = 309; // int
 * :GRIDTYPE = "E";
 * :DX = 0.111143f; // float
 * :DY = 0.106776f; // float
 * :DYN_OPT = 4; // int
 * :CEN_LAT = 21.0f; // float
 * :CEN_LON = 80.0f; // float
 * :TRUELAT1 = 1.0E20f; // float
 * :TRUELAT2 = 1.0E20f; // float
 * :MOAD_CEN_LAT = 0.0f; // float
 * :STAND_LON = 1.0E20f; // float
 * :POLE_LAT = 90.0f; // float
 * :POLE_LON = 0.0f; // float
 * :corner_lats = 3.8832555f, 36.602543f, 36.602543f, 3.8832555f, 3.8931324f, 36.61482f, 36.59018f, 3.873307f, 0.0f,
 * 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f; // float
 * :corner_lons = 65.589096f, 61.982986f, 98.01701f, 94.410904f, 65.69548f, 62.114895f, 98.14889f, 94.51727f, 0.0f,
 * 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f; // float
 * :MAP_PROJ = 203; // int
 * :MMINLU = "USGS";
 * :ISWATER = 16; // int
 * :ISICE = 24; // int
 * :ISURBAN = 1; // int
 * :ISOILWATER = 14; // int
 * :grid_id = 1; // int
 * :parent_id = 1; // int
 * :i_parent_start = 0; // int
 * :j_parent_start = 0; // int
 * :i_parent_end = 136; // int
 * :j_parent_end = 309; // int
 * :parent_grid_ratio = 1; // int
 * }
 * 
 */
