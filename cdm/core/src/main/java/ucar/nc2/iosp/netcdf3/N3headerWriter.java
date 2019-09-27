package ucar.nc2.iosp.netcdf3;

import static ucar.nc2.iosp.netcdf3.N3header.MAGIC_ATT;
import static ucar.nc2.iosp.netcdf3.N3header.MAGIC_VAR;
import static ucar.nc2.iosp.netcdf3.N3header.MAX_UNSIGNED_INT;
import static ucar.nc2.iosp.netcdf3.N3header.getType;
import static ucar.nc2.iosp.netcdf3.N3header.padding;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.iosp.netcdf3.N3header.Vinfo;

/** The part of N3header needed for writing */
public class N3headerWriter {

  private N3headerNew n3header;
  private ucar.unidata.io.RandomAccessFile raf;
  private ucar.nc2.NetcdfFile ncfile;

  private Dimension udim; // the unlimited dimension
  List<Variable> uvars = new ArrayList<>(); // vars that have the unlimited dimension

  int numrecs; // number of records written
  long recsize; // size of each record (padded)
  long recStart = Integer.MAX_VALUE; // where the record data starts

  private boolean useLongOffset;
  private long nonRecordDataSize; // size of non-record variables
  private long dataStart = Long.MAX_VALUE; // where the data starts

  private long globalAttsPos; // global attributes start here - used for update

  /**
   * Write the header out, based on ncfile structures.
   *
   * @param n3header header the header of this NetcdfFile
   * @param extra if > 0, pad header with extra bytes
   * @param largeFile if large file format
   * @param fout debugging output sent to here
   * @throws IOException on write error
   */
  void create(N3headerNew n3header, int extra, boolean largeFile, Formatter fout) throws IOException {
    this.n3header = n3header;
    this.raf = n3header.raf;
    this.ncfile = n3header.ncfile;

    writeHeader(extra, largeFile, false, fout);
  }

  /**
   * Sneaky way to make the header bigger, if there is room for it
   *
   * @param largeFile is large file format
   * @param fout put debug messages here, mnay be null
   * @return true if it worked
   */
  boolean rewriteHeader(boolean largeFile, Formatter fout) throws IOException {
    int want = sizeHeader(largeFile);
    if (want > n3header.dataStart)
      return false;

    writeHeader(0, largeFile, true, fout);
    return true;
  }

  private void writeHeader(int extra, boolean largeFile, boolean keepDataStart, Formatter fout) throws IOException {
    this.useLongOffset = largeFile;
    nonRecordDataSize = 0; // length of non-record data
    recsize = 0; // length of single record
    recStart = Long.MAX_VALUE; // where the record data starts

    // magic number
    raf.seek(0);
    raf.write(largeFile ? N3header.MAGIC_LONG : N3header.MAGIC);

    // numrecs
    raf.writeInt(0);

    // dims
    List<Dimension> dims = ncfile.getDimensions();
    int numdims = dims.size();
    if (numdims == 0) {
      raf.writeInt(0);
      raf.writeInt(0);
    } else {
      raf.writeInt(N3header.MAGIC_DIM);
      raf.writeInt(numdims);
    }
    for (int i = 0; i < numdims; i++) {
      Dimension dim = dims.get(i);
      if (fout != null)
        fout.format("  dim %d pos %d%n", i, raf.getFilePointer());
      writeString(dim.getShortName());
      raf.writeInt(dim.isUnlimited() ? 0 : dim.getLength());
      if (dim.isUnlimited())
        udim = dim;
    }

    // global attributes
    globalAttsPos = raf.getFilePointer();
    writeAtts(ncfile.getGlobalAttributes(), fout);

    // variables
    List<Variable> vars = ncfile.getVariables();

    // Track record variables.
    for (Variable curVar : vars) {
      if (curVar.isUnlimited()) {
        uvars.add(curVar);
      }
    }
    writeVars(vars, largeFile, fout);

    // now calculate where things go
    if (!keepDataStart) {
      dataStart = raf.getFilePointer();
      if (extra > 0)
        dataStart += extra;
    }
    long pos = dataStart;

    // non-record variable starting positions
    for (Variable var : vars) {
      Vinfo vinfo = (Vinfo) var.getSPobject();
      if (!vinfo.isRecord) {
        raf.seek(vinfo.begin);

        if (largeFile)
          raf.writeLong(pos);
        else {
          if (pos > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Variable starting pos=" + pos + " may not exceed " + Integer.MAX_VALUE);
          raf.writeInt((int) pos);
        }

        vinfo.begin = pos;
        if (fout != null)
          fout.format("  %s begin at = %d end= %d%n", var.getFullName(), vinfo.begin, (vinfo.begin + vinfo.vsize));
        pos += vinfo.vsize;

        // track how big each record is
        nonRecordDataSize = Math.max(nonRecordDataSize, vinfo.begin + vinfo.vsize);
      }
    }

    recStart = pos; // record variables start here

    // record variable starting positions
    for (Variable var : vars) {
      Vinfo vinfo = (Vinfo) var.getSPobject();
      if (vinfo.isRecord) {
        raf.seek(vinfo.begin);

        if (largeFile)
          raf.writeLong(pos);
        else
          raf.writeInt((int) pos);

        vinfo.begin = pos;
        if (fout != null)
          fout.format(" %s record begin at = %d%n", var.getFullName(), dataStart);
        pos += vinfo.vsize;

        // track how big each record is
        recsize += vinfo.vsize;
        recStart = Math.min(recStart, vinfo.begin);
      }
    }

    if (nonRecordDataSize > 0) // if there are non-record variables
      nonRecordDataSize -= dataStart;
    if (uvars.isEmpty()) // if there are no record variables
      recStart = 0;
  }

  // calculate the size writing a header would take
  private int sizeHeader(boolean largeFile) {
    int size = 4; // magic number
    size += 4; // numrecs

    // dims
    size += 8; // magic, ndims
    for (Dimension dim : ncfile.getDimensions())
      size += sizeString(dim.getShortName()) + 4; // name, len

    // global attributes
    size += sizeAtts(ncfile.getGlobalAttributes());

    // variables
    size += 8; // magic, nvars
    for (Variable var : ncfile.getVariables()) {
      size += sizeString(var.getShortName());

      // dimensions
      size += 4; // ndims
      size += 4 * var.getDimensions().size(); // dim id

      // variable attributes
      size += sizeAtts(var.getAttributes());

      size += 8; // data type, variable size
      size += (largeFile) ? 8 : 4;
    }

    return size;
  }

  private void writeAtts(List<Attribute> atts, Formatter fout) throws IOException {

    int n = atts.size();
    if (n == 0) {
      raf.writeInt(0);
      raf.writeInt(0);
    } else {
      raf.writeInt(MAGIC_ATT);
      raf.writeInt(n);
    }

    for (int i = 0; i < n; i++) {
      if (fout != null)
        fout.format("***att %d pos= %d%n", i, raf.getFilePointer());
      Attribute att = atts.get(i);

      writeString(att.getShortName());
      int type = getType(att.getDataType());
      raf.writeInt(type);

      if (type == 2) {
        writeStringValues(att);
      } else {
        int nelems = att.getLength();
        raf.writeInt(nelems);
        int nbytes = 0;
        for (int j = 0; j < nelems; j++)
          nbytes += writeAttributeValue(att.getNumericValue(j));
        pad(nbytes, (byte) 0);
        if (fout != null)
          fout.format(" end write val pos= %d%n", raf.getFilePointer());
      }
      if (fout != null)
        fout.format("  %s%n", att);
    }
  }

  private int sizeAtts(List<Attribute> atts) {
    int size = 8; // magic, natts

    for (Attribute att : atts) {
      size += sizeString(att.getShortName());
      size += 4; // type

      int type = getType(att.getDataType());
      if (type == 2) {
        size += sizeStringValues(att);
      } else {
        size += 4; // nelems
        int nelems = att.getLength();
        int nbytes = 0;
        for (int j = 0; j < nelems; j++)
          nbytes += sizeAttributeValue(att.getNumericValue(j));
        size += nbytes;
        size += padding(nbytes);
      }
    }
    return size;
  }

  private void writeStringValues(Attribute att) throws IOException {
    int n = att.getLength();
    if (n == 1)
      writeString(att.getStringValue());
    else {
      StringBuilder values = new StringBuilder();
      for (int i = 0; i < n; i++)
        values.append(att.getStringValue(i));
      writeString(values.toString());
    }
  }

  private int sizeStringValues(Attribute att) {
    int size = 0;
    int n = att.getLength();
    if (n == 1)
      size += sizeString(att.getStringValue());
    else {
      StringBuilder values = new StringBuilder();
      for (int i = 0; i < n; i++)
        values.append(att.getStringValue(i));
      size += sizeString(values.toString());
    }

    return size;
  }

  private int writeAttributeValue(Number numValue) throws IOException {
    if (numValue instanceof Byte) {
      raf.write(numValue.byteValue());
      return 1;

    } else if (numValue instanceof Short) {
      raf.writeShort(numValue.shortValue());
      return 2;

    } else if (numValue instanceof Integer) {
      raf.writeInt(numValue.intValue());
      return 4;

    } else if (numValue instanceof Float) {
      raf.writeFloat(numValue.floatValue());
      return 4;

    } else if (numValue instanceof Double) {
      raf.writeDouble(numValue.doubleValue());
      return 8;
    }

    throw new IllegalStateException("unknown attribute type == " + numValue.getClass().getName());
  }

  private int sizeAttributeValue(Number numValue) {

    if (numValue instanceof Byte) {
      return 1;

    } else if (numValue instanceof Short) {
      return 2;

    } else if (numValue instanceof Integer) {
      return 4;

    } else if (numValue instanceof Float) {
      return 4;

    } else if (numValue instanceof Double) {
      return 8;
    }

    throw new IllegalStateException("unknown attribute type == " + numValue.getClass().getName());
  }

  private void writeVars(List<Variable> vars, boolean largeFile, Formatter fout) throws IOException {
    int n = vars.size();
    if (n == 0) {
      raf.writeInt(0);
      raf.writeInt(0);
    } else {
      raf.writeInt(MAGIC_VAR);
      raf.writeInt(n);
    }

    for (Variable var : vars) {
      writeString(var.getShortName());

      // dimensions
      long vsize = var.getDataType().getSize(); // works for all netcdf-3 data types
      List<Dimension> dims = var.getDimensions();
      raf.writeInt(dims.size());
      for (Dimension dim : dims) {
        int dimIndex = findDimensionIndex(ncfile, dim);
        raf.writeInt(dimIndex);

        if (!dim.isUnlimited())
          vsize *= dim.getLength();
      }
      long unpaddedVsize = vsize;
      vsize += padding(vsize);

      // variable attributes
      long varAttsPos = raf.getFilePointer();
      writeAtts(var.getAttributes(), fout);

      // data type, variable size, beginning file position
      DataType dtype = var.getDataType();
      int type = getType(dtype);
      raf.writeInt(type);

      int vsizeWrite = (vsize < N3header.MAX_UNSIGNED_INT) ? (int) vsize : -1;
      raf.writeInt(vsizeWrite);
      long pos = raf.getFilePointer();
      if (largeFile)
        raf.writeLong(0); // come back to this later
      else
        raf.writeInt(0); // come back to this later

      // From nc3 file format specification
      // (http://www.unidata.ucar.edu/software/netcdf/docs/netcdf.html#NetCDF-Classic-Format):
      // Note on padding: In the special case of only a single record variable of character,
      // byte, or short type, no padding is used between data values.
      // 2/15/2011: we will continue to write the (incorrect) padded vsize into the header, but we will use the unpadded
      // size to read/write
      if (uvars.size() == 1 && uvars.get(0) == var)
        if ((dtype == DataType.CHAR) || (dtype == DataType.BYTE) || (dtype == DataType.SHORT))
          vsize = unpaddedVsize;

      var.setSPobject(new Vinfo(vsize, pos, var.isUnlimited(), varAttsPos));
    }
  }

  // write a string then pad to 4 byte boundary
  private void writeString(String s) throws IOException {
    byte[] b = s.getBytes(StandardCharsets.UTF_8); // all strings are encoded in UTF-8 Unicode.
    raf.writeInt(b.length);
    raf.write(b);
    pad(b.length, (byte) 0);
  }

  private int sizeString(String s) {
    int size = s.length() + 4;
    return size + padding(s.length());
  }

  private int findDimensionIndex(NetcdfFile ncfile, Dimension wantDim) {
    List<Dimension> dims = ncfile.getDimensions();
    for (int i = 0; i < dims.size(); i++) {
      Dimension dim = dims.get(i);
      if (dim.equals(wantDim))
        return i;
    }
    throw new IllegalStateException("unknown Dimension == " + wantDim);
  }

  // pad to a 4 byte boundary
  private void pad(int nbytes, byte fill) throws IOException {
    int pad = padding(nbytes);
    for (int i = 0; i < pad; i++)
      raf.write(fill);
  }

  void writeNumrecs() throws IOException {
    // set number of records in the header
    raf.seek(4);
    raf.writeInt(numrecs);
  }

  void setNumrecs(int n) {
    this.numrecs = n;
  }

  synchronized boolean synchNumrecs() throws IOException {
    // check number of records in the header
    // gotta bypass the RAF buffer
    int n = raf.readIntUnbuffered(4);
    if (n == this.numrecs)
      return false;
    if (n < 0) // streaming
      return false;

    // update everything
    this.numrecs = n;

    // set it in the unlimited dimension
    udim.setLength(this.numrecs);

    // set it in all of the record variables
    for (Variable uvar : uvars) {
      uvar.resetShape();
      uvar.invalidateCache();
    }

    return true;
  }

  ///////////////////////////////////////////////////////////////////////////////
  /* tricky - perhaps deprecate ?

  void updateAttribute(ucar.nc2.Variable v2, Attribute att) throws IOException {
    long pos;
    if (v2 == null)
      pos = findAtt(globalAttsPos, att.getShortName());
    else {
      N3header.Vinfo vinfo = (N3header.Vinfo) v2.getSPobject();
      pos = findAtt(vinfo.attsPos, att.getShortName());
    }

    raf.seek(pos);
    int type = raf.readInt();
    DataType have = getDataType(type);
    DataType want = att.getDataType();
    if (want == DataType.STRING)
      want = DataType.CHAR;
    if (want != have) {
      throw new IllegalArgumentException(
          "Update Attribute must have same type or original = " + have + " att = " + att);
    }

    if (type == 2) { // String
      String s = att.getStringValue();
      int org = raf.readInt();
      int size = org + padding(org); // ok to use the padding
      int max = Math.min(size, s.length()); // cant make any longer than size
      if (max > org) { // adjust if its using the padding, but not if its shorter
        raf.seek(pos + 4);
        raf.writeInt(max);
      }

      byte[] b = new byte[size];
      for (int i = 0; i < max; i++)
        b[i] = (byte) s.charAt(i);
      raf.write(b);

    } else {
      int nelems = raf.readInt();
      int max = Math.min(nelems, att.getLength()); // cant make any longer
      for (int j = 0; j < max; j++)
        writeAttributeValue(att.getNumericValue(j));
    }
  }

  private long findAtt(long start_pos, String want) throws IOException {
    raf.seek(start_pos + 4);

    int natts = raf.readInt();
    for (int i = 0; i < natts; i++) {
      String name = readString();
      if (name.equals(want))
        return raf.getFilePointer();

      int type = raf.readInt();

      if (type == 2) {
        readString();
      } else {
        int nelems = raf.readInt();
        DataType dtype = getDataType(type);
        int[] shape = {nelems};
        Array arr = Array.factory(dtype, shape);
        IndexIterator ii = arr.getIndexIterator();
        int nbytes = 0;
        for (int j = 0; j < nelems; j++)
          nbytes += readAttributeValue(dtype, ii);
        skip(nbytes);
      }
    }

    throw new IllegalArgumentException("no such attribute " + want);
  } */



}
