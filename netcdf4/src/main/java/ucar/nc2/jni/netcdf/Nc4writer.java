/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.jni.netcdf;

import static ucar.nc2.ffi.netcdf.NetcdfClibrary.isLibraryPresent;
import static ucar.nc2.jni.netcdf.Nc4prototypes.NC_CLASSIC_MODEL;
import static ucar.nc2.jni.netcdf.Nc4prototypes.NC_CLOBBER;
import static ucar.nc2.jni.netcdf.Nc4prototypes.NC_COMPOUND;
import static ucar.nc2.jni.netcdf.Nc4prototypes.NC_ENUM;
import static ucar.nc2.jni.netcdf.Nc4prototypes.NC_NAT;
import static ucar.nc2.jni.netcdf.Nc4prototypes.NC_NETCDF4;
import static ucar.nc2.jni.netcdf.Nc4prototypes.NC_OPAQUE;
import static ucar.nc2.jni.netcdf.Nc4prototypes.NC_VLEN;

import com.sun.jna.ptr.IntByReference;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ucar.array.ArrayType;
import ucar.ma2.Array;
import ucar.ma2.ArrayStructure;
import ucar.ma2.ArrayStructureBB;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataDeep;
import ucar.ma2.StructureMembers;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.EnumTypedef;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.ffi.netcdf.NetcdfClibrary;
import ucar.nc2.internal.iosp.IospFileWriter;
import ucar.nc2.internal.iosp.hdf5.H5header;
import ucar.nc2.iosp.IospHelper;
import ucar.nc2.iosp.NetcdfFileFormat;
import ucar.nc2.util.CancelTask;
import ucar.nc2.write.Nc4Chunking;
import ucar.nc2.write.Nc4ChunkingDefault;

/** IOSP for writing netcdf files through JNA interface to netcdf C library */
public class Nc4writer extends Nc4reader implements IospFileWriter {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Nc4writer.class);

  // Define reserved attributes (see Nc4DSP)
  public static final String UCARTAGOPAQUE = "_edu.ucar.opaque.size";

  private static final boolean debugCompound = false;
  private static final boolean debugDim = false;
  private static final boolean debugWrite = false;
  private static final String notImplementedMessage = "Functionality not implemented.";

  private boolean fill = true;
  private Nc4Chunking chunker = new Nc4ChunkingDefault();
  private final Map<EnumTypedef, UserType> enumUserTypes = new HashMap<>();

  public Nc4writer() {
    super(NetcdfFileFormat.NETCDF4);
  }

  // called by reflection from NetcdfFormatWriter
  public Nc4writer(NetcdfFileFormat version) {
    super(version);
  }

  // called by reflection from NetcdfFormatWriter
  public void setChunker(Nc4Chunking chunker) {
    if (chunker != null)
      this.chunker = chunker;
  }

  @Override
  public void openForWriting(String location, Group.Builder rootGroup, CancelTask cancelTask) throws IOException {
    throw new UnsupportedOperationException(notImplementedMessage);
  }

  @Override
  public NetcdfFile getOutputFile() {
    throw new UnsupportedOperationException(notImplementedMessage);
  }

  // * Create new file, populate it from the objects in ncfileb.
  @Override
  public NetcdfFile create(String filename, Group.Builder rootGroup, int extra, long preallocateSize, boolean largeFile)
      throws IOException {
    if (!isLibraryPresent()) {
      throw new UnsupportedOperationException("Couldn't load NetCDF C library (see log for details).");
    }
    this.nc4 = NetcdfClibrary.getForeignFunctionInterface();

    this.rootGroup = rootGroup;

    // create new file
    log.debug("create {}", this.location);

    IntByReference oldFormat = new IntByReference();
    int ret = nc4.nc_set_default_format(defineFormat(), oldFormat);
    if (ret != 0) {
      throw new IOException(ret + ": " + nc4.nc_strerror(ret));
    }

    IntByReference ncidp = new IntByReference();
    ret = nc4.nc_create(filename, createMode(), ncidp);
    if (ret != 0) {
      throw new IOException(ret + ": " + nc4.nc_strerror(ret));
    }

    isClosed = false;
    ncid = ncidp.getValue();

    _setFill();

    createGroup(new Group4(ncid, rootGroup, null));

    // done with define mode
    nc4.nc_enddef(ncid);
    if (debugWrite)
      System.out.printf("create done%n%n");

    NetcdfFile.Builder<?> ncfileb = NetcdfFile.builder().setRootGroup(rootGroup).setLocation(filename);
    this.ncfile = ncfileb.build();
    return this.ncfile;
  }

  /*
   * cmode The creation mode flag. The following flags are available:
   * NC_NOCLOBBER (do not overwrite existing file),
   * NC_SHARE (limit write caching - netcdf classic files onlt),
   * NC_64BIT_OFFSET (create 64-bit offset file),
   * NC_NETCDF4 (create netCDF-4/HDF5 file),
   * NC_CLASSIC_MODEL (enforce netCDF classic mode on netCDF-4/HDF5 files),
   * NC_DISKLESS (store data only in memory),
   * NC_MMAP (use MMAP for NC_DISKLESS), and
   * NC_WRITE. See discussion below.
   */
  private int createMode() {
    int ret = NC_CLOBBER;
    switch (version) {
      case NETCDF4:
        ret |= NC_NETCDF4;
        break;
      case NETCDF4_CLASSIC:
        ret |= NC_NETCDF4 | NC_CLASSIC_MODEL;
        break;
    }
    return ret;
  }

  /*
   * #define NC_FORMAT_CLASSIC (1)
   * #define NC_FORMAT_64BIT (2)
   * #define NC_FORMAT_NETCDF4 (3)
   * #define NC_FORMAT_NETCDF4_CLASSIC (4)
   */
  private int defineFormat() {
    switch (version) {
      case NETCDF4:
        return Nc4prototypes.NC_FORMAT_NETCDF4;
      case NETCDF4_CLASSIC:
        return Nc4prototypes.NC_FORMAT_NETCDF4_CLASSIC;
      case NETCDF3:
        return Nc4prototypes.NC_FORMAT_CLASSIC;
      case NETCDF3_64BIT_OFFSET:
        return Nc4prototypes.NC_FORMAT_64BIT;
    }
    throw new IllegalStateException("version = " + version);
  }

  private void createGroup(Group4 g4) throws IOException {
    groupBuilderHash.put(g4.g, g4.grpid);
    g4.dimHash = new HashMap<>();

    // attributes
    for (Attribute att : g4.g.getAttributeContainer()) {
      writeAttribute(g4.grpid, Nc4prototypes.NC_GLOBAL, att, null);
    }

    // dimensions
    for (Dimension dim : g4.g.getDimensions()) {
      int dimLength = dim.isUnlimited() ? Nc4prototypes.NC_UNLIMITED : dim.getLength();
      int dimid = addDimension(g4.grpid, dim.getShortName(), dimLength);
      g4.dimHash.put(dim, dimid);

      if (debugWrite)
        System.out.printf(" create dim '%s' len=%d id=%d in group %d%n", dim.getShortName(), dim.getLength(), dimid,
            g4.grpid);
    }

    // enums
    for (EnumTypedef en : g4.g.enumTypedefs) {
      createEnumType(g4, en);
    }

    // a type must be created for each structure.
    // LOOK we should look for variables with the same structure type.
    for (Variable.Builder<?> v : g4.g.vbuilders) {
      if (v.dataType == ArrayType.STRUCTURE) {
        createCompoundType(g4, (Structure.Builder<?>) v);
      }
    }

    // variables
    for (Variable.Builder<?> v : g4.g.vbuilders) {
      createVariable(g4, v);
    }

    // groups
    for (Group.Builder nested : g4.g.gbuilders) {
      IntByReference grpidp = new IntByReference();
      int ret = nc4.nc_def_grp(g4.grpid, nested.shortName, grpidp);

      if (ret != 0)
        throw new IOException(ret + ": " + nc4.nc_strerror(ret));
      int nestedId = grpidp.getValue();
      createGroup(new Group4(nestedId, nested, g4));
    }
  }

  private void createVariable(Group4 g4, Variable.Builder<?> v) throws IOException {
    int[] dimids = new int[v.getRank()];
    int count = 0;
    for (Dimension d : v.getDimensions()) {
      int dimid;
      if (!d.isShared()) { // LOOK can Netcdf4 support non-shared dimensions ?
        dimid = addDimension(g4.grpid, v.shortName + "_Dim" + count, d.getLength());
      } else {
        dimid = findDimensionId(g4, d);
      }
      if (debugDim) {
        System.out.printf("  use dim '%s' (%d) in variable '%s'%n", d.getShortName(), dimid, v.shortName);
      }
      dimids[count++] = dimid;
    }

    int typid;
    Vinfo vinfo; // TODO vinfo is from the input file; we act here as if its the outut file.
    if (v instanceof Structure.Builder<?>) { // vinfo and typid was stored in vinfo in createCompoundType
      vinfo = (Vinfo) v.spiObject;
      typid = vinfo.typeid;

    } else if (v.dataType.isEnum()) {
      EnumTypedef en = g4.g.findEnumeration(v.getEnumTypeName())
          .orElseThrow(() -> new IllegalStateException("Cant find enum " + v.getEnumTypeName()));
      UserType ut = enumUserTypes.get(en);
      if (ut == null) {
        throw new IllegalStateException("Cant find UserType for enum " + v.getEnumTypeName());
      }
      typid = ut.typeid;
      vinfo = new Vinfo(g4, -1, typid);

    } else if (v.dataType == ArrayType.OPAQUE) {
      // nc_def_opaque(ncid, BASE_SIZE, TYPE_NAME, &xtype)
      typid = convertDataType(v.dataType);
      if (typid < 0) {
        log.warn("Skipping Opaque Type");
        return; // not implemented yet
      }
      vinfo = new Vinfo(g4, -1, typid);

    } else {
      typid = convertDataType(v.dataType);
      if (typid < 0) {
        log.warn("Skipping Unknown data Type");
        return; // not implemented yet
      }
      vinfo = new Vinfo(g4, -1, typid);
    }

    if (debugWrite) {
      System.out.printf("adding variable %s (typeid %d) %n", v.shortName, typid);
    }
    IntByReference varidp = new IntByReference();
    int ret = nc4.nc_def_var(g4.grpid, v.shortName, new SizeT(typid), dimids.length, dimids, varidp);
    if (ret != 0)
      throw new IOException("nc_def_var ret= " + ret + " err= '" + nc4.nc_strerror(ret) + "' on " + v);
    int varid = varidp.getValue();
    vinfo.varid = varid;
    if (debugWrite)
      System.out.printf("added variable %s (grpid %d varid %d) %n", v, vinfo.g4.grpid, vinfo.varid);

    if (version.isNetdf4format() && v.getRank() > 0) {
      boolean isChunked = chunker.isChunked(v);
      int storage = isChunked ? Nc4prototypes.NC_CHUNKED : Nc4prototypes.NC_CONTIGUOUS;
      SizeT[] chunking;
      if (isChunked) {
        long[] lchunks = chunker.computeChunking(v);
        chunking = new SizeT[lchunks.length];
        for (int i = 0; i < lchunks.length; i++)
          chunking[i] = new SizeT(lchunks[i]);
      } else {
        chunking = new SizeT[v.getRank()];
      }

      if (isChunked) {
        ret = nc4.nc_def_var_chunking(g4.grpid, varid, storage, chunking);
        if (ret != 0) {
          throw new IOException(nc4.nc_strerror(ret) + " nc_def_var_chunking on variable " + v.getFullName());
        }
        int deflateLevel = chunker.getDeflateLevel(v);
        int deflate = deflateLevel > 0 ? 1 : 0;
        int shuffle = chunker.isShuffle(v) ? 1 : 0;
        if (deflateLevel > 0) {
          ret = nc4.nc_def_var_deflate(g4.grpid, varid, shuffle, deflate, deflateLevel);
          if (ret != 0)
            throw new IOException(nc4.nc_strerror(ret));
        }
      }
    }

    v.setSPobject(vinfo);

    if (v instanceof Structure.Builder<?>) {
      createCompoundMemberAtts(g4.grpid, varid, (Structure.Builder<?>) v);
    }

    for (Attribute att : v.getAttributeContainer()) {
      writeAttribute(g4.grpid, varid, att, v);
    }
  }

  private int convertDataType(ArrayType dt) {
    switch (dt) {
      case BYTE:
        return Nc4prototypes.NC_BYTE;
      case UBYTE:
        return Nc4prototypes.NC_UBYTE;
      case CHAR:
        return Nc4prototypes.NC_CHAR;
      case DOUBLE:
        return Nc4prototypes.NC_DOUBLE;
      case FLOAT:
        return Nc4prototypes.NC_FLOAT;
      case INT:
        return Nc4prototypes.NC_INT;
      case UINT:
        return Nc4prototypes.NC_UINT;
      case LONG:
        return Nc4prototypes.NC_INT64;
      case ULONG:
        return Nc4prototypes.NC_UINT64;
      case SHORT:
        return Nc4prototypes.NC_SHORT;
      case USHORT:
        return Nc4prototypes.NC_USHORT;
      case STRING:
        return Nc4prototypes.NC_STRING;
      case ENUM1:
      case ENUM2:
      case ENUM4:
        return Nc4prototypes.NC_ENUM;
      case OPAQUE:
        log.warn("Skipping Opaque Type");
        return -1;
      case STRUCTURE:
        return Nc4prototypes.NC_COMPOUND;
    }
    throw new IllegalArgumentException("unimplemented type == " + dt);
  }

  /////////////////////////////////////
  // Enum types

  /*
   * Enum data types can be defined for netCDF-4/HDF5 format files.
   * As with CDM, they are a set of (id,int) values.
   *
   * Create an enum type. Provide an ncid, a name, and base type
   * (some sort of int type).
   * After calling this function, fill out the type with repeated calls to nc_insert_enum.
   * Call nc_insert_enum once for each (id,int) you wish to insert into the enum.
   */
  private void createEnumType(Group4 g4, EnumTypedef ent) throws IOException {
    IntByReference typeidp = new IntByReference();
    String name = ent.getShortName();
    if (!name.endsWith("_t")) {
      name = name + "_t";
    }
    ArrayType enumbase = ent.getBaseArrayType();
    int basetype = NC_NAT;
    if (enumbase == ArrayType.ENUM1)
      basetype = Nc4prototypes.NC_BYTE;
    else if (enumbase == ArrayType.ENUM2)
      basetype = Nc4prototypes.NC_SHORT;
    else if (enumbase == ArrayType.ENUM4)
      basetype = Nc4prototypes.NC_INT;
    int ret = nc4.nc_def_enum(g4.grpid, basetype, name, typeidp);
    if (ret != 0)
      throw new IOException(nc4.nc_strerror(ret) + " on\n" + ent);
    int typeid = typeidp.getValue();
    Map<Integer, String> emap = ent.getMap();
    for (Map.Entry<Integer, String> entry : emap.entrySet()) {
      IntByReference val = new IntByReference(entry.getKey());
      ret = nc4.nc_insert_enum(g4.grpid, typeid, entry.getValue(), val);
      if (ret != 0)
        throw new IOException(nc4.nc_strerror(ret) + " on\n" + entry.getValue());
    }
    // keep track of the User Defined types
    UserType ut =
        new UserType(g4.grpid, typeid, name, ent.getBaseArrayType().getSize(), basetype, emap.size(), NC_ENUM);
    userTypes.put(typeid, ut);
    enumUserTypes.put(ent, ut);
  }

  /////////////////////////////////////
  // compound types

  /*
   * These are pretty serious restrictions, are they current?
   * Implication is that Netcdf-4 cant carry the full CDM model:
   * Fixed size - no vlen as members?
   * member can have up to 4 fixed-size dimensions.
   * Perhaps we have to change the CDM model ?? Or theres a workaround ??
   */

  /*
   * Originally from http://www.hdfgroup.org/HDF5/doc/H5.intro.html#Intro-PMCreateCompound, no longer exists.
   * Compound data types can be defined for netCDF-4/HDF5 format files.
   * A compound datatype is similar to a struct in C and contains a collection of one or more
   * atomic or user-defined types. The netCDF-4 compound data must comply with the properties and constraints of the
   * HDF5 compound data type in terms of which it is implemented.
   *
   * In summary these are:
   *
   * It has a fixed total size.
   * It consists of zero or more named members that do not overlap with other members.
   * Each member has a name distinct from other members.
   * Each member has its own datatype.
   * Each member is referenced by an index number between zero and N-1, where N is the number of members in the compound
   * datatype.
   * Each member has a fixed byte offset, which is the first byte (smallest byte address) of that member in the compound
   * datatype.
   * In addition to other other user-defined data types or atomic datatypes, a member can be a small fixed-size array of
   * any type
   * with up to four fixed-size dimensions (not associated with named netCDF dimensions).
   *
   * Create a compound type. Provide an ncid, a name, and a total size (in bytes) of one element of the completed
   * compound type.
   * After calling this function, fill out the type with repeated calls to nc_insert_compound (see nc_insert_compound).
   * Call nc_insert_compound once for each field you wish to insert into the compound type.
   */
  private void createCompoundType(Group4 g4, Structure.Builder<?> s) throws IOException {
    IntByReference typeidp = new IntByReference();
    long size = s.calcElementSize();
    String name = s.shortName + "_t";
    int ret = nc4.nc_def_compound(g4.grpid, new SizeT(size), name, typeidp);
    if (ret != 0)
      throw new IOException(nc4.nc_strerror(ret) + " on\n" + s);
    int typeid = typeidp.getValue();
    if (debugCompound)
      System.out.printf("added compound type %s (typeid %d) size=%d %n", name, typeid, size);

    List<Field> flds = new ArrayList<>();
    int fldidx = 0;
    long offset = 0;
    for (Variable.Builder<?> v : s.vbuilders) {
      if (v.dataType == ArrayType.STRING) {
        continue;
      }

      int field_typeid = convertDataType(v.dataType);
      if (v.getDimensions().size() == 0) {
        ret = nc4.nc_insert_compound(g4.grpid, typeid, v.shortName, new SizeT(offset), field_typeid);
      } else {
        ret = nc4.nc_insert_array_compound(g4.grpid, typeid, v.shortName, new SizeT(offset), field_typeid, v.getRank(),
            v.getShape());
      }
      if (ret != 0) {
        throw new IOException(nc4.nc_strerror(ret) + " on\n" + s.shortName);
      }

      Field fld =
          new Field(g4.grpid, typeid, fldidx, v.shortName, (int) offset, field_typeid, v.getRank(), v.getShape());
      flds.add(fld);
      if (debugCompound) {
        System.out.printf(" added compound type member %s (%s) offset=%d size=%d%n", v.shortName, v.dataType, offset,
            v.getElementSize() * v.getSize());
      }

      offset += v.getElementSize() * v.getSize();
      fldidx++;
    }

    s.setSPobject(new Vinfo(g4, -1, typeidp.getValue())); // dont know the varid yet

    // keep track of the User Defined types
    UserType ut = new UserType(g4.grpid, typeid, name, size, 0, fldidx, NC_COMPOUND);
    userTypes.put(typeid, ut);
    ut.setFields(flds); // LOOK: These were already set in the UserType ctor.
  }

  private void createCompoundMemberAtts(int grpid, int varid, Structure.Builder<?> s) throws IOException {
    // count size of attribute values
    int sizeAtts = 0;
    for (Variable.Builder<?> m : s.vbuilders) {
      for (Attribute att : m.getAttributeContainer()) {
        int elemSize;
        if (att.isString()) {
          String val = att.getStringValue();
          elemSize = val.getBytes(StandardCharsets.UTF_8).length;
          if (elemSize == 0)
            elemSize = 1;
        } else {
          elemSize = att.getDataType().getSize() * att.getLength();
        }
        sizeAtts += elemSize;
      }
    }
    if (sizeAtts == 0)
      return; // no atts; */

    // create the compound type for member_atts_t
    IntByReference typeidp = new IntByReference();
    String typeName = "_" + s.shortName + "_field_atts_t";
    int ret = nc4.nc_def_compound(grpid, new SizeT(sizeAtts), typeName, typeidp);
    if (ret != 0)
      throw new IOException(nc4.nc_strerror(ret) + " on\n" + s);
    int typeid = typeidp.getValue();
    if (debugCompound)
      System.out.printf("added compound member att type %s (typeid %d) grpid %d size=%d %n", typeName, typeid, grpid,
          sizeAtts); // */

    // add the fields to the member_atts_t and place the values in a ByteBuffer
    ByteBuffer bb = ByteBuffer.allocate(sizeAtts);
    bb.order(ByteOrder.nativeOrder());
    for (Variable.Builder<?> m : s.vbuilders) {
      for (Attribute att : m.getAttributeContainer()) {
        // add the fields to the member_atts_t
        String memberName = m.shortName + ":" + att.getShortName();
        int field_typeid = att.isString() ? Nc4prototypes.NC_CHAR : convertDataType(att.getArrayType()); // LOOK
                                                                                                         // override
        // String with
        // CHAR

        if (att.isString()) { // String gets turned into array of char; otherwise no way to pass in
          String val = att.getStringValue();
          int len = val.getBytes(StandardCharsets.UTF_8).length;
          if (len == 0)
            len = 1;
          ret = nc4.nc_insert_array_compound(grpid, typeid, memberName, new SizeT(bb.position()), field_typeid, 1,
              new int[] {len});

        } else if (!att.isArray())
          ret = nc4.nc_insert_compound(grpid, typeid, memberName, new SizeT(bb.position()), field_typeid);
        else
          ret = nc4.nc_insert_array_compound(grpid, typeid, memberName, new SizeT(bb.position()), field_typeid, 1,
              new int[] {att.getLength()});

        if (ret != 0)
          throw new IOException(nc4.nc_strerror(ret) + " on\n" + s.shortName);

        if (debugCompound) {
          int elemSize;
          if (att.isString()) {
            String val = att.getStringValue();
            elemSize = val.getBytes(StandardCharsets.UTF_8).length;
          } else {
            elemSize = att.getDataType().getSize() * att.getLength();
          }
          System.out.printf(" added compound att member %s type %s (%d) offset=%d elemSize=%d%n", memberName,
              att.getDataType(), field_typeid, bb.position(), elemSize);
        }

        // place the values in a ByteBuffer
        if (att.isString()) {
          String val = att.getStringValue();
          byte[] sby = val.getBytes(StandardCharsets.UTF_8);
          for (byte b : sby)
            bb.put(b);
          if (sby.length == 0)
            bb.put((byte) 0); // empyy string has a 0
        } else {
          for (int i = 0; i < att.getLength(); i++) {
            switch (att.getDataType()) {
              case BYTE:
                bb.put(att.getNumericValue(i).byteValue());
                break;
              case CHAR:
                bb.put(att.getNumericValue(i).byteValue()); // ??
                break;
              case DOUBLE:
                bb.putDouble(att.getNumericValue(i).doubleValue());
                break;
              case FLOAT:
                bb.putFloat(att.getNumericValue(i).floatValue());
                break;
              case INT:
                bb.putInt(att.getNumericValue(i).intValue());
                break;
              case LONG:
                bb.putLong(att.getNumericValue(i).longValue());
                break;
              case SHORT:
                bb.putShort(att.getNumericValue(i).shortValue());
                break;
              default:
                throw new IllegalStateException("Att type " + att.getDataType() + " not found");
            }
          }
        }
      } // loop over atts
    } // loop over vars */

    // now write that attribute on the variable
    String attName = "_field_atts";
    ret = nc4.nc_put_att(grpid, varid, attName, typeid, new SizeT(1), bb.array());
    if (ret != 0) {
      throw new IOException(nc4.nc_strerror(ret) + " on\n" + s.shortName);
    }
    if (debugCompound) {
      System.out.printf("write att %s (typeid %d) size=%d %n", attName, typeid, sizeAtts);
    }
  }

  public void flush() throws IOException {
    if (nc4 == null || ncid < 0)
      return; // not open yet

    int ret = nc4.nc_sync(ncid);
    if (ret != 0)
      throw new IOException(ret + ": " + nc4.nc_strerror(ret));

    // TODO reread dimension in case unlimited has grown
    // updateDimensions(ncfile.getRootGroup());
  }

  @Override
  public void setFill(boolean fill) {
    this.fill = fill;
    try {
      _setFill();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void _setFill() throws IOException {
    if (nc4 == null || ncid < 0)
      return; // not open yet

    IntByReference old_modep = new IntByReference();
    int ret = nc4.nc_set_fill(ncid, fill ? Nc4prototypes.NC_FILL : Nc4prototypes.NC_NOFILL, old_modep);
    if (ret != 0)
      throw new IOException(ret + ": " + nc4.nc_strerror(ret));
  }

  @Override
  public void updateAttribute(Variable v2, Attribute att) throws IOException {
    if (nc4 == null || ncid < 0)
      return; // not open yet

    if (v2 == null) {
      writeAttribute(ncid, Nc4prototypes.NC_GLOBAL, att, null);
    } else {
      Vinfo vinfo = (Vinfo) v2.getSPobject();
      writeAttribute(vinfo.g4.grpid, vinfo.varid, att, v2.toBuilder()); // LOOK
    }
  }

  @Override
  public void updateAttribute(Group g, Attribute att) throws IOException {
    // LOOK
  }

  private void writeAttribute(int grpid, int varid, Attribute att, Variable.Builder<?> v) throws IOException {
    if (v != null && att.getShortName().equals(CDM.FILL_VALUE)) {
      if (att.getLength() != 1) {
        log.warn("_FillValue length must be one on var = {}", v.getFullName());
        return;
      }
      if (att.getArrayType() != v.dataType) {
        // Special case hack for _FillValue type match for char typed variables
        if (att.getArrayType() != ArrayType.STRING || v.dataType != ArrayType.CHAR) {
          log.warn("_FillValue type ({}) does not agree with variable '{}' type ({}).", att.getArrayType(), v.shortName,
              v.dataType);
          return;
        } // else char typed variable with string typed _FillValue
      }
    }

    // dont propagate these - handled internally
    if (att.getShortName().equals(H5header.HDF5_DIMENSION_LIST))
      return;
    if (att.getShortName().equals(H5header.HDF5_DIMENSION_SCALE))
      return;
    if (att.getShortName().equals(H5header.HDF5_DIMENSION_LABELS))
      return;
    if (att.getShortName().equals(CDM.CHUNK_SIZES))
      return;
    if (att.getShortName().equals(CDM.COMPRESS))
      return;

    if (CDM.NETCDF4_SPECIAL_ATTS.contains(att.getShortName()))
      return;

    int ret = 0;
    Array values = att.getValues();
    Object arrayStorage = null;
    if (values != null) {
      arrayStorage = values.getStorage();
    }
    switch (att.getDataType()) {
      case STRING: // problem may be that we are mapping char * atts to string type
        if (v != null && att.getShortName().equals(CDM.FILL_VALUE) && att.getLength() == 1
            && v.dataType == ArrayType.CHAR) {
          // special handling of _FillValue if v.getDataType() == CHAR
          byte[] svalb = att.getStringValue().getBytes(StandardCharsets.UTF_8);
          // if svalb is a zero length array, force it to be the null char
          if (svalb.length == 0)
            svalb = new byte[] {0};
          ret = nc4.nc_put_att_text(grpid, varid, att.getShortName(), new SizeT(svalb.length), svalb);

        } else { // String valued attribute
          if (!this.version.isExtendedModel()) {
            // Must write it as character typed attribute
            StringBuilder text = new StringBuilder();
            // Concatenate all the attribute strings
            for (int i = 0; i < att.getLength(); i++)
              text.append(att.getStringValue(i));
            byte[] svalb = text.toString().getBytes(StandardCharsets.UTF_8);
            if (svalb.length == 0)
              svalb = new byte[] {0};
            ret = nc4.nc_put_att_text(grpid, varid, att.getShortName(), new SizeT(svalb.length), svalb);
          } else { // Can write as string typed attribute
            String[] svalues = new String[att.getLength()];
            for (int i = 0; i < att.getLength(); i++)
              svalues[i] = (String) att.getValue(i);
            ret = nc4.nc_put_att_string(grpid, varid, att.getShortName(), new SizeT(att.getLength()), svalues);
          }
        }
        break;
      case UBYTE:
        ret = nc4.nc_put_att_uchar(grpid, varid, att.getShortName(), Nc4prototypes.NC_UBYTE, new SizeT(att.getLength()),
            (byte[]) arrayStorage);
        break;
      case BYTE:
        ret = nc4.nc_put_att_schar(grpid, varid, att.getShortName(), Nc4prototypes.NC_BYTE, new SizeT(att.getLength()),
            (byte[]) arrayStorage);
        break;
      case CHAR:
        ret = nc4.nc_put_att_text(grpid, varid, att.getShortName(), new SizeT(att.getLength()),
            IospHelper.convertCharToByte((char[]) arrayStorage));
        break;
      case DOUBLE:
        ret = nc4.nc_put_att_double(grpid, varid, att.getShortName(), Nc4prototypes.NC_DOUBLE,
            new SizeT(att.getLength()), (double[]) arrayStorage);
        break;
      case FLOAT:
        ret = nc4.nc_put_att_float(grpid, varid, att.getShortName(), Nc4prototypes.NC_FLOAT, new SizeT(att.getLength()),
            (float[]) arrayStorage);
        break;
      case UINT:
        ret = nc4.nc_put_att_uint(grpid, varid, att.getShortName(), Nc4prototypes.NC_UINT, new SizeT(att.getLength()),
            (int[]) arrayStorage);
        break;
      case INT:
        ret = nc4.nc_put_att_int(grpid, varid, att.getShortName(), Nc4prototypes.NC_INT, new SizeT(att.getLength()),
            (int[]) arrayStorage);
        break;
      case ULONG:
        ret = nc4.nc_put_att_ulonglong(grpid, varid, att.getShortName(), Nc4prototypes.NC_UINT64,
            new SizeT(att.getLength()), (long[]) arrayStorage);
        break;
      case LONG:
        ret = nc4.nc_put_att_longlong(grpid, varid, att.getShortName(), Nc4prototypes.NC_INT64,
            new SizeT(att.getLength()), (long[]) arrayStorage);
        break;
      case USHORT:
        ret = nc4.nc_put_att_ushort(grpid, varid, att.getShortName(), Nc4prototypes.NC_USHORT,
            new SizeT(att.getLength()), (short[]) arrayStorage);
        break;
      case SHORT:
        ret = nc4.nc_put_att_short(grpid, varid, att.getShortName(), Nc4prototypes.NC_SHORT, new SizeT(att.getLength()),
            (short[]) arrayStorage);
        break;
    }

    if (ret != 0) {
      String where = v != null ? "var " + v.getFullName() : "global or group attribute";
      throw new IOException(ret + " (" + nc4.nc_strerror(ret) + ") on attribute '" + att + "' on " + where);
    }

    if (debugWrite)
      System.out.printf("Add attribute to var %s == %s%n", (v == null) ? "global" : v.getFullName(), att.toString());
  }

  @Override
  public void writeData(Variable v2, Section section, Array values) throws IOException, InvalidRangeException {
    Vinfo vinfo = (Vinfo) v2.getSPobject();
    if (vinfo == null) {
      log.error("vinfo null for " + v2);
      throw new IllegalStateException("vinfo null for " + v2.getFullName());
    }
    // int vlen = (int) v2.getSize();
    // int len = (int) section.computeSize();
    // if (vlen == len) // entire array
    // writeDataAll(v2, vinfo.grpid, vinfo.varid, vinfo.typeid, values);
    // else
    writeData(v2, vinfo.g4.grpid, vinfo.varid, vinfo.typeid, section, values);
  }

  private void writeData(Variable v, int grpid, int varid, int typeid, Section section, Array values)
      throws IOException, InvalidRangeException {

    // general sectioning with strides
    SizeT[] origin = convertSizeT(section.getOrigin());
    SizeT[] shape = convertSizeT(section.getShape());
    SizeT[] stride = convertSizeT(section.getStride());
    boolean isUnsigned = isUnsigned(typeid);
    int sectionLen = (int) section.computeSize();

    Object data = values.get1DJavaArray(values.getDataType());

    switch (typeid) {

      case Nc4prototypes.NC_BYTE:
      case Nc4prototypes.NC_UBYTE:
        byte[] valb = (byte[]) data;
        assert valb.length == sectionLen;
        int ret = isUnsigned ? nc4.nc_put_vars_uchar(grpid, varid, origin, shape, stride, valb)
            : nc4.nc_put_vars_schar(grpid, varid, origin, shape, stride, valb);
        if (ret != 0)
          throw new IOException(ret + ": " + nc4.nc_strerror(ret));
        break;

      case Nc4prototypes.NC_CHAR:
        char[] valc = (char[]) data; // chars are lame
        assert valc.length == sectionLen;

        valb = IospHelper.convertCharToByte(valc);
        ret = nc4.nc_put_vars_text(grpid, varid, origin, shape, stride, valb);
        // ret = nc4.nc_put_vara_text(grpid, varid, origin, shape, valb);

        if (ret != 0) {
          throw new IOException(nc4.nc_strerror(ret));
        }
        break;

      case Nc4prototypes.NC_DOUBLE:
        double[] vald = (double[]) data;
        assert vald.length == sectionLen;
        ret = nc4.nc_put_vars_double(grpid, varid, origin, shape, stride, vald);
        if (ret != 0)
          throw new IOException(ret + ": " + nc4.nc_strerror(ret));
        break;

      case Nc4prototypes.NC_FLOAT:
        float[] valf = (float[]) data;
        assert valf.length == sectionLen;
        ret = nc4.nc_put_vars_float(grpid, varid, origin, shape, stride, valf);
        if (ret != 0) {
          // log.error("{} on var {}", nc4.nc_strerror(ret), v);
          // return;
          throw new IOException(nc4.nc_strerror(ret));
        }
        break;

      case Nc4prototypes.NC_UINT:
      case Nc4prototypes.NC_INT:
        int[] vali = (int[]) data;
        assert vali.length == sectionLen;
        ret = isUnsigned ? nc4.nc_put_vars_uint(grpid, varid, origin, shape, stride, vali)
            : nc4.nc_put_vars_int(grpid, varid, origin, shape, stride, vali);

        if (ret != 0) {
          // log.error("{} on var {}", nc4.nc_strerror(ret), v);
          // return;
          throw new IOException(nc4.nc_strerror(ret));
        }
        break;

      case Nc4prototypes.NC_UINT64:
      case Nc4prototypes.NC_INT64:
        long[] vall = (long[]) data;
        assert vall.length == sectionLen;
        ret = isUnsigned ? nc4.nc_put_vars_ulonglong(grpid, varid, origin, shape, stride, vall)
            : nc4.nc_put_vars_longlong(grpid, varid, origin, shape, stride, vall);

        if (ret != 0)
          throw new IOException(ret + ": " + nc4.nc_strerror(ret));
        break;

      case Nc4prototypes.NC_USHORT:
      case Nc4prototypes.NC_SHORT:
        short[] vals = (short[]) data;
        assert vals.length == sectionLen;
        ret = isUnsigned ? nc4.nc_put_vars_ushort(grpid, varid, origin, shape, stride, vals)
            : nc4.nc_put_vars_short(grpid, varid, origin, shape, stride, vals);
        if (ret != 0)
          throw new IOException(ret + ": " + nc4.nc_strerror(ret));
        break;

      case Nc4prototypes.NC_STRING:
        String[] valss = convertStringData(data);
        assert valss.length == sectionLen;
        ret = nc4.nc_put_vars_string(grpid, varid, origin, shape, stride, valss);
        if (ret != 0)
          throw new IOException(ret + ": " + nc4.nc_strerror(ret));
        break;

      default:
        UserType userType = userTypes.get(typeid);
        if (userType == null)
          throw new IOException("Unknown userType == " + typeid);
        switch (userType.typeClass) {
          case NC_ENUM:
            ret = writeEnumData(v, userType, grpid, varid, typeid, section, values);
            if (ret != 0) {
              // log.error("{} on var {}", nc4.nc_strerror(ret), v);
              // return;
              throw new IOException(nc4.nc_strerror(ret));
            }
            break;
          case NC_COMPOUND:
            writeCompoundData((Structure) v, userType, grpid, varid, typeid, section, (ArrayStructure) values);
            return;
          case NC_VLEN:
          case NC_OPAQUE:
          default:
            throw new IOException("Unsupported writing of userType= " + userType);
        }
    }
    if (debugWrite)
      System.out.printf("OK writing var %s%n", v);
  }

  private int writeEnumData(Variable v, UserType userType, int grpid, int varid, int typeid, Section section,
      Array values) {
    SizeT[] origin = convertSizeT(section.getOrigin());
    SizeT[] shape = convertSizeT(section.getShape());
    int sectionLen = (int) section.computeSize();
    assert values.getSize() == sectionLen;

    int[] secStride = section.getStride();
    boolean stride1 = isStride1(secStride);

    int ret;
    ByteBuffer bb = values.getDataAsByteBuffer(ByteOrder.nativeOrder());
    byte[] data = bb.array();
    if (stride1) {
      ret = nc4.nc_put_vara(grpid, varid, origin, shape, data);
    } else {
      SizeT[] stride = convertSizeT(secStride);
      ret = nc4.nc_put_vars(grpid, varid, origin, shape, stride, data);
    }
    return ret;
  }

  private void writeCompoundData(Structure s, UserType userType, int grpid, int varid, int typeid, Section section,
      ArrayStructure values) throws IOException, InvalidRangeException {

    SizeT[] origin = convertSizeT(section.getOrigin());
    SizeT[] shape = convertSizeT(section.getShape());
    SizeT[] stride = convertSizeT(section.getStride());

    ArrayStructureBB valuesBB = StructureDataDeep.copyToArrayBB(s, values, ByteOrder.nativeOrder()); // LOOK embedded
    // strings getting
    // lost ??
    ByteBuffer bbuff = valuesBB.getByteBuffer();

    if (debugCompound)
      System.out.printf("writeCompoundData variable %s (grpid %d varid %d) %n", s.getShortName(), grpid, varid);

    // write the data
    // int ret = nc4.nc_put_var(grpid, varid, bbuff);
    int ret;
    if (section.isStrided())
      ret = nc4.nc_put_vars(grpid, varid, origin, shape, stride, bbuff.array());
    else
      ret = nc4.nc_put_vara(grpid, varid, origin, shape, bbuff.array());
    if (ret != 0)
      throw new IOException(errMessage("nc_put_vars", ret, grpid, varid));
  }

  @Override
  public int appendStructureData(Structure s, StructureData sdata) throws IOException, InvalidRangeException {
    Vinfo vinfo = (Vinfo) s.getSPobject();
    Dimension dim = s.getDimension(0); // LOOK must be outer dim
    int dimid = vinfo.g4.dimHash.get(dim);
    SizeTByReference lenp = new SizeTByReference();
    int ret = nc4.nc_inq_dimlen(vinfo.g4.grpid, dimid, lenp);
    if (ret != 0)
      throw new IOException(errMessage("nc_inq_dimlen", ret, vinfo.g4.grpid, dimid));

    SizeT[] origin = {lenp.getValue()};
    SizeT[] shape = {new SizeT(1)};
    SizeT[] stride = {new SizeT(1)};

    // ArrayStructureBB valuesBB = IospHelper.copyToArrayBB(sdata, ByteOrder.nativeOrder());
    // n4 wants native byte order
    // ByteBuffer bbuff = valuesBB.getByteBuffer();
    ByteBuffer bbuff = makeBB(s, sdata);

    // write the data
    // ret = nc4.nc_put_vara(vinfo.g4.grpid, vinfo.varid, origin, shape, bbuff);
    // ret = nc4.nc_put_vars(vinfo.g4.grpid, vinfo.varid, origin, shape, stride, bbuff);
    ret = nc4.nc_put_vars(vinfo.g4.grpid, vinfo.varid, origin, shape, stride, bbuff.array());
    if (ret != 0)
      throw new IOException(errMessage("appendStructureData (nc_put_vars)", ret, vinfo.g4.grpid, vinfo.varid));

    return origin[0].intValue(); // recnum
  }

  private String errMessage(String what, int ret, int grpid, int varid) {
    Formatter f = new Formatter();
    f.format("%s: %d: %s grpid=%d objid=%d", what, ret, nc4.nc_strerror(ret), grpid, varid);
    return f.toString();
  }

  private int addDimension(int grpid, String name, int length) throws IOException {
    IntByReference dimidp = new IntByReference();
    int ret = nc4.nc_def_dim(grpid, name, new SizeT(length), dimidp);
    if (ret != 0)
      throw new IOException(ret + ": " + nc4.nc_strerror(ret));
    if (debugDim)
      System.out.printf("add dimension %s len=%d%n", name, length);
    return dimidp.getValue();
  }

  // copy data out of sdata into a ByteBuffer, based on the menmbers and offsets in s
  private ByteBuffer makeBB(Structure s, StructureData sdata) {
    int size = s.getElementSize();
    ByteBuffer bb = ByteBuffer.allocate(size);
    bb.order(ByteOrder.nativeOrder());

    long offset = 0;
    for (Variable v : s.getVariables()) {
      if (v.getDataType() == DataType.STRING)
        continue; // LOOK embedded strings getting lost

      StructureMembers.Member m = sdata.findMember(v.getShortName());
      if (m == null) {
        log.warn("WARN Nc4Iosp.makeBB() cant find {}", v.getShortName());
        bb.position((int) (offset + v.getElementSize() * v.getSize())); // skip over it
      } else {
        copy(sdata, m, bb);
      }

      offset += v.getElementSize() * v.getSize();
    }

    return bb;
  }

  private void copy(StructureData sdata, StructureMembers.Member m, ByteBuffer bb) {
    DataType dtype = m.getDataType();
    if (m.isScalar()) {
      switch (dtype) {
        case FLOAT:
          bb.putFloat(sdata.getScalarFloat(m));
          break;
        case DOUBLE:
          bb.putDouble(sdata.getScalarDouble(m));
          break;
        case INT:
        case ENUM4:
          bb.putInt(sdata.getScalarInt(m));
          break;
        case SHORT:
        case ENUM2:
          bb.putShort(sdata.getScalarShort(m));
          break;
        case BYTE:
        case ENUM1:
          bb.put(sdata.getScalarByte(m));
          break;
        case CHAR:
          bb.put((byte) sdata.getScalarChar(m));
          break;
        case LONG:
          bb.putLong(sdata.getScalarLong(m));
          break;
        default:
          throw new IllegalStateException("scalar " + dtype);
        /*
         * case BOOLEAN:
         * break;
         * case SEQUENCE:
         * break;
         * case STRUCTURE:
         * break;
         * case OPAQUE:
         * break;
         */
      }
    } else {
      int n = m.getSize();
      switch (dtype) {
        case FLOAT:
          float[] fdata = sdata.getJavaArrayFloat(m);
          for (int i = 0; i < n; i++)
            bb.putFloat(fdata[i]);
          break;
        case DOUBLE:
          double[] ddata = sdata.getJavaArrayDouble(m);
          for (int i = 0; i < n; i++)
            bb.putDouble(ddata[i]);
          break;
        case INT:
        case ENUM4:
          int[] idata = sdata.getJavaArrayInt(m);
          for (int i = 0; i < n; i++)
            bb.putInt(idata[i]);
          break;
        case SHORT:
        case ENUM2:
          short[] shdata = sdata.getJavaArrayShort(m);
          for (int i = 0; i < n; i++)
            bb.putShort(shdata[i]);
          break;
        case BYTE:
        case ENUM1:
          byte[] bdata = sdata.getJavaArrayByte(m);
          for (int i = 0; i < n; i++)
            bb.put(bdata[i]);
          break;
        case CHAR:
          char[] cdata = sdata.getJavaArrayChar(m);
          bb.put(IospHelper.convertCharToByte(cdata));
          break;
        case LONG:
          long[] ldata = sdata.getJavaArrayLong(m);
          for (int i = 0; i < n; i++)
            bb.putLong(ldata[i]);
          break;
        default:
          throw new IllegalStateException("array " + dtype);
        /*
         * case BOOLEAN:
         * break;
         * case OPAQUE:
         * break;
         * case STRUCTURE:
         * break; //
         */
        case SEQUENCE:
          break; // skip
      }
    }
  }

  private String[] convertStringData(Object org) throws IOException {
    if (org instanceof String[])
      return (String[]) org;
    if (org instanceof Object[]) {
      Object[] oo = (Object[]) org;
      String[] result = new String[oo.length];
      int count = 0;
      for (Object s : oo)
        result[count++] = (String) s;
      return result;
    }
    throw new IOException("convertStringData failed on class = " + org.getClass().getName());
  }

  private Integer findDimensionId(Group4 g4, Dimension d) {
    if (g4 == null)
      return null;

    Integer dimid = g4.dimHash.get(d);
    if (dimid == null) {
      dimid = findDimensionId(g4.parent, d); // search in parent
    }

    return dimid;
  }

  private void updateDimensions(Group g) throws IOException {
    int grpid = groupBuilderHash.get(g);

    IntByReference nunlimdimsp = new IntByReference();
    int[] unlimdimids = new int[Nc4prototypes.NC_MAX_DIMS];
    int ret = nc4.nc_inq_unlimdims(grpid, nunlimdimsp, unlimdimids);
    if (ret != 0)
      throw new IOException(ret + ": " + nc4.nc_strerror(ret));

    int ndims = nunlimdimsp.getValue();
    for (int i = 0; i < ndims; i++) {
      byte[] name = new byte[Nc4prototypes.NC_MAX_NAME + 1];
      SizeTByReference lenp = new SizeTByReference();
      ret = nc4.nc_inq_dim(grpid, unlimdimids[i], name, lenp);
      if (ret != 0)
        throw new IOException(ret + ": " + nc4.nc_strerror(ret));
      String dname = makeString(name);

      Dimension d = g.findDimension(dname).orElseThrow(() -> new IllegalStateException("Cant find dimension " + dname));

      if (!d.isUnlimited())
        throw new IllegalStateException("dimension " + dname + " should be unlimited");

      // TODO udim.setLength : need UnlimitedDimension extends Dimension?
      int len = lenp.getValue().intValue();
      if (len != d.getLength()) {
        // TODO d.setLength(len);
        // must update all variables that use this dimension
        for (Variable var : g.getVariables()) {
          if (contains(var.getDimensions(), d)) {
            var.resetShape(); // LOOK
            var.invalidateCache();
          }
        }
      }
    }

    // recurse
    for (Group child : g.getGroups())
      updateDimensions(child);
  }

  // must check by name, not object equality
  private boolean contains(List<Dimension> dims, Dimension want) {
    for (Dimension have : dims) {
      if (have.getShortName().equals(want.getShortName()))
        return true;
    }
    return false;
  }


  private boolean isStride1(int[] strides) {
    if (strides == null)
      return true;
    for (int stride : strides) {
      if (stride != 1) // LOOK seems fishy
        return false;
    }
    return true;
  }
}
