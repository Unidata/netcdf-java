package ucar.nc2.internal.dataset;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import javax.annotation.Nullable;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CF;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.CoordinateTransform;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.spi.CoordSystemBuilderFactory;
import ucar.nc2.util.CancelTask;
import ucar.unidata.util.Parameter;

/**
 * Super class for implementing Convention-specific parsing of netCDF files.
 * This class processes the "_Coordinate conventions", see
 * https://www.unidata.ucar.edu/software/netcdf-java/current/reference/CoordinateAttributes.html
 *
 * A good strategy is for subclasses to add those attributes, and let this class construct the coordinate systems.
 */

/*
 * Implementation notes:
 *
 * Generally, subclasses should add the _Coordinate conventions, see
 * https://www.unidata.ucar.edu/software/netcdf-java/current/reference/CoordinateAttributes.html
 * Then let this class do the rest of the work.
 *
 * How to add Coordinate Transforms:
 * A.
 * 1) create a dummy Variable called the Coordinate Transform Variable.
 * This Coordinate Transform variable always has a name that identifies the transform,
 * and any attributes needed for the transformation.
 * 2) explicitly point to it by adding a _CoordinateTransform attribute to a Coordinate System Variable
 * _CoordinateTransforms = "LambertProjection HybridSigmaVerticalTransform"
 *
 * B. You could explicitly add it by overriding assignCoordinateTransforms()
 */
public class CoordSystemBuilder {
  protected static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CoordSystemBuilder.class);
  private static boolean useMaximalCoordSys = true;
  private static final String CONVENTION_NAME = _Coordinate.Convention;

  public static class Factory implements CoordSystemBuilderFactory {
    @Override
    public String getConventionName() {
      return CONVENTION_NAME;
    }

    @Override
    public CoordSystemBuilder open(NetcdfDataset.Builder datasetBuilder) {
      return new CoordSystemBuilder(datasetBuilder);
    }
  }

  /**
   * Calculate if this is a classic coordinate variable: has same name as its first dimension.
   * If type char, must be 2D, else must be 1D.
   *
   * @return true if a coordinate variable.
   */
  public static boolean isCoordinateVariable(Variable.Builder<?> vb) {
    // Structures and StructureMembers cant be coordinate variables
    if ((vb.dataType == DataType.STRUCTURE) || vb.parentStruct != null)
      return false;

    int rank = vb.getRank();
    if (rank == 1) {
      String firstd = vb.getFirstDimensionName();
      if (vb.shortName.equals(firstd)) {
        return true;
      }
    }
    if (rank == 2) { // two dimensional
      String firstd = vb.getFirstDimensionName();
      // must be char valued (then its really a String)
      return vb.shortName.equals(firstd) && (vb.dataType == DataType.CHAR);
    }

    return false;
  }

  public static int countDomainSize(Group.Builder gb, Variable.Builder<?>... axes) {
    Set<Dimension> domain = new HashSet<>();
    for (Variable.Builder<?> axis : axes) {
      domain.addAll(axis.getDimensions(gb));
    }
    return domain.size();
  }

  /**
   * Does this axis "fit" this variable. True if all of the dimensions in the axis also appear in
   * the variable. If char variable, last dimension is left out.
   *
   * @param axis check if this axis is ok for the given variable
   * @param vp the given variable
   * @return true if all of the dimensions in the axis also appear in the variable.
   */
  protected boolean isCoordinateAxisForVariable(CoordinateAxis.Builder<?> axis, VarProcess vp) {
    ImmutableList<Dimension> varDims = vp.vb.getDimensions(vp.gb);
    ImmutableList<Dimension> axisDims = axis.getDimensions(rootGroup);

    // a CHAR variable must really be a STRING, so leave out the last (string length) dimension
    int checkDims = axisDims.size();
    if (axis.dataType == DataType.CHAR)
      checkDims--;

    for (int i = 0; i < checkDims; i++) {
      Dimension axisDim = axisDims.get(i);
      if (!varDims.contains(axisDim)) {
        return false;
      }
    }
    return true;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////

  protected NetcdfDataset.Builder<?> datasetBuilder;
  protected Group.Builder rootGroup;
  protected CoordinatesHelper.Builder coords;

  protected List<VarProcess> varList = new ArrayList<>();
  // coordinate variables for Dimension (name)
  protected Multimap<String, VarProcess> coordVarsForDimension = ArrayListMultimap.create();
  // default name of Convention, override in subclass
  protected String conventionName = _Coordinate.Convention;
  protected Formatter parseInfo = new Formatter();
  protected Formatter userAdvice = new Formatter();
  protected boolean debug;

  // Used when using NcML to provide convention attributes.
  protected CoordSystemBuilder(NetcdfDataset.Builder<?> datasetBuilder) {
    this.datasetBuilder = datasetBuilder;
    this.rootGroup = datasetBuilder.rootGroup;
    this.coords = datasetBuilder.coords;
  }

  protected void setConventionUsed(String convName) {
    this.conventionName = convName;
  }

  public String getConventionUsed() {
    return conventionName;
  }

  protected void addUserAdvice(String advice) {
    userAdvice.format("%s", advice);
  }

  public String getParseInfo() {
    return parseInfo.toString();
  }

  public String getUserAdvice() {
    return userAdvice.toString();
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////
  // subclasses can override any of these routines

  protected void augmentDataset(CancelTask cancelTask) throws IOException {}

  // All these steps may be overriden by subclasses.
  protected void buildCoordinateSystems() {
    // put status info into parseInfo that can be shown to someone trying to debug this process
    parseInfo.format("Parsing with Convention = %s%n", conventionName);

    // Bookkeeping info for each variable is kept in the VarProcess inner class
    addVariables(datasetBuilder.rootGroup);

    // identify which variables are coordinate axes
    identifyCoordinateAxes();
    // identify which variables are used to describe coordinate systems
    identifyCoordinateSystems();
    // identify which variables are used to describe coordinate transforms
    identifyCoordinateTransforms();

    // turn Variables into CoordinateAxis objects
    makeCoordinateAxes();

    // make Coordinate Systems for all Coordinate Systems Variables
    makeCoordinateSystems();

    // assign explicit CoordinateSystem objects to variables
    assignCoordinateSystemsExplicit();

    // assign implicit CoordinateSystem objects to variables
    makeCoordinateSystemsImplicit();

    // optionally assign implicit CoordinateSystem objects to variables that dont have one yet
    if (useMaximalCoordSys) {
      makeCoordinateSystemsMaximal();
    }

    // make Coordinate Transforms
    makeCoordinateTransforms();

    // assign Coordinate Transforms
    assignCoordinateTransforms();
  }

  private void addVariables(Group.Builder group) {
    for (Variable.Builder v : group.vbuilders) {
      if (v instanceof VariableDS.Builder) {
        varList.add(new VarProcess(group, (VariableDS.Builder) v));
      }

      // TODO
      /*
       * if (v instanceof Structure.Builder) {
       * List<Variable.Builder> nested = ((Structure.Builder v).vbuilders();
       * addVariables(nested, varProcessList);
       * }
       */
    }

    for (Group.Builder nested : group.gbuilders) {
      addVariables(nested);
    }
  }

  /** Everything named in the coordinateAxes or coordinates attribute are Coordinate axes. */
  protected void identifyCoordinateAxes() {
    for (VarProcess vp : varList) {
      if (vp.coordinateAxes != null) {
        identifyCoordinateAxes(vp, vp.coordinateAxes);
      }
      if (vp.coordinates != null) {
        identifyCoordinateAxes(vp, vp.coordinates);
      }
    }
  }

  // Mark named coordinates as "isCoordinateAxis"
  private void identifyCoordinateAxes(VarProcess vp, String coordinates) {
    StringTokenizer stoker = new StringTokenizer(coordinates);
    while (stoker.hasMoreTokens()) {
      String vname = stoker.nextToken();
      VarProcess ap = findVarProcess(vname, vp);
      if (ap == null) {
        Group.Builder gb = vp.vb.getParentGroupBuilder();
        Optional<Variable.Builder<?>> vopt = gb.findVariableOrInParent(vname);
        if (vopt.isPresent()) {
          ap = findVarProcess(vopt.get().getFullName(), vp);
        } else {
          parseInfo.format("***Cant find coordAxis %s referenced from var= %s%n", vname, vp);
          userAdvice.format("***Cant find coordAxis %s referenced from var= %s%n", vname, vp);
        }
      }

      if (ap != null) {
        if (!ap.isCoordinateAxis) {
          parseInfo.format(" CoordinateAxis = %s added; referenced from var= %s%n", vname, vp);
        }
        ap.isCoordinateAxis = true;
      } else {
        parseInfo.format("***Cant find coordAxis %s referenced from var= %s%n", vname, vp);
        userAdvice.format("***Cant find coordAxis %s referenced from var= %s%n", vname, vp);
      }
    }
  }

  /** Identify coordinate systems, using _Coordinate.Systems attribute. */
  protected void identifyCoordinateSystems() {
    for (VarProcess vp : varList) {
      if (vp.coordinateSystems != null) {
        StringTokenizer stoker = new StringTokenizer(vp.coordinateSystems);
        while (stoker.hasMoreTokens()) {
          String vname = stoker.nextToken();
          VarProcess ap = findVarProcess(vname, vp);
          if (ap != null) {
            if (!ap.isCoordinateSystem) {
              parseInfo.format(" CoordinateSystem = %s added; referenced from var= %s%n", vname, vp);
            }
            ap.isCoordinateSystem = true;
          } else {
            parseInfo.format("***Cant find coordSystem %s referenced from var= %s%n", vname, vp);
            userAdvice.format("***Cant find coordSystem %s referenced from var= %s%n", vname, vp);
          }
        }
      }
    }
  }

  /** Identify coordinate transforms, using _CoordinateTransforms attribute. */
  protected void identifyCoordinateTransforms() {
    for (VarProcess vp : varList) {
      if (vp.coordinateTransforms != null) {
        StringTokenizer stoker = new StringTokenizer(vp.coordinateTransforms);
        while (stoker.hasMoreTokens()) {
          String vname = stoker.nextToken();
          VarProcess ap = findVarProcess(vname, vp);
          if (ap != null) {
            if (!ap.isCoordinateTransform) {
              parseInfo.format(" CoordinateTransform = %s added; referenced from var= %s%n", vname, vp);
            }
            ap.isCoordinateTransform = true;
          } else {
            parseInfo.format("***Cant find CoordinateTransform %s referenced from var= %s%n", vname, vp);
            userAdvice.format("***Cant find CoordinateTransform %s referenced from var= %s%n", vname, vp);
          }
        }
      }
    }
  }

  /**
   * Identify what kind of AxisType the named variable is. Only called for variables already
   * identified as Coordinate Axes. Default null - subclasses can override.
   *
   * @param vb a variable already identified as a Coordinate Axis
   * @return AxisType or null if unknown.
   */
  @Nullable
  protected AxisType getAxisType(VariableDS.Builder vb) {
    return null;
  }

  /**
   * Take previously identified Coordinate Axis and Coordinate Variables and make them into a
   * CoordinateAxis. Uses the getAxisType() method to figure out the type, if not already set.
   */
  protected void makeCoordinateAxes() {
    // The ones identified as coordinate variables or axes
    for (VarProcess vp : varList) {
      if (vp.isCoordinateAxis || vp.isCoordinateVariable) {
        if (vp.axisType == null) {
          vp.axisType = getAxisType(vp.vb);
        }
        if (vp.axisType == null) {
          userAdvice.format("Coordinate Axis %s does not have an assigned AxisType%n", vp);
        }
        vp.makeIntoCoordinateAxis();
      }
    }

    // The ones marked as Coordinate Systems, which will reference Coordinates
    for (VarProcess vp : varList) {
      if (vp.isCoordinateSystem) {
        vp.makeCoordinatesFromCoordinateSystem();
      }
    }
  }

  protected void makeCoordinateSystems() {
    // The ones marked as Coordinate Systems, which will reference Coordinates
    for (VarProcess vp : varList) {
      if (vp.isCoordinateSystem) {
        vp.makeCoordinateSystem();
      }
    }
  }

  /**
   * Assign explicit CoordinateSystem objects to variables.
   */
  protected void assignCoordinateSystemsExplicit() {

    // look for explicit references to coord sys variables
    for (VarProcess vp : varList) {
      if (vp.coordinateSystems != null && !vp.isCoordinateTransform) {
        StringTokenizer stoker = new StringTokenizer(vp.coordinateSystems);
        while (stoker.hasMoreTokens()) {
          String vname = stoker.nextToken();
          VarProcess ap = findVarProcess(vname, vp);
          if (ap == null) {
            parseInfo.format("***Cant find Coordinate System variable %s referenced from var= %s%n", vname, vp);
            userAdvice.format("***Cant find Coordinate System variable %s referenced from var= %s%n", vname, vp);
          } else if (ap.cs == null) {
            parseInfo.format("***Not a Coordinate System variable %s referenced from var= %s%n", vname, vp);
            userAdvice.format("***Not a Coordinate System variable %s referenced from var= %s%n", vname, vp);
          } else {
            String sysName = coords.makeCanonicalName(ap.cs.coordAxesNames);
            vp.vb.addCoordinateSystem(sysName);
          }
        }
      }
    }

    // look for explicit references from coord sys variables to data variables
    for (VarProcess csVar : varList) {
      if (!csVar.isCoordinateSystem || (csVar.coordinateSystemsFor == null)) {
        continue;
      }

      // get list of dimensions from '_CoordinateSystemFor' attribute
      Set<String> dimList = new HashSet<>();
      StringTokenizer stoker = new StringTokenizer(csVar.coordinateSystemsFor);
      while (stoker.hasMoreTokens()) {
        String dname = stoker.nextToken();
        Optional<Dimension> dimOpt = rootGroup.findDimension(dname);
        if (dimOpt.isPresent()) {
          dimList.add(dimOpt.get().getShortName());
        } else {
          parseInfo.format("***Cant find Dimension %s referenced from CoordSys var= %s%n", dname, csVar);
          userAdvice.format("***Cant find Dimension %s referenced from CoordSys var= %s%n", dname, csVar);
        }
      }

      // look for vars with those dimensions
      for (VarProcess vp : varList) {
        if (!vp.hasCoordinateSystem() && vp.isData() && (csVar.cs != null)) {
          if (CoordinateSystem.isSubset(dimList, vp.vb.getDimensionsAll())
              && CoordinateSystem.isSubset(vp.vb.getDimensionsAll(), dimList)) {
            vp.vb.addCoordinateSystem(csVar.cs.coordAxesNames);
          }
        }
      }
    }

    // look for explicit listings of coordinate axes
    for (VarProcess vp : varList) {
      if (!vp.hasCoordinateSystem() && (vp.coordinateAxes != null) && vp.isData()) {
        String coordSysName = coords.makeCanonicalName(vp.coordinateAxes);
        Optional<CoordinateSystem.Builder> cso = coords.findCoordinateSystem(coordSysName);
        if (cso.isPresent()) {
          vp.vb.addCoordinateSystem(coordSysName);
          parseInfo.format(" assigned explicit CoordSystem '%s' for var= %s%n", coordSysName, vp);
        } else {
          CoordinateSystem.Builder csnew = CoordinateSystem.builder().setCoordAxesNames(coordSysName);
          coords.addCoordinateSystem(csnew);
          vp.vb.addCoordinateSystem(coordSysName);
          parseInfo.format(" created explicit CoordSystem '%s' for var= %s%n", coordSysName, vp);
        }
      }
    }
  }

  /**
   * Make implicit CoordinateSystem objects for variables that dont already have one, by using the
   * variables' list of coordinate axes, and any coordinateVariables for it. Must be at least 2
   * axes. All of a variable's _Coordinate Variables_ plus any variables listed in a
   * *__CoordinateAxes_* or *_coordinates_* attribute will be made into an *_implicit_* Coordinate
   * System. If there are at least two axes, and the coordinate system uses all of the variable's
   * dimensions, it will be assigned to the data variable.
   */
  protected void makeCoordinateSystemsImplicit() {
    for (VarProcess vp : varList) {
      if (!vp.hasCoordinateSystem() && vp.maybeData()) {
        List<CoordinateAxis.Builder> dataAxesList = vp.findCoordinateAxes(true);
        if (dataAxesList.size() < 2) {
          continue;
        }

        String csName = coords.makeCanonicalName(dataAxesList);
        Optional<CoordinateSystem.Builder> csOpt = coords.findCoordinateSystem(csName);
        if (csOpt.isPresent() && coords.isComplete(csOpt.get(), vp.vb)) {
          vp.vb.addCoordinateSystem(csName);
          parseInfo.format(" assigned implicit CoordSystem '%s' for var= %s%n", csName, vp);
        } else {
          CoordinateSystem.Builder csnew = CoordinateSystem.builder().setCoordAxesNames(csName).setImplicit(true);
          if (coords.isComplete(csnew, vp.vb)) {
            vp.vb.addCoordinateSystem(csName);
            coords.addCoordinateSystem(csnew);
            parseInfo.format(" created implicit CoordSystem '%s' for var= %s%n", csName, vp);
          }
        }
      }
    }
  }

  /**
   * If a variable still doesnt have a coordinate system, use hueristics to try to find one that was
   * probably forgotten. Examine existing CS. create a subset of axes that fits the variable. Choose
   * the one with highest rank. It must have X,Y or lat,lon. If so, add it.
   */
  private void makeCoordinateSystemsMaximal() {

    boolean requireCompleteCoordSys =
        !datasetBuilder.getEnhanceMode().contains(NetcdfDataset.Enhance.IncompleteCoordSystems);

    for (VarProcess vp : varList) {
      if (vp.hasCoordinateSystem() || !vp.isData()) {
        continue;
      }

      // look through all axes that fit
      List<CoordinateAxis.Builder> axisList = new ArrayList<>();
      for (CoordinateAxis.Builder axis : coords.coordAxes) {
        if (isCoordinateAxisForVariable(axis, vp)) {
          axisList.add(axis);
        }
      }

      if (axisList.size() < 2) {
        continue;
      }

      String csName = coords.makeCanonicalName(axisList);
      Optional<CoordinateSystem.Builder> csOpt = coords.findCoordinateSystem(csName);
      boolean okToBuild = false;

      // do coordinate systems need to be complete?
      // default enhance mode is yes, they must be complete
      if (requireCompleteCoordSys) {
        if (csOpt.isPresent()) {
          // only build if coordinate system is complete
          okToBuild = coords.isComplete(csOpt.get(), vp.vb);
        }
      } else {
        // coordinate system can be incomplete, so we're ok to build if we find something
        okToBuild = true;
      }

      if (csOpt.isPresent() && okToBuild) {
        vp.vb.addCoordinateSystem(csName);
        parseInfo.format(" assigned maximal CoordSystem '%s' for var= %s%n", csName, vp);

      } else {
        CoordinateSystem.Builder csnew = CoordinateSystem.builder().setCoordAxesNames(csName);
        // again, do coordinate systems need to be complete?
        // default enhance mode is yes, they must be complete
        if (requireCompleteCoordSys) {
          // only build if new coordinate system is complete
          okToBuild = coords.isComplete(csnew, vp.vb);
        }
        if (okToBuild) {
          csnew.setImplicit(true);
          vp.vb.addCoordinateSystem(csName);
          coords.addCoordinateSystem(csnew);
          parseInfo.format(" created maximal CoordSystem '%s' for var= %s%n", csnew.coordAxesNames, vp);
        }
      }
    }
  }

  /**
   * Take all previously identified Coordinate Transforms and create a CoordinateTransform object by
   * calling CoordTransBuilder.makeCoordinateTransform().
   */
  protected void makeCoordinateTransforms() {
    for (VarProcess vp : varList) {
      if (vp.isCoordinateTransform && vp.ct == null) { // TODO dont have ncd
        vp.ct = makeCoordinateTransform(vp.vb);
        if (vp.ct != null) {
          coords.addCoordinateTransform(vp.ct);
        }
      }
    }
  }

  protected CoordinateTransform.Builder makeCoordinateTransform(VariableDS.Builder<?> vb) {
    return CoordinateTransform.builder().setName(vb.getFullName()).setAttributeContainer(vb.getAttributeContainer());
  }

  /** Assign CoordinateTransform objects to Variables and Coordinate Systems. */
  protected void assignCoordinateTransforms() {
    // look for explicit transform assignments on the coordinate systems
    for (VarProcess vp : varList) {
      if (vp.isCoordinateSystem && vp.coordinateTransforms != null) {
        StringTokenizer stoker = new StringTokenizer(vp.coordinateTransforms);
        while (stoker.hasMoreTokens()) {
          String vname = stoker.nextToken();
          VarProcess ap = findVarProcess(vname, vp);
          if (ap != null) {
            if (ap.ct != null) {
              vp.addCoordinateTransform(ap.ct);
              parseInfo.format(" assign explicit coordTransform %s to CoordSys= %s%n", ap.ct, vp.cs);
            } else {
              parseInfo.format("***Cant find coordTransform in %s referenced from var= %s%n", vname,
                  vp.vb.getFullName());
              userAdvice.format("***Cant find coordTransform in %s referenced from var= %s%n", vname,
                  vp.vb.getFullName());
            }
          } else {
            parseInfo.format("***Cant find coordTransform variable= %s referenced from var= %s%n", vname,
                vp.vb.getFullName());
            userAdvice.format("***Cant find coordTransform variable= %s referenced from var= %s%n", vname,
                vp.vb.getFullName());
          }
        }
      }
    }

    // look for explicit coordSys assignments on the coordinate transforms
    for (VarProcess vp : varList) {
      if (vp.isCoordinateTransform && (vp.ct != null) && (vp.coordinateSystems != null)) {
        StringTokenizer stoker = new StringTokenizer(vp.coordinateSystems);
        while (stoker.hasMoreTokens()) {
          String vname = stoker.nextToken();
          VarProcess vcs = findVarProcess(vname, vp);
          if (vcs == null) {
            parseInfo.format("***Cant find coordSystem variable= %s referenced from var= %s%n", vname,
                vp.vb.getFullName());
            userAdvice.format("***Cant find coordSystem variable= %s referenced from var= %s%n", vname,
                vp.vb.getFullName());
          } else {
            vcs.addCoordinateTransform(vp.ct);
            parseInfo.format("***assign explicit coordTransform %s to CoordSys=  %s%n", vp.ct, vp.cs);
          }
        }
      }
    }

    // look for coordAxes assignments on the coordinate transforms
    for (VarProcess vp : varList) {
      if (vp.isCoordinateTransform && (vp.ct != null) && (vp.coordinateAxes != null)) {
        List<CoordinateAxis.Builder> dataAxesList = vp.findCoordinateAxes(false);
        if (!dataAxesList.isEmpty()) {
          for (CoordinateSystem.Builder cs : coords.coordSys) {
            if (coords.containsAxes(cs, dataAxesList)) {
              coords.addCoordinateTransform(vp.ct);
              cs.addCoordinateTransformByName(vp.ct.name);
              parseInfo.format("***assign (implicit coordAxes) coordTransform %s to CoordSys=  %s%n", vp.ct, cs);
            }
          }
        }
      }
    }

    // look for coordAxisType assignments on the coordinate transforms
    for (VarProcess vp : varList) {
      if (vp.isCoordinateTransform && (vp.ct != null) && (vp.coordAxisTypes != null)) {
        List<AxisType> axisTypesList = new ArrayList<>();
        StringTokenizer stoker = new StringTokenizer(vp.coordAxisTypes);
        while (stoker.hasMoreTokens()) {
          String name = stoker.nextToken();
          AxisType atype;
          if (null != (atype = AxisType.getType(name))) {
            axisTypesList.add(atype);
          }
        }
        if (!axisTypesList.isEmpty()) {
          for (CoordinateSystem.Builder cs : coords.coordSys) {
            if (coords.containsAxisTypes(cs, axisTypesList)) {
              cs.addCoordinateTransformByName(vp.ct.name);
              parseInfo.format("***assign (implicit coordAxisType) coordTransform %s to CoordSys=  %s%n", vp.ct, cs);
            }
          }
        }
      }
    }

  }

  protected VarProcess findVarProcess(String name, VarProcess from) {
    if (name == null) {
      return null;
    }

    // compare full name
    for (VarProcess vp : varList) {
      if (name.equals(vp.vb.getFullName())) {
        return vp;
      }
    }

    // prefer ones in the same group
    if (from != null) {
      for (VarProcess vp : varList) {
        if (vp.vb == null || vp.vb.getParentGroupBuilder() == null || from.vb == null) {
          continue;
        }
        if (name.equals(vp.vb.shortName) && vp.vb.getParentGroupBuilder().equals(from.vb.getParentGroupBuilder())) {
          return vp;
        }
      }
    }

    // WAEF, use short name
    for (VarProcess vp : varList) {
      if (name.equals(vp.vb.shortName)) {
        return vp;
      }
    }

    return null;
  }

  protected VarProcess findCoordinateAxis(String name) {
    if (name == null) {
      return null;
    }

    for (VarProcess vp : varList) {
      if (name.equals(vp.vb.getFullName()) && (vp.isCoordinateVariable || vp.isCoordinateAxis)) {
        return vp;
      }
    }
    return null;
  }

  /**
   * Create a "dummy" Coordinate Transform Variable based on the given CoordinateTransform.
   * This creates a scalar Variable with dummy data, and adds the Parameters of the CoordinateTransform
   * as attributes.
   *
   * @param ct based on the CoordinateTransform
   * @return the Coordinate Transform Variable. You must add it to the dataset.
   */
  protected VariableDS.Builder makeCoordinateTransformVariable(CoordinateTransform ct) {
    VariableDS.Builder v = VariableDS.builder().setName(ct.getName()).setDataType(DataType.CHAR);
    List<Parameter> params = ct.getParameters();
    for (Parameter p : params) {
      if (p.isString())
        v.addAttribute(new Attribute(p.getName(), p.getStringValue()));
      else {
        double[] data = p.getNumericValues();
        Array dataA = Array.factory(DataType.DOUBLE, new int[] {data.length}, data);
        v.addAttribute(new Attribute(p.getName(), dataA));
      }
    }
    v.addAttribute(new Attribute(_Coordinate.TransformType, ct.getTransformType().toString()));

    // fake data
    Array data = Array.factory(DataType.CHAR, new int[] {}, new char[] {' '});
    v.setCachedData(data, true);

    parseInfo.format("  made CoordinateTransformVariable: %s%n", ct.getName());
    return v;
  }

  /** Classifications of Variables into axis, systems and transforms */
  protected class VarProcess {
    public Group.Builder gb;
    public VariableDS.Builder<?> vb;

    // attributes
    public String coordVarAlias; // _Coordinate.AliasForDimension
    public String positive; // _Coordinate.ZisPositive or CF.POSITIVE
    public String coordinateAxes; // _Coordinate.Axes
    public String coordinateSystems; // _Coordinate.Systems
    public String coordinateSystemsFor; // _Coordinate.SystemsFor
    public String coordinateTransforms; // _Coordinate.Transforms
    public String coordAxisTypes; // _Coordinate.AxisTypes
    public String coordTransformType; // _Coordinate.TransformType
    public String coordinates; // CF coordinates (set by subclasses)

    // coord axes
    public boolean isCoordinateVariable; // classic coordinate variable
    public boolean isCoordinateAxis;
    public AxisType axisType;
    public CoordinateAxis.Builder axis; // if its made into a Coordinate Axis, this is not null

    // coord systems
    public boolean isCoordinateSystem;
    public CoordinateSystem.Builder cs;

    // coord transform
    public boolean isCoordinateTransform;
    public CoordinateTransform.Builder ct;

    /**
     * Wrap the given variable. Identify Coordinate Variables. Process all _Coordinate attributes.
     *
     * @param v wrap this Variable
     */
    private VarProcess(Group.Builder gb, VariableDS.Builder<?> v) {
      this.gb = gb;
      this.vb = v;
      isCoordinateVariable =
          isCoordinateVariable(v) || (null != v.getAttributeContainer().findAttribute(_Coordinate.AliasForDimension));
      if (isCoordinateVariable) {
        coordVarsForDimension.put(v.getFirstDimensionName(), this);
      }

      Attribute att = v.getAttributeContainer().findAttributeIgnoreCase(_Coordinate.AxisType);
      if (att != null) {
        String axisName = att.getStringValue();
        axisType = AxisType.getType(axisName);
        isCoordinateAxis = true;
        parseInfo.format(" Coordinate Axis added = %s type= %s%n", v.getFullName(), axisName);
      }

      coordVarAlias = v.getAttributeContainer().findAttValueIgnoreCase(_Coordinate.AliasForDimension, null);
      if (coordVarAlias != null) {
        coordVarAlias = coordVarAlias.trim();
        if (v.getRank() != 1) {
          parseInfo.format("**ERROR Coordinate Variable Alias %s has rank %d%n", v.getFullName(), v.getRank());
          userAdvice.format("**ERROR Coordinate Variable Alias %s has rank %d%n", v.getFullName(), v.getRank());
        } else {
          Optional<Dimension> coordDimOpt = gb.findDimension(coordVarAlias);
          coordDimOpt.ifPresent(coordDim -> {
            String vDim = v.getFirstDimensionName();
            if (!coordDim.getShortName().equals(vDim)) {
              parseInfo.format("**ERROR Coordinate Variable Alias %s names wrong dimension %s%n", v.getFullName(),
                  coordVarAlias);
              userAdvice.format("**ERROR Coordinate Variable Alias %s names wrong dimension %s%n", v.getFullName(),
                  coordVarAlias);
            } else {
              isCoordinateAxis = true;
              coordVarsForDimension.put(coordDim.getShortName(), this);
              parseInfo.format(" Coordinate Variable Alias added = %s for dimension= %s%n", v.getFullName(),
                  coordVarAlias);
            }
          });
        }
      }

      positive = v.getAttributeContainer().findAttValueIgnoreCase(_Coordinate.ZisPositive, null);
      if (positive == null) {
        positive = v.getAttributeContainer().findAttValueIgnoreCase(CF.POSITIVE, null);
      } else {
        isCoordinateAxis = true;
        positive = positive.trim();
        parseInfo.format(" Coordinate Axis added(from positive attribute ) = %s for dimension= %s%n", v.getFullName(),
            coordVarAlias);
      }

      coordinateAxes = v.getAttributeContainer().findAttValueIgnoreCase(_Coordinate.Axes, null);
      coordinateSystems = v.getAttributeContainer().findAttValueIgnoreCase(_Coordinate.Systems, null);
      coordinateSystemsFor = v.getAttributeContainer().findAttValueIgnoreCase(_Coordinate.SystemFor, null);
      coordinateTransforms = v.getAttributeContainer().findAttValueIgnoreCase(_Coordinate.Transforms, null);
      isCoordinateSystem = (coordinateTransforms != null) || (coordinateSystemsFor != null);

      coordAxisTypes = v.getAttributeContainer().findAttValueIgnoreCase(_Coordinate.AxisTypes, null);
      coordTransformType = v.getAttributeContainer().findAttValueIgnoreCase(_Coordinate.TransformType, null);
      isCoordinateTransform = (coordTransformType != null) || (coordAxisTypes != null);
    }

    protected boolean isData() {
      return !isCoordinateVariable && !isCoordinateAxis && !isCoordinateSystem && !isCoordinateTransform;
    }

    protected boolean maybeData() {
      return !isCoordinateVariable && !isCoordinateSystem && !isCoordinateTransform;
    }

    protected boolean hasCoordinateSystem() {
      return !vb.coordSysNames.isEmpty();
    }

    public String toString() {
      return vb.shortName;
    }

    /**
     * Turn the variable into a coordinate axis.
     * Add to the dataset, replacing variable if needed.
     *
     * @return coordinate axis
     */
    protected CoordinateAxis.Builder makeIntoCoordinateAxis() {
      if (axis != null) {
        return axis;
      }

      if (vb instanceof CoordinateAxis.Builder) {
        axis = (CoordinateAxis.Builder) vb;
      } else {
        // Create a CoordinateAxis out of this variable.
        vb = axis = CoordinateAxis.fromVariableDS(vb);
      }

      if (axisType != null) {
        axis.setAxisType(axisType);
        axis.addAttribute(new Attribute(_Coordinate.AxisType, axisType.toString()));

        if (((axisType == AxisType.Height) || (axisType == AxisType.Pressure) || (axisType == AxisType.GeoZ))
            && (positive != null)) {
          axis.setPositive(positive);
          axis.addAttribute(new Attribute(_Coordinate.ZisPositive, positive));
        }
      }
      coords.replaceCoordinateAxis(axis);
      gb.replaceVariable(axis);
      return axis;
    }

    /** For any variable listed in a coordinateAxes attribute, make into a coordinate. */
    protected void makeCoordinatesFromCoordinateSystem() {
      // find referenced coordinate axes
      if (coordinateAxes != null) {
        StringTokenizer stoker = new StringTokenizer(coordinateAxes); // _CoordinateAxes attribute
        while (stoker.hasMoreTokens()) {
          String vname = stoker.nextToken();
          VarProcess ap = findVarProcess(vname, this);
          if (ap != null) {
            ap.makeIntoCoordinateAxis();
          } else {
            parseInfo.format(" Cant find axes %s for Coordinate System %s%n", vname, vb);
            userAdvice.format(" Cant find axes %s for Coordinate System %s%n", vname, vb);
          }
        }
      }
    }

    /** For explicit coordinate system variables, make a CoordinateSystem. */
    protected void makeCoordinateSystem() {
      if (coordinateAxes != null) {
        String sysName = coords.makeCanonicalName(coordinateAxes);
        this.cs = CoordinateSystem.builder().setCoordAxesNames(sysName);
        parseInfo.format(" Made Coordinate System '%s'", sysName);
        coords.addCoordinateSystem(this.cs);
      }
    }

    /**
     * Create a list of coordinate axes for this data variable. Use the list of names in axes or
     * coordinates field.
     *
     * @param addCoordVariables if true, add any coordinate variables that are missing.
     * @return list of coordinate axes for this data variable.
     */
    protected List<CoordinateAxis.Builder> findCoordinateAxes(boolean addCoordVariables) {
      List<CoordinateAxis.Builder> axesList = new ArrayList<>();

      if (coordinateAxes != null) { // explicit axes
        StringTokenizer stoker = new StringTokenizer(coordinateAxes);
        while (stoker.hasMoreTokens()) {
          String vname = stoker.nextToken();
          VarProcess ap = findVarProcess(vname, this);
          if (ap != null) {
            CoordinateAxis.Builder axis = ap.makeIntoCoordinateAxis();
            if (!axesList.contains(axis)) {
              axesList.add(axis);
            }
          }
        }
      } else if (coordinates != null) { // CF partial listing of axes
        StringTokenizer stoker = new StringTokenizer(coordinates);
        while (stoker.hasMoreTokens()) {
          String vname = stoker.nextToken();
          VarProcess ap = findVarProcess(vname, this);
          if (ap != null) {
            CoordinateAxis.Builder axis = ap.makeIntoCoordinateAxis(); // LOOK check if its legal
            if (!axesList.contains(axis)) {
              axesList.add(axis);
            }
          }
        }
      }

      if (addCoordVariables) {
        for (String d : vb.getDimensionNames()) {
          for (VarProcess vp : coordVarsForDimension.get(d)) {
            CoordinateAxis.Builder axis = vp.makeIntoCoordinateAxis();
            if (!axesList.contains(axis)) {
              axesList.add(axis);
            }
          }
        }
      }

      return axesList;
    }

    void addCoordinateTransform(CoordinateTransform.Builder ct) {
      if (cs == null) {
        parseInfo.format("  %s: no CoordinateSystem for CoordinateTransformVariable: %s%n", vb.getFullName(), ct.name);
        return;
      }
      cs.addCoordinateTransformByName(ct.name);
    }

  } // VarProcess

}
