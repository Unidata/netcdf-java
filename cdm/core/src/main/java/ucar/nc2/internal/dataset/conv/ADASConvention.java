/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.dataset.conv;

import java.io.IOException;
import java.util.Optional;
import ucar.ma2.Array;
import ucar.ma2.ArrayFloat;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateTransform;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.ProjectionCT;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.spi.CoordSystemBuilderFactory;
import ucar.nc2.internal.dataset.CoordSystemBuilder;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.util.CancelTask;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.projection.LambertConformal;

/** ADAS netcdf files. Not finished because we dont have any tests files. */
public class ADASConvention extends CoordSystemBuilder {
  private static final String CONVENTION_NAME = "ARPS/ADAS";

  // private double originX = 0.0, originY = 0.0;
  private ProjectionCT projCT;
  private static final boolean debugProj = false;

  ADASConvention(NetcdfDataset.Builder datasetBuilder) {
    super(datasetBuilder);
    this.conventionName = CONVENTION_NAME;
  }

  @Override
  protected void augmentDataset(CancelTask cancelTask) throws IOException {
    if (!rootGroup.findVariableLocal("x").isPresent()) {
      return; // check if its already been done - aggregating enhanced datasets.
    }

    // old way without attributes
    Attribute att = rootGroup.getAttributeContainer().findAttribute("MAPPROJ");
    int projType = att.getNumericValue().intValue();

    double lat1 = rootGroup.getAttributeContainer().findAttributeDouble("TRUELAT1", Double.NaN);
    double lat2 = rootGroup.getAttributeContainer().findAttributeDouble("TRUELAT2", Double.NaN);
    double lat_origin = lat1;
    double lon_origin = rootGroup.getAttributeContainer().findAttributeDouble("TRUELON", Double.NaN);
    double false_easting = 0.0;
    double false_northing = 0.0;

    // new way with attributes
    String projName = rootGroup.getAttributeContainer().findAttValueIgnoreCase(CF.GRID_MAPPING_NAME, null);
    if (projName != null) {
      projName = projName.trim();
      lat_origin = rootGroup.getAttributeContainer().findAttributeDouble("latitude_of_projection_origin", Double.NaN);
      lon_origin = rootGroup.getAttributeContainer().findAttributeDouble("longitude_of_central_meridian", Double.NaN);
      false_easting = rootGroup.getAttributeContainer().findAttributeDouble("false_easting", 0.0);
      false_northing = rootGroup.getAttributeContainer().findAttributeDouble("false_northing", 0.0);

      Attribute att2 = rootGroup.getAttributeContainer().findAttributeIgnoreCase("standard_parallel");
      if (att2 != null) {
        lat1 = att2.getNumericValue().doubleValue();
        lat2 = (att2.getLength() > 1) ? att2.getNumericValue(1).doubleValue() : lat1;
      }
    } else {
      if (projType == 2)
        projName = "lambert_conformal_conic";
    }

    Optional<Variable.Builder<?>> coordOpt = rootGroup.findVariableLocal("x_stag");
    if (coordOpt.isPresent()) {
      Variable.Builder<?> coord = coordOpt.get();
      if (!Double.isNaN(false_easting) || !Double.isNaN(false_northing)) {
        String units = coord.getAttributeContainer().findAttValueIgnoreCase(CDM.UNITS, null);
        double scalef = 1.0;
        try {
          scalef = SimpleUnit.getConversionFactor(units, "km");
        } catch (IllegalArgumentException e) {
          log.error(units + " not convertible to km");
        }
        false_easting *= scalef;
        false_northing *= scalef;
      }
    }

    ProjectionImpl proj;
    if ("lambert_conformal_conic".equalsIgnoreCase(projName)) {
      proj = new LambertConformal(lat_origin, lon_origin, lat1, lat2, false_easting, false_northing);
      projCT = new ProjectionCT("Projection", "FGDC", proj);
      if (false_easting == 0.0)
        calcCenterPoints(proj); // old way
    } else {
      parseInfo.format("ERROR: unknown projection type = %s%n", projName);
    }

    if (projCT != null) {
      VariableDS.Builder v = makeCoordinateTransformVariable(projCT);
      v.addAttribute(new Attribute(_Coordinate.AxisTypes, "GeoX GeoY"));
      rootGroup.addVariable(v);
    }

    makeCoordAxis("x");
    makeCoordAxis("y");
    makeCoordAxis("z");

    rootGroup.findVariableLocal("ZPSOIL")
        .ifPresent(vb -> vb.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.GeoZ.toString())));
  }

  // old way without attributes
  private void calcCenterPoints(Projection proj) throws IOException {
    double lat_check = rootGroup.getAttributeContainer().findAttributeDouble("CTRLAT", Double.NaN);
    double lon_check = rootGroup.getAttributeContainer().findAttributeDouble("CTRLON", Double.NaN);

    LatLonPointImpl lpt0 = new LatLonPointImpl(lat_check, lon_check);
    ProjectionPoint ppt0 = proj.latLonToProj(lpt0);

    VariableDS.Builder xstag = (VariableDS.Builder) rootGroup.findVariableLocal("x_stag")
        .orElseThrow(() -> new IllegalStateException("Must have x_stag Variable"));
    Variable xstagOrg = xstag.orgVar;
    int nxpts = (int) xstagOrg.getSize();
    ArrayFloat.D1 xstagData = (ArrayFloat.D1) xstagOrg.read();
    float center_x = xstagData.get(nxpts - 1);
    double false_easting = center_x / 2000 - ppt0.getX() * 1000.0;

    VariableDS.Builder ystag = (VariableDS.Builder) rootGroup.findVariableLocal("y_stag")
        .orElseThrow(() -> new IllegalStateException("Must have y_stag Variable"));
    Variable ystagOrg = ystag.orgVar;
    int nypts = (int) ystagOrg.getSize();
    ArrayFloat.D1 ystagData = (ArrayFloat.D1) ystagOrg.read();
    float center_y = ystagData.get(nypts - 1);
    double false_northing = center_y / 2000 - ppt0.getY() * 1000.0;
    log.debug("false easting/northing= {} {} ", false_easting, false_northing);

    double dx = rootGroup.getAttributeContainer().findAttributeDouble("DX", Double.NaN);
    double dy = rootGroup.getAttributeContainer().findAttributeDouble("DY", Double.NaN);

    double w = dx * (nxpts - 1);
    double h = dy * (nypts - 1);
    double startx = ppt0.getX() * 1000.0 - w / 2;
    double starty = ppt0.getY() * 1000.0 - h / 2;

    xstag.setAutoGen(startx, dx);
    ystag.setAutoGen(starty, dy);
  }

  /////////////////////////////////////////////////////////////////////////

  @Override
  protected void makeCoordinateTransforms() {
    if (projCT != null) {
      VarProcess vp = findVarProcess(projCT.getName(), null);
      vp.isCoordinateTransform = true;
      vp.ct = CoordinateTransform.builder().setPreBuilt(projCT);
    }
    super.makeCoordinateTransforms();
  }

  @Override
  protected AxisType getAxisType(VariableDS.Builder vb) {
    String vname = vb.shortName;

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

    if (vname.equalsIgnoreCase("time"))
      return AxisType.Time;

    String unit = vb.getUnits();
    if (unit != null) {
      if (SimpleUnit.isCompatible("millibar", unit))
        return AxisType.Pressure;

      if (SimpleUnit.isCompatible("m", unit))
        return AxisType.Height;
    }


    return null;
  }

  /**
   * Does increasing values of Z go vertical up?
   *
   * @param v for this axis
   * @return "up" if this is a Vertical (z) coordinate axis which goes up as coords get bigger,
   *         else return "down"
   */
  public String getZisPositive(CoordinateAxis v) {
    return "down"; // eta coords decrease upward
  }

  private void makeCoordAxis(String axisName) throws IOException {
    String name = axisName + "_stag";
    if (!rootGroup.findVariableLocal(name).isPresent()) {
      return;
    }
    VariableDS.Builder stagV = (VariableDS.Builder) rootGroup.findVariableLocal(name).get();
    Array data_stag = stagV.orgVar.read();
    int n = (int) data_stag.getSize() - 1;
    DataType dt = DataType.getType(data_stag);
    Array data = Array.factory(dt, new int[] {n});
    Index stagIndex = data_stag.getIndex();
    Index dataIndex = data.getIndex();
    for (int i = 0; i < n; i++) {
      double val = data_stag.getDouble(stagIndex.set(i)) + data_stag.getDouble(stagIndex.set(i + 1));
      data.setDouble(dataIndex.set(i), 0.5 * val);
    }

    DataType dtype = DataType.getType(data);
    String units = stagV.getAttributeContainer().findAttValueIgnoreCase(CDM.UNITS, "m");
    CoordinateAxis.Builder cb = CoordinateAxis1D.builder().setName(axisName).setDataType(dtype)
        .setParentGroupBuilder(rootGroup).setDimensionsByName(axisName).setUnits(units)
        .setDesc("synthesized non-staggered " + axisName + " coordinate");
    cb.setCachedData(data, true);
    datasetBuilder.replaceCoordinateAxis(rootGroup, cb);
  }

  public static class Factory implements CoordSystemBuilderFactory {
    @Override
    public String getConventionName() {
      return CONVENTION_NAME;
    }

    @Override
    public CoordSystemBuilder open(NetcdfDataset.Builder datasetBuilder) {
      return new ADASConvention(datasetBuilder);
    }
  }

}
