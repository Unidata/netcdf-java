/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.dataset.conv;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import ucar.ma2.ArrayDouble;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateTransform;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.ProjectionCT;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.spi.CoordSystemBuilderFactory;
import ucar.nc2.internal.dataset.CoordSystemBuilder;
import ucar.nc2.util.CancelTask;
import ucar.unidata.geoloc.projection.AlbersEqualArea;
import ucar.unidata.geoloc.projection.LambertAzimuthalEqualArea;
import ucar.unidata.geoloc.projection.LambertConformal;
import ucar.unidata.geoloc.projection.LatLonProjection;
import ucar.unidata.geoloc.projection.Mercator;
import ucar.unidata.geoloc.projection.Stereographic;
import ucar.unidata.geoloc.projection.TransverseMercator;
import ucar.unidata.geoloc.projection.UtmProjection;

/**
 * Models-3/EDSS Input/Output netcf format.
 * <p/>
 * The Models-3/EDSS Input/Output Applications Programming Interface (I/O API)
 * is the standard data access library for both NCSC's EDSS project and EPA's Models-3.
 * <p/>
 * 6/24/09: Modified to support multiple projection types of data by Qun He <qunhe@unc.edu>
 * and Alexis Zubrow <azubrow@unc.edu> added makePolarStereographicProjection, UTM, and modified latlon
 * <p/>
 * 09/2010 plessel.todd@epa.gov add projections 7,8,9,10
 *
 * @author plessel.todd@epa.gov
 * @author caron
 * @see "https://www.cmascenter.org/ioapi/"
 */
public class M3IOConvention extends CoordSystemBuilder {
  private static final String CONVENTION_NAME = "M3IO";
  private static final double earthRadius = 6370.000; // km

  public static class Factory implements CoordSystemBuilderFactory {
    @Override
    public String getConventionName() {
      return CONVENTION_NAME;
    }

    @Override
    public boolean isMine(NetcdfFile ncfile) {
      return (null != ncfile.findGlobalAttribute("XORIG")) && (null != ncfile.findGlobalAttribute("YORIG"))
          && (null != ncfile.findGlobalAttribute("XCELL")) && (null != ncfile.findGlobalAttribute("YCELL"))
          && (null != ncfile.findGlobalAttribute("NCOLS")) && (null != ncfile.findGlobalAttribute("NROWS"));
    }

    @Override
    public CoordSystemBuilder open(NetcdfDataset.Builder<?> datasetBuilder) {
      return new M3IOConvention(datasetBuilder);
    }
  }

  /////////////////////////////////////////////////////////////////
  private ProjectionCT projCT;

  private M3IOConvention(NetcdfDataset.Builder<?> datasetBuilder) {
    super(datasetBuilder);
    this.conventionName = CONVENTION_NAME;
  }

  @Override
  public void augmentDataset(CancelTask cancelTask) throws IOException {
    if (rootGroup.findVariableLocal("x").isPresent() || rootGroup.findVariableLocal("lon").isPresent())
      return; // check if its already been done - aggregating enhanced datasets.

    int projType = rootGroup.getAttributeContainer().findAttributeInteger("GDTYP", 1);
    boolean isLatLon = (projType == 1);
    if (isLatLon) {
      datasetBuilder.replaceCoordinateAxis(rootGroup, makeCoordLLAxis("lon", "COL", "XORIG", "XCELL", "degrees east"));
      datasetBuilder.replaceCoordinateAxis(rootGroup, makeCoordLLAxis("lat", "ROW", "YORIG", "YCELL", "degrees north"));
      projCT = makeLatLongProjection();

      VariableDS.Builder<?> v = makeCoordinateTransformVariable(projCT);
      rootGroup.addVariable(v);
      v.addAttribute(new Attribute(_Coordinate.Axes, "lon lat"));

    } else {
      datasetBuilder.replaceCoordinateAxis(rootGroup, makeCoordAxis("x", "COL", "XORIG", "XCELL", "km"));
      datasetBuilder.replaceCoordinateAxis(rootGroup, makeCoordAxis("y", "ROW", "YORIG", "YCELL", "km"));

      if (projType == 2)
        projCT = makeLCProjection();
      else if (projType == 3)
        projCT = makeTMProjection();
      else if (projType == 4)
        projCT = makeSTProjection();
      else if (projType == 5)
        projCT = makeUTMProjection();
      else if (projType == 6) // Was 7. See http://www.baronams.com/products/ioapi/GRIDS.html
        projCT = makePolarStereographicProjection();
      else if (projType == 7)
        projCT = makeEquitorialMercatorProjection();
      else if (projType == 8)
        projCT = makeTransverseMercatorProjection();
      else if (projType == 9)
        projCT = makeAlbersProjection();
      else if (projType == 10)
        projCT = makeLambertAzimuthalProjection();

      if (projCT != null) {
        VariableDS.Builder<?> v = makeCoordinateTransformVariable(projCT);
        rootGroup.addVariable(v);
        v.addAttribute(new Attribute(_Coordinate.Axes, "x y"));
      }
    }

    makeZCoordAxis("LAY", "VGLVLS", "sigma");
    makeTimeCoordAxis("TSTEP");
  }

  @Override
  protected void makeCoordinateTransforms() {
    if (projCT != null) {
      VarProcess vp = findVarProcess(projCT.getName(), null);
      vp.isCoordinateTransform = true;
      vp.ct = CoordinateTransform.builder().setPreBuilt(projCT);
    }
    super.makeCoordinateTransforms();
  }

  private CoordinateAxis.Builder<?> makeCoordAxis(String name, String dimName, String startName, String incrName,
      String unitName) {
    double start = .001 * findAttributeDouble(startName); // km
    double incr = .001 * findAttributeDouble(incrName); // km
    start = start + incr / 2.0; // shifting x and y to central
    CoordinateAxis.Builder<?> v = CoordinateAxis1D.builder().setName(name).setDataType(DataType.DOUBLE)
        .setParentGroupBuilder(rootGroup).setDimensionsByName(dimName).setUnits(unitName)
        .setDesc("synthesized coordinate from " + startName + " " + incrName + " global attributes");
    v.setAutoGen(start, incr);
    return v;
  }

  private CoordinateAxis.Builder<?> makeCoordLLAxis(String name, String dimName, String startName, String incrName,
      String unitName) {
    // Makes coordinate axes for Lat/Lon
    double start = findAttributeDouble(startName); // degrees
    double incr = findAttributeDouble(incrName); // degrees
    // The coordinate value should be the cell center.
    // I recommend also adding a bounds coordinate variable for clarity in the future.
    start = start + incr / 2.; // shiftin lon and lat to central
    CoordinateAxis.Builder<?> v = CoordinateAxis1D.builder().setName(name).setDataType(DataType.DOUBLE)
        .setParentGroupBuilder(rootGroup).setDimensionsByName(dimName).setUnits(unitName)
        .setDesc("synthesized coordinate from " + startName + " " + incrName + " global attributes");
    v.setAutoGen(start, incr);
    return v;
  }

  private void makeZCoordAxis(String dimName, String levelsName, String unitName) {
    Dimension dimz =
        rootGroup.findDimension(dimName).orElseThrow(() -> new IllegalStateException("Cant find dimension " + dimName));
    int nz = dimz.getLength();
    ArrayDouble.D1 dataLev = new ArrayDouble.D1(nz);
    ArrayDouble.D1 dataLayers = new ArrayDouble.D1(nz + 1);

    // layer values are a numeric global attribute array !!
    Attribute layers = rootGroup.getAttributeContainer().findAttribute(levelsName);
    Preconditions.checkNotNull(layers);
    Preconditions.checkArgument(layers.isArray());
    Preconditions.checkArgument(layers.getLength() == nz + 1);
    for (int i = 0; i <= nz; i++) {
      dataLayers.set(i, layers.getNumericValue(i).doubleValue());
    }

    for (int i = 0; i < nz; i++) {
      double midpoint = (dataLayers.get(i) + dataLayers.get(i + 1)) / 2;
      dataLev.set(i, midpoint);
    }

    CoordinateAxis.Builder<?> v = CoordinateAxis1D.builder().setName("level").setDataType(DataType.DOUBLE)
        .setParentGroupBuilder(rootGroup).setDimensionsByName(dimName).setUnits(unitName)
        .setDesc("synthesized coordinate from " + levelsName + " global attributes");
    v.setCachedData(dataLev);
    v.addAttribute(new Attribute("positive", "down"));
    v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.GeoZ.toString()));

    // layer edges
    String edge_name = "layer";
    Dimension lay_edge = new Dimension(edge_name, nz + 1);
    rootGroup.addDimension(lay_edge);
    CoordinateAxis.Builder<?> vedge = CoordinateAxis1D.builder().setName(edge_name).setDataType(DataType.DOUBLE)
        .setParentGroupBuilder(rootGroup).setDimensionsByName(edge_name).setUnits(unitName)
        .setDesc("synthesized coordinate from " + levelsName + " global attributes");
    vedge.setCachedData(dataLayers);
    v.setBoundary(edge_name);

    datasetBuilder.replaceCoordinateAxis(rootGroup, v);
    datasetBuilder.replaceCoordinateAxis(rootGroup, vedge);
  }

  private void makeTimeCoordAxis(String timeName) {
    int start_date = findAttributeInt("SDATE");
    int start_time = findAttributeInt("STIME");
    int time_step = findAttributeInt("TSTEP");

    int year = start_date / 1000;
    int doy = start_date % 1000;
    int hour = start_time / 10000;
    start_time = start_time % 10000;
    int min = start_time / 100;
    int sec = start_time % 100;

    Calendar cal = new GregorianCalendar(new SimpleTimeZone(0, "GMT"));
    cal.clear();
    cal.set(Calendar.YEAR, year);
    cal.set(Calendar.DAY_OF_YEAR, doy);
    cal.set(Calendar.HOUR_OF_DAY, hour);
    cal.set(Calendar.MINUTE, min);
    cal.set(Calendar.SECOND, sec);

    java.text.SimpleDateFormat dateFormatOut = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    dateFormatOut.setTimeZone(TimeZone.getTimeZone("GMT"));

    String units = "seconds since " + dateFormatOut.format(cal.getTime()) + " UTC";

    // parse the time step
    hour = time_step / 10000;
    time_step = time_step % 10000;
    min = time_step / 100;
    sec = time_step % 100;
    time_step = hour * 3600 + min * 60 + sec;

    // create the coord axis
    CoordinateAxis1D.Builder<?> timeCoord = CoordinateAxis1D.builder().setName("time").setDataType(DataType.INT)
        .setParentGroupBuilder(rootGroup).setDimensionsByName(timeName).setUnits(units)
        .setDesc("synthesized time coordinate from SDATE, STIME, STEP global attributes");
    timeCoord.setAutoGen(0, time_step);
    timeCoord.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));

    datasetBuilder.replaceCoordinateAxis(rootGroup, timeCoord);
  }

  private ProjectionCT makeLatLongProjection() {
    // Get lower left and upper right corners of domain in lat/lon
    double x1 = findAttributeDouble("XORIG");
    double x2 = x1 + findAttributeDouble("XCELL") * findAttributeDouble("NCOLS");

    LatLonProjection ll = new LatLonProjection("LatitudeLongitudeProjection", null, (x1 + x2) / 2);
    return new ProjectionCT("LatitudeLongitudeProjection", "FGDC", ll);
  }

  private ProjectionCT makeLCProjection() {
    double par1 = findAttributeDouble("P_ALP");
    double par2 = findAttributeDouble("P_BET");
    double lon0 = findAttributeDouble("XCENT");
    double lat0 = findAttributeDouble("YCENT");

    LambertConformal lc = new LambertConformal(lat0, lon0, par1, par2, 0.0, 0.0, earthRadius);
    return new ProjectionCT("LambertConformalProjection", "FGDC", lc);
  }

  private ProjectionCT makePolarStereographicProjection() {
    double lon0 = findAttributeDouble("XCENT");
    double lat0 = findAttributeDouble("YCENT");
    double latts = findAttributeDouble("P_BET");

    Stereographic sg = new Stereographic(latts, lat0, lon0, 0.0, 0.0, earthRadius);
    return new ProjectionCT("PolarStereographic", "FGDC", sg);
  }

  private ProjectionCT makeEquitorialMercatorProjection() {
    double lon0 = findAttributeDouble("XCENT");
    double par = findAttributeDouble("P_ALP");

    Mercator p = new Mercator(lon0, par, 0.0, 0.0, earthRadius);
    return new ProjectionCT("EquitorialMercator", "FGDC", p);
  }

  private ProjectionCT makeTransverseMercatorProjection() {
    double lat0 = findAttributeDouble("P_ALP");
    double tangentLon = findAttributeDouble("P_GAM");

    TransverseMercator p = new TransverseMercator(lat0, tangentLon, 1.0, 0.0, 0.0, earthRadius);
    return new ProjectionCT("TransverseMercator", "FGDC", p);
  }

  private ProjectionCT makeAlbersProjection() {
    double lat0 = findAttributeDouble("YCENT");
    double lon0 = findAttributeDouble("XCENT");
    double par1 = findAttributeDouble("P_ALP");
    double par2 = findAttributeDouble("P_BET");

    AlbersEqualArea p = new AlbersEqualArea(lat0, lon0, par1, par2, 0.0, 0.0, earthRadius);
    return new ProjectionCT("Albers", "FGDC", p);
  }

  private ProjectionCT makeLambertAzimuthalProjection() {
    double lat0 = findAttributeDouble("YCENT");
    double lon0 = findAttributeDouble("XCENT");

    LambertAzimuthalEqualArea p = new LambertAzimuthalEqualArea(lat0, lon0, 0.0, 0.0, earthRadius);
    return new ProjectionCT("LambertAzimuthal", "FGDC", p);
  }

  private ProjectionCT makeSTProjection() {
    double latt = findAttributeDouble("PROJ_ALPHA");
    if (Double.isNaN(latt))
      latt = findAttributeDouble("P_ALP");

    double lont = findAttributeDouble("PROJ_BETA");
    if (Double.isNaN(lont))
      lont = findAttributeDouble("P_BET");

    Stereographic st = new Stereographic(latt, lont, 1.0, 0.0, 0.0, earthRadius);
    return new ProjectionCT("StereographicProjection", "FGDC", st);
  }

  private ProjectionCT makeTMProjection() {
    double lat0 = findAttributeDouble("PROJ_ALPHA");
    if (Double.isNaN(lat0))
      lat0 = findAttributeDouble("P_ALP");

    double tangentLon = findAttributeDouble("PROJ_BETA");
    if (Double.isNaN(tangentLon))
      tangentLon = findAttributeDouble("P_BET");

    TransverseMercator tm = new TransverseMercator(lat0, tangentLon, 1.0, 0.0, 0.0, earthRadius);
    return new ProjectionCT("MercatorProjection", "FGDC", tm);
  }

  private ProjectionCT makeUTMProjection() {
    int zone = (int) findAttributeDouble("P_ALP");
    double ycent = findAttributeDouble("YCENT");
    boolean isNorth = true;
    if (ycent < 0)
      isNorth = false;

    UtmProjection utm = new UtmProjection(zone, isNorth);
    return new ProjectionCT("UTM", "EPSG", utm);
  }

  /////////////////////////////////////////////////////////////////////////

  @Override
  protected AxisType getAxisType(VariableDS.Builder<?> ve) {
    String vname = ve.shortName;

    if (vname.equalsIgnoreCase("x"))
      return AxisType.GeoX;

    if (vname.equalsIgnoreCase("y"))
      return AxisType.GeoY;

    if (vname.equalsIgnoreCase("lat"))
      return AxisType.Lat;

    if (vname.equalsIgnoreCase("lon"))
      return AxisType.Lon;

    if (vname.equalsIgnoreCase("time"))
      return AxisType.Time;

    if (vname.equalsIgnoreCase("level"))
      return AxisType.GeoZ;

    return null;
  }

  private double findAttributeDouble(String attname) {
    return rootGroup.getAttributeContainer().findAttributeDouble(attname, Double.NaN);
  }

  private int findAttributeInt(String attname) {
    return rootGroup.getAttributeContainer().findAttributeInteger(attname, 0);
  }

}
