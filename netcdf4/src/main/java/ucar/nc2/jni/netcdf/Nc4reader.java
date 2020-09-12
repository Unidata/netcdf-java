/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.jni.netcdf;

import static ucar.nc2.ffi.netcdf.NetcdfClibrary.isLibraryPresent;
import static ucar.nc2.jni.netcdf.Nc4prototypes.*;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.Group.Builder;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.DataFormatType;
import ucar.nc2.internal.iosp.hdf4.HdfEos;
import ucar.nc2.internal.iosp.hdf4.HdfHeaderIF;
import ucar.nc2.internal.util.URLnaming;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.iosp.IospHelper;
import ucar.nc2.ffi.netcdf.NetcdfClibrary;
import ucar.nc2.util.CancelTask;
import ucar.nc2.internal.util.EscapeStrings;
import ucar.nc2.iosp.NetcdfFileFormat;
import ucar.unidata.io.RandomAccessFile;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

/**
 * IOSP for reading netcdf files through JNA interface to netcdf C library
 *
 * @see <a href="https://www.unidata.ucar.edu/software/netcdf/docs/netcdf-c.html" />
 * @see <a href="http://earthdata.nasa.gov/sites/default/files/field/document/ESDS-RFC-022v1.pdf" />
 * @see <a href=
 *      "https://www.unidata.ucar.edu/software/netcdf/docs/faq.html#How-can-I-convert-HDF5-files-into-netCDF-4-files" />
 */
public class Nc4reader extends AbstractIOServiceProvider {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Nc4reader.class);

  public static final boolean DEBUG = false;

  // Define reserved attributes (see Nc4DSP)
  public static final String UCARTAGOPAQUE = "_edu.ucar.opaque.size";

  // IOSP messages
  public static final String TRANSLATECONTROL = "ucar.translate";
  public static final String TRANSLATE_NONE = "none";
  public static final String TRANSLATE_NC4 = "nc4";

  private static final boolean debugCompoundAtt = false;
  private static final boolean debugUserTypes = false;

  // if the default charset being used by java isn't UTF-8, then we will
  // need to transcode any string read into netCDf-Java via netCDF-C
  private static final boolean transcodeStrings = Charset.defaultCharset() != StandardCharsets.UTF_8;

  private static boolean useHdfEos;

  public static void useHdfEos(boolean val) {
    useHdfEos = val;
  }

  ///////////////////////////////////////////////
  // Moved from Attribute

  private static final String SPECIALPREFIX = "_";
  private static final String[] SPECIALS =
      {CDM.NCPROPERTIES, CDM.ISNETCDF4, CDM.SUPERBLOCKVERSION, CDM.DAP4_LITTLE_ENDIAN, CDM.EDU_UCAR_PREFIX};

  public static boolean isspecial(Attribute a) {
    String nm = a.getShortName();
    if (nm.startsWith(SPECIALPREFIX)) {
      /* Check for selected special attributes */
      for (String s : SPECIALS) {
        if (nm.startsWith(s))
          return true; /* is special */
      }
    }
    return false; /* is not special */
  }

  //////////////////////////////////////////////////
  // Instance Variables

  Nc4prototypes nc4;
  // TODO set version from file that's read in
  NetcdfFileFormat version; // can use c library to create these different version files
  int ncid = -1; // file id
  boolean markReserved;
  boolean isClosed;

  final Map<Integer, UserType> userTypes = new HashMap<>(); // hash by typeid
  final Map<Group.Builder, Integer> groupBuilderHash = new HashMap<>(); // TODO group.builder -> nc4 grpid

  private int format; // from nc_inq_format
  private boolean isEos;

  // no-arg constructor for NetcdfFiles.open()
  public Nc4reader() {
    this(NetcdfFileFormat.NETCDF4);
  }

  Nc4reader(NetcdfFileFormat version) {
    this.version = version;
  }

  /**
   * Checks whether {@code raf} is a valid file NetCDF-4 file. Actually, it checks whether it is a valid HDF-5 file of
   * any type.
   * Furthermore, it checks whether the NetCDF C library is available on the system. If both conditions are satisfied,
   * this method returns
   * {@code true}; otherwise it returns {@code false}.
   *
   * @param raf a file on disk.
   * @return {@code true} if {@code raf} is a valid HDF-5 file and the NetCDF C library is available.
   * @throws IOException if an I/O error occurs.
   */
  @Override
  public boolean isValidFile(RandomAccessFile raf) throws IOException {
    NetcdfFileFormat format = NetcdfFileFormat.findNetcdfFormatType(raf);
    boolean valid = false;
    switch (format) {
      case NETCDF4:
      case NETCDF4_CLASSIC:
      case NETCDF3_64BIT_DATA:
        valid = true;
        break;
      default:
        break;// everything else is invalid
    }
    if (valid) {
      if (isLibraryPresent()) {
        return true;
      } else {
        log.debug("File is valid but the NetCDF-4 native library isn't installed: {}", raf.getLocation());
      }
    }

    return false;
  }

  // 2016-06-06 note: Once netcdf-c v4.4.1 is released, we should be able to return much better information from
  // getFileTypeDescription(), getFileTypeId(), and getFileTypeVersion() (inherited from superclass).
  // See https://goo.gl/pSP1Bq

  @Override
  public String getFileTypeDescription() {
    return "Netcdf/JNI: " + version;
  }

  @Override
  public String getFileTypeId() {
    if (isEos)
      return "HDF5-EOS";
    return version.isNetdf4format() ? DataFormatType.NETCDF4.getDescription() : DataFormatType.HDF5.getDescription();
  }

  @Override
  public void close() throws IOException {
    if (isClosed)
      return;
    if (ncid < 0)
      return;
    int ret = nc4.nc_close(ncid);
    if (ret != 0)
      throw new IOException(ret + ": " + nc4.nc_strerror(ret));
    isClosed = true;
  }

  @Override
  public Object sendIospMessage(Object message) {
    if (message instanceof Map) {
      Map<String, String> map = (Map<String, String>) message;
      // See if we can extract some controls
      for (String key : map.keySet()) {
        if (key.equalsIgnoreCase(TRANSLATECONTROL)) {
          String value = map.get(key).toString();
          if (value.equalsIgnoreCase(TRANSLATE_NONE)) {
            this.markReserved = false;
          } else if (value.equalsIgnoreCase(TRANSLATE_NC4)) {
            this.markReserved = true;
          } // else ignore
        } // else ignore
      }
    }
    return null;
  }

  /////////////////////////////////////////////////////////////////////////////////
  // NetcdfFile building

  Group.Builder rootGroup;

  @Override
  public void build(RandomAccessFile raf, Group.Builder rootGroup, CancelTask cancelTask) throws IOException {
    super.open(raf, rootGroup.getNcfile(), cancelTask);
    boolean readOnly = true;
    this.rootGroup = rootGroup;

    if (!isLibraryPresent()) {
      throw new UnsupportedOperationException("Couldn't load NetCDF C library (see log for details).");
    }
    this.nc4 = NetcdfClibrary.getForeignFunctionInterface();

    if (raf != null) {
      raf.close(); // not used
    }

    // netcdf-c can't handle "file:" prefix. Must remove it.
    String location = URLnaming.canonicalizeUriString(this.location);
    log.debug("open {}", location);

    IntByReference ncidp = new IntByReference();
    int ret = nc4.nc_open(location, readOnly ? NC_NOWRITE : NC_WRITE, ncidp);
    if (ret != 0) {
      throw new IOException(ret + ": " + nc4.nc_strerror(ret));
    }

    isClosed = false;
    ncid = ncidp.getValue();

    // format
    IntByReference formatp = new IntByReference();
    ret = nc4.nc_inq_format(ncid, formatp);
    if (ret != 0) {
      throw new IOException(ret + ": " + nc4.nc_strerror(ret));
    }
    format = formatp.getValue();
    log.debug("open {} id={} format={}", this.location, ncid, format);

    // read root group
    makeGroup(new Group4(ncid, rootGroup, null));

    // TODO: check if its an HDF5-EOS file; we dont have an HdfHeaderIF, because we opened with JNA
    if (useHdfEos) {
      rootGroup.findGroupLocal(HdfEos.HDF5_GROUP).ifPresent(eosGroup -> {
        try {
          isEos = HdfEos.amendFromODL(this.location, new HdfEosHeader(), eosGroup);
        } catch (IOException e) {
          log.warn(" HdfEos.amendFromODL failed");
        }
      });
    }
  }

  private class HdfEosHeader implements HdfHeaderIF {

    @Override
    public Builder getRootGroup() {
      return rootGroup;
    }

    @Override
    public void makeVinfoForDimensionMapVariable(Builder parent, Variable.Builder<?> v) {
      // TODO
    }

    @Override
    public String readStructMetadata(Variable.Builder<?> structMetadataVar) throws IOException {
      // TODO
      return null;
    }
  }

  private void makeGroup(Group4 g4) throws IOException {
    groupBuilderHash.put(g4.g, g4.grpid);

    makeDimensions(g4);
    makeUserTypes(g4.grpid, g4.g);

    // group attributes
    IntByReference ngattsp = new IntByReference();
    int ret = nc4.nc_inq_natts(g4.grpid, ngattsp);
    if (ret != 0)
      throw new IOException(ret + ": " + nc4.nc_strerror(ret));
    List<Attribute> gatts = makeAttributes(g4.grpid, Nc4prototypes.NC_GLOBAL, ngattsp.getValue(), null);
    for (Attribute att : gatts) {
      g4.g.addAttribute(att);
      log.debug(" add Global Attribute {}", att);
    }

    makeVariables(g4);

    if (format == Nc4prototypes.NC_FORMAT_NETCDF4) {
      // read subordinate groups
      IntByReference numgrps = new IntByReference();
      ret = nc4.nc_inq_grps(g4.grpid, numgrps, null);
      if (ret != 0)
        throw new IOException(ret + ": " + nc4.nc_strerror(ret));
      int[] group_ids = new int[numgrps.getValue()];
      ret = nc4.nc_inq_grps(g4.grpid, numgrps, group_ids);
      if (ret != 0)
        throw new IOException(ret + ": " + nc4.nc_strerror(ret));

      for (int group_id : group_ids) {
        byte[] name = new byte[Nc4prototypes.NC_MAX_NAME + 1];
        ret = nc4.nc_inq_grpname(group_id, name);
        if (ret != 0)
          throw new IOException(ret + ": " + nc4.nc_strerror(ret));
        Group.Builder child = Group.builder().setName(makeString(name));
        g4.g.addGroup(child);
        makeGroup(new Group4(group_id, child, g4));
      }
    }
  }

  private void makeDimensions(Group4 g4) throws IOException {
    // We need this in order to allocate the correct length for dimIdsInGroup.
    IntByReference numDimsInGoup_p = new IntByReference();
    int ret = nc4.nc_inq_ndims(g4.grpid, numDimsInGoup_p);
    if (ret != 0)
      throw new IOException(ret + ": " + nc4.nc_strerror(ret));

    IntByReference numDimidsInGroup_p = new IntByReference();
    int[] dimIdsInGroup = new int[numDimsInGoup_p.getValue()];
    ret = nc4.nc_inq_dimids(g4.grpid, numDimidsInGroup_p, dimIdsInGroup, 0);
    if (ret != 0)
      throw new IOException(ret + ": " + nc4.nc_strerror(ret));

    assert numDimsInGoup_p.getValue() == numDimidsInGroup_p.getValue() : String.format(
        "Number of dimensions in group (%s) differed from number of dimension IDs in group (%s).",
        numDimsInGoup_p.getValue(), numDimidsInGroup_p.getValue());

    // We need this in order to allocate the correct length for unlimitedDimIdsInGroup.
    IntByReference numUnlimitedDimsInGroup_p = new IntByReference();
    ret = nc4.nc_inq_unlimdims(g4.grpid, numUnlimitedDimsInGroup_p, null);
    if (ret != 0)
      throw new IOException(ret + ": " + nc4.nc_strerror(ret));

    int[] unlimitedDimIdsInGroup = new int[numUnlimitedDimsInGroup_p.getValue()];
    ret = nc4.nc_inq_unlimdims(g4.grpid, numUnlimitedDimsInGroup_p, unlimitedDimIdsInGroup);
    if (ret != 0)
      throw new IOException(ret + ": " + nc4.nc_strerror(ret));

    // Ensure array is sorted so that we can use binarySearch on it.
    Arrays.sort(unlimitedDimIdsInGroup);

    for (int dimId : dimIdsInGroup) {
      byte[] dimNameBytes = new byte[Nc4prototypes.NC_MAX_NAME + 1];
      SizeTByReference dimLength_p = new SizeTByReference();
      ret = nc4.nc_inq_dim(g4.grpid, dimId, dimNameBytes, dimLength_p);
      if (ret != 0)
        throw new IOException(ret + ": " + nc4.nc_strerror(ret));

      String dimName = makeString(dimNameBytes);
      boolean isUnlimited = Arrays.binarySearch(unlimitedDimIdsInGroup, dimId) >= 0;

      Dimension dimension = new Dimension(dimName, dimLength_p.getValue().intValue(), true, isUnlimited, false);
      g4.g.addDimension(dimension);
      log.debug("add Dimension {} ({})", dimension, dimId);
    }
  }

  /**
   * By default, JNA assumes strings coming into java from the C side are using
   * the system encoding. However, netCDF-C encodes using UTF-8. Because of this,
   * if we are on a platform where java is not using UTF-8 as the default encoding,
   * we will need to transcode the incoming strings fix the incorrect assumption
   * made by JNA.
   *
   * Note, we could set the system property jna.encode=UTF-8, but would impact the
   * behavior of other libraries that use JNA, and would not be very nice of us to
   * set globally (and often times isn't the right thing to set anyways, since the
   * default in C to use the system encoding).
   *
   * @param systemStrings String array encoded using the default charset
   * @return String array encoded using the UTF-8 charset
   */
  private String[] transcodeString(String[] systemStrings) {
    return Arrays.stream(systemStrings).map(systemString -> {
      byte[] byteArray = systemString.getBytes(Charset.defaultCharset());
      return new String(byteArray, StandardCharsets.UTF_8);
    }).toArray(String[]::new);
  }

  String makeString(byte[] b) {
    // null terminates
    int count = 0;
    while (count < b.length) {
      if (b[count] == 0)
        break;
      count++; // dont include the terminating 0
    }

    // copy if its small
    if (count < b.length / 2) {
      byte[] bb = new byte[count];
      System.arraycopy(b, 0, bb, 0, count);
      b = bb;
    }
    return new String(b, 0, count, StandardCharsets.UTF_8); // all strings are considered to be UTF-8 unicode.
  }

  // follow what happens in the Java side
  private String makeAttString(byte[] b) throws IOException {
    // null terminates
    int count = 0;
    while (count < b.length) {
      if (b[count] == 0)
        break;
      count++; // dont include the terminating 0
    }
    return new String(b, 0, count, StandardCharsets.UTF_8); // all strings are considered to be UTF-8 unicode.
  }

  private List<Attribute> makeAttributes(int grpid, int varid, int natts, Variable.Builder v) throws IOException {
    List<Attribute> result = new ArrayList<>(natts);

    for (int attnum = 0; attnum < natts; attnum++) {
      byte[] name = new byte[Nc4prototypes.NC_MAX_NAME + 1];
      int ret = nc4.nc_inq_attname(grpid, varid, attnum, name);
      if (ret != 0) {
        throw new IOException(nc4.nc_strerror(ret) + " varid=" + varid + " attnum=" + attnum);
      }
      String attname = makeString(name);
      IntByReference xtypep = new IntByReference();
      ret = nc4.nc_inq_atttype(grpid, varid, attname, xtypep);
      if (ret != 0) {
        throw new IOException(nc4.nc_strerror(ret) + " varid=" + varid + "attnum=" + attnum);
      }

      /*
       * xtypep : Pointer to location for returned attribute type,
       * one of the set of predefined netCDF external data types.
       * The type of this parameter, nc_type, is defined in the netCDF
       * header file. The valid netCDF external data types are
       * NC_BYTE, NC_CHAR, NC_SHORT, NC_INT, NC_FLOAT, and NC_DOUBLE.
       * If this parameter is given as '0' (a null pointer), no type
       * will be returned so no variable to hold the type needs to be declared.
       */
      int type = xtypep.getValue();
      SizeTByReference lenp = new SizeTByReference();
      ret = nc4.nc_inq_attlen(grpid, varid, attname, lenp);
      if (ret != 0) {
        throw new IOException(ret + ": " + nc4.nc_strerror(ret));
      }
      int len = lenp.getValue().intValue();

      // deal with empty attributes
      if (len == 0) {
        Attribute att;
        switch (type) {
          case Nc4prototypes.NC_BYTE:
            att = Attribute.emptyValued(attname, DataType.BYTE);
            break;
          case Nc4prototypes.NC_UBYTE:
            att = Attribute.emptyValued(attname, DataType.UBYTE);
            break;
          case Nc4prototypes.NC_CHAR:
            // From what I can tell, the way we should treat char attrs depends on
            // the netCDF format used (3 vs. 4)
            if ((format == NC_FORMAT_NETCDF4_CLASSIC) || (format == NC_FORMAT_NETCDF4)) {
              // if netcdf4, make null char attrs null string attrs
              att = Attribute.emptyValued(attname, DataType.STRING);
            } else {
              // all others, treat null char attrs as empty string attrs
              att = new Attribute(attname, "");
            }
            break;
          case Nc4prototypes.NC_DOUBLE:
            att = Attribute.emptyValued(attname, DataType.DOUBLE);
            break;
          case Nc4prototypes.NC_FLOAT:
            att = Attribute.emptyValued(attname, DataType.FLOAT);
            break;
          case Nc4prototypes.NC_INT:
            att = Attribute.emptyValued(attname, DataType.INT);
            break;
          case Nc4prototypes.NC_UINT:
            att = Attribute.emptyValued(attname, DataType.UINT);
            break;
          case Nc4prototypes.NC_UINT64:
            att = Attribute.emptyValued(attname, DataType.ULONG);
            break;
          case Nc4prototypes.NC_INT64:
            att = Attribute.emptyValued(attname, DataType.LONG);
            break;
          case Nc4prototypes.NC_USHORT:
            att = Attribute.emptyValued(attname, DataType.USHORT);
            break;
          case Nc4prototypes.NC_SHORT:
            att = Attribute.emptyValued(attname, DataType.SHORT);
            break;
          case Nc4prototypes.NC_STRING:
            att = Attribute.emptyValued(attname, DataType.STRING);
            break;
          default:
            log.warn("Unsupported attribute data type == " + type);
            continue;
        }
        result.add(att);
        continue; // avoid reading the values since there are none.
      }

      // read the att values
      Array values = null;

      switch (type) {

        case Nc4prototypes.NC_UBYTE:
          byte[] valbu = new byte[len];
          ret = nc4.nc_get_att_uchar(grpid, varid, attname, valbu);
          if (ret != 0)
            throw new IOException(ret + ": " + nc4.nc_strerror(ret));
          values = Array.factory(DataType.UBYTE, new int[] {len}, valbu);
          break;

        case Nc4prototypes.NC_BYTE:
          byte[] valb = new byte[len];
          ret = nc4.nc_get_att_schar(grpid, varid, attname, valb);
          if (ret != 0)
            throw new IOException(ret + ": " + nc4.nc_strerror(ret));
          values = Array.factory(DataType.BYTE, new int[] {len}, valb);
          break;

        case Nc4prototypes.NC_CHAR:
          byte[] text = new byte[len];
          ret = nc4.nc_get_att_text(grpid, varid, attname, text);
          if (ret != 0)
            throw new IOException(ret + ": " + nc4.nc_strerror(ret));
          Attribute att = new Attribute(attname, makeAttString(text));
          result.add(att);
          break;

        case Nc4prototypes.NC_DOUBLE:
          double[] vald = new double[len];
          ret = nc4.nc_get_att_double(grpid, varid, attname, vald);
          if (ret != 0)
            throw new IOException(ret + ": " + nc4.nc_strerror(ret));
          values = Array.factory(DataType.DOUBLE, new int[] {len}, vald);
          break;

        case Nc4prototypes.NC_FLOAT:
          float[] valf = new float[len];
          ret = nc4.nc_get_att_float(grpid, varid, attname, valf);
          if (ret != 0)
            throw new IOException(ret + ": " + nc4.nc_strerror(ret));
          values = Array.factory(DataType.FLOAT, new int[] {len}, valf);
          break;

        case Nc4prototypes.NC_UINT:
          int[] valiu = new int[len];
          ret = nc4.nc_get_att_uint(grpid, varid, attname, valiu);
          if (ret != 0)
            throw new IOException(ret + ": " + nc4.nc_strerror(ret));
          values = Array.factory(DataType.UINT, new int[] {len}, valiu);
          break;

        case Nc4prototypes.NC_INT:
          int[] vali = new int[len];
          ret = nc4.nc_get_att_int(grpid, varid, attname, vali);
          if (ret != 0)
            throw new IOException(ret + ": " + nc4.nc_strerror(ret));
          values = Array.factory(DataType.INT, new int[] {len}, vali);
          break;

        case Nc4prototypes.NC_UINT64:
          long[] vallu = new long[len];
          ret = nc4.nc_get_att_ulonglong(grpid, varid, attname, vallu);
          if (ret != 0)
            throw new IOException(ret + ": " + nc4.nc_strerror(ret));
          values = Array.factory(DataType.ULONG, new int[] {len}, vallu);
          break;

        case Nc4prototypes.NC_INT64:
          long[] vall = new long[len];
          ret = nc4.nc_get_att_longlong(grpid, varid, attname, vall);
          if (ret != 0)
            throw new IOException(ret + ": " + nc4.nc_strerror(ret));
          values = Array.factory(DataType.LONG, new int[] {len}, vall);
          break;

        case Nc4prototypes.NC_USHORT:
          short[] valsu = new short[len];
          ret = nc4.nc_get_att_ushort(grpid, varid, attname, valsu);
          if (ret != 0)
            throw new IOException(ret + ": " + nc4.nc_strerror(ret));
          values = Array.factory(DataType.USHORT, new int[] {len}, valsu);
          break;

        case Nc4prototypes.NC_SHORT:
          short[] vals = new short[len];
          ret = nc4.nc_get_att_short(grpid, varid, attname, vals);
          if (ret != 0)
            throw new IOException(ret + ": " + nc4.nc_strerror(ret));
          values = Array.factory(DataType.SHORT, new int[] {len}, vals);
          break;

        case Nc4prototypes.NC_STRING:
          String[] valss = new String[len];
          ret = nc4.nc_get_att_string(grpid, varid, attname, valss);
          if (ret != 0)
            throw new IOException(ret + ": " + nc4.nc_strerror(ret));
          if (transcodeStrings) {
            valss = transcodeString(valss);
          }
          values = Array.factory(DataType.STRING, new int[] {len}, valss);
          break;

        default:
          UserType userType = userTypes.get(type);
          if (userType == null) {
            log.warn("Unsupported attribute data type == " + type);
            continue;
          } else if (userType.typeClass == Nc4prototypes.NC_ENUM) {
            result.add(readEnumAttValues(grpid, varid, attname, len, userType));
            continue;
          } else if (userType.typeClass == Nc4prototypes.NC_OPAQUE) {
            result.add(readOpaqueAttValues(grpid, varid, attname, len, userType));
            continue;
          } else if (userType.typeClass == Nc4prototypes.NC_VLEN) {
            values = readVlenAttValues(grpid, varid, attname, len, userType);
          } else if (userType.typeClass == Nc4prototypes.NC_COMPOUND) {
            readCompoundAttValues(grpid, varid, attname, len, userType, result, v);
            continue;
          } else {
            log.warn("Unsupported attribute data type == " + userType);
            continue;
          }
      }
      if (values != null) {
        Attribute att = Attribute.fromArray(attname, values);
        result.add(att);
      }
    }
    return result;
  }

  private Array readVlenAttValues(int grpid, int varid, String attname, int len, UserType userType) throws IOException {
    Nc4prototypes.Vlen_t[] vlen = new Nc4prototypes.Vlen_t[len];
    int ret = nc4.nc_get_att(grpid, varid, attname, vlen); // vlen
    if (ret != 0)
      throw new IOException(ret + ": " + nc4.nc_strerror(ret));

    int count = 0;
    for (int i = 0; i < len; i++)
      count += vlen[i].len;

    switch (userType.baseTypeid) {
      case Nc4prototypes.NC_INT:
        Array intArray = Array.factory(DataType.INT, new int[] {count});
        IndexIterator iter = intArray.getIndexIterator();
        for (int i = 0; i < len; i++) {
          // Coverity[FB.UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD]
          int[] ba = vlen[i].p.getIntArray(0, vlen[i].len);
          for (int aBa : ba) {
            iter.setIntNext(aBa);
          }
        }
        return intArray;

      case Nc4prototypes.NC_FLOAT:
        Array fArray = Array.factory(DataType.FLOAT, new int[] {count});
        iter = fArray.getIndexIterator();
        for (int i = 0; i < len; i++) {
          // Coverity[FB.NP_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD]
          float[] ba = vlen[i].p.getFloatArray(0, vlen[i].len);
          for (float aBa : ba)
            iter.setFloatNext(aBa);
        }
        return fArray;
    }
    return null;
  }

  private Attribute readEnumAttValues(int grpid, int varid, String attname, int len, UserType userType)
      throws IOException {
    int ret;

    DataType dtype = convertDataType(userType.baseTypeid).dt;
    int elemSize = dtype.getSize();

    byte[] bbuff = new byte[len * elemSize];
    ret = nc4.nc_get_att(grpid, varid, attname, bbuff);
    if (ret != 0)
      throw new IOException(ret + ": " + nc4.nc_strerror(ret));

    ByteBuffer bb = ByteBuffer.wrap(bbuff);
    Array data = null;
    if (false) {
      /*
       * This is incorrect; CDM technically does not support
       * enum valued attributes (see Attribute.java).
       */
      data = convertByteBuffer(bb, userType.baseTypeid, new int[] {len});
    } else {
      /*
       * So, instead use the EnumTypedef to convert to econsts
       * and store as strings.
       */
      String[] econsts = new String[len];
      EnumTypedef en = userType.e;
      for (int i = 0; i < len; i++) {
        long lval = 0;
        switch (en.getBaseType()) {
          case ENUM1:
            lval = bb.get(i);
            break;
          case ENUM2:
            lval = bb.getShort(i);
            break;
          case ENUM4:
            lval = bb.getInt(i);
            break;
        }
        int ival = (int) lval;
        String name = en.lookupEnumString(ival);
        if (name == null) {
          name = "Unknown enum value=" + ival;
        }
        econsts[i] = name;
      }
      data = Array.factory(DataType.STRING, new int[] {len}, econsts);
    }
    return Attribute.builder(attname).setValues(data).setEnumType(userType.e).build();
  }

  private Array convertByteBuffer(ByteBuffer bb, int baseType, int[] shape) {

    switch (baseType) {
      case Nc4prototypes.NC_BYTE:
        return Array.factory(DataType.BYTE, shape, bb.array());
      case Nc4prototypes.NC_UBYTE:
        return Array.factory(DataType.UBYTE, shape, bb.array());

      case Nc4prototypes.NC_SHORT:
        return Array.factory(DataType.SHORT, shape, bb.asShortBuffer().array());
      case Nc4prototypes.NC_USHORT:
        return Array.factory(DataType.USHORT, shape, bb.asShortBuffer().array());

      case Nc4prototypes.NC_INT:
        return Array.factory(DataType.INT, shape, bb.asIntBuffer().array());
      case Nc4prototypes.NC_UINT:
        return Array.factory(DataType.UINT, shape, bb.asIntBuffer().array());

      case Nc4prototypes.NC_INT64:
        return Array.factory(DataType.LONG, shape, bb.asLongBuffer().array());
      case Nc4prototypes.NC_UINT64:
        return Array.factory(DataType.ULONG, shape, bb.asLongBuffer().array());
    }
    throw new IllegalArgumentException("Illegal type=" + baseType);
  }

  private Attribute readOpaqueAttValues(int grpid, int varid, String attname, int len, UserType userType)
      throws IOException {
    int total = len * userType.size;
    byte[] bb = new byte[total];
    int ret = nc4.nc_get_att(grpid, varid, attname, bb);
    if (ret != 0)
      throw new IOException(ret + ": " + nc4.nc_strerror(ret));
    return Attribute.fromArray(attname, Array.factory(DataType.BYTE, new int[] {total}, bb));
  }

  private void readCompoundAttValues(int grpid, int varid, String attname, int len, UserType userType,
      List<Attribute> result, Variable.Builder<?> v) throws IOException {

    int buffSize = len * userType.size;
    byte[] bb = new byte[buffSize];
    int ret = nc4.nc_get_att(grpid, varid, attname, bb);
    if (ret != 0)
      throw new IOException(ret + ": " + nc4.nc_strerror(ret));

    ByteBuffer bbuff = ByteBuffer.wrap(bb);
    decodeCompoundData(len, userType, bbuff);

    // if its a Structure, distribute to matching fields
    if (v instanceof Structure.Builder) {
      Structure.Builder<?> s = (Structure.Builder<?>) v;
      for (Field fld : userType.flds) {
        Variable.Builder<?> mv = s.findMemberVariable(fld.name).orElse(null);
        if (mv != null)
          mv.addAttribute(Attribute.fromArray(attname, fld.data));
        else
          result.add(Attribute.fromArray(attname + "." + fld.name, fld.data));
      }
    } else {
      for (Field fld : userType.flds)
        result.add(Attribute.fromArray(attname + "." + fld.name, fld.data));
    }
  }

  // LOOK: placing results in the fld of the userType - ok for production ??
  private void decodeCompoundData(int len, UserType userType, ByteBuffer bbuff) throws IOException {
    bbuff.order(ByteOrder.LITTLE_ENDIAN);

    for (Field fld : userType.flds) {
      ConvertedType ct = convertDataType(fld.fldtypeid);
      if (fld.fldtypeid == Nc4prototypes.NC_CHAR) {
        fld.data = Array.factory(DataType.STRING, new int[] {len}); // LOOK ??
      } else if (ct.isVlen) {
        // fld.data = Array.makeObjectArray(ct.dt, ct.dt.getPrimitiveClassType(), new int[]{len}, null);
        // fld.data = Array.makeVlenArray(ct.dt, ct.dt.getPrimitiveClassType(), new int[]{len}, null); LOOK BAD
      } else {
        fld.data = Array.factory(ct.dt, new int[] {len});
      }
    }

    for (int i = 0; i < len; i++) {
      int record_start = i * userType.size;

      for (Field fld : userType.flds) {
        int pos = record_start + fld.offset;

        switch (fld.fldtypeid) {
          case Nc4prototypes.NC_CHAR:
            // copy bytes out of buffer, make into a String object
            int blen = 1;
            if (fld.dims != null) {
              Section s = new Section(fld.dims);
              blen = (int) s.computeSize();
            }
            byte[] dst = new byte[blen];
            bbuff.get(dst, 0, blen);

            String cval = makeAttString(dst);
            fld.data.setObject(i, cval);
            if (debugCompoundAtt)
              System.out.println("result= " + cval);
            continue;

          case Nc4prototypes.NC_UBYTE:
          case Nc4prototypes.NC_BYTE:
            byte bval = bbuff.get(pos);
            if (debugCompoundAtt)
              System.out.println("bval= " + bval);
            fld.data.setByte(i, bval);
            continue;

          case Nc4prototypes.NC_USHORT:
          case Nc4prototypes.NC_SHORT:
            short sval = bbuff.getShort(pos);
            if (debugCompoundAtt)
              System.out.println("sval= " + sval);
            fld.data.setShort(i, sval);
            continue;

          case Nc4prototypes.NC_UINT:
          case Nc4prototypes.NC_INT:
            int ival = bbuff.getInt(pos);
            if (debugCompoundAtt)
              System.out.println("ival= " + ival);
            fld.data.setInt(i, ival);
            continue;

          case Nc4prototypes.NC_UINT64:
          case Nc4prototypes.NC_INT64:
            long lval = bbuff.getLong(pos);
            if (debugCompoundAtt)
              System.out.println("lval= " + lval);
            fld.data.setLong(i, lval);
            continue;

          case Nc4prototypes.NC_FLOAT:
            float fval = bbuff.getFloat(pos);
            if (debugCompoundAtt)
              System.out.println("fval= " + fval);
            fld.data.setFloat(i, fval);
            continue;

          case Nc4prototypes.NC_DOUBLE:
            double dval = bbuff.getDouble(pos);
            if (debugCompoundAtt)
              System.out.println("dval= " + dval);
            fld.data.setDouble(i, dval);
            continue;

          case Nc4prototypes.NC_STRING:
            lval = getNativeAddr(pos, bbuff);
            Pointer p = new Pointer(lval);
            String strval = p.getString(0, CDM.UTF8);
            fld.data.setObject(i, strval);
            if (debugCompoundAtt)
              System.out.println("result= " + strval);
            continue;

          default:
            UserType subUserType = userTypes.get(fld.fldtypeid);
            if (subUserType == null) {
              throw new IOException("Unknown compound user type == " + fld);
            } else if (subUserType.typeClass == Nc4prototypes.NC_ENUM) {
              // WTF ?
            } else if (subUserType.typeClass == Nc4prototypes.NC_VLEN) {
              decodeVlenField(fld, subUserType, pos, i, bbuff);
              break;
            } else if (subUserType.typeClass == Nc4prototypes.NC_OPAQUE) {
              // return readOpaque(grpid, varid, len, userType.size);
            } else if (subUserType.typeClass == Nc4prototypes.NC_COMPOUND) {
              // return readCompound(grpid, varid, len, userType);
            }

            log.warn("UNSUPPORTED compound fld.fldtypeid= " + fld.fldtypeid);
        } // switch on fld type
      } // loop over fields
    } // loop over len
  }

  private void decodeVlenField(Field fld, UserType userType, int pos, int idx, ByteBuffer bbuff) throws IOException {
    ConvertedType cvt = convertDataType(userType.baseTypeid);
    Array array = decodeVlen(cvt.dt, pos, bbuff);
    fld.data.setObject(idx, array);
  }

  private Array decodeVlen(DataType dt, int pos, ByteBuffer bbuff) throws IOException {
    Array array;
    int n = (int) bbuff.getLong(pos); // Note that this does not increment the buffer position
    long addr = getNativeAddr(pos + com.sun.jna.Native.POINTER_SIZE, bbuff);
    Pointer p = new Pointer(addr);
    Object data;
    switch (dt) {
      case BOOLEAN: /* byte[] */
      case ENUM1:
      case BYTE:
        data = p.getByteArray(0, n);
        break;
      case ENUM2:
      case SHORT: /* short[] */
        data = p.getShortArray(0, n);
        break;
      case ENUM4:
      case INT: /* int[] */
        data = p.getIntArray(0, n);
        break;
      case LONG: /* long[] */
        data = p.getLongArray(0, n);
        break;
      case FLOAT: /* float[] */
        data = p.getFloatArray(0, n);
        break;
      case DOUBLE: /* double[] */
        data = p.getDoubleArray(0, n);
        break;
      case CHAR: /* char[] */
        data = p.getCharArray(0, n);
        break;
      case STRING: /* String[] */
        // For now we need to use p.getString()
        // because p.getStringArray(int,int) does not exist
        // in jna version 3.0.9, but does exist in
        // verssion 4.0 and possibly some intermediate versions
        String[] stringdata = new String[n];
        for (int i = 0; i < n; i++)
          stringdata[i] = p.getString(i * 8);
        data = stringdata;
        break;
      case OPAQUE:
      case STRUCTURE:
      default:
        throw new IllegalStateException();
    }
    array = Array.factory(dt, new int[] {n}, data);
    return array;
  }

  private void makeVariables(Group4 g4) throws IOException {

    IntByReference nvarsp = new IntByReference();
    int ret = nc4.nc_inq_nvars(g4.grpid, nvarsp);
    if (ret != 0)
      throw new IOException(ret + ": " + nc4.nc_strerror(ret));
    int nvars = nvarsp.getValue();
    log.debug("nvars= {}", nvars);

    int[] varids = new int[nvars];
    ret = nc4.nc_inq_varids(g4.grpid, nvarsp, varids);
    if (ret != 0)
      throw new IOException(ret + ": " + nc4.nc_strerror(ret));

    for (int i = 0; i < varids.length; i++) {
      int varno = varids[i];
      if (varno != i)
        log.error("makeVariables varno={} is not equal to {}", varno, i);

      byte[] name = new byte[Nc4prototypes.NC_MAX_NAME + 1];
      IntByReference xtypep = new IntByReference();
      IntByReference ndimsp = new IntByReference();
      int[] dimids = new int[Nc4prototypes.NC_MAX_DIMS];
      IntByReference nattsp = new IntByReference();

      ret = nc4.nc_inq_var(g4.grpid, varno, name, xtypep, ndimsp, dimids, nattsp);
      if (ret != 0)
        throw new IOException(nc4.nc_strerror(ret));

      // figure out the datatype
      int typeid = xtypep.getValue();
      // DataType dtype = convertDataType(typeid).dt;

      String vname = makeString(name);
      Vinfo vinfo = new Vinfo(g4, varno, typeid);

      // figure out the dimensions
      String dimList = makeDimList(g4.grpid, ndimsp.getValue(), dimids);
      UserType utype = userTypes.get(typeid);
      if (utype != null) {
        // Coverity[FB.URF_UNREAD_FIELD]
        vinfo.utype = utype;
        if (utype.typeClass == Nc4prototypes.NC_VLEN) // LOOK ??
          dimList = dimList + " *";
      }

      Variable.Builder<?> v = makeVariable(g4.g, vname, typeid, dimList);
      /*
       * if(dtype != DataType.STRUCTURE) {
       * v = new Variable(ncfile, g, null, vname, dtype, dimList);
       * } else if(utype != null) {
       * Structure s = new Structure(ncfile, g, null, vname);
       * s.setDimensions(dimList);
       * v = s;
       * if(utype.flds == null)
       * utype.readFields();
       * for(Field f : utype.flds) {
       * s.addMemberVariable(f.makeMemberVariable(g, s));
       * }
       * } else {
       * throw new IllegalStateException("Dunno what to with " + dtype);
       * }
       */

      // create the Variable
      g4.g.addVariable(v);
      v.setSPobject(vinfo);

      // if (isUnsigned(typeid))
      // v.addAttribute(new Attribute(CDM.UNSIGNED, "true"));

      // read Variable attributes
      List<Attribute> atts = makeAttributes(g4.grpid, varno, nattsp.getValue(), v);
      for (Attribute att : atts) {
        v.addAttribute(att);
      }

      log.debug("added Variable {}", v);
    }
  }

  private Variable.Builder<?> makeVariable(Group.Builder g, String vname, int typeid, String dimList)
      throws IOException {
    ConvertedType cvttype = convertDataType(typeid);
    DataType dtype = cvttype.dt;
    UserType utype = userTypes.get(typeid);

    Variable.Builder<?> v;
    if (dtype != DataType.STRUCTURE) {
      v = Variable.builder().setName(vname).setDataType(dtype).setParentGroupBuilder(g).setDimensionsByName(dimList);
    } else if (utype != null) {
      Structure.Builder<?> s = Structure.builder().setName(vname).setParentGroupBuilder(g).setDimensionsByName(dimList);
      v = s;
      if (utype.flds == null)
        utype.readFields();
      // Coverity[FORWARD_NULL]
      for (Field f : utype.flds) {
        s.addMemberVariable(f.makeMemberVariable(g));
      }
    } else {
      throw new IllegalStateException("Structure with no userType " + dtype);
    }

    if (dtype.isEnum()) {
      v.setEnumTypeName(utype.name);
    } else if (dtype == DataType.OPAQUE) {
      if (this.markReserved) {
        annotate(v, UCARTAGOPAQUE, utype.size);
      }
    }

    return v;
  }

  private String makeDimList(int grpid, int ndimsp, int[] dims) throws IOException {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < ndimsp; i++) {
      byte[] name = new byte[Nc4prototypes.NC_MAX_NAME + 1];
      int ret = nc4.nc_inq_dimname(grpid, dims[i], name);
      if (ret != 0)
        throw new IOException(ret + ": " + nc4.nc_strerror(ret));
      String dname = makeString(name);
      sb.append(dname);
      sb.append(" ");
    }
    return sb.toString();
  }

  private boolean nc_inq_var(Formatter f, int grpid, int varno) throws IOException {
    byte[] name = new byte[Nc4prototypes.NC_MAX_NAME + 1];
    IntByReference xtypep = new IntByReference();
    IntByReference ndimsp = new IntByReference();
    int[] dimids = new int[Nc4prototypes.NC_MAX_DIMS];
    IntByReference nattsp = new IntByReference();

    int ret = nc4.nc_inq_var(grpid, varno, name, xtypep, ndimsp, dimids, nattsp);
    if (ret != 0)
      return false;

    String vname = makeString(name);
    int typeid = xtypep.getValue();
    ConvertedType cvt = convertDataType(typeid);

    for (int i = 0; i < ndimsp.getValue(); i++) {
      f.format("%d ", dimids[i]);
    }

    String dimList = makeDimList(grpid, ndimsp.getValue(), dimids);

    f.format(") dims=(%s)%n", dimList);
    return true;
  }

  private String nc_inq_var_name(int grpid, int varno) throws IOException {
    byte[] name = new byte[Nc4prototypes.NC_MAX_NAME + 1];
    IntByReference xtypep = new IntByReference();
    IntByReference ndimsp = new IntByReference();
    IntByReference nattsp = new IntByReference();

    int ret = nc4.nc_inq_var(grpid, varno, name, xtypep, ndimsp, null, nattsp);
    if (ret != 0) {
      throw new IOException("nc_inq_var faild: code=" + ret);
    }
    return makeString(name);
  }

  private void makeUserTypes(int grpid, Group.Builder g) throws IOException {
    // find user types in this group
    IntByReference ntypesp = new IntByReference();
    int ret = nc4.nc_inq_typeids(grpid, ntypesp, null);
    if (ret != 0)
      throw new IOException(ret + ": " + nc4.nc_strerror(ret));
    int ntypes = ntypesp.getValue();
    if (ntypes == 0)
      return;
    int[] xtypes = new int[ntypes];
    ret = nc4.nc_inq_typeids(grpid, ntypesp, xtypes);
    if (ret != 0)
      throw new IOException(ret + ": " + nc4.nc_strerror(ret));

    // for each defined "user type", get information, store in Map
    for (int typeid : xtypes) {
      byte[] nameb = new byte[Nc4prototypes.NC_MAX_NAME + 1];
      SizeTByReference sizep = new SizeTByReference();
      IntByReference baseType = new IntByReference();
      SizeTByReference nfieldsp = new SizeTByReference();
      IntByReference classp = new IntByReference();

      // ncid The ncid for the group containing the user defined type.
      // xtype The typeid for this type, as returned by nc_def_compound, nc_def_opaque, nc_def_enum, nc_def_vlen, or
      // nc_inq_var.
      // name If non-NULL, the name of the user defined type will be copied here. It will be NC_MAX_NAME bytes or less.
      // sizep If non-NULL, the (in-memory) size of the type in bytes will be copied here. VLEN type size is the size of
      // nc_vlen_t.
      // String size is returned as the size of a character pointer. The size may be used to malloc space for the data,
      // no matter what the type.
      // nfieldsp If non-NULL, the number of fields will be copied here for enum and compound types.
      // classp Return the class of the user defined type, NC_VLEN, NC_OPAQUE, NC_ENUM, or NC_COMPOUND.
      ret = nc4.nc_inq_user_type(grpid, typeid, nameb, sizep, baseType, nfieldsp, classp); // size_t
      if (ret != 0)
        throw new IOException(ret + ": " + nc4.nc_strerror(ret));

      String name = makeString(nameb);
      int utype = classp.getValue();
      log.debug("user type id={} name={} size={} baseType={} nfields={} class={}", typeid, name,
          sizep.getValue().longValue(), baseType.getValue(), nfieldsp.getValue().longValue(), utype);

      UserType ut = new UserType(grpid, typeid, name, sizep.getValue().longValue(), baseType.getValue(),
          nfieldsp.getValue().longValue(), utype);
      userTypes.put(typeid, ut);

      if (utype == Nc4prototypes.NC_ENUM) {
        Map<Integer, String> map = makeEnum(grpid, typeid);
        ut.e = new EnumTypedef(name, map, ut.getEnumBaseType());
        g.addEnumTypedef(ut.e);

      } else if (utype == Nc4prototypes.NC_OPAQUE) {
        byte[] nameo = new byte[Nc4prototypes.NC_MAX_NAME + 1];
        SizeTByReference sizep2 = new SizeTByReference();
        ret = nc4.nc_inq_opaque(grpid, typeid, nameo, sizep2);
        if (ret != 0)
          throw new IOException(ret + ": " + nc4.nc_strerror(ret));
        ut.setSize(sizep2.getValue().intValue());
        // doesnt seem to be any new info
        // String nameos = makeString(nameo);
      }
    }
  }

  private Map<Integer, String> makeEnum(int grpid, int xtype) throws IOException {
    byte[] nameb = new byte[Nc4prototypes.NC_MAX_NAME + 1];
    IntByReference baseType = new IntByReference();
    SizeTByReference baseSize = new SizeTByReference();
    SizeTByReference numMembers = new SizeTByReference();

    int ret = nc4.nc_inq_enum(grpid, xtype, nameb, baseType, baseSize, numMembers);
    if (ret != 0)
      throw new IOException(ret + ": " + nc4.nc_strerror(ret));
    int nmembers = numMembers.getValue().intValue();

    Map<Integer, String> map = new HashMap<>(2 * nmembers);

    for (int i = 0; i < nmembers; i++) {
      byte[] mnameb = new byte[Nc4prototypes.NC_MAX_NAME + 1];
      IntByReference value = new IntByReference();
      ret = nc4.nc_inq_enum_member(grpid, xtype, i, mnameb, value); // void *
      if (ret != 0)
        throw new IOException(ret + ": " + nc4.nc_strerror(ret));

      String mname = makeString(mnameb);
      map.put(value.getValue(), mname);
    }
    return map;
  }

  /////////////////////////////////////////////////////////////////////////////////
  // Data reading

  @Override
  public Array readData(Variable v2, Section section) throws IOException, InvalidRangeException {
    Vinfo vinfo = (Vinfo) v2.getSPobject();
    int vlen = (int) v2.getSize();
    int len = (int) section.computeSize();
    if (vlen == len) { // entire array
      return readDataAll(vinfo.g4.grpid, vinfo.varid, vinfo.typeid, v2.getShapeAsSection());
    }

    // if(!section.isStrided()) // optimisation for unstrided section
    // return readUnstrided(vinfo.grpid, vinfo.varid, vinfo.typeid, section);

    return readDataSection(vinfo.g4.grpid, vinfo.varid, vinfo.typeid, section);
  }

  Array readDataSection(int grpid, int varid, int typeid, Section section) throws IOException, InvalidRangeException {
    // general sectioning with strides
    SizeT[] origin = convertSizeT(section.getOrigin());
    SizeT[] shape = convertSizeT(section.getShape());
    SizeT[] stride = convertSizeT(section.getStride());

    boolean isUnsigned = isUnsigned(typeid);
    int len = (int) section.computeSize();
    Array values;

    switch (typeid) {
      // int nc_get_vars_schar(int ncid, int varid, long[] startp, long[] countp, int[] stridep, byte[] ip);

      case Nc4prototypes.NC_BYTE:
      case Nc4prototypes.NC_UBYTE:
        byte[] valb = new byte[len];
        int ret;
        ret = isUnsigned ? nc4.nc_get_vars_uchar(grpid, varid, origin, shape, stride, valb)
            : nc4.nc_get_vars_schar(grpid, varid, origin, shape, stride, valb);
        if (ret != 0)
          throw new IOException(ret + ": " + nc4.nc_strerror(ret));
        values = Array.factory(DataType.BYTE, section.getShape(), valb);
        break;

      case Nc4prototypes.NC_CHAR:
        byte[] valc = new byte[len];
        ret = nc4.nc_get_vars_text(grpid, varid, origin, shape, stride, valc);
        if (ret != 0)
          throw new IOException(ret + ": " + nc4.nc_strerror(ret));
        values = Array.factory(DataType.CHAR, section.getShape(), IospHelper.convertByteToChar(valc));
        break;

      case Nc4prototypes.NC_DOUBLE:
        double[] vald = new double[len];
        ret = nc4.nc_get_vars_double(grpid, varid, origin, shape, stride, vald);
        if (ret != 0)
          throw new IOException(ret + ": " + nc4.nc_strerror(ret));
        values = Array.factory(DataType.DOUBLE, section.getShape(), vald);
        break;

      case Nc4prototypes.NC_FLOAT:
        float[] valf = new float[len];
        ret = nc4.nc_get_vars_float(grpid, varid, origin, shape, stride, valf);
        if (ret != 0)
          throw new IOException(ret + ": " + nc4.nc_strerror(ret));
        values = Array.factory(DataType.FLOAT, section.getShape(), valf);
        break;

      case Nc4prototypes.NC_INT:
      case Nc4prototypes.NC_UINT:
        int[] vali = new int[len];

        ret = isUnsigned ? nc4.nc_get_vars_uint(grpid, varid, origin, shape, stride, vali)
            : nc4.nc_get_vars_int(grpid, varid, origin, shape, stride, vali);
        if (ret != 0)
          throw new IOException(ret + ": " + nc4.nc_strerror(ret));
        values = Array.factory(DataType.INT, section.getShape(), vali);
        break;

      case Nc4prototypes.NC_INT64:
      case Nc4prototypes.NC_UINT64:
        long[] vall = new long[len];
        ret = isUnsigned ? nc4.nc_get_vars_ulonglong(grpid, varid, origin, shape, stride, vall)
            : nc4.nc_get_vars_longlong(grpid, varid, origin, shape, stride, vall);
        if (ret != 0)
          throw new IOException(ret + ": " + nc4.nc_strerror(ret));
        values = Array.factory(DataType.LONG, section.getShape(), vall);
        break;

      case Nc4prototypes.NC_SHORT:
      case Nc4prototypes.NC_USHORT:
        short[] vals = new short[len];
        ret = isUnsigned ? nc4.nc_get_vars_ushort(grpid, varid, origin, shape, stride, vals)
            : nc4.nc_get_vars_short(grpid, varid, origin, shape, stride, vals);
        if (ret != 0)
          throw new IOException(ret + ": " + nc4.nc_strerror(ret));
        values = Array.factory(DataType.SHORT, section.getShape(), vals);
        break;

      case Nc4prototypes.NC_STRING:
        String[] valss = new String[len];
        ret = nc4.nc_get_vars_string(grpid, varid, origin, shape, stride, valss);
        if (ret != 0)
          throw new IOException(ret + ": " + nc4.nc_strerror(ret));
        if (transcodeStrings) {
          valss = transcodeString(valss);
        }
        return Array.factory(DataType.STRING, section.getShape(), valss);

      default:
        UserType userType = userTypes.get(typeid);
        if (userType == null) {
          throw new IOException("Unknown userType == " + typeid);
        } else if (userType.typeClass == Nc4prototypes.NC_ENUM) {
          return readDataSection(grpid, varid, userType.baseTypeid, section);
        } else if (userType.typeClass == Nc4prototypes.NC_VLEN) { // cannot subset
          return readVlen(grpid, varid, userType, section);
        } else if (userType.typeClass == Nc4prototypes.NC_OPAQUE) {
          return readOpaque(grpid, varid, section, userType.size);
        } else if (userType.typeClass == Nc4prototypes.NC_COMPOUND) {
          return readCompound(grpid, varid, section, userType);
        }
        throw new IOException("Unsupported userType = " + typeid + " userType= " + userType);
    }
    return values;
  }

  // read entire array
  private Array readDataAll(int grpid, int varid, int typeid, Section section)
      throws IOException, InvalidRangeException {
    int ret;
    int len = (int) section.computeSize();
    int[] shape = section.getShape();

    switch (typeid) {

      case Nc4prototypes.NC_UBYTE:
        byte[] valbu = new byte[len];
        ret = nc4.nc_get_var_ubyte(grpid, varid, valbu);
        if (ret != 0)
          throw new IOException(ret + ": " + nc4.nc_strerror(ret));
        return Array.factory(DataType.UBYTE, shape, valbu);

      case Nc4prototypes.NC_BYTE:
        byte[] valb = new byte[len];
        ret = nc4.nc_get_var_schar(grpid, varid, valb);
        if (ret != 0)
          throw new IOException(ret + ": " + nc4.nc_strerror(ret));
        return Array.factory(DataType.BYTE, shape, valb);

      case Nc4prototypes.NC_CHAR:
        byte[] valc = new byte[len];
        ret = nc4.nc_get_var_text(grpid, varid, valc);
        if (ret != 0)
          throw new IOException(ret + ": " + nc4.nc_strerror(ret));
        char[] cvals = IospHelper.convertByteToChar(valc);
        return Array.factory(DataType.CHAR, shape, cvals);

      case Nc4prototypes.NC_DOUBLE:
        double[] vald = new double[len];
        ret = nc4.nc_get_var_double(grpid, varid, vald);
        if (ret != 0)
          throw new IOException(ret + ": " + nc4.nc_strerror(ret));
        return Array.factory(DataType.DOUBLE, shape, vald);

      case Nc4prototypes.NC_FLOAT:
        float[] valf = new float[len];
        ret = nc4.nc_get_var_float(grpid, varid, valf);
        if (ret != 0)
          throw new IOException(ret + ": " + nc4.nc_strerror(ret));
        return Array.factory(DataType.FLOAT, shape, valf);

      case Nc4prototypes.NC_INT:
        int[] vali = new int[len];
        ret = nc4.nc_get_var_int(grpid, varid, vali);
        if (ret != 0)
          throw new IOException(ret + ": " + nc4.nc_strerror(ret));
        return Array.factory(DataType.INT, shape, vali);

      case Nc4prototypes.NC_INT64:
        long[] vall = new long[len];
        ret = nc4.nc_get_var_longlong(grpid, varid, vall);
        if (ret != 0)
          throw new IOException(ret + ": " + nc4.nc_strerror(ret));
        return Array.factory(DataType.LONG, shape, vall);

      case Nc4prototypes.NC_UINT64:
        long[] vallu = new long[len];
        ret = nc4.nc_get_var_ulonglong(grpid, varid, vallu);
        if (ret != 0)
          throw new IOException(ret + ": " + nc4.nc_strerror(ret));
        return Array.factory(DataType.ULONG, shape, vallu);

      case Nc4prototypes.NC_SHORT:
        short[] vals = new short[len];
        ret = nc4.nc_get_var_short(grpid, varid, vals);
        if (ret != 0)
          throw new IOException(ret + ": " + nc4.nc_strerror(ret));
        return Array.factory(DataType.SHORT, shape, vals);

      case Nc4prototypes.NC_USHORT:
        short[] valsu = new short[len];
        ret = nc4.nc_get_var_ushort(grpid, varid, valsu);
        if (ret != 0)
          throw new IOException(ret + ": " + nc4.nc_strerror(ret));
        return Array.factory(DataType.USHORT, shape, valsu);

      case Nc4prototypes.NC_UINT:
        int[] valiu = new int[len];
        ret = nc4.nc_get_var_uint(grpid, varid, valiu);
        if (ret != 0)
          throw new IOException(ret + ": " + nc4.nc_strerror(ret));
        return Array.factory(DataType.UINT, shape, valiu);

      case Nc4prototypes.NC_STRING:
        String[] valss = new String[len];
        ret = nc4.nc_get_var_string(grpid, varid, valss);
        if (ret != 0)
          throw new IOException(ret + ": " + nc4.nc_strerror(ret));
        if (transcodeStrings) {
          valss = transcodeString(valss);
        }
        return Array.factory(DataType.STRING, shape, valss);

      default:
        UserType userType = userTypes.get(typeid);
        if (userType == null) {
          throw new IOException("Unknown userType == " + typeid);
        } else if (userType.typeClass == Nc4prototypes.NC_ENUM) {
          int buffSize = len * userType.size;
          byte[] bbuff = new byte[buffSize];
          // read in the data
          ret = nc4.nc_get_var(grpid, varid, bbuff);
          if (ret != 0)
            throw new IOException(ret + ": " + nc4.nc_strerror(ret));
          ByteBuffer bb = ByteBuffer.wrap(bbuff);
          bb.order(ByteOrder.nativeOrder()); // c library returns in native order i hope

          switch (userType.baseTypeid) {
            case Nc4prototypes.NC_BYTE:
              return Array.factory(DataType.BYTE, shape, bb);
            case Nc4prototypes.NC_UBYTE:
              return Array.factory(DataType.UBYTE, shape, bb);
            case Nc4prototypes.NC_SHORT:
              return Array.factory(DataType.SHORT, shape, bb);
            case Nc4prototypes.NC_USHORT:
              return Array.factory(DataType.USHORT, shape, bb);
          }
          throw new IOException("unknown type " + userType.baseTypeid);
        } else if (userType.typeClass == Nc4prototypes.NC_VLEN) {
          return readVlen(grpid, varid, userType, section);
        } else if (userType.typeClass == Nc4prototypes.NC_OPAQUE) {
          return readOpaque(grpid, varid, section, userType.size);
        } else if (userType.typeClass == Nc4prototypes.NC_COMPOUND) {
          return readCompound(grpid, varid, section, userType);
        }
        throw new IOException("Unsupported userType = " + typeid + " userType= " + userType);
    }
  }

  private Array readCompound(int grpid, int varid, Section section, UserType userType) throws IOException {
    SizeT[] origin = convertSizeT(section.getOrigin());
    SizeT[] shape = convertSizeT(section.getShape());
    SizeT[] stride = convertSizeT(section.getStride());
    int len = (int) section.computeSize();

    int buffSize = len * userType.size;
    byte[] bbuff = new byte[buffSize];

    // read in the data
    int ret;
    ret = nc4.nc_get_vars(grpid, varid, origin, shape, stride, bbuff);
    if (ret != 0)
      throw new IOException(ret + ": " + nc4.nc_strerror(ret));

    ByteBuffer bb = ByteBuffer.wrap(bbuff);
    bb.order(ByteOrder.nativeOrder()); // c library returns in native order i hope

    /*
     * This does not seem right since the user type does not
     * normally appear in the CDM representation.
     * dmh: observation is correct, var name should be used instead of
     * usertype.name, at least for now and to be consistent with H5Iosp.
     * This is not easy, however, because we have to re-read the variable's name.
     * and ideally this would be in the Vinfo, but we have no easy way to get that either.
     */
    String vname = nc_inq_var_name(grpid, varid);
    StructureMembers sm = createStructureMembers(userType, vname);
    ArrayStructureBB asbb = new ArrayStructureBB(sm, section.getShape(), bb, 0);

    // find and convert String and vlen fields, put on asbb heap
    int destPos = 0;
    for (int i = 0; i < len; i++) { // loop over each structure
      convertHeap(asbb, destPos, sm);
      destPos += userType.size;
    }
    return asbb;
  }

  private StructureMembers createStructureMembers(UserType userType, String varname) {
    // Incorrect: StructureMembers sm = new StructureMembers(userType.name);
    StructureMembers.Builder sm = StructureMembers.builder().setName(varname);
    for (Field fld : userType.flds) {
      StructureMembers.MemberBuilder mb = sm.addMember(fld.name, null, null, fld.ctype.dt, fld.dims);
      mb.setDataParam(fld.offset);
      /*
       * This should already have been taken care of
       * if(fld.ctype.isVlen) {m.setShape(new int[]{-1}); }
       */

      if (fld.ctype.dt == DataType.STRUCTURE) {
        UserType nested_utype = userTypes.get(fld.fldtypeid);
        String partfqn = EscapeStrings.backslashEscapeCDMString(varname, ".") + "."
            + EscapeStrings.backslashEscapeCDMString(fld.name, ".");
        StructureMembers nested_sm = createStructureMembers(nested_utype, partfqn);
        mb.setStructureMembers(nested_sm);
      }
    }
    sm.setStructureSize(userType.size);
    return sm.build();
  }

  // LOOK: handling nested ??
  private void convertHeap(ArrayStructureBB asbb, int pos, StructureMembers sm) throws IOException {
    ByteBuffer bb = asbb.getByteBuffer();
    for (StructureMembers.Member m : sm.getMembers()) {
      if (m.getDataType() == DataType.STRING) {
        int size = m.getSize();
        int destPos = pos + m.getDataParam();
        String[] result = new String[size];
        for (int i = 0; i < size; i++) {
          long addr = getNativeAddr(pos, bb);
          Pointer p = new Pointer(addr);
          result[i] = p.getString(0, CDM.UTF8);
        }
        int index = asbb.addObjectToHeap(result);
        bb.putInt(destPos, index); // overwrite with the index into the StringHeap

      } else if (m.isVariableLength()) {
        // We need to do like readVLEN, but store the resulting array
        // in the asbb heap (a bit of a hack).
        // we assume that pos "points" to the beginning of this structure instance
        // and so pos + m.getDataParam() "points" to field m in this structure instance.
        int nc_vlen_t_size = (new Nc4prototypes.Vlen_t()).size();
        int startPos = pos + m.getDataParam();
        // Compute rank and size upto the first (and ideally last) VLEN
        int[] fieldshape = m.getShape();
        int prefixrank = 0;
        int size = 1;
        for (; prefixrank < fieldshape.length; prefixrank++) {
          if (fieldshape[prefixrank] < 0)
            break;
          size *= fieldshape[prefixrank];
        }
        assert size == m.getSize() : "Internal error: field size mismatch";
        Array[] fieldarray = new Array[size]; // hold all the nc_vlen_t instance data
        // destPos will point to each nc_vlen_t instance in turn
        // assuming we have 'size' such instances in a row.
        int destPos = startPos;
        for (int i = 0; i < size; i++) {
          // vlenarray extracts the i'th nc_vlen_t contents (struct not supported).
          Array vlenArray = decodeVlen(m.getDataType(), destPos, bb);
          fieldarray[i] = vlenArray;
          destPos += nc_vlen_t_size;
        }
        Array result;
        if (prefixrank == 0) // if scalar, return just the singleton vlen array
          result = fieldarray[0];

        else {
          int[] newshape = new int[prefixrank];
          System.arraycopy(fieldshape, 0, newshape, 0, prefixrank);
          // result = Array.makeObjectArray(m.getDataType(), fieldarray[0].getClass(), newshape, fieldarray);
          result = Array.makeVlenArray(newshape, fieldarray);
        }

        /*
         * else if (prefixrank == 1)
         * result = Array.makeObjectArray(m.getDataType(), fieldarray[0].getClass(), new int[]{size}, fieldarray);
         * 
         * else {
         * // Otherwise create and fill in an n-dimensional Array Of Arrays
         * int[] newshape = new int[prefixrank];
         * System.arraycopy(fieldshape, 0, newshape, 0, prefixrank);
         * Array ndimarray = Array.makeObjectArray(m.getDataType(), Array.class, newshape, null);
         * // Transfer the elements of data into the n-dim arrays
         * IndexIterator iter = ndimarray.getIndexIterator();
         * for (int i = 0; iter.hasNext(); i++) {
         * iter.setObjectNext(fieldarray[i]);
         * }
         * result = ndimarray;
         * }
         */
        // Store vlen result in the heap
        int index = asbb.addObjectToHeap(result);
        bb.order(ByteOrder.nativeOrder()); // the string index is always written in "native order"
        bb.putInt(startPos, index); // overwrite with the index into the StringHeap
      }
    }
  }

  /**
   * Note that this only works for atomic base types;
   * structures will fail.
   */
  Array readVlen(int grpid, int varid, UserType userType, Section section) throws IOException {
    // Read all the vlen pointers
    int len = (int) section.computeSize();
    Nc4prototypes.Vlen_t[] vlen = new Nc4prototypes.Vlen_t[len];
    int ret = nc4.nc_get_var(grpid, varid, vlen);
    if (ret != 0)
      throw new IOException(ret + ": " + nc4.nc_strerror(ret));

    // Compute rank up to the first VLEN
    int prefixrank = 0;
    for (; prefixrank < section.getRank(); prefixrank++) {
      if (section.getRange(prefixrank) == Range.VLEN)
        break;
    }

    ConvertedType ctype = convertDataType(userType.baseTypeid);
    // ArrayObject.D1 vlenArray = new ArrayObject.D1( dtype, len);

    // Collect the vlen's data arrays
    Array[] data = new Array[len];
    switch (userType.baseTypeid) { // LOOK not complete
      case Nc4prototypes.NC_UINT:
      case Nc4prototypes.NC_INT:
        for (int i = 0; i < len; i++) {
          int slen = vlen[i].len;
          // Coverity[FB.NP_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD]
          int[] ba = vlen[i].p.getIntArray(0, slen);
          data[i] = Array.factory(ctype.dt, new int[] {slen}, ba);
        }
        break;
      case Nc4prototypes.NC_USHORT:
      case Nc4prototypes.NC_SHORT:
        for (int i = 0; i < len; i++) {
          int slen = vlen[i].len;
          // Coverity[FB.NP_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD]
          short[] ba = vlen[i].p.getShortArray(0, slen);
          data[i] = Array.factory(ctype.dt, new int[] {slen}, ba);
        }
        break;
      case Nc4prototypes.NC_FLOAT:
        for (int i = 0; i < len; i++) {
          int slen = vlen[i].len;
          // Coverity[FB.NP_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD]
          float[] ba = vlen[i].p.getFloatArray(0, slen);
          data[i] = Array.factory(DataType.FLOAT, new int[] {slen}, ba);
        }
        break;
      default:
        throw new UnsupportedOperationException("Vlen type " + userType.baseTypeid + " = " + ctype);
    }

    if (prefixrank == 0) { // if scalar, return just the len Array
      return data[0];
    } // else if (prefixrank == 1)
      // return Array.makeObjectArray(ctype.dt, data[0].getClass(), new int[]{len}, data);
      // return Array.makeVlenArray(new int[]{len}, data);

    // Otherwise create and fill in an n-dimensional Array Of Arrays
    int[] shape = new int[prefixrank];
    for (int i = 0; i < prefixrank; i++)
      shape[i] = section.getRange(i).length();

    // Array ndimarray = Array.makeObjectArray(ctype.dt, Array.class, shape, null);
    Array ndimarray = Array.makeVlenArray(shape, data);
    // Transfer the elements of data into the n-dim arrays
    // IndexIterator iter = ndimarray.getIndexIterator();
    // for (int i = 0; iter.hasNext(); i++) {
    // iter.setObjectNext(data[i]);
    // }
    return ndimarray;
  }

  // opaques use ArrayObjects of ByteBuffer
  private Array readOpaque(int grpid, int varid, Section section, int size) throws IOException, InvalidRangeException {
    int ret;
    SizeT[] origin = convertSizeT(section.getOrigin());
    SizeT[] shape = convertSizeT(section.getShape());
    SizeT[] stride = convertSizeT(section.getStride());
    int len = (int) section.computeSize();

    byte[] bbuff = new byte[len * size];
    ret = nc4.nc_get_vars(grpid, varid, origin, shape, stride, bbuff);
    if (DEBUG)
      dumpbytes(bbuff, 0, bbuff.length, "readOpaque");
    if (ret != 0)
      throw new IOException(ret + ": " + nc4.nc_strerror(ret));
    int[] intshape;

    if (shape != null) {
      // fix: this is ignoring the rank of section.
      // was: ArrayObject values = new ArrayObject(ByteBuffer.class, new int[]{len});
      intshape = new int[shape.length];
      for (int i = 0; i < intshape.length; i++) {
        intshape[i] = shape[i].intValue();
      }
    } else
      intshape = new int[] {1};

    Array values = Array.factory(DataType.OPAQUE, intshape);
    int count = 0;
    IndexIterator ii = values.getIndexIterator();
    while (ii.hasNext()) {
      ii.setObjectNext(ByteBuffer.wrap(bbuff, count * size, size));
      count++;
    }
    return values;
  }

  /*
   * private Array readEnum(int grpid, int varid, int baseType, int len, int[] shape)
   * throws IOException, InvalidRangeException
   * {
   * int ret;
   * 
   * ConvertedType ctype = convertDataType(baseType);
   * int elemSize = ctype.dt.getSize();
   * 
   * ByteBuffer bb = ByteBuffer.allocate(len * elemSize);
   * ret = nc4.nc_get_var(grpid, varid, bb);
   * if(ret != 0)
   * throw new IOException(ret+": "+nc4.nc_strerror(ret)) ;
   * 
   * switch (baseType) {
   * case NCLibrary.NC_BYTE:
   * case NCLibrary.NC_UBYTE:
   * return Array.factory( DataType.BYTE, shape, bb.array());
   * 
   * case NCLibrary.NC_SHORT:
   * case NCLibrary.NC_USHORT:
   * ShortBuffer sb = bb.asShortBuffer();
   * return Array.factory( DataType.BYTE, shape, sb.array());
   * 
   * case NCLibrary.NC_INT:
   * case NCLibrary.NC_UINT:
   * IntBuffer ib = bb.asIntBuffer();
   * return Array.factory( DataType.BYTE, shape, ib.array());
   * }
   * 
   * return null;
   * }
   */

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  boolean isUnsigned(int type) {
    return (type == Nc4prototypes.NC_UBYTE) || (type == Nc4prototypes.NC_USHORT) || (type == Nc4prototypes.NC_UINT)
        || (type == Nc4prototypes.NC_UINT64);
  }

  private boolean isVlen(int type) {
    UserType userType = userTypes.get(type);
    return (userType != null) && (userType.typeClass == Nc4prototypes.NC_VLEN);
  }

  @Nullable
  SizeT[] convertSizeT(int[] from) {
    if (from.length == 0)
      return null;
    SizeT[] to = new SizeT[from.length];
    for (int i = 0; i < from.length; i++)
      to[i] = new SizeT(from[i]);
    return to;
  }

  private static class ConvertedType {
    DataType dt;
    // boolean isUnsigned;
    boolean isVlen;

    ConvertedType(DataType dt) {
      this.dt = dt;
    }
  }

  private ConvertedType convertDataType(int type) {
    switch (type) {
      case Nc4prototypes.NC_BYTE:
        return new ConvertedType(DataType.BYTE);

      case Nc4prototypes.NC_UBYTE:
        return new ConvertedType(DataType.UBYTE);

      case Nc4prototypes.NC_CHAR:
        return new ConvertedType(DataType.CHAR);

      case Nc4prototypes.NC_SHORT:
        return new ConvertedType(DataType.SHORT);

      case Nc4prototypes.NC_USHORT:
        return new ConvertedType(DataType.USHORT);

      case Nc4prototypes.NC_INT:
        return new ConvertedType(DataType.INT);

      case Nc4prototypes.NC_UINT:
        return new ConvertedType(DataType.UINT);

      case Nc4prototypes.NC_INT64:
        return new ConvertedType(DataType.LONG);

      case Nc4prototypes.NC_UINT64:
        return new ConvertedType(DataType.ULONG);

      case Nc4prototypes.NC_FLOAT:
        return new ConvertedType(DataType.FLOAT);

      case Nc4prototypes.NC_DOUBLE:
        return new ConvertedType(DataType.DOUBLE);

      case Nc4prototypes.NC_ENUM:
        return new ConvertedType(DataType.ENUM1); // LOOK width ??

      case Nc4prototypes.NC_STRING:
        return new ConvertedType(DataType.STRING);

      default:
        UserType userType = userTypes.get(type);
        if (userType == null)
          throw new IllegalArgumentException("unknown type == " + type);

        switch (userType.typeClass) {
          case Nc4prototypes.NC_ENUM:
            switch (userType.size) {
              case 1:
                return new ConvertedType(DataType.ENUM1);
              case 2:
                return new ConvertedType(DataType.ENUM2);
              case 4:
                return new ConvertedType(DataType.ENUM4);
              default:
                throw new IllegalArgumentException("enum unknown size == " + userType);
            }

          case Nc4prototypes.NC_COMPOUND:
            return new ConvertedType(DataType.STRUCTURE);

          case Nc4prototypes.NC_OPAQUE:
            return new ConvertedType(DataType.OPAQUE);

          case Nc4prototypes.NC_VLEN:
            ConvertedType result = convertDataType(userType.baseTypeid);
            result.isVlen = true;
            return result;
        }
        throw new IllegalArgumentException("unknown type == " + type);
    }
  }

  private String getDataTypeName(int type) {
    switch (type) {
      case Nc4prototypes.NC_BYTE:
        return "byte";
      case Nc4prototypes.NC_UBYTE:
        return "ubyte";
      case Nc4prototypes.NC_CHAR:
        return "char";
      case Nc4prototypes.NC_SHORT:
        return "short";
      case Nc4prototypes.NC_USHORT:
        return "ushort";
      case Nc4prototypes.NC_INT:
        return "int";
      case Nc4prototypes.NC_UINT:
        return "uint";
      case Nc4prototypes.NC_INT64:
        return "long";
      case Nc4prototypes.NC_UINT64:
        return "ulong";
      case Nc4prototypes.NC_FLOAT:
        return "float";
      case Nc4prototypes.NC_DOUBLE:
        return "double";
      case Nc4prototypes.NC_ENUM:
        return "enum";
      case Nc4prototypes.NC_STRING:
        return "string";
      case Nc4prototypes.NC_COMPOUND:
        return "struct";
      case Nc4prototypes.NC_OPAQUE:
        return "opaque";
      case Nc4prototypes.NC_VLEN:
        return "vlen";

      default:
        UserType userType = userTypes.get(type);
        if (userType == null)
          return "unknown type " + type;

        switch (userType.typeClass) {
          case Nc4prototypes.NC_ENUM:
            return "userType-enum";
          case Nc4prototypes.NC_COMPOUND:
            return "userType-struct";
          case Nc4prototypes.NC_OPAQUE:
            return "userType-opaque";
          case Nc4prototypes.NC_VLEN:
            return "userType-vlen";
        }
        return "unknown userType " + userType.typeClass;
    }
  }


  private static long getNativeAddr(int pos, ByteBuffer buf) {
    return Platform.is64Bit() ? buf.getLong(pos) : buf.getInt(pos);
  }

  static class Vinfo {
    final Group4 g4;
    int varid, typeid;
    UserType utype; // may be null

    Vinfo(Group4 g4, int varid, int typeid) {
      this.g4 = g4;
      this.varid = varid;
      this.typeid = typeid;
    }
  }

  static class Group4 {
    final int grpid;
    final Group.Builder g;
    final Group4 parent;
    Map<Dimension, Integer> dimHash;

    Group4(int grpid, Group.Builder g, Group4 parent) {
      this.grpid = grpid;
      this.g = g;
      this.parent = parent;
    }
  }

  // Cannot be static because it references non-static parent class memebers
  // Coverity[FB.SIC_INNER_SHOULD_BE_STATIC]
  class UserType {
    int grpid;
    int typeid;
    String name;
    int size; // the size of the user defined type
    int baseTypeid; // the base typeid for vlen and enum types
    long nfields; // the number of fields for enum and compound types
    int typeClass; // the class of the user defined type: NC_VLEN, NC_OPAQUE, NC_ENUM, or NC_COMPOUND.

    EnumTypedef e;
    List<Field> flds;

    UserType(int grpid, int typeid, String name, long size, int baseTypeid, long nfields, int typeClass)
        throws IOException {
      this.grpid = grpid;
      this.typeid = typeid;
      this.name = name;
      this.size = (int) size;
      this.baseTypeid = baseTypeid;
      this.nfields = nfields;
      this.typeClass = typeClass;
      if (debugUserTypes)
        System.out.printf("%s%n", this);

      if (typeClass == Nc4prototypes.NC_COMPOUND)
        readFields();
    }

    // Allow size override for e.g. opaque
    public UserType setSize(int size) {
      this.size = size;
      return this;
    }

    DataType getEnumBaseType() {
      // set the enum's basetype
      if (baseTypeid > 0 && baseTypeid <= NC_MAX_ATOMIC_TYPE) {
        DataType cdmtype;
        switch (baseTypeid) {
          case NC_CHAR:
          case NC_UBYTE:
          case NC_BYTE:
            cdmtype = DataType.ENUM1;
            break;
          case NC_USHORT:
          case NC_SHORT:
            cdmtype = DataType.ENUM2;
            break;
          case NC_UINT:
          case NC_INT:
          default:
            cdmtype = DataType.ENUM4;
            break;
        }
        return cdmtype;
      }
      return null;
    }

    void addField(Field fld) {
      if (flds == null)
        flds = new ArrayList<>(10);
      flds.add(fld);
    }

    void setFields(List<Field> flds) {
      this.flds = flds;
    }

    public String toString2() {
      return "name='" + name + "' id=" + getDataTypeName(typeid) + " userType=" + getDataTypeName(typeClass)
          + " baseType=" + getDataTypeName(baseTypeid);
    }

    @Override
    public String toString() {
      String sb = "UserType" + "{grpid=" + grpid + ", typeid=" + typeid + ", name='" + name + '\'' + ", size=" + size
          + ", baseTypeid=" + baseTypeid + ", nfields=" + nfields + ", typeClass=" + typeClass + ", e=" + e + '}';
      return sb;
    }

    void readFields() throws IOException {
      for (int fldidx = 0; fldidx < nfields; fldidx++) {
        byte[] fldname = new byte[Nc4prototypes.NC_MAX_NAME + 1];
        IntByReference field_typeidp = new IntByReference();
        IntByReference ndimsp = new IntByReference();
        SizeTByReference offsetp = new SizeTByReference();

        int[] dims = new int[Nc4prototypes.NC_MAX_DIMS];
        int ret = nc4.nc_inq_compound_field(grpid, typeid, fldidx, fldname, offsetp, field_typeidp, ndimsp, dims);
        if (ret != 0)
          throw new IOException(ret + ": " + nc4.nc_strerror(ret));

        Field fld = new Field(grpid, typeid, fldidx, makeString(fldname), offsetp.getValue().intValue(),
            field_typeidp.getValue(), ndimsp.getValue(), dims);

        addField(fld);
        if (debugUserTypes)
          System.out.printf(" %s add field= %s%n", name, fld);
      }
    }
  }

  // encapsolate the fields in a compound type
  // Cannot be static because it references non-static parent class members
  // Coverity[FB.SIC_INNER_SHOULD_BE_STATIC]
  class Field {
    int grpid;
    int typeid; // containing structure
    int fldidx;
    String name;
    int offset;
    int fldtypeid;
    int ndims;
    int[] dims;

    ConvertedType ctype;
    // int total_size;
    Array data;

    // grpid, varid, fldidx, fldname, offsetp, field_typeidp, ndimsp, dim_sizesp
    Field(int grpid, int typeid, int fldidx, String name, int offset, int fldtypeid, int ndims, int[] dimz) {
      this.grpid = grpid;
      this.typeid = typeid;
      this.fldidx = fldidx;
      this.name = name;
      this.offset = offset;
      this.fldtypeid = fldtypeid;
      // Reduce the stored dimensions to match the actual rank
      // because some code (i.e. Section) is using this.dims.length
      // to compute the rank.
      this.ndims = ndims;
      this.dims = new int[ndims];
      System.arraycopy(dimz, 0, this.dims, 0, ndims);

      ctype = convertDataType(fldtypeid);
      // Section s = new Section(this.dims);
      // total_size = (int) s.computeSize() * ctype.dt.getSize();

      if (isVlen(fldtypeid)) {
        int[] edims = new int[ndims + 1];
        if (ndims > 0)
          System.arraycopy(dimz, 0, edims, 0, ndims);
        edims[ndims] = -1;
        this.dims = edims;
        this.ndims++;
      }
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Field field = (Field) o;
      return grpid == field.grpid && typeid == field.typeid && fldidx == field.fldidx && offset == field.offset
          && fldtypeid == field.fldtypeid && ndims == field.ndims && Objects.equals(name, field.name)
          && Arrays.equals(dims, field.dims);
    }

    @Override
    public int hashCode() {
      return Objects.hash(grpid, typeid, fldidx, name, offset, fldtypeid, ndims, dims);
    }

    public String toString2() {
      return "name='" + name + " fldtypeid=" + getDataTypeName(fldtypeid) + " ndims=" + ndims + " offset=" + offset;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("Field");
      sb.append("{grpid=").append(grpid);
      sb.append(", typeid=").append(typeid);
      sb.append(", fldidx=").append(fldidx);
      sb.append(", name='").append(name).append('\'');
      sb.append(", offset=").append(offset);
      sb.append(", fldtypeid=").append(fldtypeid);
      sb.append(", ndims=").append(ndims);
      sb.append(", dims=").append(dims == null ? "null" : "");
      for (int i = 0; dims != null && i < dims.length; ++i)
        sb.append(i == 0 ? "" : ", ").append(dims[i]);
      sb.append(", dtype=").append(ctype.dt);
      if (ctype.isVlen)
        sb.append("(vlen)");
      sb.append('}');
      return sb.toString();
    }

    /*
     * Variable makeMemberVariable(Group g, Structure parent)
     * {
     * Variable v = new Variable(ncfile, g, parent, name);
     * v.setDataType(convertDataType(fldtypeid).dt);
     * if(isUnsigned(fldtypeid))
     * v.addAttribute(new Attribute(CDM.UNSIGNED, "true"));
     *
     * if(ctype.isVlen) {
     * v.setDimensions("*");
     * } else {
     * try {
     * v.setDimensionsAnonymous(dims);
     * } catch (InvalidRangeException e) {
     * e.printStackTrace();
     * }
     * }
     * return v;
     * }
     */

    Variable.Builder<?> makeMemberVariable(Group.Builder g) throws IOException {
      Variable.Builder<?> v = makeVariable(g, name, fldtypeid, "");
      v.setDimensionsAnonymous(dims); // LOOK no shared dimensions ?
      return v;
    }
  }

  /////////////////////////////////////////////////////////////////////////
  // TODO eliminate

  static class Annotation {
    Object key;
    Object value;

    public Annotation(Object key, Object value) {
      this.key = key;
      this.value = value;
    }
  }

  HashMap<Object, List<Annotation>> annotations = new HashMap<>();

  void annotate(Object elem, Object key, Object value) {
    List<Annotation> list = annotations.computeIfAbsent(elem, k -> new ArrayList<>());
    int index = -1;
    Iterator<Annotation> iter = list.iterator();
    while (iter.hasNext()) {
      Annotation ann = iter.next();
      if (ann.key.equals(key)) {
        iter.remove();
        break;
      }
    }
    list.add(new Annotation(key, value));
  }

  private static void dumpbytes(byte[] bytes, int start, int len, String tag) {
    System.err.println("++++++++++ " + tag + " ++++++++++ ");
    int stop = start + len;
    try {
      for (int i = 0; i < stop; i++) {
        byte b = bytes[i];
        int ib = (int) b;
        int ub = (ib & 0xFF);
        char c = (char) ub;
        String s = Character.toString(c);
        if (c == '\r')
          s = "\\r";
        else if (c == '\n')
          s = "\\n";
        else if (c < ' ')
          s = "?";
        System.err.printf("[%03d] %02x %03d %4d '%s'", i, ub, ub, ib, s);
        System.err.println();
        System.err.flush();
      }

    } catch (Exception e) {
      System.err.println("failure:" + e);
    } finally {
      System.err.println("++++++++++ " + tag + " ++++++++++ ");
      System.err.flush();
    }
  }



}
