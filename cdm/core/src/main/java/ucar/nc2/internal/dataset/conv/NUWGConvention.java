/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.dataset.conv;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CDM;
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
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.projection.LambertConformal;
import ucar.unidata.geoloc.projection.Stereographic;
import ucar.unidata.util.Format;
import ucar.unidata.util.StringUtil2;

/**
 * NUWG Convention (ad hoc).
 * see https://www.unidata.ucar.edu/software/netcdf/NUWG/
 */
public class NUWGConvention extends CoordSystemBuilder {
  private static final String CONVENTION_NAME = "NUWG";

  public static class Factory implements CoordSystemBuilderFactory {
    @Override
    public String getConventionName() {
      return CONVENTION_NAME;
    }

    @Override
    public CoordSystemBuilder open(NetcdfDataset.Builder datasetBuilder) {
      return new NUWGConvention(datasetBuilder);
    }
  }

  private NavInfoList navInfo = new NavInfoList();
  private String xaxisName = "", yaxisName = "";
  private Grib1 grib;
  private static final boolean dumpNav = false;

  NUWGConvention(NetcdfDataset.Builder datasetBuilder) {
    super(datasetBuilder);
    this.conventionName = CONVENTION_NAME;
  }

  @Override
  protected void augmentDataset(CancelTask cancelTask) throws IOException {
    if (null != rootGroup.getAttributeContainer().findAttribute("_enhanced"))
      return; // check if its already been done - aggregating enhanced datasets.
    rootGroup.getAttributeContainer().addAttribute("_enhanced", "");

    // find all variables that have the nav dimension
    // put them into a NavInfoList
    // make their data into metadata
    for (Variable.Builder vb : rootGroup.vbuilders) {
      VariableDS.Builder v = (VariableDS.Builder) vb;
      if (0 <= v.orgVar.findDimensionIndex("nav")) {
        if (dumpNav)
          parseInfo.format("NUWG has NAV var = %s%n", v);
        try {
          navInfo.add(new NavInfo(v));
        } catch (IOException ex) {
          parseInfo.format("ERROR NUWG reading NAV var = %s%n", v);
        }
      }
    }
    navInfo.sort(new NavComparator());
    parseInfo.format("%s%n%n", navInfo);

    // problem is NUWG doesnt identify the x, y coords.
    // so we get to hack it in here
    int mode = 3; // default is LambertConformal
    try {
      mode = navInfo.getInt("grid_type_code");
    } catch (NoSuchElementException e) {
      log.warn("No mode in navInfo - assume 3");
    }

    try {
      if (mode == 0) {
        xaxisName = navInfo.getString("i_dim");
        yaxisName = navInfo.getString("j_dim");
      } else {
        xaxisName = navInfo.getString("x_dim");
        yaxisName = navInfo.getString("y_dim");
      }
    } catch (NoSuchElementException e) {
      log.warn("No mode in navInfo - assume = 1");
      // LOOK could match variable grid_type, data = "tangential lambert conformal "
    }
    grib = new Grib1(mode);

    if (!rootGroup.findVariableLocal(xaxisName).isPresent()) {
      grib.makeXCoordAxis(xaxisName);
      parseInfo.format("Generated x axis from NUWG nav= %s%n", xaxisName);

    } else if (xaxisName.equalsIgnoreCase("lon")) {
      try {
        // check monotonicity
        boolean ok = true;
        VariableDS.Builder dc = (VariableDS.Builder) rootGroup.findVariableLocal(xaxisName).get();
        Array coordVal = dc.orgVar.read();
        IndexIterator coordIndex = coordVal.getIndexIterator();

        double coord1 = coordIndex.getDoubleNext();
        double coord2 = coordIndex.getDoubleNext();
        boolean increase = coord1 > coord2;
        coord1 = coord2;
        while (coordIndex.hasNext()) {
          coord2 = coordIndex.getDoubleNext();
          if ((coord1 > coord2) ^ increase) {
            ok = false;
            break;
          }
          coord1 = coord2;
        }

        if (!ok) {
          parseInfo.format("ERROR lon axis is not monotonic, regen from nav%n");
          grib.makeXCoordAxis(xaxisName);
        }
      } catch (IOException ioe) {
        log.warn("IOException when reading xaxis = " + xaxisName);
      }
    }

    if (!rootGroup.findVariableLocal(yaxisName).isPresent()) {
      grib.makeYCoordAxis(yaxisName);
      parseInfo.format("Generated y axis from NUWG nav=%s%n", yaxisName);
    }

    // "referential" variables
    for (Dimension dim : rootGroup.getDimensions()) {
      String dimName = dim.getShortName();
      if (rootGroup.findVariableLocal(dimName).isPresent())
        continue; // already has coord axis
      List<Variable.Builder> ncvars = searchAliasedDimension(dim);
      if ((ncvars == null) || (ncvars.isEmpty())) // no alias
        continue;

      if (ncvars.size() == 1) {
        Variable.Builder ncvar = ncvars.get(0);
        if (ncvar.dataType == DataType.STRUCTURE)
          continue; // cant be a structure
        if (makeCoordinateAxis(ncvar, dim)) {
          parseInfo.format("Added referential coordAxis = %s%n", ncvar.shortName);
        } else {
          parseInfo.format("Couldnt add referential coordAxis = %s%n", ncvar.shortName);
        }

      } else if (ncvars.size() == 2) {

        if (dimName.equals("record")) {
          Variable.Builder ncvar0 = ncvars.get(0);
          Variable.Builder ncvar1 = ncvars.get(1);
          VariableDS.Builder ncvar =
              (VariableDS.Builder) (ncvar0.shortName.equalsIgnoreCase("valtime") ? ncvar0 : ncvar1);

          if (makeCoordinateAxis(ncvar, dim)) {
            parseInfo.format("Added referential coordAxis (2) = %s%n", ncvar.shortName);

            // the usual crap - clean up time units
            String units = ncvar.getUnits();
            if (units != null) {
              units = StringUtil2.remove(units, '(');
              units = StringUtil2.remove(units, ')');
              ncvar.addAttribute(new Attribute(CDM.UNITS, units));
              ncvar.setUnits(units);
            }
          } else {
            parseInfo.format("Couldnt add referential coordAxis = %s%n", ncvar.shortName);
          }
        } else {
          // lower(?) bound
          Variable.Builder ncvar = ncvars.get(0);
          if (ncvar.dataType == DataType.STRUCTURE)
            continue; // cant be a structure

          if (makeCoordinateAxis(ncvar, dim)) {
            parseInfo.format("Added referential boundary coordAxis (2) = %s%n", ncvar.shortName);
          } else {
            parseInfo.format("Couldnt add referential coordAxis = %s%n", ncvar.shortName);
          }
        }
      } // 2
    } // loop over dims

    if (grib.projectionCT != null) {
      VariableDS.Builder v = makeCoordinateTransformVariable(grib.projectionCT);
      v.addAttribute(new Attribute(_Coordinate.Axes, xaxisName + " " + yaxisName));
      rootGroup.addVariable(v);
    }
  }

  private boolean makeCoordinateAxis(Variable.Builder ncvar, Dimension dim) {
    if (ncvar.getRank() != 1)
      return false;
    String vdimName = ncvar.getFirstDimensionName();
    if (!vdimName.equals(dim.getShortName()))
      return false;

    if (!dim.getShortName().equals(ncvar.shortName)) {
      ncvar.addAttribute(new Attribute(_Coordinate.AliasForDimension, dim.getShortName()));
    }
    return true;
  }

  /**
   * Search for an aliased coord that may have multiple variables
   * :dimName = alias1, alias2;
   * Variable alias1(dim);
   * Variable alias2(dim);
   * 
   * @param dim: look for this dimension name
   * @return Collection of nectdf variables, or null if none
   */
  private List<Variable.Builder> searchAliasedDimension(Dimension dim) {
    String dimName = dim.getShortName();
    String alias = rootGroup.getAttributeContainer().findAttributeString(dimName, null);
    if (alias == null)
      return null;

    List<Variable.Builder> vars = new ArrayList<>();
    StringTokenizer parser = new StringTokenizer(alias, " ,");
    while (parser.hasMoreTokens()) {
      String token = parser.nextToken();
      if (!rootGroup.findVariableLocal(token).isPresent()) {
        continue;
      }
      Variable.Builder ncvar = rootGroup.findVariableLocal(token).get();
      if (ncvar.getRank() != 1)
        continue;
      String firstDimName = ncvar.getFirstDimensionName();
      if (dimName.equals(firstDimName)) {
        vars.add(ncvar);
      }
    }
    return vars;
  }

  private StringBuilder buf = new StringBuilder(2000);

  public String extraInfo() {
    buf.setLength(0);
    buf.append(navInfo).append("%n");
    return buf.toString();
  }

  @Override
  protected void makeCoordinateTransforms() {
    if ((grib != null) && (grib.projectionCT != null)) {
      VarProcess vp = findVarProcess(grib.projectionCT.getName(), null);
      if (vp != null) {
        vp.isCoordinateTransform = true;
        vp.ct = CoordinateTransform.builder().setPreBuilt(grib.projectionCT);
        coords.addCoordinateTransform(vp.ct);
      }
    }
    super.makeCoordinateTransforms();
  }

  @Override
  protected AxisType getAxisType(VariableDS.Builder v) {
    String vname = v.shortName;

    if (vname.equalsIgnoreCase("lat"))
      return AxisType.Lat;

    if (vname.equalsIgnoreCase("lon"))
      return AxisType.Lon;

    if (vname.equalsIgnoreCase(xaxisName))
      return AxisType.GeoX;

    if (vname.equalsIgnoreCase(yaxisName))
      return AxisType.GeoY;

    if (vname.equalsIgnoreCase("record"))
      return AxisType.Time;

    String dimName = v.getFirstDimensionName();
    if ((dimName != null) && dimName.equalsIgnoreCase("record")) { // wow thats bad!
      return AxisType.Time;
    }

    String unit = v.getUnits();
    if (unit != null) {
      if (SimpleUnit.isCompatible("millibar", unit))
        return AxisType.Pressure;

      if (SimpleUnit.isCompatible("m", unit))
        return AxisType.Height;

      if (SimpleUnit.isCompatible("sec", unit))
        return null;
    }

    return AxisType.GeoZ; // AxisType.GeoZ;
  }

  /**
   * @return "up" if this is a Vertical (z) coordinate axis which goes up as coords get bigger
   * @param v for this axis
   */
  public String getZisPositive(CoordinateAxis v) {
    // gotta have a length unit
    String unit = v.getUnitsString();
    if ((unit != null) && SimpleUnit.isCompatible("m", unit))
      return "up";

    return "down";

    // lame NUWG Conventions! units of millibar might be "millibars above ground" !
    // heres a kludge that should work
    // return v.getName().equalsIgnoreCase("fhg") ? "up" : "down";
  }

  private static class NavComparator implements Comparator<NavInfo> {
    public int compare(NavInfo n1, NavInfo n2) {
      return n1.getName().compareTo(n2.getName());
    }
  }

  private class NavInfo {
    VariableDS.Builder vb;
    Variable orgVar;
    DataType valueType;
    String svalue;
    byte bvalue;
    int ivalue;
    double dvalue;

    NavInfo(VariableDS.Builder vb) throws IOException {
      this.vb = vb;
      this.orgVar = vb.orgVar;
      valueType = vb.dataType;
      try {
        if ((valueType == DataType.CHAR) || (valueType == DataType.STRING))
          svalue = orgVar.readScalarString();
        else if (valueType == DataType.BYTE)
          bvalue = orgVar.readScalarByte();
        else if ((valueType == DataType.INT) || (valueType == DataType.SHORT))
          ivalue = orgVar.readScalarInt();
        else
          dvalue = orgVar.readScalarDouble();
      } catch (UnsupportedOperationException e) {
        parseInfo.format("Nav variable %s  not a scalar%n", getName());
      }
    }

    public String getName() {
      return vb.shortName;
    }

    public String getDescription() {
      Attribute att = vb.getAttributeContainer().findAttributeIgnoreCase(CDM.LONG_NAME);
      return (att == null) ? getName() : att.getStringValue();
    }

    public String getStringValue() {
      if ((valueType == DataType.CHAR) || (valueType == DataType.STRING))
        return svalue;
      else if (valueType == DataType.BYTE)
        return Byte.toString(bvalue);
      else if ((valueType == DataType.INT) || (valueType == DataType.SHORT))
        return Integer.toString(ivalue);
      else
        return Double.toString(dvalue);
    }

    private StringBuilder buf = new StringBuilder(200);

    public String toString() {
      buf.setLength(0);
      buf.append(getName());
      buf.append(" ");
      Format.tab(buf, 15, true);
      buf.append(getStringValue());
      buf.append(" ");
      Format.tab(buf, 35, true);
      buf.append(getDescription());
      return buf.toString();
    }
  }

  private static class NavInfoList extends ArrayList<NavInfo> {

    public NavInfo findInfo(String name) {
      for (NavInfo nav : this) {
        if (name.equalsIgnoreCase(nav.getName()))
          return nav;
      }
      return null;
    }

    public double getDouble(String name) throws NoSuchElementException {
      NavInfo nav = findInfo(name);
      if (nav == null)
        throw new NoSuchElementException("GRIB1 " + name);

      if ((nav.valueType == DataType.DOUBLE) || (nav.valueType == DataType.FLOAT))
        return nav.dvalue;
      else if ((nav.valueType == DataType.INT) || (nav.valueType == DataType.SHORT))
        return (double) nav.ivalue;
      else if (nav.valueType == DataType.BYTE)
        return (double) nav.bvalue;

      throw new IllegalArgumentException("NUWGConvention.GRIB1.getDouble " + name + " type = " + nav.valueType);
    }

    public int getInt(String name) throws NoSuchElementException {
      NavInfo nav = findInfo(name);
      if (nav == null)
        throw new NoSuchElementException("GRIB1 " + name);

      if ((nav.valueType == DataType.INT) || (nav.valueType == DataType.SHORT))
        return nav.ivalue;
      else if ((nav.valueType == DataType.DOUBLE) || (nav.valueType == DataType.FLOAT))
        return (int) nav.dvalue;
      else if (nav.valueType == DataType.BYTE)
        return (int) nav.bvalue;

      throw new IllegalArgumentException("NUWGConvention.GRIB1.getInt " + name + " type = " + nav.valueType);
    }

    public String getString(String name) throws NoSuchElementException {
      NavInfo nav = findInfo(name);
      if (nav == null)
        throw new NoSuchElementException("GRIB1 " + name);
      return nav.svalue;
    }

    public String toString() {
      StringBuilder buf = new StringBuilder(2000);
      buf.append("\nNav Info\n");
      buf.append("Name___________Value_____________________Description\n");
      for (NavInfo nava : this) {
        buf.append(nava).append("\n");
      }
      buf.append("\n");
      return buf.toString();
    }

  }

  // encapsolates GRIB-specific processing
  private class Grib1 {
    private String grid_name;
    private int grid_code;
    private ProjectionCT projectionCT;

    private int nx, ny;
    private double startx, starty;
    private double dx, dy;

    Grib1(int mode) {
      // horiz system
      grid_name = "Projection";
      if (grid_name.isEmpty())
        grid_name = "grid_var";

      grid_code = mode;
      if (0 == grid_code)
        processLatLonProjection();
      else if (3 == grid_code)
        projectionCT = makeLCProjection();
      else if (5 == grid_code)
        projectionCT = makePSProjection();
      else
        throw new IllegalArgumentException("NUWGConvention: unknown grid_code= " + grid_code);

      // vertical system
    }

    void makeXCoordAxis(String xname) {
      CoordinateAxis.Builder v = CoordinateAxis1D.builder().setName(xname).setDataType(DataType.DOUBLE)
          .setParentGroupBuilder(rootGroup).setDimensionsByName(xname).setUnits((0 == grid_code) ? CDM.LON_UNITS : "km")
          .setDesc("synthesized X coord");
      v.addAttribute(
          new Attribute(_Coordinate.AxisType, (0 == grid_code) ? AxisType.Lon.toString() : AxisType.GeoX.toString()));
      v.setAutoGen(startx, dx);
      datasetBuilder.replaceCoordinateAxis(rootGroup, v);
    }

    void makeYCoordAxis(String yname) {
      CoordinateAxis.Builder v = CoordinateAxis1D.builder().setName(yname).setDataType(DataType.DOUBLE)
          .setParentGroupBuilder(rootGroup).setDimensionsByName(yname).setUnits((0 == grid_code) ? CDM.LAT_UNITS : "km")
          .setDesc("synthesized Y coord");
      v.addAttribute(
          new Attribute(_Coordinate.AxisType, (0 == grid_code) ? AxisType.Lat.toString() : AxisType.GeoY.toString()));
      v.setAutoGen(starty, dy);
      datasetBuilder.replaceCoordinateAxis(rootGroup, v);
    }

    private ProjectionCT makeLCProjection() throws NoSuchElementException {
      double latin1 = navInfo.getDouble("Latin1");
      double latin2 = navInfo.getDouble("Latin2");
      double lov = navInfo.getDouble("Lov");
      double la1 = navInfo.getDouble("La1");
      double lo1 = navInfo.getDouble("Lo1");

      // we have to project in order to find the origin
      LambertConformal lc = new LambertConformal(latin1, lov, latin1, latin2);
      ProjectionPoint start = lc.latLonToProj(new LatLonPointImpl(la1, lo1));
      if (debug)
        System.out.println("start at proj coord " + start);
      startx = start.getX();
      starty = start.getY();

      nx = navInfo.getInt("Nx");
      ny = navInfo.getInt("Ny");
      dx = navInfo.getDouble("Dx") / 1000.0; // need to be km : unit conversion LOOK;
      dy = navInfo.getDouble("Dy") / 1000.0; // need to be km : unit conversion LOOK;

      return new ProjectionCT(grid_name, "FGDC", lc);
    }

    // polar stereographic
    private ProjectionCT makePSProjection() throws NoSuchElementException {
      double lov = navInfo.getDouble("Lov");
      double la1 = navInfo.getDouble("La1");
      double lo1 = navInfo.getDouble("Lo1");

      // Why the scale factor?. accordining to GRID docs:
      // "Grid lengths are in units of meters, at the 60 degree latitude circle nearest to the pole"
      // since the scale factor at 60 degrees = k = 2*k0/(1+sin(60)) [Snyder,Working Manual p157]
      // then to make scale = 1 at 60 degrees, k0 = (1+sin(60))/2 = .933
      Stereographic ps = new Stereographic(90.0, lov, .933);

      // we have to project in order to find the origin
      ProjectionPoint start = ps.latLonToProj(new LatLonPointImpl(la1, lo1));
      if (debug)
        System.out.println("start at proj coord " + start);
      startx = start.getX();
      starty = start.getY();

      nx = navInfo.getInt("Nx");
      ny = navInfo.getInt("Ny");
      dx = navInfo.getDouble("Dx") / 1000.0;
      dy = navInfo.getDouble("Dy") / 1000.0;

      return new ProjectionCT(grid_name, "FGDC", ps);
    }

    private void processLatLonProjection() throws NoSuchElementException {
      // get stuff we need to construct axes
      starty = navInfo.getDouble("La1");
      startx = navInfo.getDouble("Lo1");
      nx = navInfo.getInt("Ni");
      ny = navInfo.getInt("Nj");
      dx = navInfo.getDouble("Di");
      dy = navInfo.getDouble("Dj");
    }

  } // GRIB1 */

}
