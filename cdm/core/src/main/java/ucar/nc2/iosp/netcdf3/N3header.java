/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp.netcdf3;

import java.nio.charset.StandardCharsets;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.iosp.NCheader;
import ucar.unidata.io.RandomAccessFile;
import java.util.*;
import java.io.IOException;

/**
 * Netcdf header reading and writing for version 3 file format.
 * This is used by N3iosp.
 *
 * @author caron
 */

public class N3header extends NCheader {
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(N3header.class);

  static final byte[] MAGIC = {0x43, 0x44, 0x46, 0x01};
  static final long MAX_UNSIGNED_INT = 0x00000000ffffffffL;
  static final byte[] MAGIC_LONG = {0x43, 0x44, 0x46, 0x02}; // 64-bit offset format : only affects the
                                                             // variable offset value
  static final int MAGIC_DIM = 10;
  static final int MAGIC_VAR = 11;
  static final int MAGIC_ATT = 12;

  public static boolean isValidFile(ucar.unidata.io.RandomAccessFile raf) throws IOException {
    switch (checkFileType(raf)) {
      case NC_FORMAT_NETCDF3:
      case NC_FORMAT_64BIT_OFFSET:
        return true;
      default:
        break;
    }
    return false;
  }

  public static boolean disallowFileTruncation; // see NetcdfFile.setDebugFlags
  public static boolean debugHeaderSize; // see NetcdfFile.setDebugFlags

  private static boolean debugVariablePos = false;
  private static boolean debugStreaming = false;


  // variable info for reading/writing
  static class Vinfo {
    long vsize; // size of array in bytes. if isRecord, size per record.
    long begin; // offset of start of data from start of file
    boolean isRecord; // is it a record variable?
    long attsPos; // attributes start here - used for update

    Vinfo(long vsize, long begin, boolean isRecord, long attsPos) {
      this.vsize = vsize;
      this.begin = begin;
      this.isRecord = isRecord;
      this.attsPos = attsPos;
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  ucar.unidata.io.RandomAccessFile raf;
  ucar.nc2.NetcdfFile ncfile;

  // N3iosp needs access to these
  boolean isStreaming; // is streaming (numrecs = -1)
  int numrecs; // number of records written
  long recsize; // size of each record (padded)
  long recStart = Integer.MAX_VALUE; // where the record data starts

  private boolean useLongOffset;
  private long nonRecordDataSize; // size of non-record variables
  private Dimension udim; // the unlimited dimension

  long dataStart = Long.MAX_VALUE; // where the data starts

  /*
   * Notes:
   * In netcdf-3 are dimensions signed or unsigned?
   * In java, integers are signed, so are limited to 2^31, not 2^32
   * "Each fixed-size variable and the data for one record's worth of a single record variable are limited in size to a
   * little less than 4 GiB, which is twice the size limit in versions earlier than netCDF 3.6."
   */

  /**
   * Read the header and populate the ncfile
   *
   * @param raf read from this file
   * @param ncfile fill this NetcdfFile object (originally empty)
   * @param fout optional for debug message, may be null
   * @throws IOException on read error
   */
  void read(RandomAccessFile raf, NetcdfFile ncfile, Formatter fout) throws IOException {
    this.ncfile = ncfile;
    Group.Builder root = Group.builder().setNcfile(ncfile);
    read(raf, root, fout);
    // LOOK ncfile.setRootGroup(root.build());
  }

    /**
     * Read the header and populate the ncfile
     *
     * @param raf read from this file
     * @param root the root Group builder to populate.
     * @param debugOut optional for debug message, may be null
     * @throws IOException on read error
     */
  private void read(ucar.unidata.io.RandomAccessFile raf, Group.Builder root, Formatter debugOut) throws IOException {
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
    for (int i = 0; i < 3; i++)
      if (b[i] != MAGIC[i])
        throw new IOException("Not a netCDF file " + raf.getLocation());
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
    List<Variable.Builder> uvars = new ArrayList<>(); // vars that have the unlimited dimension
    ArrayList<Dimension> fileDimensions = new ArrayList<>();
    for (int i = 0; i < numdims; i++) {
      if (debugOut != null)
        debugOut.format("  dim %d pos= %d%n", i, raf.getFilePointer());
      String name = readString();
      int len = raf.readInt();
      Dimension dim;
      if (len == 0) {
        dim = Dimension.builder(name, numrecs).setIsUnlimited(true).build();
        udim = dim;
      } else {
        dim = Dimension.builder(name, numrecs).build();
      }
      fileDimensions.add(dim);
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
      long startPos = raf.getFilePointer();
      String name = readString();
      Variable.Builder var = Variable.builder().setName(name);

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

      var.setSPobject(new Vinfo(vsize, begin, isRecord, varAttsPos));

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

    /* LOOK Check if file affected by bug CDM-52 (netCDF-Java library used incorrect padding when
    // the file contained only one record variable and it was of type byte, char, or short).
    if (uvars.size() == 1) {
      Variable uvar = uvars.get(0);
      DataType dtype = uvar.getDataType();
      if ((dtype == DataType.CHAR) || (dtype == DataType.BYTE) || (dtype == DataType.SHORT)) {
        long vsize = uvar.getDataType().getSize(); // works for all netcdf-3 data types
        for (Dimension curDim : uvar.getDimensions()) {
          if (!curDim.isUnlimited())
            vsize *= curDim.getLength();
        }
        Vinfo vinfo = (Vinfo) uvar.getSPobject();
        if (vsize != vinfo.vsize) {
          // log.info( "Misformed netCDF file - file written with incorrect padding for record variable (CDM-52):
          // fvsize=" + vinfo.vsize+"!= calc size =" + vsize );
          recsize = vsize;
          vinfo.vsize = vsize;
        }
      }
    } */

    if (debugHeaderSize) {
      System.out.println("  filePointer = " + pos + " dataStart=" + dataStart);
      System.out
          .println("  recStart = " + recStart + " dataStart+nonRecordDataSize =" + (dataStart + nonRecordDataSize));
      System.out.println("  nonRecordDataSize size= " + nonRecordDataSize);
      System.out.println("  recsize= " + recsize);
      System.out.println("  numrecs= " + numrecs);
      System.out.println("  actualSize= " + actualSize);
    }

    /* LOOK check for streaming file - numrecs must be calculated
    if (isStreaming) {
      long recordSpace = actualSize - recStart;
      numrecs = recsize == 0 ? 0 : (int) (recordSpace / recsize);
      if (debugStreaming) {
        long extra = recsize == 0 ? 0 : recordSpace % recsize;
        System.out
            .println(" isStreaming recordSpace=" + recordSpace + " numrecs=" + numrecs + " has extra bytes = " + extra);
      }

      // set it in the unlimited dimension, all of the record variables
      if (udim != null) {
        udim.setLength(this.numrecs);
        for (Variable uvar : uvars) {
          uvar.resetShape();
          uvar.invalidateCache();
        }
      }
    } */

    // check for truncated files
    // theres a "wart" that allows a file to be up to 3 bytes smaller than you expect.
    long calcSize = dataStart + nonRecordDataSize + recsize * numrecs;
    if (calcSize > actualSize + 3) {
      if (disallowFileTruncation)
        throw new IOException("File is truncated calculated size= " + calcSize + " actual = " + actualSize);
      else {
        // log.info("File is truncated calculated size= "+calcSize+" actual = "+actualSize);
        raf.setExtendMode();
      }
    }

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
    for (Variable v : ncfile.getVariables()) {
      Vinfo vinfo = (Vinfo) v.getSPobject();
      out.format("  %20s %8d %8d  %s %n", v.getShortName(), vinfo.begin, vinfo.vsize, vinfo.isRecord);
    }
  }

  /* LOOK
  synchronized boolean removeRecordStructure() {
    boolean found = false;
    for (Variable v : uvars) {
      if (v.getFullName().equals("record")) {
        uvars.remove(v);
        ncfile.getRootGroup().getVariables().remove(v);
        found = true;
        break;
      }
    }

    ncfile.finish();
    return found;
  }

  synchronized boolean makeRecordStructure() {
    // create record structure
    if (!uvars.isEmpty()) {
      Structure.Builder builder = Structure.builder().setName("record").setNcfile(ncfile).setParent(ncfile.getRootGroup());
      builder.setDimensions(udim.getShortName());
      for (Variable v : uvars) {
        Variable memberV;
        try {
          memberV = v.slice(0, 0); // set unlimited dimension to 0
        } catch (InvalidRangeException e) {
          log.warn("N3header.makeRecordStructure cant slice variable " + v + " " + e.getMessage());
          return false;
        }
        memberV.setParentStructure(recordStructure);
        builder.addMemberVariable(memberV);
      }

      uvars.add(recordStructure);
      ncfile.getRootGroup().addVariable(recordStructure);
      ncfile.finish();
      return true;
    }

    return false;
  } */

  private int readAtts(AttributeContainer atts, Formatter fout) throws IOException {
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
        String val = readString();
        if (val == null)
          val = "";
        if (fout != null)
          fout.format(" end read String val pos= %d%n", raf.getFilePointer());
        att = Attribute.builder(name).setStringValue(val).build();

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

  private int readAttributeValue(DataType type, IndexIterator ii) throws IOException {
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
  private String readString() throws IOException {
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

    return new String(b, 0, count, StandardCharsets.UTF_8); // all strings are considered to be UTF-8 unicode.
  }

  // skip to a 4 byte boundary in the file
  private void skip(int nbytes) throws IOException {
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
}
