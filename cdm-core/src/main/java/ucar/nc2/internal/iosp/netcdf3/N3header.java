/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.iosp.netcdf3;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import ucar.array.ArrayType;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.iosp.NetcdfFileFormat;
import ucar.unidata.io.RandomAccessFile;
import java.util.*;
import java.io.IOException;

/** Netcdf version 3 header. Read-only version using Builders for immutablility. */
public class N3header {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(N3header.class);

  static final byte[] MAGIC = {0x43, 0x44, 0x46, 0x01};
  // 64-bit offset format : only affects the variable offset value
  static final byte[] MAGIC_LONG = {0x43, 0x44, 0x46, 0x02};
  static final int MAGIC_DIM = 10;
  static final int MAGIC_VAR = 11;
  static final int MAGIC_ATT = 12;

  public static boolean disallowFileTruncation; // see NetcdfFile.setDebugFlags
  public static boolean debugHeaderSize; // see NetcdfFile.setDebugFlags

  public static boolean isValidFile(ucar.unidata.io.RandomAccessFile raf) throws IOException {
    switch (NetcdfFileFormat.findNetcdfFormatType(raf)) {
      case NETCDF3:
      case NETCDF3_64BIT_OFFSET:
        return true;
      default:
        return false;
    }
  }

  // variable info for reading/writing
  static class Vinfo {
    String name;
    long vsize; // size of array in bytes. if isRecord, size per record.
    long begin; // offset of start of data from start of file
    boolean isRecord; // is it a record variable?
    long attsPos; // attributes start here - used for update

    Vinfo(String name, long vsize, long begin, boolean isRecord, long attsPos) {
      this.name = name;
      this.vsize = vsize;
      this.begin = begin;
      this.isRecord = isRecord;
      this.attsPos = attsPos;
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  protected ucar.unidata.io.RandomAccessFile raf;

  // N3iosp needs access to these
  private boolean isStreaming; // is streaming (numrecs = -1)
  int numrecs; // number of records written
  long recsize; // size of each record (padded) LOOK can it really be bigger than MAX_INTEGER ?
  long recStart = Integer.MAX_VALUE; // where the record data starts LOOK can it really be bigger than MAX_INTEGER ?

  boolean useLongOffset;
  private final N3iosp n3iospNew;
  long nonRecordDataSize; // size of non-record variables
  Dimension udim; // the unlimited dimension
  private final List<Vinfo> vars = new ArrayList<>();
  long dataStart = Long.MAX_VALUE; // where the data starts

  private final Charset valueCharset;

  N3header(N3iosp n3iospNew) {
    this.n3iospNew = n3iospNew;
    this.valueCharset = n3iospNew.getValueCharset().orElse(StandardCharsets.UTF_8);
  }

  /**
   * Return defined {@link Charset value charset} that
   * will be used by reading HDF3 header.
   * 
   * @return {@link Charset charset}
   */
  private Charset getValueCharset() {
    return valueCharset;
  }

  /**
   * Read the header and populate the ncfile
   *
   * @param raf read from this file
   * @param root the root Group builder to populate.
   * @param debugOut optional for debug message, may be null
   * @throws IOException on read error
   */
  void read(RandomAccessFile raf, Group.Builder root, Formatter debugOut) throws IOException {
    this.raf = raf;

    long actualSize = raf.length();
    nonRecordDataSize = 0; // length of non-record data
    recsize = 0; // length of single record
    recStart = Integer.MAX_VALUE; // where the record data starts

    // netcdf magic number
    long pos = 0;
    raf.order(RandomAccessFile.BIG_ENDIAN);
    raf.seek(pos);

    byte[] b = new byte[4];
    raf.readFully(b);
    if (!isMagicBytes(b)) {
      throw new IOException("Not a netCDF file " + raf.getLocation());
    }
    if ((b[3] != 1) && (b[3] != 2))
      throw new IOException("Not a netCDF file " + raf.getLocation());
    useLongOffset = (b[3] == 2);

    // number of records
    numrecs = raf.readInt();
    if (debugOut != null)
      debugOut.format("numrecs= %d%n", numrecs);
    if (numrecs == -1) {
      isStreaming = true;
      numrecs = 0;
    }

    // dimensions
    int numdims = 0;
    int magic = raf.readInt();
    if (magic == 0) {
      raf.readInt(); // skip 32 bits
    } else {
      if (magic != MAGIC_DIM)
        throw new IOException("Misformed netCDF file - dim magic number wrong " + raf.getLocation());
      numdims = raf.readInt();
      if (debugOut != null)
        debugOut.format("numdims= %d%n", numdims);
    }

    // Must keep dimensions in strict order
    List<Variable.Builder<?>> uvars = new ArrayList<>(); // vars that have the unlimited dimension
    ArrayList<Dimension> fileDimensions = new ArrayList<>();
    for (int i = 0; i < numdims; i++) {
      if (debugOut != null)
        debugOut.format("  dim %d pos= %d%n", i, raf.getFilePointer());
      String name = readString();
      int len = raf.readInt();
      Dimension dim;
      if (len == 0) {
        dim = Dimension.builder().setName(name).setIsUnlimited(true).setLength(numrecs).build();
        udim = dim;
      } else {
        dim = new Dimension(name, len);
      }
      fileDimensions.add(dim);
      root.addDimension(dim);
      if (debugOut != null)
        debugOut.format(" added dimension %s%n", dim);
    }

    // global attributes
    readAtts(root.getAttributeContainer(), debugOut);

    // variables
    int nvars = 0;
    magic = raf.readInt();
    if (magic == 0) {
      raf.readInt(); // skip 32 bits
    } else {
      if (magic != MAGIC_VAR)
        throw new IOException("Misformed netCDF file  - var magic number wrong " + raf.getLocation());
      nvars = raf.readInt();
      if (debugOut != null)
        debugOut.format("numdims= %d%n", numdims);
    }
    if (debugOut != null)
      debugOut.format("num variables= %d%n", nvars);

    // loop over variables
    for (int i = 0; i < nvars; i++) {
      String name = readString();
      Variable.Builder<?> var = Variable.builder().setName(name);

      // get element count in non-record dimensions
      long velems = 1;
      boolean isRecord = false;
      int rank = raf.readInt();
      List<Dimension> dims = new ArrayList<>();
      for (int j = 0; j < rank; j++) {
        int dimIndex = raf.readInt();
        Dimension dim = fileDimensions.get(dimIndex);
        if (dim.isUnlimited()) {
          isRecord = true;
          uvars.add(var); // track record variables
        } else
          velems *= dim.getLength();

        dims.add(dim);
      }
      var.addDimensions(dims);

      if (debugOut != null) {
        debugOut.format("---name=<%s> dims = [", name);
        for (Dimension dim : dims)
          debugOut.format("%s ", dim.getShortName());
        debugOut.format("]%n");
      }

      // variable attributes
      long varAttsPos = raf.getFilePointer();
      readAtts(var.getAttributeContainer(), debugOut);

      // data type
      int type = raf.readInt();
      DataType dataType = getDataType(type);
      var.setDataType(dataType);

      // size and beginning data position in file
      long vsize = raf.readInt();
      long begin = useLongOffset ? raf.readLong() : (long) raf.readInt();

      if (debugOut != null) {
        debugOut.format(" name= %s type=%d vsize=%s velems=%d begin= %d isRecord=%s attsPos=%d%n", name, type, vsize,
            velems, begin, isRecord, varAttsPos);
        long calcVsize = (velems + padding(velems)) * dataType.getSize();
        if (vsize != calcVsize)
          debugOut.format(" *** readVsize %d != calcVsize %d%n", vsize, calcVsize);
      }
      if (vsize < 0) {
        vsize = (velems + padding(velems)) * dataType.getSize();
      }

      Vinfo vinfo = new Vinfo(name, vsize, begin, isRecord, varAttsPos);
      vars.add(vinfo);
      var.setSPobject(vinfo);

      // track how big each record is
      if (isRecord) {
        recsize += vsize;
        recStart = Math.min(recStart, begin);
      } else {
        nonRecordDataSize = Math.max(nonRecordDataSize, begin + vsize);
      }

      dataStart = Math.min(dataStart, begin);
      root.addVariable(var);
    }

    pos = raf.getFilePointer();

    // if nvars == 0
    if (dataStart == Long.MAX_VALUE) {
      dataStart = pos;
    }

    if (nonRecordDataSize > 0) // if there are non-record variables
      nonRecordDataSize -= dataStart;
    if (uvars.isEmpty()) // if there are no record variables
      recStart = 0;

    // check for streaming file - numrecs must be calculated
    // Example: TestDir.cdmUnitTestDir + "ft/station/madis2.nc"
    if (isStreaming) {
      long recordSpace = actualSize - recStart;
      numrecs = recsize == 0 ? 0 : (int) (recordSpace / recsize);

      // set size of the unlimited dimension, reset the record variables
      if (udim != null) {
        udim = udim.toBuilder().setLength(numrecs).build();
        root.replaceDimension(udim);
        uvars.forEach(v -> v.replaceDimensionByName(udim));
      }
    }

    // Check if file affected by bug CDM-52 (netCDF-Java library used incorrect padding when
    // the file contained only one record variable and it was of type byte, char, or short).
    // Example TestDir.cdmLocalTestDataDir + "byteArrayRecordVarPaddingTest-bad.nc"
    if (uvars.size() == 1) {
      Variable.Builder<?> uvar = uvars.get(0);
      ArrayType dtype = uvar.dataType;
      if ((dtype == ArrayType.CHAR) || (dtype == ArrayType.BYTE) || (dtype == ArrayType.SHORT)) {
        long vsize = dtype.getSize(); // works for all netcdf-3 data types
        List<Dimension> dims = uvar.getDimensions();
        for (Dimension curDim : dims) {
          if (!curDim.isUnlimited())
            vsize *= curDim.getLength();
        }
        Vinfo vinfo = (Vinfo) uvar.spiObject;
        if (vsize != vinfo.vsize) {
          log.info("Misformed netCDF file - file written with incorrect padding for record variable (CDM-52): fvsize="
              + vinfo.vsize + "!= calc size =" + vsize);
          recsize = vsize;
          vinfo.vsize = vsize;
        }
      }
    }

    if (debugHeaderSize) {
      System.out.println("  filePointer = " + pos + " dataStart=" + dataStart);
      System.out
          .println("  recStart = " + recStart + " dataStart+nonRecordDataSize =" + (dataStart + nonRecordDataSize));
      System.out.println("  nonRecordDataSize size= " + nonRecordDataSize);
      System.out.println("  recsize= " + recsize);
      System.out.println("  numrecs= " + numrecs);
      System.out.println("  actualSize= " + actualSize);
    }

    // check for truncated files
    // theres a "wart" that allows a file to be up to 3 bytes smaller than you expect.
    long calcSize = dataStart + nonRecordDataSize + recsize * numrecs;
    if (calcSize > actualSize + 3) {
      if (disallowFileTruncation)
        throw new IOException("File is truncated, calculated size= " + calcSize + " actual = " + actualSize);
      else {
        // log.info("File is truncated calculated size= "+calcSize+" actual = "+actualSize);
        raf.setExtendMode();
      }
    }

    // add a record structure if asked to do so
    if (n3iospNew.useRecordStructure && uvars.size() > 0) {
      makeRecordStructure(root, uvars);
    }
  }

  /**
   * Check if the given bytes correspond to
   * {@link #MAGIC magic bytes} of the header.
   * 
   * @param bytes given bytes.
   * @return <code>true</code> if the given bytes correspond to
   *         {@link #MAGIC magic bytes} of the header. Otherwise <code>false</code>.
   */
  private boolean isMagicBytes(byte[] bytes) {
    for (int i = 0; i < 3; i++) {
      if (bytes[i] != MAGIC[i]) {
        return false;
      }
    }
    return true;
  }

  long calcFileSize() {
    if (udim != null)
      return recStart + recsize * numrecs;
    else
      return dataStart + nonRecordDataSize;
  }

  void showDetail(Formatter out) throws IOException {
    long actual = raf.length();
    out.format("  raf length= %s %n", actual);
    out.format("  isStreaming= %s %n", isStreaming);
    out.format("  useLongOffset= %s %n", useLongOffset);
    out.format("  dataStart= %d%n", dataStart);
    out.format("  nonRecordData size= %d %n", nonRecordDataSize);
    out.format("  unlimited dimension = %s %n", udim);

    if (udim != null) {
      out.format("  record Data starts = %d %n", recStart);
      out.format("  recsize = %d %n", recsize);
      out.format("  numrecs = %d %n", numrecs);
    }

    long calcSize = calcFileSize();
    out.format("  computedSize = %d %n", calcSize);
    if (actual < calcSize)
      out.format("  TRUNCATED!! actual size = %d (%d bytes) %n", actual, (calcSize - actual));
    else if (actual != calcSize)
      out.format(" actual size larger = %d (%d byte extra) %n", actual, (actual - calcSize));

    out.format("%n  %20s____start_____size__unlim%n", "name");
    for (Vinfo vinfo : this.vars) {
      out.format("  %20s %8d %8d  %s %n", vinfo.name, vinfo.begin, vinfo.vsize, vinfo.isRecord);
    }
  }

  private int readAtts(AttributeContainerMutable atts, Formatter fout) throws IOException {
    int natts = 0;
    int magic = raf.readInt();
    if (magic == 0) {
      raf.readInt(); // skip 32 bits
    } else {
      if (magic != MAGIC_ATT)
        throw new IOException("Misformed netCDF file  - att magic number wrong");
      natts = raf.readInt();
    }
    if (fout != null)
      fout.format(" num atts= %d%n", natts);

    for (int i = 0; i < natts; i++) {
      if (fout != null)
        fout.format("***att %d pos= %d%n", i, raf.getFilePointer());
      String name = readString();
      int type = raf.readInt();
      Attribute att;

      if (type == 2) {
        if (fout != null)
          fout.format(" begin read String val pos= %d%n", raf.getFilePointer());
        String val = readString(getValueCharset());
        if (val == null)
          val = "";
        if (fout != null)
          fout.format(" end read String val pos= %d%n", raf.getFilePointer());
        att = new Attribute(name, val);

      } else {
        if (fout != null)
          fout.format(" begin read val pos= %d%n", raf.getFilePointer());
        int nelems = raf.readInt();

        DataType dtype = getDataType(type);
        Attribute.Builder builder = Attribute.builder(name).setDataType(dtype);

        if (nelems > 0) {
          int[] shape = {nelems};
          Array arr = Array.factory(dtype, shape);
          IndexIterator ii = arr.getIndexIterator();
          int nbytes = 0;
          for (int j = 0; j < nelems; j++)
            nbytes += readAttributeValue(dtype, ii);

          builder.setValues(arr); // no validation !!
          skip(nbytes);
        }
        att = builder.build();

        if (fout != null)
          fout.format(" end read val pos= %d%n", raf.getFilePointer());
      }

      atts.addAttribute(att);
      if (fout != null)
        fout.format("  %s%n", att);
    }

    return natts;
  }

  int readAttributeValue(DataType type, IndexIterator ii) throws IOException {
    if (type == DataType.BYTE) {
      byte b = (byte) raf.read();
      // if (debug) out.println(" byte val = "+b);
      ii.setByteNext(b);
      return 1;

    } else if (type == DataType.CHAR) {
      char c = (char) raf.read();
      // if (debug) out.println(" char val = "+c);
      ii.setCharNext(c);
      return 1;

    } else if (type == DataType.SHORT) {
      short s = raf.readShort();
      // if (debug) out.println(" short val = "+s);
      ii.setShortNext(s);
      return 2;

    } else if (type == DataType.INT) {
      int i = raf.readInt();
      // if (debug) out.println(" int val = "+i);
      ii.setIntNext(i);
      return 4;

    } else if (type == DataType.FLOAT) {
      float f = raf.readFloat();
      // if (debug) out.println(" float val = "+f);
      ii.setFloatNext(f);
      return 4;

    } else if (type == DataType.DOUBLE) {
      double d = raf.readDouble();
      // if (debug) out.println(" double val = "+d);
      ii.setDoubleNext(d);
      return 8;
    }
    return 0;
  }

  // read a string = (nelems, byte array), then skip to 4 byte boundary
  String readString() throws IOException {
    return readString(StandardCharsets.UTF_8);
  }

  private String readString(Charset charset) throws IOException {
    int nelems = raf.readInt();
    byte[] b = new byte[nelems];
    raf.readFully(b);
    skip(nelems); // pad to 4 byte boundary

    if (nelems == 0)
      return null;

    // null terminates
    int count = 0;
    while (count < nelems) {
      if (b[count] == 0)
        break;
      count++;
    }

    return new String(b, 0, count, charset); // all strings are considered to be UTF-8 unicode.
  }

  // skip to a 4 byte boundary in the file
  void skip(int nbytes) throws IOException {
    int pad = padding(nbytes);
    if (pad > 0)
      raf.seek(raf.getFilePointer() + pad);
  }

  // find number of bytes needed to pad to a 4 byte boundary
  static int padding(int nbytes) {
    int pad = nbytes % 4;
    if (pad != 0)
      pad = 4 - pad;
    return pad;
  }

  // find number of bytes needed to pad to a 4 byte boundary
  static int padding(long nbytes) {
    int pad = (int) (nbytes % 4);
    if (pad != 0)
      pad = 4 - pad;
    return pad;
  }

  static DataType getDataType(int type) {
    switch (type) {
      case 1:
        return DataType.BYTE;
      case 2:
        return DataType.CHAR;
      case 3:
        return DataType.SHORT;
      case 4:
        return DataType.INT;
      case 5:
        return DataType.FLOAT;
      case 6:
        return DataType.DOUBLE;
      default:
        throw new IllegalArgumentException("unknown type == " + type);
    }
  }

  static int getType(DataType dt) {
    if (dt == DataType.BYTE)
      return 1;
    else if ((dt == DataType.CHAR) || (dt == DataType.STRING))
      return 2;
    else if (dt == DataType.SHORT)
      return 3;
    else if (dt == DataType.INT)
      return 4;
    else if (dt == DataType.FLOAT)
      return 5;
    else if (dt == DataType.DOUBLE)
      return 6;

    throw new IllegalArgumentException("unknown DataType == " + dt);
  }

  private boolean makeRecordStructure(Group.Builder root, List<Variable.Builder<?>> uvars) {
    Structure.Builder<?> recordStructure = Structure.builder().setName("record");
    recordStructure.setParentGroupBuilder(root).setDimensionsByName(udim.getShortName());
    for (Variable.Builder<?> v : uvars) {
      Variable.Builder<?> memberV = v.makeSliceBuilder(0, 0); // set unlimited dimension to 0
      recordStructure.addMemberVariable(memberV);
    }
    root.addVariable(recordStructure);
    uvars.add(recordStructure);
    return true;
  }
}
