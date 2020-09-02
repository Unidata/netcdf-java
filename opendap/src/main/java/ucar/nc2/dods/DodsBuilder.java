/*
 * (c) 1998-2017 University Corporation for Atmospheric Research/Unidata
 */
package ucar.nc2.dods;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import opendap.dap.AttributeTable;
import opendap.dap.BaseType;
import opendap.dap.DAP2Exception;
import opendap.dap.DAS;
import opendap.dap.DArray;
import opendap.dap.DArrayDimension;
import opendap.dap.DByte;
import opendap.dap.DConnect2;
import opendap.dap.DConstructor;
import opendap.dap.DDS;
import opendap.dap.DFloat32;
import opendap.dap.DFloat64;
import opendap.dap.DGrid;
import opendap.dap.DInt16;
import opendap.dap.DInt32;
import opendap.dap.DSequence;
import opendap.dap.DString;
import opendap.dap.DStructure;
import ucar.ma2.Array;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.constants.CF;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.util.CancelTask;
import ucar.unidata.util.StringUtil2;

/**
 * DODSNetcdfFile builder
 */
@NotThreadSafe
abstract class DodsBuilder<T extends DodsBuilder<T>> extends NetcdfFile.Builder<T> {

  private static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DodsBuilder.class);

  HashSet<Dimension> sharedDimensions = new HashSet<>();
  DConnect2 dodsConnection;
  DDS dds;
  DAS das;

  // Connect to the remote server and read in the DDS and DAS
  void connect(String datasetUrl, CancelTask cancelTask) throws IOException {
    long start = System.currentTimeMillis();

    // canonicalize name
    String urlName = datasetUrl; // actual URL uses http:
    this.location = datasetUrl; // canonical name uses "dods:"
    if (datasetUrl.startsWith("dods:")) {
      urlName = "http:" + datasetUrl.substring(5);
    } else if (datasetUrl.startsWith("http:")) {
      this.location = "dods:" + datasetUrl.substring(5);
    } else if (datasetUrl.startsWith("https:")) {
      this.location = "dods:" + datasetUrl.substring(6);
    } else if (datasetUrl.startsWith("file:")) {
      this.location = datasetUrl;
    } else {
      throw new java.net.MalformedURLException(datasetUrl + " must start with dods: or http: or file:");
    }

    if (DodsNetcdfFiles.debugServerCall) {
      System.out.println("DConnect to = <" + urlName + ">");
    }
    dodsConnection = new DConnect2(urlName, DodsNetcdfFile.accept_compress);
    if (cancelTask != null && cancelTask.isCancel()) {
      return;
    }

    // fetch the DDS and DAS
    try {
      dds = dodsConnection.getDDS();
      if (DodsNetcdfFiles.debugServerCall) {
        System.out.println("DodsBuilder readDDS");
      }
      if (DodsNetcdfFiles.debugOpenResult) {
        System.out.println("DDS = ");
        dds.print(System.out);
      }
      if (cancelTask != null && cancelTask.isCancel()) {
        return;
      }

      das = dodsConnection.getDAS();
      if (DodsNetcdfFiles.debugServerCall) {
        System.out.println("DodsBuilder readDAS");
      }
      if (DodsNetcdfFiles.debugOpenResult) {
        System.out.println("DAS = ");
        das.print(System.out);
      }
      if (cancelTask != null && cancelTask.isCancel()) {
        return;
      }

      if (DodsNetcdfFiles.debugOpenResult) {
        System.out.println("dodsVersion = " + dodsConnection.getServerVersion());
      }

    } catch (DAP2Exception dodsE) {
      // dodsE.printStackTrace();
      if (dodsE.getErrorCode() == DAP2Exception.NO_SUCH_FILE) {
        throw new FileNotFoundException(dodsE.getMessage());
      } else {
        dodsE.printStackTrace(System.err);
        throw new IOException("DodsBuilder url=" + this.location, dodsE);
      }

    } catch (Throwable t) {
      logger.info("DodsBuilder " + this.location, t);
      throw new IOException("DodsBuilder url=" + this.location, t);
    }

    // Parse the dds and das and get a tree of DodsV with root rootDodsV
    DodsV rootDodsV = DodsV.parseDDS(dds);
    rootDodsV.parseDAS(das);
    if (cancelTask != null && cancelTask.isCancel()) {
      return;
    }

    // LOOK why do we want to do the primitives seperate from compounds?
    constructTopVariables(rootDodsV, cancelTask);
    if (cancelTask != null && cancelTask.isCancel()) {
      return;
    }

    // preload(dodsVlist, cancelTask); LOOK not using preload yet
    // if (cancelTask != null && cancelTask.isCancel()) return;

    constructConstructors(rootDodsV, cancelTask);
    if (cancelTask != null && cancelTask.isCancel()) {
      return;
    }

    parseGlobalAttributes(das, rootDodsV);
    if (cancelTask != null && cancelTask.isCancel()) {
      return;
    }

    /*
     * if (RC.getUseGroups()) { TODO
     * try {
     * reGroup();
     * } catch (DAP2Exception dodsE) {
     * dodsE.printStackTrace(System.err);
     * throw new IOException(dodsE);
     * }
     * }
     */

    /*
     * look for coordinate variables
     * for (Variable v : variables) {
     * if (v instanceof DODSVariable)
     * ((DODSVariable) v).calcIsCoordinateVariable();
     * }
     */

    // see if theres a CE: if so, we need to reset the dodsConnection without it,
    // since we are reusing dodsConnection; perhaps this is not needed?
    // may be true now that weve consolidated metadata reading
    // no comprende TODO
    int pos;
    if (0 <= (pos = urlName.indexOf('?'))) {
      String datasetName = urlName.substring(0, pos);
      if (DodsNetcdfFiles.debugServerCall) {
        System.out.println(" reconnect to = <" + datasetName + ">");
      }
      dodsConnection = new DConnect2(datasetName, DodsNetcdfFile.accept_compress);

      // parse the CE for projections
      String CE = urlName.substring(pos + 1);
      StringTokenizer stoke = new StringTokenizer(CE, " ,");
      while (stoke.hasMoreTokens()) {
        String proj = stoke.nextToken();
        int subsetPos = proj.indexOf('[');
        if (DodsNetcdfFiles.debugCE) {
          System.out.println(" CE = " + proj + " " + subsetPos);
        }
        if (subsetPos > 0) {
          String vname = proj.substring(0, subsetPos);
          String vCE = proj.substring(subsetPos);
          if (DodsNetcdfFiles.debugCE) {
            System.out.println(" vCE = <" + vname + "><" + vCE + ">");
          }
          Variable.Builder<?> var =
              rootGroup.findVariable(vname).orElseThrow(() -> new IOException("Variable not found: " + vname));

          DodsVariableBuilder<?> dodsVar = (DodsVariableBuilder<?>) var;
          dodsVar.setCE(vCE);
          dodsVar.setCaching(true);
        }
      }
    }

    if (DodsNetcdfFiles.showNCfile) {
      System.out.println("DODS nc file = " + this);
    }
    long took = System.currentTimeMillis() - start;
    if (DodsNetcdfFiles.debugOpenTime) {
      System.out.printf(" took %d msecs %n", took);
    }
  }

  private void parseGlobalAttributes(DAS das, DodsV root) {
    for (Attribute ncatt : root.attributes) {
      rootGroup.addAttribute(ncatt);
    }

    // loop over attribute tables, collect global attributes
    for (String tableName : das) {
      AttributeTable attTable = das.getAttributeTableN(tableName);
      if (attTable == null) {
        continue; // should probably never happen
      }

      if (tableName.equals("DODS_EXTRA")) {
        for (String attName : attTable) {
          if (attName.equals("Unlimited_Dimension")) {
            opendap.dap.Attribute att = attTable.getAttribute(attName);
            Attribute ncatt = DODSAttribute.create(attName, att);
            setUnlimited(ncatt.getStringValue());
          } else {
            logger.warn(" Unknown DODS_EXTRA attribute = " + attName + " " + location);
          }
        }

      } else if (tableName.equals("EXTRA_DIMENSION")) {
        for (String attName : attTable) {
          opendap.dap.Attribute att = attTable.getAttribute(attName);
          Attribute ncatt = DODSAttribute.create(attName, att);
          int length = ncatt.getNumericValue().intValue();
          rootGroup.addDimension(new Dimension(attName, length));
        }
      }
    }
  }

  private void constructTopVariables(DodsV rootDodsV, CancelTask cancelTask) throws IOException {
    List<DodsV> topVariables = rootDodsV.children;
    for (DodsV dodsV : topVariables) {
      if (dodsV.bt instanceof DConstructor) {
        continue;
      }
      addVariable(rootGroup, null, dodsV);
      if (cancelTask != null && cancelTask.isCancel()) {
        return;
      }
    }
  }

  private void addAttributes(Group.Builder g, DodsV dodsV) {
    for (Attribute ncatt : dodsV.attributes) {
      g.addAttribute(ncatt);
    }
  }

  private void addAttributes(Variable.Builder<?> v, DodsV dodsV) {
    for (Attribute ncatt : dodsV.attributes) {
      v.addAttribute(ncatt);
    }

    // this is the case where its (probably) a Grid, and so _Coordinate.Axes has been assigned, but if
    // theres also a coordinates attribute, need to add that info
    Attribute axes = v.getAttributeContainer().findAttribute(CF.COORDINATES);
    Attribute _axes = v.getAttributeContainer().findAttribute(_Coordinate.Axes);
    if ((null != axes) && (null != _axes)) {
      v.addAttribute(combineAxesAttrs(axes, _axes));
    }
  }

  // recursively make new variables: all new variables come through here
  Variable.Builder<?> addVariable(Group.Builder parentGroup, @Nullable Structure.Builder<?> parentStructure,
      DodsV dodsV) throws IOException {
    Variable.Builder<?> v = makeVariable(parentGroup, parentStructure, dodsV);
    if (v != null) {
      addAttributes(v, dodsV);
      if (parentStructure != null) {
        parentStructure.addMemberVariable(v);
      } else {
        parentGroup = computeGroup(v, parentGroup);
        parentGroup.addVariable(v);
      }
      dodsV.isDone = true;
    }
    return v;
  }

  @Nullable
  private Variable.Builder<?> makeVariable(Group.Builder parentGroup, Structure.Builder<?> parentStructure, DodsV dodsV)
      throws IOException {
    BaseType dodsBT = dodsV.bt;
    String dodsShortName = dodsBT.getClearName();
    if (DodsNetcdfFiles.debugConstruct) {
      System.out.print("DODSNetcdf makeVariable try to init <" + dodsShortName + "> :");
    }

    // Strings
    if (dodsBT instanceof DString) {
      if (dodsV.darray == null) {
        if (DodsNetcdfFiles.debugConstruct) {
          System.out.println("  assigned to DString: name = " + dodsShortName);
        }
        return DodsVariable.builder(this, parentGroup, dodsShortName, dodsV);
      } else {
        if (DodsNetcdfFiles.debugConstruct) {
          System.out.println("  assigned to Array of Strings: name = " + dodsShortName);
        }
        return DodsVariable.builder(this, parentGroup, dodsShortName, dodsV.darray, dodsV);
      }

      // primitives
    } else if ((dodsBT instanceof DByte) || (dodsBT instanceof DFloat32) || (dodsBT instanceof DFloat64)
        || (dodsBT instanceof DInt16) || (dodsBT instanceof DInt32)) {
      if (dodsV.darray == null) {
        if (DodsNetcdfFiles.debugConstruct) {
          System.out.printf("  assigned to scalar %s: name=%s%n", dodsBT.getTypeName(), dodsShortName);
        }
        return DodsVariable.builder(this, parentGroup, dodsShortName, dodsV);
      } else {
        if (DodsNetcdfFiles.debugConstruct) {
          System.out.printf("  assigned to array of type %s: name = %s%n", dodsBT.getClass().getName(), dodsShortName);
        }
        return DodsVariable.builder(this, parentGroup, dodsShortName, dodsV.darray, dodsV);
      }
    }

    if (dodsBT instanceof DGrid) {
      if (dodsV.darray == null) {
        if (DodsNetcdfFiles.debugConstruct) {
          System.out.println(" assigned to DGrid <" + dodsShortName + ">");
        }

        // common case is that the map vectors already exist as top level variables
        // this is how the netcdf servers do it
        for (int i = 1; i < dodsV.children.size(); i++) {
          DodsV map = dodsV.children.get(i);
          String shortName = DodsNetcdfFiles.makeShortName(map.bt.getEncodedName());
          Variable.Builder<?> mapV = parentGroup.findVariableLocal(shortName).orElse(null); // LOOK WRONG
          if (mapV == null) { // if not, add it LOOK need to compare values
            mapV = addVariable(parentGroup, parentStructure, map);
            makeCoordinateVariable(parentGroup, mapV, map.data);
          }
        }
        return DodsGrid.builder(parentGroup, dodsShortName, dodsV);
      } else {
        if (DodsNetcdfFiles.debugConstruct) {
          System.out.println(" ERROR! array of DGrid <" + dodsShortName + ">");
        }
        return null;
      }
    }

    if (dodsBT instanceof DSequence) {
      if (dodsV.darray == null) {
        if (DodsNetcdfFiles.debugConstruct) {
          System.out.println(" assigned to DSequence <" + dodsShortName + ">");
        }
        return DodsStructure.builder(this, parentGroup, dodsShortName, dodsV);
      } else {
        if (DodsNetcdfFiles.debugConstruct) {
          System.out.println(" ERROR! array of DSequence <" + dodsShortName + ">");
        }
        return null;
      }
    }

    if (dodsBT instanceof DStructure) {
      DStructure dstruct = (DStructure) dodsBT;
      if (dodsV.darray == null) {
        if (RC.getUseGroups() && (parentStructure == null) && isGroup(dstruct)) { // turn into a group
          Group.Builder gnested = Group.builder().setName(DodsNetcdfFiles.makeShortName(dodsShortName));
          addAttributes(gnested, dodsV);
          parentGroup.addGroup(gnested);
          for (DodsV nested : dodsV.children) {
            addVariable(gnested, null, nested);
          }
          return null;
        } else {
          return DodsStructure.builder(this, parentGroup, dodsShortName, dodsV);
        }
      } else { // darray not null
        return DodsStructure.builder(this, parentGroup, dodsShortName, dodsV.darray, dodsV);
      }
    }

    logger.warn("DODSNetcdf " + location + " didnt process basetype <" + dodsBT.getTypeName() + "> variable = "
        + dodsShortName);
    return null;
  }

  private void makeCoordinateVariable(Group.Builder parentGroup, Variable.Builder<?> v, Array data) {
    String name = v.shortName;

    // replace in Variable
    ArrayList<Dimension> dims = new ArrayList<>(v.getDimensions());
    Dimension oldDimension = dims.get(0);
    Dimension newDimension = new Dimension(name, oldDimension.getLength());
    dims.set(0, newDimension);
    v.setDimensions(dims);

    // replace old (if it exists) in Group with shared dimension
    parentGroup.replaceDimension(newDimension);

    // might as well cache the data
    if (data != null) {
      v.setCachedData(data, false);
    }
  }

  private void constructConstructors(DodsV rootDodsV, CancelTask cancelTask) throws IOException {
    List<DodsV> topVariables = rootDodsV.children;
    // do non-grids first
    for (DodsV dodsV : topVariables) {
      if (dodsV.isDone) {
        continue;
      }
      if (dodsV.bt instanceof DGrid) {
        continue;
      }
      addVariable(rootGroup, null, dodsV);
      if (cancelTask != null && cancelTask.isCancel()) {
        return;
      }
    }

    // then do the grids
    for (DodsV dodsV : topVariables) {
      if (dodsV.isDone) {
        continue;
      }
      // If using groups, then if the grid does not have a group name
      // and its array does, then transfer the group name.
      if (RC.getUseGroups() && dodsV.bt instanceof DGrid) {
        DodsV array = dodsV.findByIndex(0);
        if (array != null) {
          String arrayname = array.getClearName();
          String gridname = dodsV.getClearName();
          int ai = arrayname.lastIndexOf('/');
          int gi = gridname.lastIndexOf('/');
          if (gi >= 0 && ai < 0) {
            String gpath = gridname.substring(0, gi);
            arrayname = gpath + "/" + arrayname;
            array.getBaseType().setClearName(arrayname);
          } else if (gi < 0 && ai >= 0) {
            String apath = arrayname.substring(0, ai);
            gridname = apath + "/" + gridname;
            dodsV.getBaseType().setClearName(gridname);
          } else if (gi >= 0) {
            String apath = arrayname.substring(0, ai);
            String gpath = gridname.substring(0, gi);
            if (!gpath.equals(apath)) {// choose gridpath over the array path
              String arraysuffix = arrayname.substring(gi + 1);
              arrayname = gpath + "/" + arraysuffix;
              array.getBaseType().setClearName(arrayname);
            }
          } // else gi < 0 && ai < 0
        }
      }
      addVariable(rootGroup, null, dodsV);
      if (cancelTask != null && cancelTask.isCancel()) {
        return;
      }
    }
  }

  private Group.Builder computeGroup(Variable.Builder<?> v, Group.Builder parentGroup/* Ostensibly */) {
    /*
     * if (RC.getUseGroups()) { // LOOK WTF?
     * // If the path has '/' in it, then we need to insert
     * // this variable into the proper group and rename it. However,
     * // if this variable is within a structure, we cannot do it.
     * if (v.getParentStructureBuilder() == null) {
     * // HACK: Since only the grid array is used in converting
     * // to netcdf-3, we look for group info on the array.
     * String dodsname = getDodsName(v);
     * int sindex = dodsname.indexOf('/');
     * if (sindex >= 0) {
     * assert (parentGroup != null);
     * Group.Builder nested = makeRelativeGroup(parentGroup, dodsname, true);
     * parentGroup = nested;
     * }
     * }
     * }
     */
    return parentGroup;
  }

  Group.Builder makeRelativeGroup(Group.Builder parent, String path, boolean ignorelast) {
    path = path.trim();
    path = path.replace("//", "/");
    boolean isabsolute = (path.charAt(0) == '/');
    if (isabsolute) {
      path = path.substring(1);
    }

    // iteratively create path
    String[] pieces = path.split("/");
    if (ignorelast) {
      pieces[pieces.length - 1] = null;
    }

    Group.Builder current = isabsolute ? rootGroup : parent;
    for (String name : pieces) {
      if (name == null) {
        continue;
      }
      String clearname = NetcdfFiles.makeNameUnescaped(name); // ??
      Group.Builder next = current.findGroupLocal(clearname).orElse(null);
      if (next == null) {
        next = Group.builder().setName(clearname);
        current.addGroup(next);
      }
      current = next;
    }
    return current;
  }

  // make a structure into a group if its scalar and all parents are groups
  private boolean isGroup(DStructure dstruct) {
    BaseType parent = (BaseType) dstruct.getParent();
    if (parent == null) {
      return true;
    }
    if (parent instanceof DStructure) {
      return isGroup((DStructure) parent);
    }
    return true;
  }

  /**
   * Safely combine the multiple axis attributes without duplication
   *
   * @param axis1 axis attribute 1
   * @param axis2 axis attribute 2
   * @return the combined axis attribute
   */
  static Attribute combineAxesAttrs(Attribute axis1, Attribute axis2) {
    List<String> axesCombinedValues = new ArrayList<>();
    // each axis attribute is a whitespace delimited string, so just join the strings to make
    // an uber string of all values
    String axisValuesStr = axis1.getStringValue() + " " + axis2.getStringValue();
    // axis attributes are whitespace delimited, so split on whitespace to get each axis name
    String[] axisValues = axisValuesStr.split("\\s");
    for (String ax : axisValues) {
      // only add if axis name is unique - no dupes
      if (!axesCombinedValues.contains(ax) && !ax.equals("")) {
        axesCombinedValues.add(ax);
      }
    }

    return new Attribute(_Coordinate.Axes, String.join(" ", axesCombinedValues));
  }

  /**
   * Checks to see if this is netcdf char array.
   *
   * @param v must be type STRING
   * @return string length dimension, else null
   */
  Dimension getNetcdfStrlenDim(DodsVariable.Builder<?> v) {
    AttributeTable table = das.getAttributeTableN(v.getFullName()); // LOOK this probably doesnt work for nested
    // variables
    if (table == null) {
      return null;
    }

    opendap.dap.Attribute dodsAtt = table.getAttribute("DODS");
    if (dodsAtt == null) {
      return null;
    }

    AttributeTable dodsTable = dodsAtt.getContainerN();
    if (dodsTable == null) {
      return null;
    }

    opendap.dap.Attribute att = dodsTable.getAttribute("strlen");
    if (att == null) {
      return null;
    }
    String strlen = att.getValueAtN(0);

    opendap.dap.Attribute att2 = dodsTable.getAttribute("dimName");
    String dimName = (att2 == null) ? null : att2.getValueAtN(0);
    if (DodsNetcdfFiles.debugCharArray) {
      System.out.println(v.getFullName() + " has strlen= " + strlen + " dimName= " + dimName);
    }

    int dimLength;
    try {
      dimLength = Integer.parseInt(strlen);
    } catch (NumberFormatException e) {
      logger.warn("DodsBuilder " + location + " var = " + v.getFullName() + " error on strlen attribute = " + strlen);
      return null;
    }

    if (dimLength <= 0) {
      return null; // LOOK what about unlimited ??
    }
    return Dimension.builder(dimName, dimLength).setIsShared(dimName != null).build();
  }

  /**
   * If an equivilent shared dimension already exists, use it, else add d to shared dimensions. Equivilent is same name
   * and length.
   *
   * @param group from this group, if null, use rootGroup
   * @param d find equivilent shared dimension to this one.
   * @return equivilent shared dimension or d.
   */
  Dimension getSharedDimension(Group.Builder group, Dimension d) {
    if (d.getShortName() == null) {
      return d;
    }

    boolean has = sharedDimensions.contains(d);

    if (group == null) {
      group = rootGroup;
    }
    for (Dimension sd : group.getDimensions()) {
      if (sd.getShortName().equals(d.getShortName()) && sd.getLength() == d.getLength()) {
        return sd;
      }
    }

    // TODO d.setShared(true);
    group.addDimension(d);
    return d;
  }

  // construct list of dimensions to use
  List<Dimension> constructDimensions(Group.Builder group, DArray dodsArray) {
    if (group == null) {
      group = rootGroup;
    }

    List<Dimension> dims = new ArrayList<Dimension>();
    for (DArrayDimension dad : dodsArray.getDimensions()) {
      String name = dad.getEncodedName();
      if (name != null) {
        name = StringUtil2.unescape(name);
      }

      Dimension myd;

      if (name == null) { // if no name, make an anonymous dimension
        myd = Dimension.builder(null, dad.getSize()).setIsShared(false).build();

      } else { // see if shared
        if (RC.getUseGroups()) {
          if (name.indexOf('/') >= 0) {// place dimension in proper group
            group = makeRelativeGroup(group, name, true);
            // change our name
            name = name.substring(name.lastIndexOf('/') + 1);
          }
        }
        myd = group.findDimensionLocal(name).orElse(null);
        if (myd == null) { // add as shared
          myd = new Dimension(name, dad.getSize());
          group.addDimension(myd);
        } else if (myd.getLength() != dad.getSize()) { // make a non-shared dimension
          myd = Dimension.builder(name, dad.getSize()).setIsShared(false).build();
        } // else use existing, shared dimension
      }
      dims.add(myd); // add it to the list
    }

    return dims;
  }

  private void setUnlimited(String dimName) {
    Dimension dim = rootGroup.findDimension(dimName).orElse(null);
    if (dim == null) {
      logger.error(" DODS Unlimited_Dimension = " + dimName + " not found on " + location);
    }
    // dim.setUnlimited(true); // TODO
  }

  private boolean built;

  protected abstract T self();

  DodsNetcdfFile build(String datasetUrl, CancelTask cancelTask) throws IOException {
    if (built) {
      throw new IllegalStateException("already built");
    }
    built = true;
    connect(datasetUrl, cancelTask);
    return new DodsNetcdfFile(this);
  }
}
