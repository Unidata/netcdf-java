/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.dataset.conv;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import javax.annotation.Nullable;

import ucar.array.ArrayType;
import ucar.array.Array;
import ucar.array.Arrays;
import ucar.array.InvalidRangeException;
import ucar.array.Section;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.*;
import ucar.nc2.dataset.spi.CoordSystemBuilderFactory;
import ucar.nc2.internal.dataset.CoordSystemBuilder;
import ucar.nc2.internal.dataset.transform.horiz.ProjectionCTV;
import ucar.nc2.iosp.NetcdfFormatUtils;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.util.CancelTask;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.projection.LambertConformal;
import ucar.unidata.geoloc.projection.Stereographic;
import ucar.unidata.util.StringUtil2;

/** AWIPS netcdf output. */
public class AWIPSConvention extends CoordSystemBuilder {
  private static final String CONVENTION_NAME = "AWIPS";

  private static final boolean debugProj = false;
  private static final boolean debugBreakup = false;

  AWIPSConvention(NetcdfDataset.Builder<?> datasetBuilder) {
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
      return (null != ncfile.findAttribute("projName")) && (null != ncfile.findDimension("charsPerLevel"))
          && (null != ncfile.findDimension("x")) && (null != ncfile.findDimension("y"));
    }

    @Override
    public CoordSystemBuilder open(NetcdfDataset.Builder<?> datasetBuilder) {
      return new AWIPSConvention(datasetBuilder);
    }
  }

  ProjectionCTV projCT;
  double startx, starty, dx, dy;

  @Override
  public void augmentDataset(CancelTask cancelTask) {
    if (rootGroup.findVariableLocal("x").isPresent()) {
      return; // check if its already been done - aggregating enhanced datasets.
    }

    int nx = rootGroup.findDimension("x").map(Dimension::getLength)
        .orElseThrow(() -> new RuntimeException("missing dimension x"));
    int ny = rootGroup.findDimension("y").map(Dimension::getLength)
        .orElseThrow(() -> new RuntimeException("missing dimension y"));

    String projName = rootGroup.getAttributeContainer().findAttributeString("projName", "none");
    if (projName.equalsIgnoreCase("LATLON")) {
      datasetBuilder.replaceCoordinateAxis(rootGroup, makeLonCoordAxis(nx, "x"));
      datasetBuilder.replaceCoordinateAxis(rootGroup, makeLatCoordAxis(ny, "y"));
    } else if (projName.equalsIgnoreCase("LAMBERT_CONFORMAL")) {
      projCT = makeLCProjection(projName);
      datasetBuilder.replaceCoordinateAxis(rootGroup, makeXCoordAxis("x"));
      datasetBuilder.replaceCoordinateAxis(rootGroup, makeYCoordAxis("y"));
    } else if (projName.equalsIgnoreCase("STEREOGRAPHIC")) {
      projCT = makeStereoProjection(projName);
      datasetBuilder.replaceCoordinateAxis(rootGroup, makeXCoordAxis("x"));
      datasetBuilder.replaceCoordinateAxis(rootGroup, makeYCoordAxis("y"));
    }

    CoordinateAxis.Builder<?> timeCoord = makeTimeCoordAxis();
    if (timeCoord != null) {
      datasetBuilder.replaceCoordinateAxis(rootGroup, timeCoord);
      String dimName = timeCoord.getFirstDimensionName();
      if (!timeCoord.shortName.equals(dimName)) {
        timeCoord.addAttribute(new Attribute(_Coordinate.AliasForDimension, dimName));
      }
    }

    // AWIPS cleverly combines multiple z levels into a single variable (!!)
    for (Variable.Builder<?> ncvar : ImmutableList.copyOf(rootGroup.vbuilders)) {
      String levelName = ncvar.shortName + "Levels";
      if (rootGroup.findVariableLocal(levelName).isPresent()) {
        VariableDS.Builder<?> levelVar = (VariableDS.Builder<?>) rootGroup.findVariableLocal(levelName).get();
        if (levelVar.getRank() != 2)
          continue;
        if (levelVar.dataType != ArrayType.CHAR)
          continue;

        try {
          List<Dimension> levels = breakupLevels(levelVar);
          createNewVariables((VariableDS.Builder<?>) ncvar, levels, levelVar.orgVar.getDimension(0));
        } catch (InvalidRangeException ioe) {
          parseInfo.format("createNewVariables IOException%n");
        }
      }
    }

    if (projCT != null) {
      VariableDS.Builder<?> v = makeCoordinateTransformVariable(projCT);
      v.addAttribute(new Attribute(_Coordinate.Axes, "x y"));
      rootGroup.addVariable(v);
    }

    // kludge in fixing the units
    for (Variable.Builder<?> v : rootGroup.vbuilders) {
      String units = v.getAttributeContainer().findAttributeString(CDM.UNITS, null);
      if (units != null) {
        ((VariableDS.Builder<?>) v).setUnits(normalize(units)); // removes the old
      }
    }
  }

  // pretty much WRF specific
  private String normalize(String units) {
    if (units.equals("/second"))
      units = "1/sec";
    if (units.equals("degrees K"))
      units = "K";
    else {
      units = units.replace("**", "^");
      units = StringUtil2.remove(units, ')');
      units = StringUtil2.remove(units, '(');
    }
    return units;
  }

  // TODO not dealing with "FHAG 0 10 ", "FHAG 0 30 "
  // take a combined level variable and create multiple levels out of it
  // return the list of Dimensions that were created
  private List<Dimension> breakupLevels(VariableDS.Builder<?> levelVar) {
    if (debugBreakup)
      parseInfo.format("breakupLevels = %s%n", levelVar.shortName);

    List<Dimension> dimList = new ArrayList<>();
    Array<Byte> levelVarData;
    try {
      levelVarData = (Array<Byte>) levelVar.orgVar.readArray();
    } catch (IOException ioe) {
      return dimList;
    }

    List<String> values = null;
    String currentUnits = null;
    Array<String> levels = Arrays.makeStringsFromChar(levelVarData);
    for (String levelS : levels) {
      StringTokenizer stoke = new StringTokenizer(levelS);

      /*
       * problem with blank string:
       * char pvvLevels(levels_35=35, charsPerLevel=10);
       * "MB 1000   ", "MB 975    ", "MB 950    ", "MB 925    ", "MB 900    ", "MB 875    ", "MB 850    ", "MB 825    ",
       * "MB 800    ", "MB 775    ", "MB 750    ",
       * "MB 725    ", "MB 700    ", "MB 675    ", "MB 650    ", "MB 625    ", "MB 600    ", "MB 575    ", "MB 550    ",
       * "MB 525    ", "MB 500    ", "MB 450    ",
       * "MB 400    ", "MB 350    ", "MB 300    ", "MB 250    ", "MB 200    ", "MB 150    ", "MB 100    ", "BL 0 30   ",
       * "BL 60 90  ", "BL 90 120 ", "BL 120 150",
       * "BL 150 180", ""
       */
      if (!stoke.hasMoreTokens())
        continue; // skip it

      // first token is the unit
      String units = stoke.nextToken().trim();
      if (!units.equals(currentUnits)) {
        if (values != null)
          dimList.add(makeZCoordAxis(values, currentUnits));
        values = new ArrayList<>();
        currentUnits = units;
      }

      // next token is the value
      if (stoke.hasMoreTokens())
        values.add(stoke.nextToken());
      else
        values.add("0");
    }
    if (values != null)
      dimList.add(makeZCoordAxis(values, currentUnits));

    if (debugBreakup)
      parseInfo.format("  done breakup%n");

    return dimList;
  }

  // make a new variable out of the list in "values"
  private Dimension makeZCoordAxis(List<String> values, String units) {
    int len = values.size();
    String name = makeZCoordName(units);
    if (len > 1) {
      name = name + len;
    } else {
      name = name + values.get(0);
    }
    StringUtil2.replace(name, ' ', "-");

    if (rootGroup.findDimension(name).isPresent()) {
      Dimension dim = rootGroup.findDimension(name).get();
      if (dim.getLength() == len) {
        if (rootGroup.findVariableLocal(name).isPresent()) {
          return dim;
        }
      }
    }

    String orgName = name;
    int count = 1;
    while (rootGroup.findDimension(name).isPresent()) {
      name = orgName + "-" + count;
      count++;
    }

    // create new one
    Dimension dim = new Dimension(name, len);
    rootGroup.addDimension(dim);
    if (debugBreakup) {
      parseInfo.format("  make Dimension = %s length = %d%n", name, len);
      parseInfo.format("  make ZCoordAxis = = %s length = %d%n", name, len);
    }

    CoordinateAxis1D.Builder<?> v =
        CoordinateAxis1D.builder().setName(name).setArrayType(ArrayType.DOUBLE).setParentGroupBuilder(rootGroup)
            .setDimensionsByName(name).setUnits(makeUnitsName(units)).setDesc(makeLongName(name));
    String positive = getZisPositive(v);
    if (null != positive) {
      v.addAttribute(new Attribute(_Coordinate.ZisPositive, positive));
    }

    double[] dvalues = new double[values.size()];
    int countv = 0;
    for (String s : values) {
      try {
        dvalues[countv++] = Double.parseDouble(s);
      } catch (NumberFormatException e) {
        System.out.printf("NumberFormatException '%s'", s);
        throw e;
      }
    }

    Array<Double> data = Arrays.factory(ArrayType.DOUBLE, new int[] {values.size()}, dvalues);
    v.setSourceData(data);
    datasetBuilder.replaceCoordinateAxis(rootGroup, v);

    parseInfo.format("Created Z Coordinate Axis = %s%n", name);
    return dim;
  }

  private String makeZCoordName(String units) {
    if (units.equalsIgnoreCase("MB"))
      return "PressureLevels";
    if (units.equalsIgnoreCase("K"))
      return "PotTempLevels";
    if (units.equalsIgnoreCase("BL"))
      return "BoundaryLayers";

    if (units.equalsIgnoreCase("FHAG"))
      return "FixedHeightAboveGround";
    if (units.equalsIgnoreCase("FH"))
      return "FixedHeight";
    if (units.equalsIgnoreCase("SFC"))
      return "Surface";
    if (units.equalsIgnoreCase("MSL"))
      return "MeanSeaLevel";
    if (units.equalsIgnoreCase("FRZ"))
      return "FreezingLevel";
    if (units.equalsIgnoreCase("TROP"))
      return "Tropopause";
    if (units.equalsIgnoreCase("MAXW"))
      return "MaxWindLevel";
    return units;
  }

  private String makeUnitsName(String units) {
    if (units.equalsIgnoreCase("MB"))
      return "hPa";
    if (units.equalsIgnoreCase("BL"))
      return "hPa";
    if (units.equalsIgnoreCase("FHAG"))
      return "m";
    if (units.equalsIgnoreCase("FH"))
      return "m";
    return "";
  }

  private String makeLongName(String name) {
    if (name.equalsIgnoreCase("PotTempLevels"))
      return "Potential Temperature Level";
    if (name.equalsIgnoreCase("BoundaryLayers"))
      return "BoundaryLayer hectoPascals above ground";
    else
      return name;
  }

  // create new variables as sections of ncVar
  private void createNewVariables(VariableDS.Builder<?> ncVar, List<Dimension> newDims, Dimension levelDim)
      throws InvalidRangeException {

    ArrayList<Dimension> dims = new ArrayList<>(ncVar.orgVar.getDimensions());
    int newDimIndex = dims.indexOf(levelDim);

    int[] origin = new int[ncVar.getRank()];
    int[] shape = ncVar.orgVar.getShape();
    int count = 0;
    for (Dimension dim : newDims) {
      origin[newDimIndex] = count;
      shape[newDimIndex] = dim.getLength();
      Variable varSection = ncVar.orgVar.section(new Section(origin, shape));

      String name = ncVar.shortName + "-" + dim.getShortName();
      VariableDS.Builder<?> varNew =
          VariableDS.builder().setName(name).setOriginalVariable(varSection).setArrayType(ncVar.dataType);
      dims.set(newDimIndex, dim);
      varNew.addDimensions(dims);
      varNew.addAttributes(ncVar.getAttributeContainer());

      // synthesize long name
      String long_name = ncVar.getAttributeContainer().findAttributeString(CDM.LONG_NAME, ncVar.shortName);
      long_name = long_name + "-" + dim.getShortName();
      varNew.getAttributeContainer().addAttribute(new Attribute(CDM.LONG_NAME, long_name));

      rootGroup.addVariable(varNew);
      parseInfo.format("Created New Variable as section = %s%n", name);
      count += dim.getLength();
    }
  }

  @Override
  @Nullable
  protected AxisType getAxisType(VariableDS.Builder<?> v) {
    String vname = v.shortName;

    if (vname.equalsIgnoreCase("x"))
      return AxisType.GeoX;
    if (vname.equalsIgnoreCase("lon"))
      return AxisType.Lon;
    if (vname.equalsIgnoreCase("y"))
      return AxisType.GeoY;
    if (vname.equalsIgnoreCase("lat"))
      return AxisType.Lat;
    if (vname.equalsIgnoreCase("record"))
      return AxisType.Time;

    String dimName = v.getFirstDimensionName();
    if ((dimName != null) && dimName.equalsIgnoreCase("record"))
      return AxisType.Time;

    String unit = v.getUnits();
    if (unit != null) {
      if (SimpleUnit.isCompatible("millibar", unit))
        return AxisType.Pressure;
      if (SimpleUnit.isCompatible("m", unit))
        return AxisType.Height;
    }
    // otherwise guess
    return AxisType.GeoZ;
  }

  @Override
  protected void makeCoordinateTransforms() {
    if (projCT != null) {
      VarProcess vp = findVarProcess(projCT.getName(), null);
      vp.isCoordinateTransform = true;
      vp.ctv = projCT;
    }
    super.makeCoordinateTransforms();
  }

  private String getZisPositive(CoordinateAxis.Builder<?> v) {
    String attValue = v.getAttributeContainer().findAttributeString("positive", null);
    if (null != attValue) {
      return attValue.equalsIgnoreCase("up") ? "up" : "down";
    }

    String unit = v.getUnits();
    if ((unit != null) && SimpleUnit.isCompatible("millibar", unit))
      return "down";
    if ((unit != null) && SimpleUnit.isCompatible("m", unit))
      return "up";

    // dunno
    return null;
  }

  private ProjectionCTV makeLCProjection(String name) throws NoSuchElementException {
    double centralLat = findAttributeDouble("centralLat");
    double centralLon = findAttributeDouble("centralLon");
    double rotation = findAttributeDouble("rotation");

    // we have to project in order to find the origin
    LambertConformal lc = new LambertConformal(rotation, centralLon, centralLat, centralLat);
    double lat0 = findAttributeDouble("lat00");
    double lon0 = findAttributeDouble("lon00");
    ProjectionPoint start = lc.latLonToProj(LatLonPoint.create(lat0, lon0));
    if (debugProj)
      parseInfo.format("getLCProjection start at proj coord %s%n", start);
    startx = start.getX();
    starty = start.getY();
    dx = findAttributeDouble("dxKm");
    dy = findAttributeDouble("dyKm");

    return new ProjectionCTV(name, lc);
  }

  private ProjectionCTV makeStereoProjection(String name) throws NoSuchElementException {
    double centralLat = findAttributeDouble("centralLat");
    double centralLon = findAttributeDouble("centralLon");

    // scale factor at lat = k = 2*k0/(1+sin(lat)) [Snyder,Working Manual p157]
    // then to make scale = 1 at lat, k0 = (1+sin(lat))/2
    double latDxDy = findAttributeDouble("latDxDy");
    double latR = Math.toRadians(latDxDy);
    double scale = (1.0 + Math.abs(Math.sin(latR))) / 2; // thanks to R Schmunk

    // Stereographic(double latt, double lont, double scale)

    Stereographic proj = new Stereographic(centralLat, centralLon, scale);
    // we have to project in order to find the origin
    double lat0 = findAttributeDouble("lat00");
    double lon0 = findAttributeDouble("lon00");
    ProjectionPoint start = proj.latLonToProj(LatLonPoint.create(lat0, lon0));
    startx = start.getX();
    starty = start.getY();
    dx = findAttributeDouble("dxKm");
    dy = findAttributeDouble("dyKm");

    // projection info
    parseInfo.format("---makeStereoProjection start at proj coord %s%n", start);

    double latN = findAttributeDouble("latNxNy");
    double lonN = findAttributeDouble("lonNxNy");
    ProjectionPoint pt = proj.latLonToProj(LatLonPoint.create(latN, lonN));
    parseInfo.format("                        end at proj coord %s%n", pt);
    parseInfo.format("                        scale= %f%n", scale);

    return new ProjectionCTV(name, proj);
  }

  CoordinateAxis.Builder<?> makeXCoordAxis(String xname) {
    CoordinateAxis1D.Builder<?> v = CoordinateAxis1D.builder().setName(xname).setArrayType(ArrayType.DOUBLE)
        .setParentGroupBuilder(rootGroup).setDimensionsByName(xname).setUnits("km").setDesc("x on projection");
    v.setAutoGen(startx, dx);

    parseInfo.format("Created X Coordinate Axis = %s%n", xname);
    return v;
  }

  CoordinateAxis.Builder<?> makeYCoordAxis(String yname) {
    CoordinateAxis1D.Builder<?> v = CoordinateAxis1D.builder().setName(yname).setArrayType(ArrayType.DOUBLE)
        .setParentGroupBuilder(rootGroup).setDimensionsByName(yname).setUnits("km").setDesc("y on projection");
    v.setAutoGen(starty, dy);

    parseInfo.format("Created Y Coordinate Axis = %s%n", yname);
    return v;
  }

  @Nullable
  private CoordinateAxis.Builder<?> makeLonCoordAxis(int n, String xname) {
    double min = findAttributeDouble("xMin");
    double max = findAttributeDouble("xMax");
    double d = findAttributeDouble("dx");
    if (Double.isNaN(min) || Double.isNaN(max) || Double.isNaN(d))
      return null;

    CoordinateAxis1D.Builder<?> v = CoordinateAxis1D.builder().setName(xname).setArrayType(ArrayType.DOUBLE)
        .setParentGroupBuilder(rootGroup).setDimensionsByName(xname).setUnits(CDM.LON_UNITS).setDesc("longitude");
    v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));
    v.setAutoGen(min, d);

    double maxCalc = min + d * n;
    parseInfo.format("Created Lon Coordinate Axis (max calc= %f should be = %f)%n", maxCalc, max);
    return v;
  }

  private CoordinateAxis.Builder<?> makeLatCoordAxis(int n, String name) {
    double min = findAttributeDouble("yMin");
    double max = findAttributeDouble("yMax");
    double d = findAttributeDouble("dy");
    if (Double.isNaN(min) || Double.isNaN(max) || Double.isNaN(d))
      return null;

    CoordinateAxis1D.Builder<?> v = CoordinateAxis1D.builder().setName(name).setArrayType(ArrayType.DOUBLE)
        .setParentGroupBuilder(rootGroup).setDimensionsByName(name).setUnits(CDM.LAT_UNITS).setDesc("latitude");
    v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));
    v.setAutoGen(min, d);

    double maxCalc = min + d * n;
    parseInfo.format("Created Lat Coordinate Axis (max calc= %f should be = %f)%n", maxCalc, max);
    return v;
  }

  private CoordinateAxis.Builder<?> makeTimeCoordAxis() {
    VariableDS.Builder<?> timeVar = (VariableDS.Builder<?>) rootGroup.findVariableLocal("valtimeMINUSreftime")
        .orElseThrow(() -> new RuntimeException("must have varible 'valtimeMINUSreftime'"));

    Dimension recordDim =
        rootGroup.findDimension("record").orElseThrow(() -> new RuntimeException("must have dimension 'record'"));

    Array<Number> vals;
    try {
      vals = (Array<Number>) timeVar.orgVar.readArray();
    } catch (IOException ioe) {
      return null;
    }

    // it seems that the record dimension does not always match valtimeMINUSreftime dimension!!
    // HAHAHAHAHAHAHAHA !
    int recLen = recordDim.getLength();
    int valLen = (int) vals.getSize();
    if (recLen != valLen) {
      try {
        Section section = new Section(new int[] {0}, new int[] {recordDim.getLength()});
        vals = Arrays.section(vals, section);
        parseInfo.format(" corrected the TimeCoordAxis length%n");
      } catch (InvalidRangeException e) {
        parseInfo.format("makeTimeCoordAxis InvalidRangeException%n");
      }
    }

    // create the units out of the filename if possible
    String units = makeTimeUnitFromFilename(datasetBuilder.location);
    if (units == null) { // ok that didnt work, try something else
      try {
        return makeTimeCoordAxisFromReference(vals);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    // create the coord axis
    String name = "timeCoord";
    String desc = "synthesized time coordinate from valtimeMINUSreftime and filename YYYYMMDD_HHMM";
    CoordinateAxis1D.Builder<?> timeCoord =
        CoordinateAxis1D.builder().setName(name).setArrayType(ArrayType.INT).setParentGroupBuilder(rootGroup)
            .setDimensionsByName("record").setUnits(units).setDesc(desc).setSourceData(vals);

    parseInfo.format("Created Time Coordinate Axis = %s%n", name);
    return timeCoord;
  }

  private String makeTimeUnitFromFilename(String dsName) {
    dsName = dsName.replace('\\', '/');

    // posFirst: last '/' if it exists
    int posFirst = dsName.lastIndexOf('/');
    if (posFirst < 0)
      posFirst = 0;

    // posLast: next '.' if it exists
    int posLast = dsName.indexOf('.', posFirst);
    if (posLast < 0)
      dsName = dsName.substring(posFirst + 1);
    else
      dsName = dsName.substring(posFirst + 1, posLast);

    // gotta be YYYYMMDD_HHMM
    if (dsName.length() != 13)
      return null;

    String year = dsName.substring(0, 4);
    String mon = dsName.substring(4, 6);
    String day = dsName.substring(6, 8);
    String hour = dsName.substring(9, 11);
    String min = dsName.substring(11, 13);

    return "seconds since " + year + "-" + mon + "-" + day + " " + hour + ":" + min + ":0";
  }

  // construct time coordinate from reftime variable
  @Nullable
  private CoordinateAxis.Builder<?> makeTimeCoordAxisFromReference(Array<Number> vals) throws IOException {
    if (rootGroup.findVariableLocal("reftime").isEmpty()) {
      return null;
    }
    VariableDS.Builder<?> refVar = (VariableDS.Builder<?>) rootGroup.findVariableLocal("reftime").get();

    double refValue = refVar.orgVar.readScalarDouble();
    if (refValue == NetcdfFormatUtils.NC_FILL_DOUBLE) { // why?
      return null;
    }

    // construct the values array - make it a double to be safe
    double[] dvals = new double[(int) vals.length()];
    int count = 0;
    for (Number val : vals) {
      dvals[count++] = val.doubleValue() + refValue;
    }
    Array<Double> dvalArray = Arrays.factory(ArrayType.DOUBLE, vals.getShape(), dvals);

    String name = "timeCoord";
    String units = refVar.getAttributeContainer().findAttributeString(CDM.UNITS, "seconds since 1970-1-1 00:00:00");
    units = normalize(units);
    String desc = "synthesized time coordinate from reftime, valtimeMINUSreftime";
    CoordinateAxis1D.Builder<?> timeCoord =
        CoordinateAxis1D.builder().setName(name).setArrayType(ArrayType.DOUBLE).setParentGroupBuilder(rootGroup)
            .setDimensionsByName("record").setUnits(units).setDesc(desc).setSourceData(dvalArray);

    parseInfo.format("Created Time Coordinate Axis From reftime Variable%n");
    return timeCoord;
  }

  double findAttributeDouble(String attname) {
    Attribute att = rootGroup.getAttributeContainer().findAttributeIgnoreCase(attname);
    if (att == null || att.isString()) {
      parseInfo.format("ERROR cant find numeric attribute= %s%n", attname);
      return Double.NaN;
    }
    return att.getNumericValue().doubleValue();
  }

}

