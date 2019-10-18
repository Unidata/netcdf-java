package ucar.nc2.internal.dataset.conv;

import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;
import java.util.StringTokenizer;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.ArrayObject;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CF;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.ProjectionCT;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.internal.dataset.CoordSystemBuilder;
import ucar.nc2.internal.dataset.CoordSystemBuilderFactory;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.util.CancelTask;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.projection.LambertConformal;
import ucar.unidata.geoloc.projection.Stereographic;
import ucar.unidata.geoloc.projection.TransverseMercator;

/**
 * Default Coordinate Conventions.
 */
public class DefaultConventions extends CoordSystemBuilder {

  private static final Logger logger = LoggerFactory.getLogger(ucar.nc2.dataset.conv.DefaultConvention.class);

  protected ProjectionCT projCT;

  private DefaultConventions(NetcdfDataset.Builder datasetBuilder) {
    super(datasetBuilder);
    this.conventionName = "Default";
  }

  @Override
  public void augmentDataset(CancelTask cancelTask) {
    projCT = makeProjectionCT();
    if (projCT != null) {
      VariableDS.Builder vb = makeCoordinateTransformVariable(projCT);
      rootGroup.addVariable(vb);

      String xname = findCoordinateName(AxisType.GeoX);
      String yname = findCoordinateName(AxisType.GeoY);
      if (xname != null && yname != null) {
        vb.addAttribute(new Attribute(_Coordinate.Axes, xname + " " + yname));
      }
    }
  }

  @Override
  protected void identifyCoordinateAxes() {

    // Look for coord_axis or coord_alias attribute
    for (VarProcess vp : varList) {
      if (vp.isCoordinateVariable) {
        continue;
      }

      String dimName = findAlias(vp.vb);
      if (dimName.isEmpty()) {
        continue;
      }
      Optional<Dimension> dimOpt = this.rootGroup.findDimension(dimName);
      dimOpt.ifPresent(dime -> {
        vp.isCoordinateAxis = true;
        parseInfo.format(" Coordinate Axis added (alias) = %s for dimension %s%n", vp, dimName);
      });
    }

    // coordinates is an alias for _CoordinateAxes
    for (VarProcess vp : varList) {
      if (vp.coordinateAxes == null) { // dont override if already set
        String coordsString = vp.vb.getAttributeContainer().findAttValueIgnoreCase(CF.COORDINATES, null);
        if (coordsString != null) {
          vp.coordinates = coordsString;
        }
      }
    }

    super.identifyCoordinateAxes();

    /////////////////////////
    // now we start forcing
    HashMap<AxisType, VarProcess> map = new HashMap<>();

    // find existing axes, so we dont duplicate
    for (VarProcess vp : varList) {
      if (vp.isCoordinateAxis) {
        AxisType atype = getAxisType(vp.vb);
        if (atype != null) {
          map.put(atype, vp);
        }
      }
    }

    // look for time axes based on units
    if (map.get(AxisType.Time) == null) {
      for (VarProcess vp : varList) {
        String unit = vp.vb.units;
        if (unit != null && SimpleUnit.isDateUnit(unit)) {
          vp.isCoordinateAxis = true;
          map.put(AxisType.Time, vp);
          parseInfo.format(" Time Coordinate Axis added (unit) = %s from unit %s%n", vp.vb.getFullName(), unit);
          // break; // allow multiple time coords
        }
      }
    }

    // look for missing axes by using name hueristics
    for (VarProcess vp : varList) {
      if (vp.isCoordinateVariable) {
        continue;
      }
      AxisType atype = getAxisType(vp.vb);
      if (atype != null) {
        if (map.get(atype) == null) {
          vp.isCoordinateAxis = true;
          parseInfo.format(" Coordinate Axis added (Default forced) = %s for axis %s%n", vp.vb.getFullName(), atype);
          map.put(atype, vp);
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
  @Nullable
  private String findCoordinateName(AxisType axisType) {
    for (Variable.Builder vb : rootGroup.vbuilders) {
      if (vb instanceof VariableDS.Builder) {
        VariableDS.Builder vds = (VariableDS.Builder) vb;
        if (axisType == getAxisType(vds)) {
          return vds.getFullName();
        }
      }
    }
    return null;
  }

  /* @Override
  protected void makeCoordinateTransforms() {
    if (projCT != null) {
      VarProcess vp = findVarProcess(projCT.getName(), null);
      if (vp != null) {
        vp.ct = projCT;
      }
    }
    super.makeCoordinateTransforms();
  } */

  @Override
  @Nullable
  protected AxisType getAxisType(VariableDS.Builder vb) {
    AxisType result = getAxisTypeCoards(vb);
    if (result != null) {
      return result;
    }

    String vname = vb.shortName;
    if (vname == null) {
      return null;
    }
    String unit = vb.units;
    if (unit == null) {
      unit = "";
    }
    String desc = vb.desc;
    if (desc == null) {
      desc = "";
    }

    if (vname.equalsIgnoreCase("x") || findAlias(vb).equalsIgnoreCase("x")) {
      return AxisType.GeoX;
    }

    if (vname.equalsIgnoreCase("lon") || vname.equalsIgnoreCase("longitude") || findAlias(vb).equalsIgnoreCase("lon")) {
      return AxisType.Lon;
    }

    if (vname.equalsIgnoreCase("y") || findAlias(vb).equalsIgnoreCase("y")) {
      return AxisType.GeoY;
    }

    if (vname.equalsIgnoreCase("lat") || vname.equalsIgnoreCase("latitude") || findAlias(vb).equalsIgnoreCase("lat")) {
      return AxisType.Lat;
    }

    if (vname.equalsIgnoreCase("lev") || findAlias(vb).equalsIgnoreCase("lev")
        || (vname.equalsIgnoreCase("level") || findAlias(vb).equalsIgnoreCase("level"))) {
      return AxisType.GeoZ;
    }

    if (vname.equalsIgnoreCase("z") || findAlias(vb).equalsIgnoreCase("z") || vname.equalsIgnoreCase("altitude")
        || desc.contains("altitude") || vname.equalsIgnoreCase("depth") || vname.equalsIgnoreCase("elev")
        || vname.equalsIgnoreCase("elevation")) {
      if (SimpleUnit.isCompatible("m", unit)) // units of meters
      {
        return AxisType.Height;
      }
    }

    if (vname.equalsIgnoreCase("time") || findAlias(vb).equalsIgnoreCase("time")) {
      if (SimpleUnit.isDateUnit(unit)) {
        return AxisType.Time;
      }
    }

    if (vname.equalsIgnoreCase("time") && vb.dataType == DataType.STRING) {
      if (vb.orgVar != null) {
        try {
          Array firstValue = vb.orgVar.read("0");
          if (firstValue instanceof ArrayObject.D1) {
            ArrayObject.D1 sarry = (ArrayObject.D1) firstValue;
            String firstStringValue = (String) sarry.get(0);
            if (CalendarDate.parseISOformat(null, firstStringValue) != null) { // valid iso date string LOOK
              return AxisType.Time;
            }
          }
        } catch (IOException | InvalidRangeException e) {
          logger.warn("time string error", e);
        }
      }
    }

    return null;
  }

  // look for an coord_axis or coord_alias attribute
  private String findAlias(VariableDS.Builder vb) {
    String alias = vb.getAttributeContainer().findAttValueIgnoreCase("coord_axis", null);
    if (alias == null) {
      alias = vb.getAttributeContainer().findAttValueIgnoreCase("coord_alias", "");
    }
    if (alias == null) {
      alias = "";
    }
    return alias;
  }

  // replicated from COARDS, but we need to diverge from COARDS
  // we assume that coordinate axes get identified by being coordinate variables
  @Nullable
  private AxisType getAxisTypeCoards(VariableDS.Builder vb) {

    String unit = vb.units;
    if (unit == null) {
      return null;
    }

    if (unit.equalsIgnoreCase("degrees_east") || unit.equalsIgnoreCase("degrees_E") || unit.equalsIgnoreCase("degreesE")
        || unit.equalsIgnoreCase("degree_east") || unit.equalsIgnoreCase("degree_E")
        || unit.equalsIgnoreCase("degreeE")) {
      return AxisType.Lon;
    }

    if (unit.equalsIgnoreCase("degrees_north") || unit.equalsIgnoreCase("degrees_N")
        || unit.equalsIgnoreCase("degreesN") || unit.equalsIgnoreCase("degree_north")
        || unit.equalsIgnoreCase("degree_N") || unit.equalsIgnoreCase("degreeN")) {
      return AxisType.Lat;
    }

    if (SimpleUnit.isDateUnit(unit)) // || SimpleUnit.isTimeUnit(unit)) removed dec 18, 2008
    {
      return AxisType.Time;
    }

    // look for other z coordinate
    // if (SimpleUnit.isCompatible("m", unit))
    // return AxisType.Height;
    if (SimpleUnit.isCompatible("mbar", unit)) {
      return AxisType.Pressure;
    }
    if (unit.equalsIgnoreCase("level") || unit.equalsIgnoreCase("layer") || unit.equalsIgnoreCase("sigma_level")) {
      return AxisType.GeoZ;
    }

    String positive = vb.getAttributeContainer().findAttValueIgnoreCase("positive", null);
    if (positive != null) {
      if (SimpleUnit.isCompatible("m", unit)) {
        return AxisType.Height;
      } else {
        return AxisType.GeoZ;
      }
    }
    return null;
  }

  private ProjectionCT makeProjectionCT() {
    // look for projection in global attribute
    String projection = rootGroup.getAttributeContainer().findAttValueIgnoreCase("projection", null);
    if (null == projection) {
      parseInfo.format("Default Conventions error: NO projection name found %n");
      return null;
    }
    String params = rootGroup.getAttributeContainer().findAttValueIgnoreCase("projection_params", null);
    if (null == params) {
      params = rootGroup.getAttributeContainer().findAttValueIgnoreCase("proj_params", null);
    }
    if (null == params) {
      parseInfo.format("Default Conventions error: NO projection parameters found %n");
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

    parseInfo.format("Default Conventions projection %s params = %f %f %f %f%n", projection, p[0], p[1], p[2], p[3]);

    ProjectionImpl proj;
    if (projection.equalsIgnoreCase("LambertConformal")) {
      proj = new LambertConformal(p[0], p[1], p[2], p[3]);
    } else if (projection.equalsIgnoreCase("TransverseMercator")) {
      proj = new TransverseMercator(p[0], p[1], p[2]);
    } else if (projection.equalsIgnoreCase("Stereographic") || projection.equalsIgnoreCase("Oblique_Stereographic")) {
      proj = new Stereographic(p[0], p[1], p[2]);
    } else {
      parseInfo.format("Default Conventions error: Unknown projection %s%n", projection);
      return null;
    }

    return new ProjectionCT(proj.getClassName(), "FGDC", proj);
  }

  public static class Factory implements CoordSystemBuilderFactory {
    @Override
    @Nullable
    public String getConventionName() {
      return null;
    }

    @Override
    public CoordSystemBuilder open(NetcdfDataset.Builder datasetBuilder) {
      return new DefaultConventions(datasetBuilder);
    }
  }

}


