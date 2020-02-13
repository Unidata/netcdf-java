/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.dataset.conv;

import java.io.IOException;
import java.util.HashMap;
import java.util.StringTokenizer;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.CoordinateTransform;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.ProjectionCT;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.spi.CoordSystemBuilderFactory;
import ucar.nc2.internal.dataset.CoordSystemBuilder;
import ucar.nc2.util.CancelTask;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.projection.LambertConformal;
import ucar.unidata.geoloc.projection.Stereographic;
import ucar.unidata.geoloc.projection.TransverseMercator;

/** GDV Conventions. */
public class GDVConvention extends CSMConvention {
  private static final String CONVENTION_NAME = "GDV";

  public static class Factory implements CoordSystemBuilderFactory {
    @Override
    public String getConventionName() {
      return CONVENTION_NAME;
    }

    @Override
    public CoordSystemBuilder open(NetcdfDataset.Builder datasetBuilder) {
      return new GDVConvention(datasetBuilder);
    }
  }

  protected ProjectionCT projCT;

  GDVConvention(NetcdfDataset.Builder datasetBuilder) {
    super(datasetBuilder);
    this.conventionName = CONVENTION_NAME;
    checkForMeter = false;
  }

  @Override
  protected void augmentDataset(CancelTask cancelTask) throws IOException {
    projCT = makeProjectionCT();
    if (projCT != null) {
      VariableDS.Builder vb = makeCoordinateTransformVariable(projCT);
      rootGroup.addVariable(vb);

      String xname = findCoordinateName(AxisType.GeoX);
      String yname = findCoordinateName(AxisType.GeoY);
      if (xname != null && yname != null)
        vb.addAttribute(new Attribute(_Coordinate.Axes, xname + " " + yname));
    }
  }

  /** look for aliases. */
  @Override
  protected void identifyCoordinateAxes() {

    for (VarProcess vp : varList) {
      if (vp.isCoordinateVariable)
        continue;

      if (vp.vb.dataType == DataType.STRUCTURE)
        continue; // cant be a structure

      String dimName = findAlias(vp.vb);
      if (dimName.isEmpty()) // none
        continue;
      rootGroup.findDimension(dimName).ifPresent(dim -> {
        vp.isCoordinateAxis = true;
        parseInfo.format(" Coordinate Axis added (GDV alias) = %s for dimension %s%n", vp.vb.getFullName(), dimName);
      });
    }

    super.identifyCoordinateAxes();

    // desperado
    identifyCoordinateAxesForce();
  }

  private void identifyCoordinateAxesForce() {

    HashMap<AxisType, VarProcess> map = new HashMap<>();

    // find existing axes, so we dont duplicate
    for (VarProcess vp : varList) {
      if (vp.isCoordinateAxis) {
        AxisType atype = getAxisType(vp.vb);
        if (atype != null)
          map.put(atype, vp);
      }
    }

    // look for variables to turn into axes
    for (VarProcess vp : varList) {
      if (vp.isCoordinateVariable)
        continue;
      if (vp.vb.dataType == DataType.STRUCTURE)
        continue; // cant be a structure

      AxisType atype = getAxisType(vp.vb);
      if (atype != null) {
        if (map.get(atype) == null) {
          vp.isCoordinateAxis = true;
          parseInfo.format(" Coordinate Axis added (GDV forced) = %s  for axis %s%n", vp.vb.getFullName(), atype);
        }
      }
    }
  }

  /**
   * look for aliases.
   * 
   * @param axisType look for this axis type
   * @return name of axis of that type
   */
  private String findCoordinateName(AxisType axisType) {
    for (Variable.Builder aVlist : rootGroup.vbuilders) {
      VariableDS.Builder ve = (VariableDS.Builder) aVlist;
      if (axisType == getAxisType(ve)) {
        return ve.getFullName();
      }
    }
    return null;
  }

  @Override
  protected void makeCoordinateTransforms() {
    if (projCT != null) {
      VarProcess vp = findVarProcess(projCT.getName(), null);
      if (vp != null) {
        vp.isCoordinateTransform = true;
        vp.ct = CoordinateTransform.builder().setPreBuilt(projCT);
        coords.addCoordinateTransform(vp.ct);
      }
    }
    super.makeCoordinateTransforms();
  }

  @Override
  protected AxisType getAxisType(VariableDS.Builder v) {
    String vname = v.shortName;

    if (vname.equalsIgnoreCase("x") || findAlias(v).equalsIgnoreCase("x"))
      return AxisType.GeoX;

    if (vname.equalsIgnoreCase("lon") || vname.equalsIgnoreCase("longitude") || findAlias(v).equalsIgnoreCase("lon"))
      return AxisType.Lon;

    if (vname.equalsIgnoreCase("y") || findAlias(v).equalsIgnoreCase("y"))
      return AxisType.GeoY;

    if (vname.equalsIgnoreCase("lat") || vname.equalsIgnoreCase("latitude") || findAlias(v).equalsIgnoreCase("lat"))
      return AxisType.Lat;

    if (vname.equalsIgnoreCase("lev") || findAlias(v).equalsIgnoreCase("lev")
        || (vname.equalsIgnoreCase("level") || findAlias(v).equalsIgnoreCase("level")))
      return AxisType.GeoZ;

    if (vname.equalsIgnoreCase("z") || findAlias(v).equalsIgnoreCase("z")
        || (vname.equalsIgnoreCase("altitude") || vname.equalsIgnoreCase("depth")))
      return AxisType.Height;

    if (vname.equalsIgnoreCase("time") || findAlias(v).equalsIgnoreCase("time"))
      return AxisType.Time;

    return super.getAxisType(v);
  }

  // look for an coord_axis or coord_alias attribute
  private String findAlias(Variable.Builder v) {
    String alias = v.getAttributeContainer().findAttValueIgnoreCase("coord_axis", null);
    if (alias == null)
      alias = v.getAttributeContainer().findAttValueIgnoreCase("coord_alias", "");
    return alias;
  }

  private ProjectionCT makeProjectionCT() {
    // look for projection in global attribute
    String projection = rootGroup.getAttributeContainer().findAttValueIgnoreCase("projection", null);
    if (null == projection) {
      parseInfo.format("GDV Conventions error: NO projection name found %n");
      return null;
    }
    String params = rootGroup.getAttributeContainer().findAttValueIgnoreCase("projection_params", null);
    if (null == params)
      params = rootGroup.getAttributeContainer().findAttValueIgnoreCase("proj_params", null);
    if (null == params) {
      parseInfo.format("GDV Conventions error: NO projection parameters found %n");
      return null;
    }

    // parse the parameters
    int count = 0;
    double[] p = new double[4];
    try {
      // new way : just the parameters
      StringTokenizer stoke = new StringTokenizer(params, " ,");
      while (stoke.hasMoreTokens() && (count < 4)) {
        p[count++] = Double.parseDouble(stoke.nextToken());
      }
    } catch (NumberFormatException e) {
      // old way : every other one
      StringTokenizer stoke = new StringTokenizer(params, " ,");
      while (stoke.hasMoreTokens() && (count < 4)) {
        stoke.nextToken(); // skip
        p[count++] = Double.parseDouble(stoke.nextToken());
      }

    }

    parseInfo.format("GDV Conventions projection %s params = %f %f %f %f%n", projection, p[0], p[1], p[2], p[3]);

    ProjectionImpl proj;
    if (projection.equalsIgnoreCase("LambertConformal"))
      proj = new LambertConformal(p[0], p[1], p[2], p[3]);
    else if (projection.equalsIgnoreCase("TransverseMercator"))
      proj = new TransverseMercator(p[0], p[1], p[2]);
    else if (projection.equalsIgnoreCase("Stereographic") || projection.equalsIgnoreCase("Oblique_Stereographic"))
      proj = new Stereographic(p[0], p[1], p[2]);
    else {
      parseInfo.format("GDV Conventions error: Unknown projection %s%n", projection);
      return null;
    }

    return new ProjectionCT(proj.getClassName(), "FGDC", proj);
  }

}
