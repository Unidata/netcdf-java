/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.dataset.conv;

import java.io.IOException;
import java.util.NoSuchElementException;
import ucar.ma2.ArrayByte;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.ProjectionCT;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.spi.CoordSystemBuilderFactory;
import ucar.nc2.internal.dataset.CoordSystemBuilder;
import ucar.nc2.util.CancelTask;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.projection.LambertConformal;
import ucar.unidata.geoloc.projection.Mercator;

/**
 * AWIPS satellite netcdf output.
 *
 * @author caron
 *
 * @see <a href=
 *      "http://www-md.fsl.noaa.gov/eft/AWIPS/16c/onlinehelp/ifpServerSatelliteNETCDF.html">http://www-md.fsl.noaa.gov/eft/AWIPS/16c/onlinehelp/ifpServerSatelliteNETCDF.html</a>
 * @see <a href=
 *      "http://www.nws.noaa.gov/mdl/awips/aifmdocs/sec_4_e.htm">http://www.nws.noaa.gov/mdl/awips/aifmdocs/sec_4_e.htm</a>
 */

/** AWIPS netcdf output. */
public class AWIPSSatConvention extends AWIPSConvention {
  private static final String CONVENTION_NAME = "AWIPS-Sat";

  private static final boolean debugProj = false;
  private static final boolean debugBreakup = false;

  private AWIPSSatConvention(NetcdfDataset.Builder datasetBuilder) {
    super(datasetBuilder);
    this.conventionName = CONVENTION_NAME;
  }

  public static class Factory implements CoordSystemBuilderFactory {
    @Override
    public String getConventionName() {
      return CONVENTION_NAME;
    }

    @Override
    public boolean isMine(NetcdfFile ncfile) {
      return (null != ncfile.findGlobalAttribute("projName")) && (null != ncfile.findGlobalAttribute("lon00"))
          && (null != ncfile.findGlobalAttribute("lat00")) && (null != ncfile.findGlobalAttribute("lonNxNy"))
          && (null != ncfile.findGlobalAttribute("latNxNy")) && (null != ncfile.findGlobalAttribute("centralLon"))
          && (null != ncfile.findGlobalAttribute("centralLat")) && (null != ncfile.findDimension("x"))
          && (null != ncfile.findDimension("y")) && (null != ncfile.findVariable("image"));
    }

    @Override
    public CoordSystemBuilder open(NetcdfDataset.Builder datasetBuilder) {
      return new AWIPSSatConvention(datasetBuilder);
    }
  }

  @Override
  public void augmentDataset(CancelTask cancelTask) throws IOException {
    if (rootGroup.findVariableLocal("x").isPresent()) {
      return; // check if its already been done - aggregating enhanced datasets.
    }

    int nx = rootGroup.findDimension("x").map(Dimension::getLength)
        .orElseThrow(() -> new RuntimeException("missing dimension x"));
    int ny = rootGroup.findDimension("y").map(Dimension::getLength)
        .orElseThrow(() -> new RuntimeException("missing dimension y"));

    String projName = rootGroup.getAttributeContainer().findAttValueIgnoreCase("projName", "none");
    if (projName.equalsIgnoreCase("CYLINDRICAL_EQUIDISTANT")) {
      makeLatLonProjection(nx, ny);
      datasetBuilder.replaceCoordinateAxis(rootGroup, makeLonCoordAxis("x"));
      datasetBuilder.replaceCoordinateAxis(rootGroup, makeLatCoordAxis("y"));
    } else {
      if (projName.equalsIgnoreCase("LAMBERT_CONFORMAL"))
        projCT = makeLCProjection(projName, nx, ny);

      if (projName.equalsIgnoreCase("MERCATOR"))
        projCT = makeMercatorProjection(projName, nx, ny);

      datasetBuilder.replaceCoordinateAxis(rootGroup, makeXCoordAxis("x"));
      datasetBuilder.replaceCoordinateAxis(rootGroup, makeYCoordAxis("y"));
    }

    // long_name; LOOK: not sure of units
    VariableDS.Builder datav = (VariableDS.Builder) rootGroup.findVariableLocal("image")
        .orElseThrow(() -> new RuntimeException("must have varible 'image'"));
    String long_name = rootGroup.getAttributeContainer().findAttValueIgnoreCase("channel", null);
    if (null != long_name) {
      datav.addAttribute(new Attribute(CDM.LONG_NAME, long_name));
    }
    /*
     * In the old code, the next line was effectively not done. In new code, if its done, then it gets promoted to
     * ushort in VariableDS.build: his.dataType = scaleMissingUnsignedProxy.getUnsignedConversionType(), because the new
     * default policy in Enhance.ConvertUnsigned.
     * Its likely that that is actually corrrect, but im going to leave it off for now to agree with old
     * CoordSysBuilder.
     */
    // datav.setDataType(DataType.UBYTE);

    // missing values
    ArrayByte.D1 missing_values = new ArrayByte.D1(2, true);
    missing_values.set(0, (byte) 0);
    missing_values.set(1, (byte) -127);
    datav.addAttribute(new Attribute(CDM.MISSING_VALUE, missing_values));

    if (projCT != null) {
      VariableDS.Builder v = makeCoordinateTransformVariable(projCT);
      v.addAttribute(new Attribute(_Coordinate.Axes, "x y"));
      rootGroup.addVariable(v);
    }
  }

  @Override
  protected AxisType getAxisType(VariableDS.Builder v) {
    String units = v.getUnits();

    if (units.equalsIgnoreCase(CDM.LON_UNITS))
      return AxisType.Lon;

    if (units.equalsIgnoreCase(CDM.LAT_UNITS))
      return AxisType.Lat;

    return super.getAxisType(v);
  }

  private void makeLatLonProjection(int nx, int ny) throws NoSuchElementException {
    double lat0 = findAttributeDouble("lat00");
    double lon0 = findAttributeDouble("lon00");
    double latEnd = findAttributeDouble("latNxNy");
    double lonEnd = findAttributeDouble("lonNxNy");
    if (lonEnd < lon0) {
      lonEnd += 360;
    }

    startx = lon0;
    starty = lat0;
    dx = (lonEnd - lon0) / nx;
    dy = (latEnd - lat0) / ny;
  }

  private ProjectionCT makeLCProjection(String name, int nx, int ny) throws NoSuchElementException {
    double centralLat = findAttributeDouble("centralLat");
    double centralLon = findAttributeDouble("centralLon");
    double rotation = findAttributeDouble("rotation");

    // lat0, lon0, par1, par2
    LambertConformal proj = new LambertConformal(rotation, centralLon, centralLat, centralLat);
    // we have to project in order to find the origin
    double lat0 = findAttributeDouble("lat00");
    double lon0 = findAttributeDouble("lon00");
    ProjectionPoint start = proj.latLonToProj(new LatLonPointImpl(lat0, lon0));
    if (debugProj)
      parseInfo.format("getLCProjection start at proj coord %s%n", start);
    startx = start.getX();
    starty = start.getY();

    // we will use the end to compute grid size LOOK may be wrong
    double latN = findAttributeDouble("latNxNy");
    double lonN = findAttributeDouble("lonNxNy");
    ProjectionPoint end = proj.latLonToProj(new LatLonPointImpl(latN, lonN));
    dx = (end.getX() - startx) / nx;
    dy = (end.getY() - starty) / ny;

    if (debugProj) {
      parseInfo.format("  makeProjectionLC start at proj coord %f %f%n", startx, starty);
      parseInfo.format("  makeProjectionLC end at proj coord %f %f%n", end.getX(), end.getY());
      double fdx = findAttributeDouble("dxKm");
      double fdy = findAttributeDouble("dyKm");
      parseInfo.format("  makeProjectionLC calc dx= %f file dx= %f%n", dx, fdx);
      parseInfo.format("  makeProjectionLC calc dy= %f file dy= %f%n", dy, fdy);
    }

    return new ProjectionCT(name, "FGDC", proj);
  }

  private ProjectionCT makeMercatorProjection(String name, int nx, int ny) throws NoSuchElementException {
    // double centralLat = findAttributeDouble( ds, "centralLat");
    // Center longitude for the mercator projection, where the mercator projection is parallel to the Earth's surface.
    // from this, i guess is actually transverse mercator
    // double centralLon = findAttributeDouble( ds, "centralLon");
    // lat0, central meridian, scale factor
    // TransverseMercator proj = new TransverseMercator(centralLat, centralLon, 1.0);

    double latDxDy = findAttributeDouble("latDxDy");
    double lonDxDy = findAttributeDouble("lonDxDy");

    // lat0, lon0, par
    Mercator proj = new Mercator(lonDxDy, latDxDy);

    // we have to project in order to find the start LOOK may be wrong
    double lat0 = findAttributeDouble("lat00");
    double lon0 = findAttributeDouble("lon00");
    ProjectionPoint start = proj.latLonToProj(new LatLonPointImpl(lat0, lon0));
    startx = start.getX();
    starty = start.getY();

    // we will use the end to compute grid size
    double latN = findAttributeDouble("latNxNy");
    double lonN = findAttributeDouble("lonNxNy");
    ProjectionPoint end = proj.latLonToProj(new LatLonPointImpl(latN, lonN));
    dx = (end.getX() - startx) / nx;
    dy = (end.getY() - starty) / ny;

    if (debugProj) {
      parseInfo.format("  makeProjectionMercator start at proj coord %f %f%n", startx, starty);
      parseInfo.format("  makeProjectionMercator end at proj coord %f %f%n", end.getX(), end.getY());
      double fdx = findAttributeDouble("dxKm");
      double fdy = findAttributeDouble("dyKm");
      parseInfo.format("  makeProjectionMercator calc dx= %f file dx= %f%n", dx, fdx);
      parseInfo.format("  makeProjectionMercator calc dy= %f file dy= %f%n", dy, fdy);
    }

    return new ProjectionCT(name, "FGDC", proj);
  }

  private CoordinateAxis.Builder makeLonCoordAxis(String xname) {
    CoordinateAxis1D.Builder v = CoordinateAxis1D.builder().setName(xname).setDataType(DataType.DOUBLE)
        .setParentGroupBuilder(rootGroup).setDimensionsByName(xname).setUnits(CDM.LON_UNITS).setDesc("longitude");
    v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));
    v.setAutoGen(startx, dx);

    parseInfo.format("Created Lon Coordinate Axis = %s%n", xname);
    return v;
  }

  private CoordinateAxis.Builder makeLatCoordAxis(String yname) {
    CoordinateAxis1D.Builder v = CoordinateAxis1D.builder().setName(yname).setDataType(DataType.DOUBLE)
        .setParentGroupBuilder(rootGroup).setDimensionsByName(yname).setUnits(CDM.LAT_UNITS).setDesc("latitude");
    v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));
    v.setAutoGen(starty, dy);
    parseInfo.format("Created Lat Coordinate Axis = %s%n", yname);
    return v;
  }

}
