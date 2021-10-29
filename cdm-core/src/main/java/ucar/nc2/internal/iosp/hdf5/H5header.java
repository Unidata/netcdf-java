/*
 * Copyright (c) 1998-2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.iosp.hdf5;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import com.google.common.base.Preconditions;
import ucar.array.ArrayType;
import ucar.array.Array;
import ucar.array.ArrayVlen;
import ucar.array.Arrays;
import ucar.array.StorageMutable;
import ucar.array.StructureDataArray;
import ucar.array.StructureDataStorageBB;
import ucar.array.StructureMembers;
import ucar.array.InvalidRangeException;
import ucar.array.Section;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainerMutable;
import ucar.nc2.Dimension;
import ucar.nc2.EnumTypedef;
import ucar.nc2.Group;
import ucar.nc2.Group.Builder;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.internal.iosp.hdf4.HdfEos;
import ucar.nc2.internal.iosp.hdf4.HdfHeaderIF;
import ucar.nc2.internal.iosp.hdf5.H5objects.DataObject;
import ucar.nc2.internal.iosp.hdf5.H5objects.DataObjectFacade;
import ucar.nc2.internal.iosp.hdf5.H5objects.Filter;
import ucar.nc2.internal.iosp.hdf5.H5objects.GlobalHeap;
import ucar.nc2.internal.iosp.hdf5.H5objects.H5Group;
import ucar.nc2.internal.iosp.hdf5.H5objects.HeaderMessage;
import ucar.nc2.internal.iosp.hdf5.H5objects.HeapIdentifier;
import ucar.nc2.internal.iosp.hdf5.H5objects.MessageAttribute;
import ucar.nc2.internal.iosp.hdf5.H5objects.MessageComment;
import ucar.nc2.internal.iosp.hdf5.H5objects.MessageDataspace;
import ucar.nc2.internal.iosp.hdf5.H5objects.MessageDatatype;
import ucar.nc2.internal.iosp.hdf5.H5objects.MessageFillValue;
import ucar.nc2.internal.iosp.hdf5.H5objects.MessageFillValueOld;
import ucar.nc2.internal.iosp.hdf5.H5objects.MessageFilter;
import ucar.nc2.internal.iosp.hdf5.H5objects.MessageType;
import ucar.nc2.internal.iosp.hdf5.H5objects.StructureMember;
import ucar.nc2.iosp.IospArrayHelper;
import ucar.nc2.iosp.NetcdfFileFormat;
import ucar.nc2.iosp.Layout;
import ucar.nc2.iosp.LayoutRegular;
import ucar.nc2.iosp.NetcdfFormatUtils;
import ucar.unidata.io.RandomAccessFile;

/** Read all of the metadata of an HD5 file. */
public class H5header implements HdfHeaderIF {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(H5header.class);

  // special attribute names in HDF5
  public static final String HDF5_CLASS = "CLASS";
  public static final String HDF5_DIMENSION_LIST = "DIMENSION_LIST";
  public static final String HDF5_DIMENSION_SCALE = "DIMENSION_SCALE";
  public static final String HDF5_DIMENSION_LABELS = "DIMENSION_LABELS";
  public static final String HDF5_DIMENSION_NAME = "NAME";
  public static final String HDF5_REFERENCE_LIST = "REFERENCE_LIST";

  // debugging
  private static boolean debugVlen;
  private static boolean debug1, debugDetail, debugPos, debugHeap, debugV;
  private static boolean debugGroupBtree, debugDataBtree, debugBtree2;
  private static boolean debugContinueMessage, debugTracker, debugSoftLink, debugHardLink, debugSymbolTable;
  private static boolean warnings = true, debugReference, debugCreationOrder, debugStructure;
  private static boolean debugDimensionScales;

  // NULL string value, following netCDF-C, set to NIL
  private static final String NULL_STRING_VALUE = "NIL";

  public static void setWarnings(boolean warn) {
    warnings = warn;
  }

  public static void setDebugFlags(ucar.nc2.util.DebugFlags debugFlag) {
    debug1 = debugFlag.isSet("H5header/header");
    debugBtree2 = debugFlag.isSet("H5header/btree2");
    debugContinueMessage = debugFlag.isSet("H5header/continueMessage");
    debugDetail = debugFlag.isSet("H5header/headerDetails");
    debugDataBtree = debugFlag.isSet("H5header/dataBtree");
    debugGroupBtree = debugFlag.isSet("H5header/groupBtree");
    debugHeap = debugFlag.isSet("H5header/Heap");
    debugPos = debugFlag.isSet("H5header/filePos");
    debugReference = debugFlag.isSet("H5header/reference");
    debugSoftLink = debugFlag.isSet("H5header/softLink");
    debugHardLink = debugFlag.isSet("H5header/hardLink");
    debugSymbolTable = debugFlag.isSet("H5header/symbolTable");
    debugTracker = debugFlag.isSet("H5header/memTracker");
    debugV = debugFlag.isSet("H5header/Variable");
    debugStructure = debugFlag.isSet("H5header/structure");
  }

  private static final byte[] magic = {(byte) 0x89, 'H', 'D', 'F', '\r', '\n', 0x1a, '\n'};
  private static final String magicString = new String(magic, StandardCharsets.UTF_8);
  private static final boolean transformReference = true;

  public static boolean isValidFile(RandomAccessFile raf) throws IOException {
    NetcdfFileFormat format = NetcdfFileFormat.findNetcdfFormatType(raf);
    return format != null && format.isNetdf4format();
  }

  ////////////////////////////////////////////////////////////////////////////////


  /*
   * Implementation notes
   * any field called address is actually relative to the base address.
   * any field called filePos or dataPos is a byte offset within the file.
   *
   * it appears theres no sure fire way to tell if the file was written by netcdf4 library
   * 1) if one of the the NETCF4-XXX atts are set
   * 2) dimension scales:
   * 1) all dimensions have a dimension scale
   * 2) they all have the same length as the dimension
   * 3) all variables' dimensions have a dimension scale
   */

  private static final int KNOWN_FILTERS = 3;

  private final RandomAccessFile raf;
  private final Group.Builder root;
  private final H5iosp h5iosp;

  private long baseAddress;
  byte sizeOffsets, sizeLengths;
  boolean isOffsetLong, isLengthLong;

  /*
   * Cant always tell if written with netcdf library. if all dimensions have coordinate variables, eg:
   * Q:/cdmUnitTest/formats/netcdf4/ncom_relo_fukushima_1km_tmp_2011040800_t000.nc4
   */
  private boolean isNetcdf4;
  private H5Group h5rootGroup;
  private final Map<String, DataObjectFacade> symlinkMap = new HashMap<>(200);
  private final Map<Long, DataObject> addressMap = new HashMap<>(200);
  private java.text.SimpleDateFormat hdfDateParser;

  H5objects h5objects;
  private PrintWriter debugOut;
  private MemTracker memTracker;

  private final Charset valueCharset;

  H5header(RandomAccessFile myRaf, Group.Builder root, H5iosp h5iosp) {
    this.raf = myRaf;
    this.root = root;
    this.h5iosp = h5iosp;
    valueCharset = h5iosp.getValueCharset().orElse(StandardCharsets.UTF_8);
  }

  /** Return defined {@link Charset value charset} that will be used when reading HDF5 header. */
  protected Charset getValueCharset() {
    return valueCharset;
  }

  // Public for debugging
  public void read(PrintWriter debugPS) throws IOException {
    if (debugPS != null) {
      debugOut = debugPS;
    } else if (debug1 || debugContinueMessage || debugCreationOrder || debugDetail || debugDimensionScales
        || debugGroupBtree || debugHardLink || debugHeap || debugPos || debugReference || debugTracker || debugV
        || debugSoftLink || warnings) {
      debugOut = new PrintWriter(new OutputStreamWriter(System.out));
    }
    h5objects = new H5objects(this, debugOut, memTracker);

    long actualSize = raf.length();

    if (debugTracker)
      memTracker = new MemTracker(actualSize);

    // find the superblock - no limits on how far in
    boolean ok = false;
    long filePos = 0;
    while ((filePos < actualSize - 8)) {
      raf.seek(filePos);
      String magic = raf.readString(8);
      if (magic.equals(magicString)) {
        ok = true;
        break;
      }
      filePos = (filePos == 0) ? 512 : 2 * filePos;
    }
    if (!ok) {
      throw new IOException("Not a netCDF4/HDF5 file ");
    }
    if (debug1) {
      log.debug("H5header opened file to read:'{}' size= {}", raf.getLocation(), actualSize);
    }
    // now we are positioned right after the header

    // header information is in le byte order
    raf.order(RandomAccessFile.LITTLE_ENDIAN);

    long superblockStart = raf.getFilePointer() - 8;
    if (debugTracker)
      memTracker.add("header", 0, superblockStart);

    // superblock version
    byte versionSB = raf.readByte();

    if (versionSB < 2) {
      readSuperBlock1(superblockStart, versionSB);
    } else if (versionSB == 2) {
      readSuperBlock2(superblockStart);
    } else {
      throw new IOException("Unknown superblock version= " + versionSB);
    }

    // now look for symbolic links LOOK this doesnt work; probably remove 10/27/14 jc
    replaceSymbolicLinks(h5rootGroup);

    // recursively run through all the dataObjects and add them to the ncfile
    boolean allSharedDimensions = makeNetcdfGroup(root, h5rootGroup);
    if (allSharedDimensions)
      isNetcdf4 = true;

    if (debugTracker) {
      Formatter f = new Formatter();
      memTracker.report(f);
      log.debug(f.toString());
    }

    debugOut = null;
  }

  private void readSuperBlock1(long superblockStart, byte versionSB) throws IOException {
    byte versionFSS, versionGroup, versionSHMF;
    short btreeLeafNodeSize, btreeInternalNodeSize;
    int fileFlags;

    long heapAddress;
    long eofAddress;
    long driverBlockAddress;

    versionFSS = raf.readByte();
    versionGroup = raf.readByte();
    raf.readByte(); // skip 1 byte
    versionSHMF = raf.readByte();
    if (debugDetail) {
      log.debug(" versionSB= " + versionSB + " versionFSS= " + versionFSS + " versionGroup= " + versionGroup
          + " versionSHMF= " + versionSHMF);
    }

    sizeOffsets = raf.readByte();
    isOffsetLong = (sizeOffsets == 8);

    sizeLengths = raf.readByte();
    isLengthLong = (sizeLengths == 8);
    if (debugDetail) {
      log.debug(" sizeOffsets= {} sizeLengths= {}", sizeOffsets, sizeLengths);
      log.debug(" isLengthLong= {} isOffsetLong= {}", isLengthLong, isOffsetLong);
    }

    raf.read(); // skip 1 byte
    // log.debug(" position="+mapBuffer.position());

    btreeLeafNodeSize = raf.readShort();
    btreeInternalNodeSize = raf.readShort();
    if (debugDetail) {
      log.debug(" btreeLeafNodeSize= {} btreeInternalNodeSize= {}", btreeLeafNodeSize, btreeInternalNodeSize);
    }
    // log.debug(" position="+mapBuffer.position());

    fileFlags = raf.readInt();
    if (debugDetail) {
      log.debug(" fileFlags= 0x{}", Integer.toHexString(fileFlags));
    }

    if (versionSB == 1) {
      short storageInternalNodeSize = raf.readShort();
      raf.skipBytes(2);
    }

    baseAddress = readOffset();
    heapAddress = readOffset();
    eofAddress = readOffset();
    driverBlockAddress = readOffset();

    if (baseAddress != superblockStart) {
      baseAddress = superblockStart;
      eofAddress += superblockStart;
      if (debugDetail) {
        log.debug(" baseAddress set to superblockStart");
      }
    }

    if (debugDetail) {
      log.debug(" baseAddress= 0x{}", Long.toHexString(baseAddress));
      log.debug(" global free space heap Address= 0x{}", Long.toHexString(heapAddress));
      log.debug(" eof Address={}", eofAddress);
      log.debug(" raf length= {}", raf.length());
      log.debug(" driver BlockAddress= 0x{}", Long.toHexString(driverBlockAddress));
      log.debug("");
    }
    if (debugTracker)
      memTracker.add("superblock", superblockStart, raf.getFilePointer());

    // look for file truncation
    long fileSize = raf.length();
    if (fileSize < eofAddress)
      throw new IOException(
          "File is truncated should be= " + eofAddress + " actual = " + fileSize + "%nlocation= " + raf.getLocation());

    // next comes the root object's SymbolTableEntry
    // extract the root group object, recursively read all objects
    h5rootGroup = h5objects.readRootSymbolTable(raf.getFilePointer());
  }

  private void readSuperBlock2(long superblockStart) throws IOException {
    sizeOffsets = raf.readByte();
    isOffsetLong = (sizeOffsets == 8);

    sizeLengths = raf.readByte();
    isLengthLong = (sizeLengths == 8);
    if (debugDetail) {
      log.debug(" sizeOffsets= {} sizeLengths= {}", sizeOffsets, sizeLengths);
      log.debug(" isLengthLong= {} isOffsetLong= {}", isLengthLong, isOffsetLong);
    }

    byte fileFlags = raf.readByte();
    if (debugDetail) {
      log.debug(" fileFlags= 0x{}", Integer.toHexString(fileFlags));
    }

    baseAddress = readOffset();
    long extensionAddress = readOffset();
    long eofAddress = readOffset();
    long rootObjectAddress = readOffset();
    int checksum = raf.readInt();

    if (debugDetail) {
      log.debug(" baseAddress= 0x{}", Long.toHexString(baseAddress));
      log.debug(" extensionAddress= 0x{}", Long.toHexString(extensionAddress));
      log.debug(" eof Address={}", eofAddress);
      log.debug(" rootObjectAddress= 0x{}", Long.toHexString(rootObjectAddress));
      log.debug("");
    }

    if (debugTracker)
      memTracker.add("superblock", superblockStart, raf.getFilePointer());

    if (baseAddress != superblockStart) {
      baseAddress = superblockStart;
      eofAddress += superblockStart;
      if (debugDetail) {
        log.debug(" baseAddress set to superblockStart");
      }
    }

    // look for file truncation
    long fileSize = raf.length();
    if (fileSize < eofAddress) {
      throw new IOException("File is truncated should be= " + eofAddress + " actual = " + fileSize);
    }

    h5rootGroup = h5objects.readRootObject(rootObjectAddress);
  }

  private void replaceSymbolicLinks(H5Group group) {
    if (group == null)
      return;

    List<DataObjectFacade> objList = group.nestedObjects;
    int count = 0;
    while (count < objList.size()) {
      DataObjectFacade dof = objList.get(count);

      if (dof.group != null) { // group - recurse
        replaceSymbolicLinks(dof.group);

      } else if (dof.linkName != null) { // symbolic links
        DataObjectFacade link = symlinkMap.get(dof.linkName);
        if (link == null) {
          log.warn(" WARNING Didnt find symbolic link={} from {}", dof.linkName, dof.name);
          objList.remove(count);
          continue;
        }

        // dont allow loops
        if (link.group != null) {
          if (group.isChildOf(link.group)) {
            log.warn(" ERROR Symbolic Link loop found ={}", dof.linkName);
            objList.remove(count);
            continue;
          }
        }

        // dont allow in the same group. better would be to replicate the group with the new name
        if (dof.parent == link.parent) {
          objList.remove(dof);
          count--; // negate the incr
        } else // replace
          objList.set(count, link);

        if (debugSoftLink) {
          log.debug("  Found symbolic link={}", dof.linkName);
        }
      }

      count++;
    }
  }

  void addSymlinkMap(String name, DataObjectFacade facade) {
    symlinkMap.put(name, facade);
  }

  ///////////////////////////////////////////////////////////////
  // construct netcdf objects

  private boolean makeNetcdfGroup(Group.Builder parentGroup, H5Group h5group) throws IOException {

    /*
     * 6/21/2013 new algorithm for dimensions.
     * 1. find all objects with all CLASS = "DIMENSION_SCALE", make into a dimension. use shape(0) as length. keep in
     * order
     * 2. if also a variable (NAME != "This is a ...") then first dim = itself, second matches length, if multiple
     * match, use :_Netcdf4Coordinates = 0, 3 and order of dimensions.
     * 3. use DIMENSION_LIST to assign dimensions to data variables.
     */

    // 1. find all objects with all CLASS = "DIMENSION_SCALE", make into a dimension. use shape(0) as length. keep in
    // order
    for (DataObjectFacade facade : h5group.nestedObjects) {
      if (facade.isVariable)
        findDimensionScales(parentGroup, h5group, facade);
    }

    // 2. if also a variable (NAME != "This is a ...") then first dim = itself, second matches length, if multiple
    // match, use :_Netcdf4Coordinates = 0, 3 and order of dimensions.
    for (DataObjectFacade facade : h5group.nestedObjects) {
      if (facade.is2DCoordinate)
        findDimensionScales2D(h5group, facade);
    }

    boolean allHaveSharedDimensions = true;

    // 3. use DIMENSION_LIST to assign dimensions to other variables.
    for (DataObjectFacade facade : h5group.nestedObjects) {
      if (facade.isVariable)
        allHaveSharedDimensions &= findSharedDimensions(parentGroup, h5group, facade);
    }

    createDimensions(parentGroup, h5group);

    // process types first
    for (DataObjectFacade facadeNested : h5group.nestedObjects) {
      if (facadeNested.isTypedef) {
        if (debugReference && facadeNested.dobj.mdt.type == 7) {
          log.debug("{}", facadeNested);
        }

        if (facadeNested.dobj.mdt.map != null) {
          EnumTypedef enumTypedef = parentGroup.findEnumeration(facadeNested.name).orElse(null);
          if (enumTypedef == null) {
            ArrayType basetype;
            switch (facadeNested.dobj.mdt.byteSize) {
              case 1:
                basetype = ArrayType.ENUM1;
                break;
              case 2:
                basetype = ArrayType.ENUM2;
                break;
              default:
                basetype = ArrayType.ENUM4;
                break;
            }
            enumTypedef = new EnumTypedef(facadeNested.name, facadeNested.dobj.mdt.map, basetype);
            parentGroup.addEnumTypedef(enumTypedef);
          }
        }
        if (debugV) {
          log.debug("  made enumeration {}", facadeNested.name);
        }
      }

    } // loop over typedefs

    // nested objects - groups and variables
    for (DataObjectFacade facadeNested : h5group.nestedObjects) {

      if (facadeNested.isGroup) {
        H5Group h5groupNested = h5objects.readH5Group(facadeNested);
        if (facadeNested.group == null) // hard link with cycle
          continue; // just skip it
        Group.Builder nestedGroup = Group.builder().setName(facadeNested.name);
        parentGroup.addGroup(nestedGroup);
        allHaveSharedDimensions &= makeNetcdfGroup(nestedGroup, h5groupNested);
        if (debug1) {
          log.debug("--made Group " + nestedGroup.shortName + " add to " + parentGroup.shortName);
        }

      } else if (facadeNested.isVariable) {
        if (debugReference && facadeNested.dobj.mdt.type == 7) {
          log.debug("{}", facadeNested);
        }

        Variable.Builder<?> v = makeVariable(parentGroup, facadeNested);
        if ((v != null) && (v.dataType != null)) {
          parentGroup.addVariable(v);

          if (v.dataType.isEnum()) {
            String enumTypeName = v.getEnumTypeName();
            if (enumTypeName == null) {
              log.warn("EnumTypedef is missing for variable: {}", v.shortName);
              throw new IllegalStateException("EnumTypedef is missing for variable: " + v.shortName);
            }
            // This code apparently addresses the possibility of an anonymous enum LOOK ??
            if (enumTypeName.isEmpty()) {
              EnumTypedef enumTypedef = parentGroup.findEnumeration(facadeNested.name).orElse(null);
              if (enumTypedef == null) {
                enumTypedef = new EnumTypedef(facadeNested.name, facadeNested.dobj.mdt.map);
                parentGroup.addEnumTypedef(enumTypedef);
                v.setEnumTypeName(enumTypedef.getShortName());
              }
            }
          }

          Vinfo vinfo = (Vinfo) v.spiObject;
          if (debugV) {
            log.debug("  made Variable " + v.shortName + "  vinfo= " + vinfo + "\n" + v);
          }
        }
      }

    } // loop over nested objects

    // create group attributes last. need enums to be found first
    List<MessageAttribute> fatts = filterAttributes(h5group.facade.dobj.attributes);
    for (MessageAttribute matt : fatts) {
      try {
        makeAttributes(null, matt, parentGroup.getAttributeContainer());
      } catch (InvalidRangeException e) {
        throw new IOException(e.getMessage());
      }
    }

    // add system attributes
    processSystemAttributes(h5group.facade.dobj.messages, parentGroup.getAttributeContainer());
    return allHaveSharedDimensions;
  }

  /////////////////////////
  /*
   * from https://www.unidata.ucar.edu/software/netcdf/docs/netcdf.html#NetCDF_002d4-Format
   * C.3.7 Attributes
   *
   * Attributes in HDF5 and netCDF-4 correspond very closely. Each attribute in an HDF5 file is represented as an
   * attribute
   * in the netCDF-4 file, with the exception of the attributes below, which are ignored by the netCDF-4 API.
   *
   * _Netcdf4Coordinates An integer array containing the dimension IDs of a variable which is a multi-dimensional
   * coordinate variable.
   * _nc3_strict When this (scalar, H5T_NATIVE_INT) attribute exists in the root group of the HDF5 file, the netCDF API
   * will enforce
   * the netCDF classic model on the data file.
   * REFERENCE_LIST This attribute is created and maintained by the HDF5 dimension scale API.
   * CLASS This attribute is created and maintained by the HDF5 dimension scale API.
   * DIMENSION_LIST This attribute is created and maintained by the HDF5 dimension scale API.
   * NAME This attribute is created and maintained by the HDF5 dimension scale API.
   *
   * ----------
   * from dim_scales_wk9 - Nunes.ppt
   *
   * Attribute named "CLASS" with the value "DIMENSION_SCALE"
   * Optional attribute named "NAME"
   * Attribute references to any associated Dataset
   *
   * -------------
   * from https://www.unidata.ucar.edu/mailing_lists/archives/netcdfgroup/2008/msg00093.html
   *
   * Then comes the part you will have to do for your datasets. You open the data
   * dataset, get an ID, DID variable here, open the latitude dataset, get its ID,
   * DSID variable here, and "link" the 2 with this call
   *
   * if (H5DSattach_scale(did,dsid,DIM0) < 0)
   *
   * what this function does is to associated the dataset DSID (latitude) with the
   * dimension* specified by the parameter DIM0 (0, in this case, the first
   * dimension of the 2D array) of the dataset DID
   *
   * If you open HDF Explorer and expand the attributes of the "data" dataset you
   * will see an attribute called DIMENSION_LIST.
   * This is done by this function. It is an array that contains 2 HDF5 references,
   * one for the latitude dataset, other for the longitude)
   *
   * If you expand the "lat" dataset , you will see that it contains an attribute
   * called REFERENCE_LIST. It is a compound type that contains
   * 1) a reference to my "data" dataset
   * 2) the index of the data dataset this scale is to be associated with (0
   * for the lat, 1 for the lon)
   */

  // find the Dimension Scale objects, turn them into shared dimensions
  // always has attribute CLASS = "DIMENSION_SCALE"
  // note that we dont bother looking at their REFERENCE_LIST
  private void findDimensionScales(Group.Builder g, H5Group h5group, DataObjectFacade facade) throws IOException {
    Iterator<MessageAttribute> iter = facade.dobj.attributes.iterator();
    while (iter.hasNext()) {
      MessageAttribute matt = iter.next();
      if (matt.name.equals(HDF5_CLASS)) {
        Attribute att = makeAttribute(matt);
        if (att == null)
          throw new IllegalStateException();
        String val = att.getStringValue();
        if (val.equals(HDF5_DIMENSION_SCALE) && facade.dobj.mds.ndims > 0) {

          // create a dimension - always use the first dataspace length
          facade.dimList =
              addDimension(g, h5group, facade.name, facade.dobj.mds.dimLength[0], facade.dobj.mds.maxLength[0] == -1);
          facade.hasNetcdfDimensions = true;
          if (!h5iosp.includeOriginalAttributes)
            iter.remove();

          if (facade.dobj.mds.ndims > 1)
            facade.is2DCoordinate = true;
        }
      }
    }
  }

  private void findDimensionScales2D(H5Group h5group, DataObjectFacade facade) {
    int[] lens = facade.dobj.mds.dimLength;
    if (lens.length > 2) {
      log.warn("DIMENSION_LIST: dimension scale > 2 = {}", facade.getName());
      return;
    }

    // first dimension is itself
    String name = facade.getName();
    int pos = name.lastIndexOf('/');
    String dimName = (pos >= 0) ? name.substring(pos + 1) : name;

    StringBuilder sbuff = new StringBuilder();
    sbuff.append(dimName);
    sbuff.append(" ");

    // second dimension is really an anonymous dimension, ironically now we go through amazing hoops to keep it shared
    // 1. use dimids if they exist
    // 2. if length matches and unique, use it
    // 3. if no length matches or multiple matches, then use anonymous

    int want_len = lens[1]; // second dimension
    Dimension match = null;
    boolean unique = true;
    for (Dimension d : h5group.dimList) {
      if (d.getLength() == want_len) {
        if (match == null)
          match = d;
        else
          unique = false;
      }
    }
    if (match != null && unique) {
      sbuff.append(match.getShortName()); // 2. if length matches and unique, use it

    } else {
      if (match == null) { // 3. if no length matches or multiple matches, then use anonymous
        log.warn("DIMENSION_LIST: dimension scale {} has second dimension {} but no match", facade.getName(), want_len);
        sbuff.append(want_len);
      } else {
        log.warn("DIMENSION_LIST: dimension scale {} has second dimension {} but multiple matches", facade.getName(),
            want_len);
        sbuff.append(want_len);
      }
    }

    facade.dimList = sbuff.toString();
  }

  /*
   * private void findNetcdf4DimidAttribute(DataObjectFacade facade) throws IOException {
   * for (MessageAttribute matt : facade.dobj.attributes) {
   * if (matt.name.equals(Nc4.NETCDF4_DIMID)) {
   * if (dimIds == null) dimIds = new HashMap<Integer, DataObjectFacade>();
   * Attribute att_dimid = makeAttribute(matt);
   * Integer dimid = (Integer) att_dimid.getNumericValue();
   * dimIds.put(dimid, facade);
   * return;
   * }
   * }
   * if (dimIds != null) // supposed to all have them
   * log.warn("Missing "+Nc4.NETCDF4_DIMID+" attribute on "+facade.getName());
   * }
   */


  /*
   * the case of multidimensional dimension scale. We need to identify which index to use as the dimension length.
   * the pattern is, eg:
   * _Netcdf4Coordinates = 6, 4
   * _Netcdf4Dimid = 6
   *
   * private int findCoordinateDimensionIndex(DataObjectFacade facade, H5Group h5group) throws IOException {
   * Attribute att_coord = null;
   * Attribute att_dimid = null;
   * for (MessageAttribute matt : facade.dobj.attributes) {
   * if (matt.name.equals(Nc4.NETCDF4_COORDINATES))
   * att_coord = makeAttribute(matt);
   * if (matt.name.equals(Nc4.NETCDF4_DIMID))
   * att_dimid = makeAttribute(matt);
   * }
   * if (att_coord != null && att_dimid != null) {
   * facade.netcdf4CoordinatesAtt = att_coord;
   * Integer want = (Integer) att_dimid.getNumericValue();
   * for (int i=0; i<att_coord.getLength(); i++) {
   * Integer got = (Integer) att_dimid.getNumericValue(i);
   * if (want.equals(got))
   * return i;
   * }
   * log.warn("Multidimension dimension scale attributes "+Nc4.NETCDF4_COORDINATES+" and "+Nc4.
   * NETCDF4_DIMID+" dont match. Assume Dimension is index 0 (!)");
   * return 0;
   * }
   * if (att_coord != null) {
   * facade.netcdf4CoordinatesAtt = att_coord;
   * int n = h5group.dimList.size(); // how many dimensions are already defined
   * facade.dimList = "%REDO%"; // token to create list when all dimensions found
   * for (int i=0 ;i<att_coord.getLength(); i++) {
   * if (att_coord.getNumericValue(i).intValue() == n) return i;
   * }
   * log.warn("Multidimension dimension scale attribute "+Nc4.
   * NETCDF4_DIMID+" missing. Dimension ordering is not found. Assume index 0 (!)");
   * return 0;
   * }
   *
   * log.warn("Multidimension dimension scale doesnt have "+Nc4.
   * NETCDF4_COORDINATES+" attribute. Assume Dimension is index 0 (!)");
   * return 0;
   * }
   */

  // look for references to dimension scales, ie the variables that use them
  // return true if this variable is compatible with netcdf4 data model
  private boolean findSharedDimensions(Group.Builder g, H5Group h5group, DataObjectFacade facade) throws IOException {
    Iterator<MessageAttribute> iter = facade.dobj.attributes.iterator();
    while (iter.hasNext()) {
      MessageAttribute matt = iter.next();
      // find the dimensions - set length to maximum
      // DIMENSION_LIST contains, for each dimension, a list of references to Dimension Scales
      switch (matt.name) {
        case HDF5_DIMENSION_LIST: { // references : may extend the dimension length
          Attribute att = makeAttribute(matt); // this reads in the data

          if (att == null) {
            log.warn("DIMENSION_LIST: failed to read on variable {}", facade.getName());

          } else if (att.getLength() != facade.dobj.mds.dimLength.length) { // some attempts to writing hdf5 directly
                                                                            // fail here
            log.warn("DIMENSION_LIST: must have same number of dimension scales as dimensions att={} on variable {}",
                att, facade.getName());

          } else {
            StringBuilder sbuff = new StringBuilder();
            for (int i = 0; i < att.getLength(); i++) {
              String name = att.getStringValue(i);
              String dimName = extendDimension(g, h5group, name, facade.dobj.mds.dimLength[i]);
              sbuff.append(dimName).append(" ");
            }
            facade.dimList = sbuff.toString();
            facade.hasNetcdfDimensions = true;
            if (debugDimensionScales) {
              log.debug("Found dimList '{}' for group '{}' matt={}", facade.dimList, g.shortName, matt);
            }
            if (!h5iosp.includeOriginalAttributes)
              iter.remove();
          }

          break;
        }
        case HDF5_DIMENSION_NAME: {
          Attribute att = makeAttribute(matt);
          if (att == null)
            throw new IllegalStateException();
          String val = att.getStringValue();
          if (val.startsWith("This is a netCDF dimension but not a netCDF variable")) {
            facade.isVariable = false;
            isNetcdf4 = true;
          }
          if (!h5iosp.includeOriginalAttributes)
            iter.remove();
          if (debugDimensionScales) {
            log.debug("Found {}", val);
          }

          break;
        }
        case HDF5_REFERENCE_LIST:
          if (!h5iosp.includeOriginalAttributes)
            iter.remove();
          break;
      }
    }
    return facade.hasNetcdfDimensions || facade.dobj.mds.dimLength.length == 0;

  }

  // add a dimension, return its name
  private String addDimension(Group.Builder parent, H5Group h5group, String name, int length, boolean isUnlimited) {
    int pos = name.lastIndexOf('/');
    String dimName = (pos >= 0) ? name.substring(pos + 1) : name;

    Dimension d = h5group.dimMap.get(dimName); // first look in current group
    if (d == null) { // create if not found
      d = Dimension.builder().setName(name).setIsUnlimited(isUnlimited).setLength(length).build();
      h5group.dimMap.put(dimName, d);
      h5group.dimList.add(d);
      parent.addDimension(d);
      if (debugDimensionScales) {
        log.debug("addDimension name=" + name + " dim= " + d + " to group " + parent.shortName);
      }

    } else { // check has correct length
      if (d.getLength() != length)
        throw new IllegalStateException(
            "addDimension: DimScale has different length than dimension it references dimScale=" + dimName);
    }

    return d.getShortName();
  }

  // look for unlimited dimensions without dimension scale - must get length from the variable
  private String extendDimension(Group.Builder parent, H5Group h5group, String name, int length) {
    int pos = name.lastIndexOf('/');
    String dimName = (pos >= 0) ? name.substring(pos + 1) : name;

    Dimension d = h5group.dimMap.get(dimName); // first look in current group
    if (d == null) {
      d = parent.findDimension(dimName).orElse(null); // then look in parent groups
    }

    if (d != null) {
      if (d.isUnlimited() && (length > d.getLength())) {
        parent.replaceDimension(d.toBuilder().setLength(length).build());
      }

      if (!d.isUnlimited() && (length != d.getLength())) {
        throw new IllegalStateException(
            "extendDimension: DimScale has different length than dimension it references dimScale=" + dimName);
      }
      return d.getShortName();
    }

    return dimName;
  }

  private void createDimensions(Group.Builder g, H5Group h5group) {
    for (Dimension d : h5group.dimList) {
      g.addDimensionIfNotExists(d);
    }
  }

  private List<MessageAttribute> filterAttributes(List<MessageAttribute> attList) {
    List<MessageAttribute> result = new ArrayList<>(attList.size());
    for (MessageAttribute matt : attList) {
      if (matt.name.equals(NetcdfFormatUtils.NETCDF4_COORDINATES) || matt.name.equals(NetcdfFormatUtils.NETCDF4_DIMID)
          || matt.name.equals(NetcdfFormatUtils.NETCDF4_STRICT)) {
        isNetcdf4 = true;
      } else {
        result.add(matt);
      }
    }
    return result;
  }

  /**
   * Create Attribute objects from the MessageAttribute and add to list
   *
   * @param sb if attribute for a Structure, then deconstruct and add to member variables
   * @param matt attribute message
   * @param attContainer add Attribute to this
   * @throws IOException on io error
   * @throws InvalidRangeException on shape error
   */
  private void makeAttributes(Structure.Builder<?> sb, MessageAttribute matt, AttributeContainerMutable attContainer)
      throws IOException, InvalidRangeException {
    MessageDatatype mdt = matt.mdt;

    if (mdt.type == 6) { // structure
      Vinfo vinfo = new Vinfo(matt.mdt, matt.mds, matt.dataPos);
      StructureDataArray attData = readAttributeStructureData(matt, vinfo);

      if (null == sb) {
        // flatten and add to list
        for (StructureMembers.Member sm : attData.getStructureMembers().getMembers()) {
          Array<?> memberData = attData.extractMemberArray(sm);
          attContainer.addAttribute(Attribute.fromArray(matt.name + "." + sm.getName(), memberData));
        }

      } else if (matt.name.equals(CDM.FIELD_ATTS)) {
        // flatten and add to list
        for (StructureMembers.Member sm : attData.getStructureMembers().getMembers()) {
          String memberName = sm.getName();
          int pos = memberName.indexOf(":");
          if (pos < 0) {
            continue; // LOOK
          }
          String fldName = memberName.substring(0, pos);
          String attName = memberName.substring(pos + 1);
          Array<?> memberData = attData.extractMemberArray(sm);
          sb.findMemberVariable(fldName)
              .ifPresent(vb -> vb.getAttributeContainer().addAttribute(Attribute.fromArray(attName, memberData)));
        }

      } else { // assign separate attribute for each member
        StructureMembers attMembers = attData.getStructureMembers();
        for (Variable.Builder<?> v : sb.vbuilders) {
          // does the compound attribute have a member with same name as nested variable ?
          StructureMembers.Member sm = attMembers.findMember(v.shortName);
          if (null != sm) {
            // if so, add the att to the member variable, using the name of the compound attribute
            Array<?> memberData = attData.extractMemberArray(sm);
            v.addAttribute(Attribute.fromArray(matt.name, memberData)); // LOOK check for missing values
          }
        }

        // look for unassigned members, add to the list
        for (StructureMembers.Member sm : attData.getStructureMembers().getMembers()) {
          Variable.Builder<?> vb = sb.findMemberVariable(sm.getName()).orElse(null);
          if (vb == null) {
            Array<?> memberData = attData.extractMemberArray(sm);
            attContainer.addAttribute(Attribute.fromArray(matt.name + "." + sm.getName(), memberData));
          }
        }
      }

    } else {
      // make a single attribute
      Attribute att = makeAttribute(matt);
      if (att != null)
        attContainer.addAttribute(att);
    }

    // reading attribute values might change byte order during a read
    // put back to little endian for further header processing
    raf.order(RandomAccessFile.LITTLE_ENDIAN);
  }

  private Attribute makeAttribute(MessageAttribute matt) throws IOException {
    Vinfo vinfo = new Vinfo(matt.mdt, matt.mds, matt.dataPos);
    ArrayType dtype = vinfo.getNCArrayType();

    // check for empty attribute case
    if (matt.mds.type == 2) {
      if (dtype == ArrayType.CHAR) {
        // empty char considered to be a null string attr
        return Attribute.builder(matt.name).setArrayType(ArrayType.STRING).build();
      } else {
        return Attribute.builder(matt.name).setArrayType(dtype).build();
      }
    }

    Array<?> attData;
    try {
      attData = readAttributeData(matt, vinfo, dtype);
    } catch (InvalidRangeException e) {
      log.warn("failed to read Attribute " + matt.name + " HDF5 file=" + raf.getLocation());
      return null;
    }

    Attribute result;
    if (attData.isVlen()) {
      List<Object> dataList = new ArrayList<>();
      for (Object val : attData) {
        Array<?> nestedArray = (Array<?>) val;
        for (Object nested : nestedArray) {
          dataList.add(nested);
        }
      }
      // LOOK probably wrong? flattening them out ??
      result = Attribute.builder(matt.name).setValues(dataList, matt.mdt.unsigned).build();
    } else {
      result = Attribute.fromArray(matt.name, attData);
    }

    raf.order(RandomAccessFile.LITTLE_ENDIAN);
    return result;
  }

  // read non-Structure attribute values without creating a Variable
  private Array<?> readAttributeData(MessageAttribute matt, H5header.Vinfo vinfo, ArrayType dataType)
      throws IOException, InvalidRangeException {
    int[] shape = matt.mds.dimLength;

    Layout layout2 = new LayoutRegular(matt.dataPos, matt.mdt.byteSize, shape, new Section(shape));

    // Strings
    if ((vinfo.typeInfo.hdfType == 9) && (vinfo.typeInfo.isVString)) {
      int size = (int) layout2.getTotalNelems();
      String[] sarray = new String[size];
      int count = 0;
      while (layout2.hasNext()) {
        Layout.Chunk chunk = layout2.next();
        if (chunk == null) {
          continue;
        }
        for (int i = 0; i < chunk.getNelems(); i++) {
          long address = chunk.getSrcPos() + layout2.getElemSize() * i;
          String sval = readHeapString(address);
          sarray[count++] = sval;
        }
      }
      return Arrays.factory(ArrayType.STRING, new int[] {size}, sarray);
    } // vlen Strings case

    // Vlen (non-String) LOOK Attribute vlen?
    if (vinfo.typeInfo.hdfType == 9) {
      ByteOrder endian = vinfo.typeInfo.endian;
      ArrayType readType = dataType;
      if (vinfo.typeInfo.base.hdfType == 7) { // reference
        readType = ArrayType.LONG;
        endian = ByteOrder.LITTLE_ENDIAN; // apparently always LE
      }

      // variable length array of references, get translated into strings
      if (vinfo.typeInfo.base.hdfType == 7) {
        List<String> refsList = new ArrayList<>();
        while (layout2.hasNext()) {
          Layout.Chunk chunk = layout2.next();
          if (chunk == null) {
            continue;
          }
          for (int i = 0; i < chunk.getNelems(); i++) {
            long address = chunk.getSrcPos() + layout2.getElemSize() * i;
            Array<?> vlenArray = getHeapDataArray(address, readType, endian);
            ucar.array.Array<String> refsArray = h5iosp.convertReferenceArray((Array<Long>) vlenArray);
            for (String s : refsArray) {
              refsList.add(s);
            }
          }
        }
        return Arrays.factory(ArrayType.STRING, new int[] {refsList.size()}, refsList.toArray(new String[0]));
      }

      // general case is to read an array of vlen objects
      // each vlen generates an Array - so return ArrayObject of Array
      int size = (int) layout2.getTotalNelems();
      StorageMutable<Array<String>> vlenStorage = ArrayVlen.createStorage(readType, size, null);
      int count = 0;
      while (layout2.hasNext()) {
        Layout.Chunk chunk = layout2.next();
        if (chunk == null) {
          continue;
        }
        for (int i = 0; i < chunk.getNelems(); i++) {
          long address = chunk.getSrcPos() + layout2.getElemSize() * i;
          Array<?> vlenArray = getHeapDataArray(address, readType, endian);
          if (vinfo.typeInfo.base.hdfType == 7) {
            vlenStorage.setPrimitiveArray(count, h5iosp.convertReferenceArray((Array<Long>) vlenArray));
          } else {
            vlenStorage.setPrimitiveArray(count, vlenArray);
          }
          count++;
        }
      }
      return ArrayVlen.createFromStorage(readType, shape, vlenStorage);

    } // vlen case

    // NON-STRUCTURE CASE
    ArrayType readDtype = dataType;
    int elemSize = dataType.getSize();
    ByteOrder endian = vinfo.typeInfo.endian;

    if (vinfo.typeInfo.hdfType == 2) { // time
      readDtype = vinfo.mdt.timeType;
      elemSize = readDtype.getSize();

    } else if (vinfo.typeInfo.hdfType == 3) { // char
      if (vinfo.mdt.byteSize > 1) {
        int[] newShape = new int[shape.length + 1];
        System.arraycopy(shape, 0, newShape, 0, shape.length);
        newShape[shape.length] = vinfo.mdt.byteSize;
        shape = newShape;
      }

    } else if (vinfo.typeInfo.hdfType == 5) { // opaque
      elemSize = vinfo.mdt.byteSize;

    } else if (vinfo.typeInfo.hdfType == 8) { // enum
      Hdf5Type baseInfo = vinfo.typeInfo.base;
      readDtype = baseInfo.dataType;
      elemSize = readDtype.getSize();
      endian = baseInfo.endian;
    }

    Layout layout = new LayoutRegular(matt.dataPos, elemSize, shape, new Section(shape));

    // LOOK The problem is that we dont have the Variable, needed to read Structure Data.
    // So an attribute cant be a Structure ??
    Object pdata = h5iosp.readArrayOrPrimitive(vinfo, null, layout, dataType, shape, null, endian, false);
    Array<?> dataArray;

    if (dataType == ArrayType.OPAQUE) {
      dataArray = (Array) pdata;

    } else if ((dataType == ArrayType.CHAR)) {
      if (vinfo.mdt.byteSize > 1) { // chop back into pieces
        byte[] bdata = (byte[]) pdata;
        int strlen = vinfo.mdt.byteSize;
        int n = bdata.length / strlen;
        String[] sarray = new String[n];
        for (int i = 0; i < n; i++) {
          String sval = convertString(bdata, i * strlen, strlen);
          sarray[i] = sval;
        }
        dataArray = Arrays.factory(ArrayType.STRING, new int[] {n}, sarray);

      } else {
        String sval = convertString((byte[]) pdata);
        dataArray = Arrays.factory(ArrayType.STRING, new int[] {1}, new String[] {sval});
      }

    } else {
      dataArray = (pdata instanceof Array) ? (Array<?>) pdata : Arrays.factory(readDtype, shape, pdata);
    }

    // convert attributes to enum strings
    if ((vinfo.typeInfo.hdfType == 8) && (matt.mdt.map != null)) {
      dataArray = convertEnums(matt.mdt.map, dataType, (Array<Number>) dataArray);
    }

    return dataArray;
  }

  // read attribute values without creating a Variable
  private StructureDataArray readAttributeStructureData(MessageAttribute matt, H5header.Vinfo vinfo)
      throws IOException, InvalidRangeException {

    int[] shape = matt.mds.dimLength;
    boolean hasStrings = false;

    StructureMembers.Builder builder = StructureMembers.builder().setName(matt.name);
    for (StructureMember h5sm : matt.mdt.members) {
      ArrayType dt;
      int[] dim;
      switch (h5sm.mdt.type) {
        case 9: // STRING
          dt = ArrayType.STRING;
          dim = new int[] {1};
          break;
        case 10: // ARRAY
          dt = new Hdf5Type(h5sm.mdt).dataType;
          dim = h5sm.mdt.dim;
          break;
        default: // PRIMITIVE
          dt = new Hdf5Type(h5sm.mdt).dataType;
          dim = new int[] {1};
          break;
      }
      StructureMembers.MemberBuilder mb = builder.addMember(h5sm.name, null, null, dt, dim);

      if (h5sm.mdt.endian != null) { // apparently each member may have separate byte order (!!!??)
        mb.setByteOrder(h5sm.mdt.endian);
      }
      mb.setOffset(h5sm.offset); // offset since start of Structure
      if (dt == ArrayType.STRING) {
        hasStrings = true;
      }
    }

    int recsize = matt.mdt.byteSize;
    Layout layout = new LayoutRegular(matt.dataPos, recsize, shape, new Section(shape));
    builder.setStructureSize(recsize);
    StructureMembers members = builder.build();
    Preconditions.checkArgument(members.getStorageSizeBytes() == recsize);

    // copy data into an byte[] for efficiency
    int nrows = (int) Arrays.computeSize(shape);
    byte[] result = new byte[(int) (nrows * members.getStorageSizeBytes())];
    while (layout.hasNext()) {
      Layout.Chunk chunk = layout.next();
      if (chunk == null) {
        continue;
      }
      if (debugStructure) {
        log.debug(" readStructure " + matt.name + " chunk= " + chunk + " index.getElemSize= " + layout.getElemSize());
      }

      // LOOK copy bytes directly into the underlying byte[], not a great idea, efficiency not needed
      raf.seek(chunk.getSrcPos());
      raf.readFully(result, (int) chunk.getDestElem() * recsize, chunk.getNelems() * recsize);
    }

    ByteBuffer bb = ByteBuffer.wrap(result);
    StructureDataStorageBB storage = new ucar.array.StructureDataStorageBB(members, bb, nrows);

    // strings are stored on the heap, and must be read separately
    if (hasStrings) {
      int destPos = 0;
      for (int i = 0; i < layout.getTotalNelems(); i++) { // loop over each structure
        h5iosp.convertHeapArray(bb, storage, destPos, members);
        destPos += layout.getElemSize();
      }
    }
    return new ucar.array.StructureDataArray(members, shape, storage);
  } // readAttributeStructureData

  private String convertString(byte[] b) {
    // null terminates
    int count = 0;
    while (count < b.length) {
      if (b[count] == 0)
        break;
      count++;
    }
    return new String(b, 0, count, valueCharset); // all strings are considered to be UTF-8 unicode
  }

  private String convertString(byte[] b, int start, int len) {
    // null terminates
    int count = start;
    while (count < start + len) {
      if (b[count] == 0)
        break;
      count++;
    }
    return new String(b, start, count - start, valueCharset); // all strings are considered to be UTF-8
    // unicode
  }

  protected Array<String> convertEnums(Map<Integer, String> map, ArrayType dataType, Array<Number> values) {
    int size = (int) Arrays.computeSize(values.getShape());
    String[] sarray = new String[size];
    int count = 0;
    for (Number val : values) {
      int ival = val.intValue();
      String sval = map.get(ival);
      if (sval == null) {
        sval = "Unknown enum value=" + ival;
      }
      sarray[count++] = sval;
    }
    return Arrays.factory(ArrayType.STRING, values.getShape(), sarray);
  }

  private Variable.Builder<?> makeVariable(Group.Builder parentGroup, DataObjectFacade facade) throws IOException {

    Vinfo vinfo = new Vinfo(facade);
    if (vinfo.getNCArrayType() == null) {
      log.debug("SKIPPING ArrayType= " + vinfo.typeInfo.hdfType + " for variable " + facade.name);
      return null;
    }

    // deal with filters, cant do SZIP
    if (facade.dobj.mfp != null) {
      for (Filter f : facade.dobj.mfp.filters) {
        if (f.id == 4) {
          log.debug("SKIPPING variable with SZIP Filter= " + facade.dobj.mfp + " for variable " + facade.name);
          return null;
        }
      }
    }

    Attribute fillAttribute = null;
    for (HeaderMessage mess : facade.dobj.messages) {
      if (mess.mtype == MessageType.FillValue) {
        MessageFillValue fvm = (MessageFillValue) mess.messData;
        if (fvm.hasFillValue)
          vinfo.fillValue = fvm.value;
      } else if (mess.mtype == MessageType.FillValueOld) {
        MessageFillValueOld fvm = (MessageFillValueOld) mess.messData;
        if (fvm.size > 0)
          vinfo.fillValue = fvm.value;
      }

      Object fillValue = vinfo.getFillValueNonDefault();
      if (fillValue != null) {
        Object defFillValue = NetcdfFormatUtils.getFillValueDefault(vinfo.typeInfo.dataType);
        if (!fillValue.equals(defFillValue))
          fillAttribute =
              Attribute.builder(CDM.FILL_VALUE).setNumericValue((Number) fillValue, vinfo.typeInfo.unsigned).build();
      }
    }

    long dataAddress = facade.dobj.msl.dataAddress;

    // deal with unallocated data
    if (dataAddress == -1) {
      vinfo.useFillValue = true;

      // if didnt find, use zeroes !!
      if (vinfo.fillValue == null) {
        vinfo.fillValue = new byte[vinfo.typeInfo.dataType.getSize()];
      }
    }

    Variable.Builder<?> vb;
    Structure.Builder<?> sb = null;
    if (facade.dobj.mdt.type == 6) { // Compound
      String vname = facade.name;
      vb = sb = Structure.builder().setName(vname);
      vb.setParentGroupBuilder(parentGroup);
      if (!makeVariableShapeAndType(parentGroup, sb, facade.dobj.mdt, facade.dobj.mds, vinfo, facade.dimList))
        return null;
      addMembersToStructure(parentGroup, sb, facade.dobj.mdt);
      sb.setElementSize(facade.dobj.mdt.byteSize);

    } else {
      String vname = facade.name;
      if (vname.startsWith(NetcdfFormatUtils.NETCDF4_NON_COORD))
        vname = vname.substring(NetcdfFormatUtils.NETCDF4_NON_COORD.length()); // skip prefix
      vb = Variable.builder().setName(vname);
      vb.setParentGroupBuilder(parentGroup);
      if (!makeVariableShapeAndType(parentGroup, vb, facade.dobj.mdt, facade.dobj.mds, vinfo, facade.dimList))
        return null;

      // special case of variable length strings
      if (vb.dataType == ArrayType.STRING)
        vb.setElementSize(16); // because the array has elements that are HeapIdentifier
      else if (vb.dataType == ArrayType.OPAQUE) // special case of opaque
        vb.setElementSize(facade.dobj.mdt.getBaseSize());
    }

    vb.setSPobject(vinfo);

    // look for attributes
    List<MessageAttribute> fatts = filterAttributes(facade.dobj.attributes);
    for (MessageAttribute matt : fatts) {
      try {
        makeAttributes(sb, matt, vb.getAttributeContainer());
      } catch (InvalidRangeException e) {
        throw new IOException(e.getMessage());
      }
    }

    AttributeContainerMutable atts = vb.getAttributeContainer();
    processSystemAttributes(facade.dobj.messages, atts);
    if (fillAttribute != null && atts.findAttribute(CDM.FILL_VALUE) == null)
      vb.addAttribute(fillAttribute);
    // if (vinfo.typeInfo.unsigned)
    // v.addAttribute(new Attribute(CDM.UNSIGNED, "true"));
    if (facade.dobj.mdt.type == 5) {
      String desc = facade.dobj.mdt.opaque_desc;
      if ((desc != null) && (!desc.isEmpty()))
        vb.addAttribute(new Attribute("_opaqueDesc", desc));
    }

    int[] shape = makeVariableShape(facade.dobj.mdt, facade.dobj.mds, facade.dimList);
    if (vinfo.isChunked) { // make the data btree, but entries are not read in
      vinfo.btree = new DataBTree(this, dataAddress, shape, vinfo.storageSize, memTracker);

      if (vinfo.isChunked) { // add an attribute describing the chunk size
        List<Integer> chunksize = new ArrayList<>();
        for (int i = 0; i < vinfo.storageSize.length - 1; i++) // skip last one - its the element size
          chunksize.add(vinfo.storageSize[i]);
        vb.addAttribute(Attribute.builder(CDM.CHUNK_SIZES).setValues(chunksize, true).build());
      }
    }

    if (transformReference && (facade.dobj.mdt.type == 7) && (facade.dobj.mdt.referenceType == 0)) { // object reference
      // System.out.printf("new transform object Reference: facade= %s variable name=%s%n", facade.name, vb.shortName);
      vb.setArrayType(ArrayType.STRING);
      Array rawData = vinfo.readArray();
      Array refData = findReferenceObjectNames(rawData);
      vb.setSourceData(refData); // so H5iosp.read() is never called
      vb.addAttribute(new Attribute("_HDF5ReferenceType", "values are names of referenced Variables"));
    }

    if (transformReference && (facade.dobj.mdt.type == 7) && (facade.dobj.mdt.referenceType == 1)) { // region reference
      if (warnings)
        log.warn("transform region Reference: facade=" + facade.name + " variable name=" + vb.shortName);

      /*
       * TODO doesnt work yet
       * int nelems = (int) vb.getSize();
       * int heapIdSize = 12;
       * for (int i = 0; i < nelems; i++) {
       * H5header.RegionReference heapId = new RegionReference(vinfo.dataPos + heapIdSize * i);
       * }
       */

      // fake data for now
      vb.setArrayType(ArrayType.LONG);
      Array<?> newData = Arrays.factory(ArrayType.LONG, shape, new long[(int) Arrays.computeSize(shape)]);
      vb.setSourceData(newData); // so H5iosp.read() is never called
      vb.addAttribute(new Attribute("_HDF5ReferenceType", "values are regions of referenced Variables"));
    }

    // debugging
    vinfo.setOwner(vb);
    if ((vinfo.mfp != null) && warnings) {
      for (Filter f : vinfo.mfp.getFilters()) {
        if (f.id > KNOWN_FILTERS) {
          log.warn("  Variable " + facade.name + " has unknown Filter(s) = " + vinfo.mfp);
          break;
        }
      }
    }
    if (debug1) {
      log.debug("makeVariable " + vb.shortName + "; vinfo= " + vinfo);
    }

    return vb;
  }

  // convert an array of longs which are data object references to an array of strings,
  // the names of the data objects (dobj.who)
  private Array<?> findReferenceObjectNames(Array<Long> data) throws IOException {
    int size = (int) Arrays.computeSize(data.getShape());
    String[] sarray = new String[size];
    int count = 0;
    for (long val : data) {
      DataObject dobj = getDataObject(val, null);
      if (dobj == null) {
        log.warn("readReferenceObjectNames cant find obj= {}", val);
      } else {
        if (debugReference) {
          log.debug(" Referenced object= {}", dobj.who);
        }
        sarray[count] = dobj.who;
      }
      count++;
    }
    return Arrays.factory(ArrayType.STRING, data.getShape(), sarray);
  }

  private void addMembersToStructure(Group.Builder parent, Structure.Builder<?> s, MessageDatatype mdt)
      throws IOException {
    for (StructureMember m : mdt.members) {
      Variable.Builder<?> v = makeVariableMember(parent, m.name, m.offset, m.mdt);
      if (v != null) {
        s.addMemberVariable(v);
        if (debug1) {
          log.debug("  made Member Variable " + v.shortName + "\n" + v);
        }
      }
    }
  }

  // Used for Structure Members
  private Variable.Builder<?> makeVariableMember(Group.Builder parentGroup, String name, long dataPos,
      MessageDatatype mdt) throws IOException {

    Vinfo vinfo = new Vinfo(mdt, null, dataPos); // LOOK need mds
    if (vinfo.getNCArrayType() == null) {
      log.debug("SKIPPING ArrayType= " + vinfo.typeInfo.hdfType + " for variable " + name);
      return null;
    }

    if (mdt.type == 6) {
      Structure.Builder<?> sb = Structure.builder().setName(name).setParentGroupBuilder(parentGroup);
      makeVariableShapeAndType(parentGroup, sb, mdt, null, vinfo, null);
      addMembersToStructure(parentGroup, sb, mdt);
      sb.setElementSize(mdt.byteSize);

      sb.setSPobject(vinfo);
      vinfo.setOwner(sb);
      return sb;

    } else {
      Variable.Builder<?> vb = Variable.builder().setName(name).setParentGroupBuilder(parentGroup);
      makeVariableShapeAndType(parentGroup, vb, mdt, null, vinfo, null);

      // special case of variable length strings
      if (vb.dataType == ArrayType.STRING)
        vb.setElementSize(16); // because the array has elements that are HeapIdentifier
      else if (vb.dataType == ArrayType.OPAQUE) // special case of opaque
        vb.setElementSize(mdt.getBaseSize());

      vb.setSPobject(vinfo);
      vinfo.setOwner(vb);
      return vb;
    }
  }

  private void processSystemAttributes(List<HeaderMessage> messages, AttributeContainerMutable attContainer) {
    for (HeaderMessage mess : messages) {
      if (mess.mtype == MessageType.Comment) {
        MessageComment m = (MessageComment) mess.messData;
        attContainer.addAttribute(new Attribute("_comment", m.comment));
      }
    }
  }

  private java.text.SimpleDateFormat getHdfDateFormatter() {
    if (hdfDateParser == null) {
      hdfDateParser = new java.text.SimpleDateFormat("yyyyMMddHHmmss");
      hdfDateParser.setTimeZone(TimeZone.getTimeZone("GMT")); // same as UTC
    }
    return hdfDateParser;
  }

  // get the shape of the Variable
  private int[] makeVariableShape(MessageDatatype mdt, MessageDataspace msd, String dimNames) {
    int[] shape = (msd != null) ? msd.dimLength : new int[0];
    if (shape == null) {
      shape = new int[0]; // scaler
    }

    // merge the shape for array type (10)
    if (mdt.type == 10) {
      int len = shape.length + mdt.dim.length;
      if (mdt.isVlen()) {
        len++;
      }
      int[] combinedDim = new int[len];
      System.arraycopy(shape, 0, combinedDim, 0, shape.length);
      System.arraycopy(mdt.dim, 0, combinedDim, shape.length, mdt.dim.length); // // type 10 is the inner dimensions
      if (mdt.isVlen()) {
        combinedDim[len - 1] = -1;
      }
      shape = combinedDim;
    }

    // dimension names were not passed in
    if (dimNames == null) {
      if (mdt.type == 3) { // fixed length string - ArrayType.CHAR, add string length
        if (mdt.byteSize != 1) { // scalar string member variable
          int[] rshape = new int[shape.length + 1];
          System.arraycopy(shape, 0, rshape, 0, shape.length);
          rshape[shape.length] = mdt.byteSize;
          return rshape;
        }
      } else if (mdt.isVlen()) { // variable length (not a string)
        if ((shape.length == 1) && (shape[0] == 1)) { // replace scalar with vlen
          return new int[] {-1};

        } else if (mdt.type != 10) { // add vlen dimension already done above for array
          int[] rshape = new int[shape.length + 1];
          System.arraycopy(shape, 0, rshape, 0, shape.length);
          rshape[shape.length] = -1;
          return rshape;
        }
      }
    }
    return shape;
  }

  // set the type and shape of the Variable
  private boolean makeVariableShapeAndType(Group.Builder parent, Variable.Builder<?> v, MessageDatatype mdt,
      MessageDataspace msd, Vinfo vinfo, String dimNames) {

    int[] shape = makeVariableShape(mdt, msd, dimNames);

    // set dimensions on the variable
    if (dimNames != null) { // dimensions were passed in
      if ((mdt.type == 9) && !mdt.isVString)
        v.setDimensionsByName(dimNames + " *");
      else
        v.setDimensionsByName(dimNames);
    } else {
      v.setDimensionsAnonymous(shape);
    }

    // set the type
    ArrayType dt = vinfo.getNCArrayType();
    if (dt == null)
      return false;
    v.setArrayType(dt);

    // set the enumTypedef
    if (dt.isEnum()) {
      // TODO Not sure why, but there may be both a user type and a "local" mdt enum. May need to do a value match?
      EnumTypedef enumTypedef = parent.findEnumeration(mdt.enumTypeName).orElse(null);
      if (enumTypedef == null) { // if shared object, wont have a name, shared version gets added later
        EnumTypedef local = new EnumTypedef(mdt.enumTypeName, mdt.map);
        enumTypedef = parent.enumTypedefs.stream().filter((e) -> e.equalsMapOnly(local)).findFirst().orElse(local);
        parent.addEnumTypedef(enumTypedef);
      }
      v.setEnumTypeName(enumTypedef.getShortName());
    }

    return true;
  }

  @Override
  public Builder getRootGroup() {
    return root;
  }

  @Override
  public void makeVinfoForDimensionMapVariable(Builder parent, Variable.Builder<?> v) {
    // this is a self contained variable, doesnt need any extra info, just make a dummy.
    Vinfo vinfo = new Vinfo();
    vinfo.owner = v;
  }

  @Override
  public String readStructMetadata(Variable.Builder<?> structMetadataVar) throws IOException {
    Vinfo vinfo = (Vinfo) structMetadataVar.spiObject;
    return vinfo.readString();
  }

  // Holder of all H5 specific information for a Variable, needed to do IO.
  public class Vinfo {
    Variable.Builder<?> owner; // debugging
    DataObjectFacade facade; // debugging

    long dataPos; // for regular variables, needs to be absolute, with baseAddress added if needed
    // for member variables, is the offset from start of structure

    Hdf5Type typeInfo;
    int[] storageSize; // for type 1 (continuous) : mds.dimLength;
    // for type 2 (chunked) : msl.chunkSize (last number is element size)
    // null for attributes

    boolean isvlen; // VLEN, but not vlenstring

    // chunked stuff
    boolean isChunked;
    DataBTree btree; // only if isChunked

    MessageDatatype mdt;
    MessageDataspace mds;
    MessageFilter mfp;

    boolean useFillValue;
    byte[] fillValue;

    public String getCompression() {
      if (mfp == null)
        return null;
      Formatter f = new Formatter();
      for (Filter filt : mfp.filters) {
        f.format("%s ", filt.name);
      }
      return f.toString();
    }

    public int[] getChunking() {
      return storageSize;
    }

    public boolean isChunked() {
      return isChunked;
    }

    public boolean useFillValue() {
      return useFillValue;
    }

    public long[] countStorageSize(Formatter f) throws IOException {
      long[] result = new long[2];
      if (btree == null) {
        if (f != null)
          f.format("btree is null%n");
        return result;
      }
      if (useFillValue) {
        if (f != null)
          f.format("useFillValue - no data is stored%n");
        return result;
      }

      int count = 0;
      long total = 0;
      DataBTree.DataChunkIterator iter = btree.getDataChunkIteratorFilter(null);
      while (iter.hasNext()) {
        DataBTree.DataChunk dc = iter.next();
        if (f != null)
          f.format(" %s%n", dc);
        total += dc.size;
        count++;
      }

      result[0] = total;
      result[1] = count;
      return result;
    }

    Vinfo() {
      // nuthing
    }

    /**
     * Constructor
     *
     * @param facade DataObjectFacade: always has an mdt and an msl
     */
    Vinfo(DataObjectFacade facade) {
      this.facade = facade;
      // LOOK if compact, do not use fileOffset
      this.dataPos =
          (facade.dobj.msl.type == 0) ? facade.dobj.msl.dataAddress : getFileOffset(facade.dobj.msl.dataAddress);
      this.mdt = facade.dobj.mdt;
      this.mds = facade.dobj.mds;
      this.mfp = facade.dobj.mfp;

      isvlen = this.mdt.isVlen();
      if (!facade.dobj.mdt.isOK && warnings) {
        log.debug("WARNING HDF5 file " + raf.getLocation() + " not handling " + facade.dobj.mdt);
        return; // not a supported datatype
      }

      this.isChunked = (facade.dobj.msl.type == 2);
      if (isChunked) {
        this.storageSize = facade.dobj.msl.chunkSize;
      } else {
        this.storageSize = facade.dobj.mds.dimLength;
      }

      // figure out the data type
      this.typeInfo = new Hdf5Type(facade.dobj.mdt);
    }

    /**
     * Constructor, used for reading attributes
     *
     * @param mdt datatype
     * @param mds dataspace
     * @param dataPos start of data in file
     */
    Vinfo(MessageDatatype mdt, MessageDataspace mds, long dataPos) {
      this.mdt = mdt;
      this.mds = mds;
      this.dataPos = dataPos;

      if (!mdt.isOK && warnings) {
        log.debug("WARNING HDF5 file " + raf.getLocation() + " not handling " + mdt);
        return; // not a supported datatype
      }

      isvlen = this.mdt.isVlen();

      // figure out the data type
      this.typeInfo = new Hdf5Type(mdt);
    }

    void setOwner(Variable.Builder<?> owner) {
      this.owner = owner;
      if (btree != null) {
        btree.setOwner(owner);
      }
    }

    public String toString() {
      StringBuilder buff = new StringBuilder();
      buff.append("dataPos=").append(dataPos).append(" datatype=").append(typeInfo);
      if (isChunked) {
        buff.append(" isChunked (");
        for (int size : storageSize)
          buff.append(size).append(" ");
        buff.append(")");
      }
      if (mfp != null)
        buff.append(" hasFilter");
      buff.append("; // ").append(extraInfo());
      if (null != facade)
        buff.append("\n").append(facade);

      return buff.toString();
    }

    public String extraInfo() {
      StringBuilder buff = new StringBuilder();
      if ((typeInfo.dataType != ArrayType.CHAR) && (typeInfo.dataType != ArrayType.STRING))
        buff.append(typeInfo.unsigned ? " unsigned" : " signed");
      buff.append("ByteOrder= " + typeInfo.endian);
      if (useFillValue)
        buff.append(" useFillValue");
      return buff.toString();
    }

    ArrayType getNCArrayType() {
      return typeInfo.dataType;
    }

    /**
     * Get the Fill Value, return default if one was not set.
     *
     * @return wrapped primitive (Byte, Short, Integer, Double, Float, Long), or null if none
     */
    Object getFillValue() {
      return (fillValue == null) ? NetcdfFormatUtils.getFillValueDefault(typeInfo.dataType) : getFillValueNonDefault();
    }

    Object getFillValueNonDefault() {
      if (fillValue == null)
        return null;

      if ((typeInfo.dataType.getPrimitiveClass() == Byte.class) || (typeInfo.dataType == ArrayType.CHAR))
        return fillValue[0];

      ByteBuffer bbuff = ByteBuffer.wrap(fillValue);
      if (typeInfo.endian != null)
        bbuff.order(typeInfo.endian);

      if (typeInfo.dataType.getPrimitiveClass() == Short.class) {
        ShortBuffer tbuff = bbuff.asShortBuffer();
        return tbuff.get();

      } else if (typeInfo.dataType.getPrimitiveClass() == Integer.class) {
        IntBuffer tbuff = bbuff.asIntBuffer();
        return tbuff.get();

      } else if (typeInfo.dataType.getPrimitiveClass() == Long.class) {
        LongBuffer tbuff = bbuff.asLongBuffer();
        return tbuff.get();

      } else if (typeInfo.dataType == ArrayType.FLOAT) {
        FloatBuffer tbuff = bbuff.asFloatBuffer();
        return tbuff.get();

      } else if (typeInfo.dataType == ArrayType.DOUBLE) {
        DoubleBuffer tbuff = bbuff.asDoubleBuffer();
        return tbuff.get();
      }

      return null;
    }

    // limited reader; Variable is not built yet.
    Array<?> readArray() throws IOException {
      int[] shape = mds.dimLength;
      ArrayType dataType = typeInfo.dataType;
      Layout layout;
      try {
        if (isChunked) {
          layout = new H5tiledLayout(this, dataType, new Section(shape));
        } else {
          layout = new LayoutRegular(dataPos, dataType.getSize(), shape, null);
        }
      } catch (InvalidRangeException e2) {
        // cant happen because we use null for wantSection
        throw new IllegalStateException();
      }

      Object data = IospArrayHelper.readDataFill(raf, layout, dataType, getFillValue(), typeInfo.endian, false);
      return Arrays.factory(dataType, shape, data);
    }

    // limited reader; Variable is not built yet.
    String readString() throws IOException {
      int[] shape = new int[] {mdt.byteSize};
      ArrayType dataType = typeInfo.dataType;
      Layout layout;
      try {
        if (isChunked) {
          layout = new H5tiledLayout(this, dataType, new Section(shape));
        } else {
          layout = new LayoutRegular(dataPos, dataType.getSize(), shape, null);
        }
      } catch (InvalidRangeException e) {
        // cant happen because we use null for wantSection
        throw new IllegalStateException();
      }
      Object data = IospArrayHelper.readDataFill(raf, layout, dataType, getFillValue(), typeInfo.endian, true);

      // read and parse the ODL
      String result = "";
      if (data instanceof String) {
        // Sometimes StructMetadata.0 is stored as a string,
        // and IospHelper returns it directly as a string, so pass it along
        result = (String) data;
      } else {
        Array<?> dataArray = Arrays.factory(dataType, shape, data);
        // read and parse the ODL
        if (dataArray.getArrayType() == ArrayType.CHAR) {
          result = Arrays.makeStringFromChar((Array<Byte>) dataArray);
        } else {
          log.error("Unsupported array type {} for StructMetadata", dataArray.getArrayType());
        }
      }
      return result;
    }
  }


  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Fetch a Vlen data array.
   *
   * @param globalHeapIdAddress address of the heapId, used to get the String out of the heap
   * @param dataType type of data
   * @param endian byteOrder of the data (0 = BE, 1 = LE)
   * @return the Array read from the heap
   * @throws IOException on read error
   */
  Array<?> getHeapDataArray(long globalHeapIdAddress, ArrayType dataType, ByteOrder endian) throws IOException {
    HeapIdentifier heapId = h5objects.readHeapIdentifier(globalHeapIdAddress);
    if (debugHeap) {
      log.debug(" heapId= {}", heapId);
    }
    return getHeapDataArray(heapId, dataType, endian);
  }

  Array<?> getHeapDataArray(HeapIdentifier heapId, ArrayType dataType, ByteOrder endian) throws IOException {
    GlobalHeap.HeapObject ho = heapId.getHeapObject();
    if (ho == null) {
      throw new IllegalStateException("Illegal Heap address, HeapObject = " + heapId);
    }
    if (debugHeap) {
      log.debug(" HeapObject= {}", ho);
    }
    if (endian != null) {
      raf.order(endian);
    }

    if (ArrayType.FLOAT == dataType) {
      float[] pa = new float[heapId.nelems];
      raf.seek(ho.dataPos);
      raf.readFloat(pa, 0, pa.length);
      return Arrays.factory(dataType, new int[] {pa.length}, pa);

    } else if (ArrayType.DOUBLE == dataType) {
      double[] pa = new double[heapId.nelems];
      raf.seek(ho.dataPos);
      raf.readDouble(pa, 0, pa.length);
      return Arrays.factory(dataType, new int[] {pa.length}, pa);

    } else if (dataType.getPrimitiveClass() == Byte.class) {
      byte[] pa = new byte[heapId.nelems];
      raf.seek(ho.dataPos);
      raf.readFully(pa, 0, pa.length);
      return Arrays.factory(dataType, new int[] {pa.length}, pa);

    } else if (dataType.getPrimitiveClass() == Short.class) {
      short[] pa = new short[heapId.nelems];
      raf.seek(ho.dataPos);
      raf.readShort(pa, 0, pa.length);
      return Arrays.factory(dataType, new int[] {pa.length}, pa);

    } else if (dataType.getPrimitiveClass() == Integer.class) {
      int[] pa = new int[heapId.nelems];
      raf.seek(ho.dataPos);
      raf.readInt(pa, 0, pa.length);
      return Arrays.factory(dataType, new int[] {pa.length}, pa);

    } else if (dataType.getPrimitiveClass() == Long.class) {
      long[] pa = new long[heapId.nelems];
      raf.seek(ho.dataPos);
      raf.readLong(pa, 0, pa.length);
      return Arrays.factory(dataType, new int[] {pa.length}, pa);
    }

    throw new UnsupportedOperationException("getHeapDataAsArray dataType=" + dataType);
  }

  /**
   * Fetch a String from the heap.
   *
   * @param heapIdAddress address of the heapId, used to get the String out of the heap
   * @return String the String read from the heap
   * @throws IOException on read error
   */
  String readHeapString(long heapIdAddress) throws IOException {
    HeapIdentifier heapId = h5objects.readHeapIdentifier(heapIdAddress);
    if (heapId.isEmpty()) {
      return NULL_STRING_VALUE;
    }
    GlobalHeap.HeapObject ho = heapId.getHeapObject();
    if (ho == null)
      throw new IllegalStateException("Cant find Heap Object,heapId=" + heapId);
    if (ho.dataSize > 1000 * 1000)
      return String.format("Bad HeapObject.dataSize=%s", ho);
    raf.seek(ho.dataPos);
    return raf.readString((int) ho.dataSize, valueCharset);
  }

  /**
   * Fetch a String from the heap, when the heap identifier has already been put into a ByteBuffer at given pos
   *
   * @param bb heap id is here
   * @param pos at this position
   * @return String the String read from the heap
   * @throws IOException on read error
   */
  String readHeapString(ByteBuffer bb, int pos) throws IOException {
    HeapIdentifier heapId = h5objects.readHeapIdentifier(bb, pos);
    if (heapId.isEmpty()) {
      return NULL_STRING_VALUE;
    }
    GlobalHeap.HeapObject ho = heapId.getHeapObject();
    if (ho == null)
      throw new IllegalStateException("Cant find Heap Object,heapId=" + heapId);
    raf.seek(ho.dataPos);
    return raf.readString((int) ho.dataSize, valueCharset);
  }

  Array<?> readHeapVlen(ByteBuffer bb, int pos, ArrayType dataType, ByteOrder endian) throws IOException {
    HeapIdentifier heapId = h5objects.readHeapIdentifier(bb, pos);
    return getHeapDataArray(heapId, dataType, endian);
  }

  /**
   * Get a data object's name, using the objectId you get from a reference (aka hard link).
   *
   * @param objId address of the data object
   * @return String the data object's name, or null if not found
   * @throws IOException on read error
   */
  String getDataObjectName(long objId) throws IOException {
    DataObject dobj = getDataObject(objId, null);
    if (dobj == null) {
      log.error("H5iosp.readVlenData cant find dataObject id= {}", objId);
      return null;
    } else {
      if (debugVlen) {
        log.debug(" Referenced object= {}", dobj.who);
      }
      return dobj.who;
    }
  }

  //////////////////////////////////////////////////////////////
  // Internal organization of Data Objects

  /**
   * All access to data objects come through here, so we can cache.
   * Look in cache first; read if not in cache.
   *
   * @param address object address (aka id)
   * @param name optional name
   * @return DataObject
   * @throws IOException on read error
   */
  DataObject getDataObject(long address, String name) throws IOException {
    // find it
    DataObject dobj = addressMap.get(address);
    if (dobj != null) {
      if ((dobj.who == null) && name != null)
        dobj.who = name;
      return dobj;
    }
    // if (name == null) return null; // ??

    // read it
    dobj = h5objects.readDataObject(address, name);
    addressMap.put(address, dobj); // look up by address (id)
    return dobj;
  }

  //////////////////////////////////////////////////////////////
  // utilities

  public int makeIntFromBytes(byte[] bb, int start, int n) {
    int result = 0;
    for (int i = start + n - 1; i >= start; i--) {
      result <<= 8;
      byte b = bb[i];
      result += (b < 0) ? b + 256 : b;
    }
    return result;
  }

  public boolean isOffsetLong() {
    return isOffsetLong;
  }

  public long readLength() throws IOException {
    return isLengthLong ? raf.readLong() : (long) raf.readInt();
  }

  public long readOffset() throws IOException {
    return isOffsetLong ? raf.readLong() : (long) raf.readInt();
  }

  public long readAddress() throws IOException {
    return getFileOffset(readOffset());
  }

  public byte getSizeLengths() {
    return sizeLengths;
  }

  // size of data depends on "maximum possible number"
  public int getNumBytesFromMax(long maxNumber) {
    int size = 0;
    while (maxNumber != 0) {
      size++;
      maxNumber >>>= 8; // right shift with zero extension
    }
    return size;
  }

  public long readVariableSizeUnsigned(int size) throws IOException {
    long vv;
    if (size == 1) {
      vv = ArrayType.unsignedByteToShort(raf.readByte());
    } else if (size == 2) {
      if (debugPos) {
        log.debug("position={}", raf.getFilePointer());
      }
      short s = raf.readShort();
      vv = ArrayType.unsignedShortToInt(s);
    } else if (size == 4) {
      vv = ArrayType.unsignedIntToLong(raf.readInt());
    } else if (size == 8) {
      vv = raf.readLong();
    } else {
      vv = readVariableSizeN(size);
    }
    return vv;
  }

  // Little endian
  private long readVariableSizeN(int nbytes) throws IOException {
    int[] ch = new int[nbytes];
    for (int i = 0; i < nbytes; i++)
      ch[i] = raf.read();

    long result = ch[nbytes - 1];
    for (int i = nbytes - 2; i >= 0; i--) {
      result = result << 8;
      result += ch[i];
    }

    return result;
  }

  public RandomAccessFile getRandomAccessFile() {
    return raf;
  }

  public long getFileOffset(long address) {
    return baseAddress + address;
  }

  public byte getSizeOffsets() {
    return sizeOffsets;
  }

  boolean isNetcdf4() {
    return isNetcdf4;
  }

  boolean isClassic() {
    return false; // TODO
  }

  public void close() {
    if (debugTracker) {
      Formatter f = new Formatter();
      memTracker.report(f);
      log.debug("{}", f);
    }
  }

  public void getEosInfo(Formatter f) throws IOException {
    HdfEos.getEosInfo(raf.getLocation(), this, root, f);
  }

  // debug - hdf5Table
  public List<DataObject> getDataObjects() {
    ArrayList<DataObject> result = new ArrayList<>(addressMap.values());
    result.sort(Comparator.comparingLong(o -> o.address));
    return result;
  }

}
