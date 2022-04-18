/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.iosp.netcdf3;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Formatter;
import java.util.List;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.unidata.io.RandomAccessFile;

/** Class to write a netcdf3 header. */
class N3headerWriter extends N3headerNew {
  private static final long MAX_UNSIGNED_INT = 0x00000000ffffffffL;

  private NetcdfFile ncfile;
  private ImmutableList<Variable> uvars; // vars that have the unlimited dimension
  private long globalAttsPos; // global attributes start here - used for update

  /**
   * Constructor.
   * 
   * @param n3iospNew the iosp
   * @param raf write to this file
   * @param ncfile the header of this NetcdfFile
   */
  N3headerWriter(N3iospNew n3iospNew, RandomAccessFile raf, NetcdfFile ncfile) {
    super(n3iospNew);
    this.raf = raf;
    this.ncfile = ncfile;
  }

  /**
   * Write the header out, based on ncfile structures.
   *
   * @param extra if > 0, pad header with extra bytes
   * @param largeFile if large file format
   * @param fout debugging output sent to here
   * @throws IOException on write error
   */
  void create(int extra, boolean largeFile, Formatter fout) throws IOException {
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
    if (want > dataStart)
      return false;

    writeHeader(0, largeFile, true, fout);
    return true;
  }

  void writeHeader(int extra, boolean largeFile, boolean keepDataStart, Formatter fout) throws IOException {
    this.useLongOffset = largeFile;
    this.nonRecordDataSize = 0; // length of non-record data
    recsize = 0; // length of single record
    recStart = Long.MAX_VALUE; // where the record data starts

    // magic number
    raf.seek(0);
    raf.write(largeFile ? N3headerNew.MAGIC_LONG : N3headerNew.MAGIC);

    // numrecs
    raf.writeInt(0);

    // dims
    List<Dimension> dims = ncfile.getRootGroup().getDimensions();
    int numdims = dims.size();
    if (numdims == 0) {
      raf.writeInt(0);
      raf.writeInt(0);
    } else {
      raf.writeInt(N3headerNew.MAGIC_DIM);
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
    globalAttsPos = raf.getFilePointer(); // position where global attributes start
    writeAtts(ncfile.getRootGroup().attributes(), fout);

    // variables
    List<Variable> vars = ncfile.getVariables();

    // Track record variables.
    ImmutableList.Builder<Variable> uvarb = ImmutableList.builder();
    for (Variable curVar : vars) {
      if (curVar.isUnlimited()) {
        uvarb.add(curVar);
      }
    }
    uvars = uvarb.build();
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
      N3headerNew.Vinfo vinfo = (N3headerNew.Vinfo) var.getSPobject();
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
      N3headerNew.Vinfo vinfo = (N3headerNew.Vinfo) var.getSPobject();
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
  int sizeHeader(boolean largeFile) {
    int size = 4; // magic number
    size += 4; // numrecs

    // dims
    size += 8; // magic, ndims
    for (Dimension dim : ncfile.getRootGroup().getDimensions()) {
      size += sizeString(dim.getShortName()) + 4; // name, len
    }

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
      size += sizeAtts(var.attributes());

      size += 8; // data type, variable size
      size += (largeFile) ? 8 : 4;
    }

    return size;
  }

  private void writeAtts(Iterable<Attribute> atts, Formatter fout) throws IOException {

    int n = Iterables.size(atts);
    if (n == 0) {
      raf.writeInt(0);
      raf.writeInt(0);
    } else {
      raf.writeInt(MAGIC_ATT);
      raf.writeInt(n);
    }

    int count = 0;
    for (Attribute att : atts) {
      if (fout != null)
        fout.format("***att %d pos= %d%n", count, raf.getFilePointer());

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

  private int sizeAtts(Iterable<Attribute> atts) {
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
      writeAtts(var.attributes(), fout);

      // data type, variable size, beginning file position
      DataType dtype = var.getDataType();
      int type = getType(dtype);
      raf.writeInt(type);

      int vsizeWrite = (vsize < MAX_UNSIGNED_INT) ? (int) vsize : -1;
      raf.writeInt(vsizeWrite);
      long pos = raf.getFilePointer();
      if (largeFile)
        raf.writeLong(0); // come back to this later
      else
        raf.writeInt(0); // come back to this later

      // From nc3 file format specification
      // (https://www.unidata.ucar.edu/software/netcdf/docs/netcdf.html#NetCDF-Classic-Format):
      // Note on padding: In the special case of only a single record variable of character,
      // byte, or short type, no padding is used between data values.
      // 2/15/2011: we will continue to write the (incorrect) padded vsize into the header, but we will use the unpadded
      // size to read/write
      if (uvars.size() == 1 && uvars.get(0) == var) {
        if ((dtype == DataType.CHAR) || (dtype == DataType.BYTE) || (dtype == DataType.SHORT)) {
          vsize = unpaddedVsize;
        }
      }
      var.setSPobject(new N3headerNew.Vinfo(var.getShortName(), vsize, pos, var.isUnlimited(), varAttsPos));
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
    List<Dimension> dims = ncfile.getRootGroup().getDimensions();
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

  void initFromExisting(N3iospNew existingIosp) {
    N3headerNew existingHeader = existingIosp.header;
    this.dataStart = existingHeader.dataStart;
    this.nonRecordDataSize = existingHeader.nonRecordDataSize;
    this.numrecs = existingHeader.numrecs;
    this.recsize = existingHeader.recsize;
    this.recStart = existingHeader.recStart;
    this.useLongOffset = existingHeader.useLongOffset;
    this.udim = existingHeader.udim;
  }

  void setNumrecs(int n) {
    this.numrecs = n;
  }

  // TODO udim.setLength : need UnlimitedDimension extends Dimension?
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
  // tricky - perhaps deprecate ?

  void updateAttribute(ucar.nc2.Variable v2, Attribute att) throws IOException {
    long pos;
    if (v2 == null)
      pos = findAtt(globalAttsPos, att.getShortName());
    else {
      N3headerNew.Vinfo vinfo = (N3headerNew.Vinfo) v2.getSPobject();
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
  }

}
