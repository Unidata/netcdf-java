/*
 * (c) 1998-2017 University Corporation for Atmospheric Research/Unidata
 */
package ucar.nc2.dods;

import com.google.common.base.Preconditions;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
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
import opendap.util.RC;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.constants.CF;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.util.CancelTask;
import ucar.unidata.util.StringUtil2;

/** DODSNetcdfFile builder */
@NotThreadSafe
abstract class DodsBuilder<T extends DodsBuilder<T>> extends NetcdfFile.Builder<T> {
  private static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DodsBuilder.class);

  DConnect2 dodsConnection;
  DDS dds;
  DAS das;

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

    if (DODSNetcdfFile.debugServerCall) {
      System.out.println("DConnect to = <" + urlName + ">");
    }
    dodsConnection = new DConnect2(urlName, DODSNetcdfFile.accept_compress);
    if (cancelTask != null && cancelTask.isCancel())
      return;

    // fetch the DDS and DAS
    try {
      dds = dodsConnection.getDDS();
      if (DODSNetcdfFile.debugServerCall)
        System.out.println("DODSNetcdfFile readDDS");
      if (DODSNetcdfFile.debugOpenResult) {
        System.out.println("DDS = ");
        dds.print(System.out);
      }
      if (cancelTask != null && cancelTask.isCancel())
        return;

      das = dodsConnection.getDAS();
      if (DODSNetcdfFile.debugServerCall)
        System.out.println("DODSNetcdfFile readDAS");
      if (DODSNetcdfFile.debugOpenResult) {
        System.out.println("DAS = ");
        das.print(System.out);
      }
      if (cancelTask != null && cancelTask.isCancel())
        return;

      if (DODSNetcdfFile.debugOpenResult)
        System.out.println("dodsVersion = " + dodsConnection.getServerVersion());

    } catch (DAP2Exception dodsE) {
      // dodsE.printStackTrace();
      if (dodsE.getErrorCode() == DAP2Exception.NO_SUCH_FILE)
        throw new FileNotFoundException(dodsE.getMessage());
      else {
        dodsE.printStackTrace(System.err);
        throw new IOException("DODSNetcdfFile url=" + this.location, dodsE);
      }

    } catch (Throwable t) {
      logger.info("DODSNetcdfFile " + this.location, t);
      throw new IOException("DODSNetcdfFile url=" + this.location, t);
    }

    // now initialize the DODSNetcdf metadata
    DodsV rootDodsV = DodsV.parseDDS(dds);
    rootDodsV.parseDAS(das);
    if (cancelTask != null && cancelTask.isCancel())
      return;

    // LOOK why do we want to do the primitives seperate from compounds?
    constructTopVariables(rootDodsV, cancelTask);
    if (cancelTask != null && cancelTask.isCancel())
      return;

    // preload(dodsVlist, cancelTask); LOOK not using preload yet
    // if (cancelTask != null && cancelTask.isCancel()) return;

    constructConstructors(rootDodsV, cancelTask);
    if (cancelTask != null && cancelTask.isCancel())
      return;

    parseGlobalAttributes(das, rootDodsV);
    if (cancelTask != null && cancelTask.isCancel())
      return;

    if (RC.getUseGroups()) {
      try {
        reGroup();
      } catch (DAP2Exception dodsE) {
        dodsE.printStackTrace(System.err);
        throw new IOException(dodsE);
      }
    }

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
    // no comprende
    int pos;
    if (0 <= (pos = urlName.indexOf('?'))) {
      String datasetName = urlName.substring(0, pos);
      if (DODSNetcdfFile.debugServerCall)
        System.out.println(" reconnect to = <" + datasetName + ">");
      dodsConnection = new DConnect2(datasetName, DODSNetcdfFile.accept_compress);

      // parse the CE for projections
      String CE = urlName.substring(pos + 1);
      StringTokenizer stoke = new StringTokenizer(CE, " ,");
      while (stoke.hasMoreTokens()) {
        String proj = stoke.nextToken();
        int subsetPos = proj.indexOf('[');
        if (DODSNetcdfFile.debugCE)
          System.out.println(" CE = " + proj + " " + subsetPos);
        if (subsetPos > 0) {
          String vname = proj.substring(0, subsetPos);
          String vCE = proj.substring(subsetPos);
          if (DODSNetcdfFile.debugCE)
            System.out.println(" vCE = <" + vname + "><" + vCE + ">");
          DodsVariable.Builder dodsVar = findVariable(vname);
          if (dodsVar == null)
            throw new IOException("Variable not found: " + vname);

          dodsVar.setCE(vCE);
          dodsVar.setCaching(true);
        }
      }
    }

    if (DODSNetcdfFile.showNCfile)
      System.out.println("DODS nc file = " + this);
    long took = System.currentTimeMillis() - start;
    if (DODSNetcdfFile.debugOpenTime)
      System.out.printf(" took %d msecs %n", took);
  }

  private void parseGlobalAttributes(DAS das, DodsV root) {
    List<DODSAttribute> atts = root.attributes;
    for (Attribute ncatt : atts) {
      rootGroup.addAttribute(ncatt);
    }

    // loop over attribute tables, collect global attributes
    for (String tableName : das) {
      AttributeTable attTable = das.getAttributeTableN(tableName);
      if (attTable == null)
        continue; // should probably never happen

      if (tableName.equals("DODS_EXTRA")) {
        for (String attName : attTable) {
          if (attName.equals("Unlimited_Dimension")) {
            opendap.dap.Attribute att = attTable.getAttribute(attName);
            DODSAttribute ncatt = DODSAttribute.create(attName, att);
            setUnlimited(ncatt.getStringValue());
          } else
            logger.warn(" Unknown DODS_EXTRA attribute = " + attName + " " + location);
        }

      } else if (tableName.equals("EXTRA_DIMENSION")) {
        for (String attName : attTable) {
          opendap.dap.Attribute att = attTable.getAttribute(attName);
          DODSAttribute ncatt = DODSAttribute.create(attName, att);
          int length = ncatt.getNumericValue().intValue();
          rootGroup.addDimension(new Dimension(attName, length));
        }
      }
    }
  }

  private void constructTopVariables(DodsV rootDodsV, CancelTask cancelTask) throws IOException {
    List<DodsV> topVariables = rootDodsV.children;
    for (DodsV dodsV : topVariables) {
      if (dodsV.bt instanceof DConstructor)
        continue;
      addVariable(rootGroup, null, dodsV);
      if (cancelTask != null && cancelTask.isCancel())
        return;
    }
  }

  private void addAttributes(Group.Builder g, DodsV dodsV) {
    List<DODSAttribute> atts = dodsV.attributes;
    for (Attribute ncatt : atts) {
      g.addAttribute(ncatt);
    }
  }

  private void addAttributes(Variable.Builder<?> v, DodsV dodsV) {
    List<DODSAttribute> atts = dodsV.attributes;
    for (Attribute ncatt : atts) {
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
  @Nullable
  private Variable.Builder<?> addVariable(Group.Builder parentGroup, @Nullable Structure.Builder<?> parentStructure, DodsV dodsV) throws IOException {
    Variable.Builder<?> v = makeVariable(parentGroup, parentStructure, dodsV);
    if (v != null) {
      addAttributes(v, dodsV);
      if (parentStructure != null)
        parentStructure.addMemberVariable(v);
      else {
        parentGroup = computeGroup(v, parentGroup);
        parentGroup.addVariable(v);
      }
      dodsV.isDone = true;
      parentGroup.addVariable(v);
    }
    return v;
  }

  @Nullable
  private Variable.Builder<?> makeVariable(Group.Builder parentGroup, Structure.Builder<?> parentStructure, DodsV dodsV) throws IOException {
    BaseType dodsBT = dodsV.bt;
    String dodsShortName = dodsBT.getClearName();
    if (DODSNetcdfFile.debugConstruct)
      System.out.print("DODSNetcdf makeVariable try to init <" + dodsShortName + "> :");

    // Strings
    if (dodsBT instanceof DString) {
      if (dodsV.darray == null) {
        if (DODSNetcdfFile.debugConstruct) {
          System.out.println("  assigned to DString: name = " + dodsShortName);
        }
        return DodsVariable.builder(dodsShortName, dodsV);
      } else {
        if (DODSNetcdfFile.debugConstruct) {
          System.out.println("  assigned to Array of Strings: name = " + dodsShortName);
        }
        return DodsVariable.builder(dodsShortName, dodsV.darray, dodsV);
      }

      // primitives
    } else if ((dodsBT instanceof DByte) || (dodsBT instanceof DFloat32) || (dodsBT instanceof DFloat64)
        || (dodsBT instanceof DInt16) || (dodsBT instanceof DInt32)) {
      if (dodsV.darray == null) {
        if (DODSNetcdfFile.debugConstruct) {
          System.out.printf("  assigned to scalar %s: name=%s%n", dodsBT.getTypeName(), dodsShortName);
        }
        return DodsVariable.builder(dodsShortName, dodsV);
      } else {
        if (DODSNetcdfFile.debugConstruct) {
          System.out.printf("  assigned to array of type %s: name = %s%n", dodsBT.getClass().getName(), dodsShortName);
        }
        return DodsVariable.builder(dodsShortName, dodsV.darray, dodsV);
      }
    }

    if (dodsBT instanceof DGrid) {
      if (dodsV.darray == null) {
        if (DODSNetcdfFile.debugConstruct)
          System.out.println(" assigned to DGrid <" + dodsShortName + ">");

        // common case is that the map vectors already exist as top level variables
        // this is how the netccdf servers do it
        for (int i = 1; i < dodsV.children.size(); i++) {
          DodsV map = dodsV.children.get(i);
          String shortName = DODSNetcdfFile.makeShortName(map.bt.getEncodedName());
          Variable.Builder<?> mapV = parentGroup.findVariableLocal(shortName).orElse(null); // LOOK WRONG
          if (mapV == null) { // if not, add it LOOK need to compare values
            mapV = addVariable(parentGroup, parentStructure, map);
            makeCoordinateVariable(parentGroup, mapV, map.data);
          }
        }

        return DodsGrid.builder(dodsShortName, dodsV);

      } else {
        if (DODSNetcdfFile.debugConstruct)
          System.out.println(" ERROR! array of DGrid <" + dodsShortName + ">");
        return null;
      }

    } else if (dodsBT instanceof DSequence) {
      if (dodsV.darray == null) {
        if (DODSNetcdfFile.debugConstruct) {
          System.out.println(" assigned to DSequence <" + dodsShortName + ">");
        }
        return DodsStructure.builder(dodsShortName, dodsV);
      } else {
        if (DODSNetcdfFile.debugConstruct) {
          System.out.println(" ERROR! array of DSequence <" + dodsShortName + ">");
        }
        return null;
      }

    } else if (dodsBT instanceof DStructure) {
      DStructure dstruct = (DStructure) dodsBT;
      if (dodsV.darray == null) {
        if (RC.getUseGroups() && (parentStructure == null) && isGroup(dstruct)) { // turn into a group
          if (DODSNetcdfFile.debugConstruct)
            System.out.println(" assigned to Group <" + dodsShortName + ">");
          Group.Builder gnested = Group.builder().setName(DODSNetcdfFile.makeShortName(dodsShortName));
          addAttributes(gnested, dodsV);
          parentGroup.addGroup(gnested);

          for (DodsV nested : dodsV.children) {
            addVariable(gnested, null, nested);
          }
          return null;
        } else {
          if (DODSNetcdfFile.debugConstruct) {
            System.out.println(" assigned to DStructure <" + dodsShortName + ">");
          }
          return DodsStructure.builder(dodsShortName, dodsV);
        }
      } else {
        if (DODSNetcdfFile.debugConstruct) {
          System.out.println(" assigned to Array of DStructure <" + dodsShortName + "> ");
        }
        return DodsStructure.builder(dodsShortName, dodsV.darray, dodsV);
      }

    } else {
      logger.warn("DODSNetcdf " + location + " didnt process basetype <" + dodsBT.getTypeName() + "> variable = "
          + dodsShortName);
      return null;
    }
  }

  private void makeCoordinateVariable(Group.Builder parentGroup, Variable.Builder<?> v, Array data) {
    String name = v.shortName;

    // replace in Variable
    Dimension oldDimension = v.getDimension(0);
    Dimension newDimension = new Dimension(name, oldDimension.getLength());
    // newDimension.setCoordinateAxis( v); calcCoordinateVaribale will do this
    v.setDimension(0, newDimension);

    // replace old (if it exists) in Group with shared dimension
    Dimension old = parentGroup.findDimension(name);
    parentGroup.remove(old);
    parentGroup.addDimension(newDimension);

    // might as well cache the data
    if (data != null) {
      v.setCachedData(data, false);
      if (DODSNetcdfFile.debugCached)
        System.out.println(" cache for <" + name + "> length =" + data.getSize());
    }
  }

  private void constructConstructors(DodsV rootDodsV, CancelTask cancelTask) throws IOException {
    List<DodsV> topVariables = rootDodsV.children;
    // do non-grids first
    for (DodsV dodsV : topVariables) {
      if (dodsV.isDone)
        continue;
      if (dodsV.bt instanceof DGrid)
        continue;
      addVariable(rootGroup, null, dodsV);
      if (cancelTask != null && cancelTask.isCancel())
        return;
    }

    // then do the grids
    for (DodsV dodsV : topVariables) {
      if (dodsV.isDone)
        continue;
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
              String arraysuffix = arrayname.substring(gi + 1, arrayname.length());
              arrayname = gpath + "/" + arraysuffix;
              array.getBaseType().setClearName(arrayname);
            }
          } // else gi < 0 && ai < 0
        }
      }
      addVariable(rootGroup, null, dodsV);
      if (cancelTask != null && cancelTask.isCancel())
        return;
    }
  }

  private Group.Builder computeGroup(DodsVariable.Builder<?> v, Group.Builder parentGroup/* Ostensibly */) {
    if (RC.getUseGroups()) {
      // If the path has '/' in it, then we need to insert
      // this variable into the proper group and rename it. However,
      // if this variable is within a structure, we cannot do it.
      if (v.getParentStructure() == null) {
        // HACK: Since only the grid array is used in converting
        // to netcdf-3, we look for group info on the array.
        String dodsname = v.getDodsName();
        int sindex = dodsname.indexOf('/');
        if (sindex >= 0) {
          assert (parentGroup != null);
          Group.Builder g = parentGroup.makeRelativeGroup(this, dodsname, true/* ignorelast */);
          parentGroup = g;
        }
      }
    }
    return parentGroup;
  }

  // make a structure into a group if its scalar and all parents are groups
  private boolean isGroup(DStructure dstruct) {
    BaseType parent = (BaseType) dstruct.getParent();
    if (parent == null)
      return true;
    if (parent instanceof DStructure)
      return isGroup((DStructure) parent);
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

    List axesCombinedValues = new ArrayList<String>();
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
  private Dimension getNetcdfStrlenDim(DodsVariable v) {
    AttributeTable table = das.getAttributeTableN(v.getFullName()); // LOOK this probably doesnt work for nested
    // variables
    if (table == null)
      return null;

    opendap.dap.Attribute dodsAtt = table.getAttribute("DODS");
    if (dodsAtt == null)
      return null;

    AttributeTable dodsTable = dodsAtt.getContainerN();
    if (dodsTable == null)
      return null;

    opendap.dap.Attribute att = dodsTable.getAttribute("strlen");
    if (att == null)
      return null;
    String strlen = att.getValueAtN(0);

    opendap.dap.Attribute att2 = dodsTable.getAttribute("dimName");
    String dimName = (att2 == null) ? null : att2.getValueAtN(0);
    if (DODSNetcdfFile.debugCharArray)
      System.out.println(v.getFullName() + " has strlen= " + strlen + " dimName= " + dimName);

    int dimLength;
    try {
      dimLength = Integer.parseInt(strlen);
    } catch (NumberFormatException e) {
      logger
          .warn("DODSNetcdfFile " + location + " var = " + v.getFullName() + " error on strlen attribute = " + strlen);
      return null;
    }

    if (dimLength <= 0)
      return null; // LOOK what about unlimited ??
    return Dimension.builder(dimName, dimLength).setIsShared(dimName != null).build();
  }

  /**
   * If an equivilent shared dimension already exists, use it, else add d to shared dimensions. Equivilent is same name and length.
   *
   * @param group from this group, if null, use rootGroup
   * @param d     find equivilent shared dimension to this one.
   * @return equivilent shared dimension or d.
   */
  Dimension getSharedDimension(Group group, Dimension d) {
    if (d.getShortName() == null)
      return d;

    if (group == null)
      group = rootGroup;
    for (Dimension sd : group.getDimensions()) {
      if (sd.getShortName().equals(d.getShortName()) && sd.getLength() == d.getLength())
        return sd;
    }
    d.setShared(true);
    group.addDimension(d);
    return d;
  }

  // construct list of dimensions to use

  List<Dimension> constructDimensions(Group.Builder group, DArray dodsArray) {
    if (group == null)
      group = rootGroup;

    List<Dimension> dims = new ArrayList<Dimension>();
    Enumeration enumerate = dodsArray.getDimensions();
    while (enumerate.hasMoreElements()) {
      DArrayDimension dad = (DArrayDimension) enumerate.nextElement();
      String name = dad.getEncodedName();
      if (name != null)
        name = StringUtil2.unescape(name);

      Dimension myd;

      if (name == null) { // if no name, make an anonymous dimension
        myd = Dimension.builder(null, dad.getSize()).setIsShared(false).build();

      } else { // see if shared
        if (RC.getUseGroups()) {
          if (name.indexOf('/') >= 0) {// place dimension in proper group
            group = group.makeRelativeGroup(this, name, true);
            // change our name
            name = name.substring(name.lastIndexOf('/') + 1);
          }
        }
        myd = group.findDimension(name);
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
    if (dim != null)
      dim.setUnlimited(true);
    else
      logger.error(" DODS Unlimited_Dimension = " + dimName + " not found on " + location);
  }

  private boolean built;

  protected abstract T self();

  DODSNetcdfFile build(String datasetUrl, CancelTask cancelTask) throws IOException {
    if (built)
      throw new IllegalStateException("already built");
    built = true;
    connect(datasetUrl, cancelTask);
    return new DODSNetcdfFile(this);
  }
}